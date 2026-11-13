package tech.ada.pix.emissor;

import tech.ada.pix.contracts.ComprovanteAceito;
import tech.ada.pix.contracts.ComprovanteRequest;
import tech.ada.pix.emissor.dominio.ChavePix;
import tech.ada.pix.emissor.dominio.Comprovante;
import tech.ada.pix.emissor.dominio.ContaBancaria;
import tech.ada.pix.emissor.dominio.Dinheiro;
import tech.ada.pix.emissor.gravacao.GravacaoAdapter;
import tech.ada.pix.emissor.gravacao.GravacaoFalhouException;
import tech.ada.pix.emissor.persistencia.ComprovanteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Serviço de APLICAÇÃO do bounded context EMISSÃO.
 *
 * Aula 1: transforma o envelope (ComprovanteRequest) no modelo de domínio.
 * Aula 2: é o ORQUESTRADOR da SAGA "emitir → gravar".
 *
 *   Passo 1 (local)  : emitir o comprovante e guardar como ACEITO na base do emissor
 *   Passo 2 (remoto) : pedir ao gravador para gravar
 *   Deu certo        : marcar GRAVADO e responder 202
 *   Deu errado       : COMPENSAR o passo 1 (invalidar) e responder 503
 *
 * Aula 3: no fim da SAGA, avisava que o comprovante foi gravado (um evento do Spring), e as
 * reações (SMS, antifraude, BI) moravam aqui dentro: era COREOGRAFIA, mas sem sair do processo.
 * Aula 6: quem avisa agora é o GRAVADOR, porque é ele que sabe que gravou. Ele publica o fato
 * no Kafka, e as reações moram num serviço próprio, o comprovante-reacoes.
 *
 * ⚠️ Este método NÃO tem @Transactional, de propósito. Cada salvar(...) é uma transação
 * local com o seu próprio COMMIT. Se o método inteiro fosse uma transação só:
 *   - o banco ficaria "preso" esperando a resposta do gravador (chamada pela rede);
 *   - quando a gravação falha, a exceção faria ROLLBACK de tudo, inclusive do
 *     INVALIDADO: a compensação sumiria junto e não sobraria rastro nenhum.
 */
@Service
public class EmissaoService {

    private static final Logger log = LoggerFactory.getLogger(EmissaoService.class);

    private final ComprovanteRepository comprovantes;
    private final GravacaoAdapter gravacao;

    public EmissaoService(ComprovanteRepository comprovantes, GravacaoAdapter gravacao) {
        this.comprovantes = comprovantes;
        this.gravacao = gravacao;
    }

    public ComprovanteAceito aceitar(ComprovanteRequest r) {
        validarCamposRestantes(r);

        // Passo 1 — transação LOCAL, na base do emissor
        Comprovante comprovante = Comprovante.emitir(
                r.nome(),
                new ContaBancaria(r.numeroAgencia(), r.numeroConta(), r.digitoVerificadorConta()),
                new Dinheiro(r.valorTransacao()),
                new ChavePix(r.tipoChavePixDestino(), r.chavePixDestino()));
        comprovantes.salvar(comprovante);                       // ACEITO

        // Passo 2 — passo REMOTO, em outro serviço e outro banco
        try {
            gravacao.gravar(comprovante, r);
            comprovante.confirmarGravacao();                    // GRAVADO
            comprovantes.salvar(comprovante);
        } catch (GravacaoFalhouException falha) {
            // Compensação do passo 1: operação de negócio, não DELETE
            comprovante.invalidar("gravação falhou: " + falha.getCause().getMessage());
            comprovantes.salvar(comprovante);                   // INVALIDADO
            log.warn("SAGA compensada: comprovante {} INVALIDADO ({})",
                    comprovante.id(), comprovante.motivoInvalidacao());
            throw falha;                                        // → 503, nunca 202
        }

        log.info("SAGA concluída: comprovante {} GRAVADO", comprovante.id());
        return new ComprovanteAceito(comprovante.id(), LocalDateTime.now());
    }

    /** Campos que ainda não viraram Value Object (próximo candidato: Documento = tipo + número). */
    private void validarCamposRestantes(ComprovanteRequest r) {
        if (r.tipoDocumento() == null) throw new ComprovanteInvalidoException("tipo_documento é obrigatório");
        exigir(r.numeroDocumento(), "numero_documento");
        exigir(r.nomeClienteDestino(), "nome_cliente_destino");
        if (r.dataHoraTransacao() == null) throw new ComprovanteInvalidoException("data_hora_transacao é obrigatória");
    }

    private void exigir(String valor, String campo) {
        if (valor == null || valor.isBlank())
            throw new ComprovanteInvalidoException(campo + " é obrigatório");
    }
}
