package tech.ada.pix.gravador;

import tech.ada.pix.contracts.ComprovanteView;
import tech.ada.pix.contracts.GravarComprovanteCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Núcleo do bounded context GRAVAÇÃO.
 *
 * Aula 2: IDEMPOTENTE. O id do comprovante é a chave: se o mesmo comando chegar
 * duas vezes (retry, reenvio da fila...), o segundo não grava de novo.
 *
 * Aula 4: também LÊ. É a fonte da verdade da consulta.
 */
@Service
public class GravadorService {

    private final ComprovanteRepository repository;

    public GravadorService(ComprovanteRepository repository) {
        this.repository = repository;
    }

    /** @return true se gravou agora; false se já estava gravado (nada a fazer). */
    @Transactional
    public boolean gravar(GravarComprovanteCommand command) {
        if (repository.existsById(command.identificadorComprovante())) {
            return false;
        }
        repository.save(ComprovanteEntity.de(command.identificadorComprovante(), command.dados()));
        return true;
    }

    /** Vazio se esse comprovante não está gravado. */
    @Transactional(readOnly = true)
    public Optional<ComprovanteView> buscar(UUID id) {
        return repository.findById(id).map(ComprovanteEntity::paraView);
    }
}
