package tech.ada.pix.gravador;

import tech.ada.pix.contracts.ComprovanteRequest;
import tech.ada.pix.contracts.ComprovanteView;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persistência do comprovante no bounded context GRAVAÇÃO.
 * Base SEGREGADA deste serviço (não compartilhar tabela com outros serviços — Aula 1).
 */
@Entity
public class ComprovanteEntity {

    @Id
    private UUID id; // = identificadorComprovante (chave de idempotência)

    private String nome;
    private String numeroDocumento;
    private BigDecimal valorTransacao;
    private String chavePixDestino;
    private LocalDateTime dataHoraTransacao;
    private LocalDateTime dataHoraGravacao;

    protected ComprovanteEntity() {
        // exigido pelo JPA
    }

    private ComprovanteEntity(UUID id, ComprovanteRequest r) {
        this.id = id;
        this.nome = r.nome();
        this.numeroDocumento = r.numeroDocumento();
        this.valorTransacao = r.valorTransacao();
        this.chavePixDestino = r.chavePixDestino();
        this.dataHoraTransacao = r.dataHoraTransacao();
        this.dataHoraGravacao = LocalDateTime.now();
    }

    /** Constrói a entidade a partir do comando de gravação. */
    public static ComprovanteEntity de(UUID id, ComprovanteRequest dados) {
        return new ComprovanteEntity(id, dados);
    }

    /** O comprovante do jeito que a consulta mostra (Aula 4). */
    public ComprovanteView paraView() {
        return new ComprovanteView(id, nome, numeroDocumento, valorTransacao, chavePixDestino,
                dataHoraTransacao, dataHoraGravacao);
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public BigDecimal getValorTransacao() { return valorTransacao; }
    public String getChavePixDestino() { return chavePixDestino; }
    public LocalDateTime getDataHoraTransacao() { return dataHoraTransacao; }
    public LocalDateTime getDataHoraGravacao() { return dataHoraGravacao; }
}
