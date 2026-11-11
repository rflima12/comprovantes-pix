# Arquitetura — Comprovantes PIX (projeto guiado)

Documento vivo: cresce com as aulas. Estado-alvo ao fim do módulo.

## Contextos e fluxo

```
                 POST /comprovantes
                        │  (202 + id)
                        ▼
              ┌───────────────────┐
              │ comprovante-emissor│  bounded context: EMISSÃO
              └─────────┬─────────┘
                        │ publica GravarComprovanteCommand
                        ▼  (FILA — RabbitMQ)             Aula 5
              ┌───────────────────┐
              │ comprovante-gravador│ bounded context: GRAVAÇÃO (base própria)
              └─────────┬─────────┘  idempotente (Aula 2), DLQ (Aula 5)
                        │ publica ComprovanteGravadoEvent
                        ▼  (TÓPICO — Kafka)              Aula 7
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼   consumer groups (Aula 7)
   notificação      antifraude          BI
   (@RetryableTopic, Aula 8)

              GET /comprovantes/{id}
                        ▼
              ┌───────────────────┐
              │ comprovante-consulta│ bounded context: CONSULTA (sem base própria)
              └───────────────────┘  cache (Redis) → GET no gravador → 404  (Aula 4)
```

## Mapa de incrementos por aula
A numeração é a do material do aluno. A Aula 5 juntou as Aulas 5 e 6 do material (fila e DLQ).

| Aula | Padrão | Onde aparece |
|---|---|---|
| 1 | DDD / bases segregadas | 3 serviços, H2 por serviço, ADR-001 |
| 2 | SAGA / idempotência | orquestração emissão→gravação |
| 3 | Comunicação (o panorama) | evento `ComprovanteGravado` no emissor: SMS, antifraude e BI reagem |
| 4 | Cache (Redis) | comprovante-consulta: cache-aside na frente do gravador |
| 5 | Fila (RabbitMQ) + DLQ | emissor publica em `gravacao.ex`, gravador consome `gravacao.q`; retry com backoff e `gravacao.dlq` |
| 7 | Tópicos (Kafka) | evento "comprovante-gravado" |
| 8 | Resiliência / contract test | `@RetryableTopic` + circuit breaker; pacts em shared-contracts |

## Contratos entre serviços
Em `shared-contracts`: `ComprovanteRequest`, `ComprovanteAceito`, `GravarComprovanteCommand` (fila), `FilaDeGravacao` (os nomes da exchange, da fila e da DLQ, Aula 5), `ComprovanteView` (o que o gravador devolve para a consulta, Aula 4), `ComprovanteGravadoEvent` (tópico). Pact files (Aula 8) em `shared-contracts/src/pacts`.
