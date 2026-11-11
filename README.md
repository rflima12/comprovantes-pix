# Comprovantes PIX — Aulas 1 (DDD), 2 (SAGA), 3 (comunicação), 4 (cache) e 5 (fila)

Exemplo do projeto guiado. Uma API que **emite** e **consulta** comprovantes de PIX que já aconteceram.

**O exercício da Aula 5 está em [`EXERCICIO-AULA-05.md`](EXERCICIO-AULA-05.md).**

## Comece aqui

Você só precisa de duas coisas instaladas:

- o **[Git](https://git-scm.com/downloads)**. No Windows, ele traz o **Git Bash**: use ele como terminal;
- o **[Docker Desktop](https://www.docker.com/products/docker-desktop/)**, **aberto**.

Não precisa instalar Java, Maven, Redis nem RabbitMQ: eles rodam dentro do Docker.

**1. Baixe o projeto.** O endereço está no botão verde **Code**, aqui no GitHub:

```bash
git clone <o endereço do botão Code>
cd <a pasta que o git criou>
```

**2. Suba o RabbitMQ, o emissor, a consulta e o Redis** (em segundo plano):

```bash
docker compose up -d --build rabbitmq emissor consulta redis
```

**3. Suba o gravador.** Este terminal fica mostrando o log dele:

```bash
docker compose up --build gravador
```

Está pronto quando aparecer `Started GravadorApplication`. **A primeira vez demora uns minutos**: o Docker baixa o Java, o Maven, o Redis, o RabbitMQ e as bibliotecas. Se puder, faça isso **antes da aula**, numa internet boa. Depois, fica rápido.

**4. Abra o painel do RabbitMQ**, no navegador: `http://localhost:15672`, usuário `guest`, senha `guest`. Na aba **Queues and Streams**, aparecem a fila de gravação (`gravacao.q`) e a DLQ (`gravacao.dlq`), vazias.

**5. Em outro terminal**, na mesma pasta, mande um PIX. O PIX está no arquivo [`pix.json`](pix.json):

```bash
curl -i -X POST localhost:8081/comprovantes -H "Content-Type: application/json" -d "@pix.json"
```

A primeira linha da resposta deve ser `HTTP/1.1 202`. A última traz o `identificador_comprovante`: é o id do comprovante. No log do gravador, aparece `Recebi por HTTP o pedido de gravação`: o emissor ainda chama o gravador por HTTP. A fila entra no exercício.

**6. Consulte o comprovante**, trocando `<o id>` pelo id que veio na resposta:

```bash
curl -i localhost:8083/comprovantes/<o id>
```

A resposta é `HTTP/1.1 200`, com o comprovante, e leva 1 segundo: o gravador demora **de propósito** (a demo da Aula 4).

Os logs dos outros serviços aparecem com `docker compose logs emissor` (a SAGA e as reações das aulas anteriores) e `docker compose logs consulta`.

> **No PowerShell**, troque `curl` por `curl.exe`. O resto do comando é igual. Para o exercício, use o Git Bash.

### Os comandos do dia a dia

| Para... | Comando |
|---|---|
| desligar o gravador | `docker compose stop gravador` |
| ligar o gravador de novo, mostrando o log | `docker compose up gravador` |
| reiniciar o gravador depois de mudar o código | `docker compose stop gravador`, e `docker compose up --build gravador` |
| reiniciar o emissor depois de mudar o código | `docker compose up -d --build emissor` |
| reiniciar a consulta depois de mudar o código | `docker compose up -d --build consulta` |
| abrir o painel do RabbitMQ | `http://localhost:15672`, usuário `guest`, senha `guest` |
| publicar a mensagem envenenada (exercício da Aula 5) | `curl -s -u guest:guest -X POST localhost:15672/api/exchanges/%2F/gravacao.ex/publish -H "Content-Type: application/json" -d "@envenenada.json"` |
| desligar o Redis | `docker compose stop redis` |
| ligar o Redis de novo | `docker compose start redis` |
| ver as chaves guardadas no Redis | `docker compose exec -T redis redis-cli KEYS '*'` |
| derrubar o emissor de repente, como uma queda | `docker compose kill emissor` |
| ver o log de um serviço | `docker compose logs emissor` (ou `gravador`, ou `consulta`) |
| ver o que está rodando | `docker compose ps` |
| rodar os testes | `docker compose run --rm testes` |
| desligar tudo | `docker compose down` |

### Os bancos

O emissor e o gravador têm cada um o seu banco (H2), e cada banco tem um console no navegador:

| | Emissor | Gravador |
|---|---|---|
| Console | `http://localhost:8081/h2-console` | `http://localhost:8082/h2-console` |
| JDBC URL | `jdbc:h2:mem:emissor` | `jdbc:h2:mem:gravador` |

Usuário `sa`, senha em branco. **Confira a JDBC URL antes de clicar em Connect**: o console às vezes lembra a do outro serviço. Os bancos ficam na memória: **reiniciar um serviço apaga o banco dele**.

A consulta não tem banco: ela lê do gravador, que é a **fonte da verdade**. O cache dela fica no Redis, que também não salva nada em disco.

### Sem Docker (com Java 21 e Maven)

Quem já tem **Java 21** e **Maven** pode rodar direto na máquina. Instale uma vez (e de novo sempre que trocar de branch). Sem isso, os serviços não acham o `shared-contracts`:

```bash
mvn install -DskipTests
```

Depois, troque os comandos do Docker por estes, cada serviço no seu terminal:

| Com Docker | Sem Docker |
|---|---|
| `docker compose up -d --build rabbitmq emissor consulta redis` | `mvn -pl comprovante-emissor spring-boot:run` e `mvn -pl comprovante-consulta spring-boot:run -Dspring-boot.run.arguments=--spring.cache.type=simple` |
| `docker compose up --build gravador` | `mvn -pl comprovante-gravador spring-boot:run` |
| `docker compose stop gravador` | `Ctrl+C` no terminal do gravador |
| `docker compose kill emissor` | `Ctrl+C` no terminal do emissor |
| `DEMO_FALHAR_ANTES_DO_ACK=true docker compose up gravador` | `mvn -pl comprovante-gravador spring-boot:run -Dspring-boot.run.arguments=--demo.falhar-antes-do-ack=true` |
| `docker compose run --rm testes` | `mvn test` |

Sem Docker, não tem Redis: o `--spring.cache.type=simple` guarda o cache na memória da própria consulta. E não tem RabbitMQ: o gravador fica tentando se conectar e, depois do passo 2 do exercício, o emissor não consegue publicar. **Os exercícios das Aulas 4 e 5 precisam do Docker**: eles olham dentro do Redis e do RabbitMQ. Quem tem Java pode rodar os serviços na máquina e só o RabbitMQ no Docker (`docker compose up -d rabbitmq`): os serviços acham ele em `localhost:5672`.

No Git Bash, às vezes o `Ctrl+C` não fecha o Java, e ele não sobe de novo (`Port 8081 was already in use`). Rode `netstat -ano | grep :8081` (troque pela porta da mensagem): o último número da linha é o PID. Depois, `taskkill //PID <o número> //F`.

## As branches

| Branch | O que tem |
|---|---|
| `main` | **O ponto de partida da aula de agora.** Hoje, é igual à `aula-05`. |
| `as-is` | O "antes" da Aula 1: um monólito ruim de propósito, para a turma criticar. |
| `v0` | Só a **estrutura**: os módulos, as classes vazias e os `// TODO`. É o ponto de partida da Aula 1. |
| `gabarito` | Fim da Aula 1: validação no emissor + o modelo de domínio com **entidade** e **Value Objects**. |
| `aula-02` | Fim da Aula 2: o emissor chama o gravador numa **SAGA** e sabe desfazer quando a gravação falha. |
| `aula-03` | Fim da Aula 3: no fim da SAGA, o emissor **avisa** com um evento, e SMS, antifraude e BI reagem cada um por conta própria. |
| `aula-04` | Aula 4: a consulta passa a responder, lendo do gravador, e já tem o Redis do lado. O **cache** entra no exercício. |
| `aula-04-gabarito` | A correção do exercício da Aula 4: o cache pronto na consulta e a folha com as respostas. |
| `aula-05` | Aula 5: o **RabbitMQ** entra no projeto, com a fila de gravação e a DLQ. O gravador já escuta a fila. Trocar o HTTP pela fila, no emissor, e ligar o retry entram no exercício. |

A correção de cada exercício entra numa branch com `-gabarito` no nome, publicada **depois** da correção em aula.

O Docker existe a partir da `aula-03`. Nas branches anteriores, rode sem Docker.

Para trocar de uma branch para a outra:

```bash
git switch aula-04
git switch aula-05
```

Para ver tudo o que muda entre duas branches:

```bash
git diff aula-04 aula-05
```

## Os módulos

Cada serviço é um **bounded context**. O emissor e o gravador têm o seu próprio banco.

| Módulo | Porta | O que faz |
|---|---|---|
| `comprovante-emissor` | 8081 | Recebe o `POST /comprovantes`, valida, chama o gravador e responde `202` (ou `503` se a gravação falhou). No exercício da Aula 5, passa a publicar o pedido de gravação na fila, em vez de chamar o gravador |
| `comprovante-gravador` | 8082 | Recebe o pedido de gravação pela fila `gravacao.q` ou pelo `POST /interno/comprovantes`, e salva no banco dele, sem gravar duas vezes o mesmo id. Responde o `GET /interno/comprovantes/{id}`, que a consulta chama |
| `comprovante-consulta` | 8083 | Responde o `GET /comprovantes/{id}`, lendo do gravador. Guarda o que leu no Redis (no exercício) |
| `redis` | — | O cache da consulta. Não tem porta aberta para fora: só a consulta fala com ele, por dentro do Docker |
| `rabbitmq` | 5672 e 15672 | O broker: guarda a fila de gravação (`gravacao.q`) e a DLQ (`gravacao.dlq`). Os serviços falam com ele na 5672; o painel fica na 15672 |
| `shared-contracts` | — | Classes que os serviços trocam entre si (os "envelopes" de dados) |

```
.
├── pom.xml                    # projeto "pai", junta os módulos
├── Dockerfile                 # como montar a imagem de cada serviço
├── docker-compose.yml         # sobe os serviços, o Redis e o RabbitMQ (e roda os testes)
├── pix.json                   # o PIX de exemplo, para o curl
├── gravar.json                # um comprovante para gravar direto no gravador (exercício da Aula 4)
├── envenenada.json            # um pedido de gravação com defeito, para a fila (exercício da Aula 5)
├── EXERCICIO-AULA-03.md       # o exercício da Aula 3
├── EXERCICIO-AULA-04.md       # o exercício da Aula 4
├── EXERCICIO-AULA-05.md       # o exercício da Aula 5
├── shared-contracts/
├── comprovante-emissor/
├── comprovante-gravador/
├── comprovante-consulta/
└── docs/
    ├── arquitetura.md         # desenho dos serviços e de como eles crescem nas aulas
    └── adr/                   # decisões registradas (ADR-001: cada serviço com seu banco)
```

**O que o emissor responde:**

| Situação | Resposta do emissor |
|---|---|
| Tudo certo | `202`, e o comprovante fica `GRAVADO` |
| Campo errado (troque `"numero_conta": "00276"` por `"002761"` no `pix.json`) | `400`, com o campo errado na `message`, e ninguém chama o gravador |
| Gravador desligado (`docker compose stop gravador`) | `503`, com o id do comprovante na `message`, e o comprovante fica `INVALIDADO` (a SAGA desfez) |

Depois do passo 2 do exercício da Aula 5, o gravador desligado não derruba mais o PIX: o emissor responde `202`, e o pedido de gravação espera na fila até o gravador voltar.

**O que a consulta responde:**

| Situação | Resposta da consulta |
|---|---|
| Comprovante gravado | `200`, com o comprovante |
| Id que o gravador não tem | `404` |

## As demos da Aula 2

**Gravador lento** ("demorar não quer dizer não"). Suba o gravador com um atraso maior que o tempo de espera do emissor (2 segundos):

```bash
DEMO_ATRASO_MS=3000 docker compose up -d gravador
```

O emissor desiste, responde `503` e marca o comprovante como `INVALIDADO`... mas o gravador **salvou**. Para ver, abra os **dois consoles do H2** e rode `SELECT * FROM COMPROVANTE_ENTITY;` nos dois: o emissor mostra o comprovante `INVALIDADO`, com o motivo, e o gravador mostra o mesmo id, **gravado**.

Para voltar ao normal: `docker compose up -d gravador`.

**Gravador idempotente.** Mande o mesmo comprovante direto para o gravador duas vezes (no Git Bash). A primeira responde `201` (gravei agora), a segunda `200` (já tinha):

```bash
curl -i -X POST localhost:8082/interno/comprovantes -H 'Content-Type: application/json' \
  -d '{"identificador_comprovante":"11111111-1111-1111-1111-111111111111","dados":{"nome":"Giovanni",
       "tipo_documento":"CPF","numero_documento":"50329291076","numero_agencia":"2022",
       "numero_conta":"00276","digito_verificador_conta":"0","valor_transacao":23.99,
       "tipo_chave_pix_destino":"CELULAR","chave_pix_destino":"11948755536",
       "nome_cliente_destino":"Fernando","identificacao_pix":"churrasco",
       "data_hora_transacao":"2022-04-10T20:03:57"}}'
```

## A demo da Aula 3: o SMS fora do ar

Suba o emissor com a "operadora de SMS" fora, mostrando o log dele:

```bash
DEMO_SMS_FORA=true docker compose up --build emissor
```

- **Como está na branch (com `@Async`):** o `POST` responde `202`. O erro do SMS aparece só no log, numa thread `task-N`. O PIX não cai junto.
- **Tire o `@Async` da `NotificacaoSms` e suba de novo:** o `POST` responde `500` ("operadora de SMS fora do ar")... mas o comprovante ficou `GRAVADO` nos dois bancos. Evento **não** quer dizer assíncrono: são duas decisões diferentes.

No PowerShell, a variável é assim: `$env:DEMO_SMS_FORA="true"; docker compose up --build emissor`. Para voltar ao normal, feche o terminal (ou `$env:DEMO_SMS_FORA="false"`) e suba de novo.

## O que mudou nesta branch (`aula-05`)

O RabbitMQ entra no projeto, com a fila de gravação pronta. O gravador **já escuta** a fila. O emissor **ainda** chama o gravador por HTTP: trocar o HTTP pela fila é o passo 2 do exercício, e muda um arquivo só, o `GravacaoAdapter`.

```
 emissor ──publica──► gravacao.ex ──binding "gravacao"──► gravacao.q ──► gravador
                     (a exchange)                            │
                                                             └─ falhou de vez ──► gravacao.dlx ──► gravacao.dlq
```

**shared-contracts**: o `FilaDeGravacao`, com os nomes da exchange, da fila, da DLQ e da etiqueta (a routing key) num lugar só. O emissor e o gravador usam os mesmos nomes.

**Gravador**:

| Onde | O que entrou |
|---|---|
| `FilasConfig` | Cria no RabbitMQ, quando o gravador sobe, a exchange `gravacao.ex`, a fila `gravacao.q` (que manda para a DLQ o que falhar de vez) e a `gravacao.dlq`, com os bindings. E o conversor de JSON, em `snake_case`, igual ao HTTP. |
| `ConsumidorDeGravacao` | O `@RabbitListener` da `gravacao.q`. Grava pelo `GravadorService`, que é idempotente desde a Aula 2. Terminou sem erro: o Spring manda o ack. Deu erro: sem ack, e o RabbitMQ entrega de novo. Escreve uma linha no log a cada entrega, dizendo se é uma reentrega. |
| `GravadorController` | Continua recebendo por HTTP, porque o emissor ainda usa. Agora escreve no log quando recebe. |
| `application.yml` | O endereço do RabbitMQ, o ack automático e o retry com backoff **desligado** (`enabled: false`: o passo 5 liga). E duas chaves só para o exercício: `demo.falhar-antes-do-ack` (passo 3) e `demo.falhas-passageiras` (desafio extra). |
| `pom.xml` | `spring-boot-starter-amqp` |

**Emissor**:

| Onde | O que entrou |
|---|---|
| `gravacao/FilasConfig` | As mesmas peças do gravador, declaradas igualzinho: se uma fosse diferente, o RabbitMQ recusaria. O emissor só cria as peças quando se conecta ao RabbitMQ pela primeira vez, na primeira mensagem que publica. |
| `application.yml` | O endereço do RabbitMQ |
| `pom.xml` | `spring-boot-starter-amqp` |

**Docker**: o `rabbitmq` entra no `docker-compose.yml`, numa imagem que já traz o painel. O emissor e o gravador só sobem depois que ele responde. O gravador ganhou duas variáveis para o exercício: `DEMO_FALHAR_ANTES_DO_ACK` e `DEMO_FALHAS_PASSAGEIRAS`.

**`envenenada.json`**: um pedido de gravação sem os dados do comprovante, para publicar direto na exchange (passo 4 do exercício).

**Testes** (`docker compose run --rm testes`, ou `mvn test`; 27 no total). Nenhum precisa do RabbitMQ: nos testes, o gravador não escuta a fila (`spring.rabbitmq.listener.simple.auto-startup=false`).
- `ConsumidorDeGravacaoTest`: a mesma mensagem duas vezes grava uma vez só; com a chave do passo 3, o gravador grava, cai antes do ack, e a reentrega não grava de novo; a mensagem sem os dados falha sem gravar nada; o banco que "pisca" falha nas primeiras tentativas e depois grava.
- `FilasConfigTest`, no gravador e no emissor: a fila é durável, manda o que falhar para a DLQ, e os bindings ligam a etiqueta `gravacao` às filas certas.

## O exercício da Aula 5: a fila da gravação

Está em [`EXERCICIO-AULA-05.md`](EXERCICIO-AULA-05.md). O negócio avisou que a emissão não pode cair junto com o gravador, e que nenhum comprovante pode se perder. Cada pessoa coloca a fila entre o emissor e o gravador em 5 passos curtos: achar a exchange e as filas no painel, trocar o HTTP pela fila, ver a mesma mensagem chegar duas vezes, estragar uma mensagem de propósito e ligar o retry com a DLQ. Em cada passo, roda, olha o log do gravador, o painel ou o banco, e responde perguntas sobre as palavras da aula. O Java é um arquivo só, pronto para copiar.

O desafio extra é uma mensagem **boa** caída na DLQ, e o reprocessamento dela.

## O que mudou na branch `aula-04`

A consulta, que até a Aula 3 respondia sempre `404`, agora responde de verdade. Ela não tem banco: pergunta ao gravador, que é a **fonte da verdade** do comprovante (cada serviço com o seu banco, desde a Aula 1).

**Gravador**: ganhou a leitura.

| Onde | O que entrou |
|---|---|
| `GravadorController` | `GET /interno/comprovantes/{id}`: `200` com o comprovante, ou `404`. Demora 1 segundo **de propósito**, para fingir um banco cheio no horário de pico. |
| `GravadorService` | `buscar(id)`, uma transação só de leitura |
| `ComprovanteEntity` | `paraView()`: a linha da tabela vira o `ComprovanteView` |
| `application.yml` | `demo.leitura-lenta-ms: 1000` (só para a aula) |

**shared-contracts**: o `ComprovanteView`, o comprovante como a consulta mostra. É o que o gravador devolve, o que a consulta repassa e o que o cache guarda.

**Consulta**:

| Onde | O que entrou |
|---|---|
| `GravadorClient` | A chamada HTTP ao gravador, com tempo máximo de espera (`gravador.timeout-ms`). O `404` do gravador vira "vazio". |
| `ConsultaService` | Busca no gravador. **Ainda sem cache**: o `@Cacheable` entra no exercício. |
| `ConsultaController` | `GET /comprovantes/{id}`: `200` ou `404` |
| `ConsultaApplication` | `@EnableCaching`, que liga o `@Cacheable` |
| `CacheConfig` | Como guardar no Redis: em JSON (dá para ler no `redis-cli`) e com validade (o **TTL**) |
| `application.yml` | O endereço do Redis, o TTL (`consulta.cache.ttl: 60s`, curto só para a aula) e o endereço do gravador |
| `pom.xml` | `spring-boot-starter-cache` e `spring-boot-starter-data-redis` |

**Docker**: a consulta e o Redis entram no `docker-compose.yml`. O Redis não tem porta aberta para fora (só a consulta precisa falar com ele) e não salva nada em disco: é cache, e se reiniciar, começa vazio.

**Testes** (`docker compose run --rm testes`, ou `mvn test`; 21 no total):
- `GravadorLeituraTest`: o gravador devolve o que gravou (`200`) e responde `404` para um id que não tem.
- `ConsultaControllerTest`: `200`, com os campos em `snake_case`, e `404`. O gravador é de mentira (Mockito) e o cache fica na memória do teste: não precisa do Redis.

O exercício da Aula 4 (o cache) está em [`EXERCICIO-AULA-04.md`](EXERCICIO-AULA-04.md).

## O que mudou na branch `aula-03`

**Emissor**: o núcleo do dinheiro continua **orquestrado** (a SAGA da Aula 2). A periferia passa a ser **coreografada**: quando a SAGA termina, o emissor publica um evento, e quem se interessa reage.

| Onde | O que entrou |
|---|---|
| `dominio/ComprovanteGravado` | O **evento**: "o comprovante foi gravado". Leva o id e o valor. |
| `EmissaoService` | No fim da SAGA, publica o `ComprovanteGravado` (`ApplicationEventPublisher`). Não sabe quem escuta. |
| `reacoes/NotificacaoSms`, `reacoes/Antifraude`, `reacoes/PainelBi` | As reações (aqui, só um log). Cada uma com `@EventListener` + `@Async`: roda na sua thread, e a falha de uma não derruba o PIX. |
| `EmissorApplication` | `@EnableAsync`, que liga o `@Async`. |
| `application.yml` | `demo.sms-fora` (só para a demo). |

⚠️ As reações moram no emissor **só por enquanto**: sem um broker, o evento não sai do processo. Se o emissor cair entre o `GRAVADO` e o SMS, o SMS se perde. Quem resolve isso são as aulas de fila e de Kafka.

**Docker**: o `Dockerfile` e o `docker-compose.yml` sobem o gravador e o emissor sem instalar Java nem Maven. Dentro do Docker, o emissor acha o gravador pelo nome do serviço (`http://gravador:8082`), e não por `localhost`.

**Testes** (17 no total):
- `EventoComprovanteGravadoTest`: a SAGA terminou → **um** evento, com o valor. A SAGA compensou → **nenhum** evento.
- `SmsForaTest`: com o SMS fora, o PIX continua `GRAVADO`. Quebra se alguém tirar o `@Async`. (Por isso aparece um `ERROR` do SMS no meio dos testes: é de propósito.)

O exercício da Aula 3 (o cashback) está em [`EXERCICIO-AULA-03.md`](EXERCICIO-AULA-03.md).

## O que mudou na branch `aula-02`

**Emissor**: virou o **orquestrador** de uma SAGA de dois passos.

1. Cria o `Comprovante` e guarda como `ACEITO`.
2. Chama o gravador.
3. Deu certo → marca `GRAVADO` e responde `202`.
4. Deu errado → **compensa**: marca `INVALIDADO` (com o motivo, sem apagar) e responde `503`.

| Onde | O que entrou |
|---|---|
| `dominio/StatusComprovante` | `ACEITO`, `GRAVADO`, `INVALIDADO` |
| `dominio/Comprovante` | `confirmarGravacao()` e `invalidar(motivo)`, a compensação. Invalidar duas vezes não muda nada. `reconstituir(...)` monta de volta o que veio do banco. |
| `persistencia/ComprovanteEntity` | A linha da tabela (H2, `jdbc:h2:mem:emissor`). Separada do `Comprovante`, para o domínio continuar sem anotação de banco. |
| `persistencia/ComprovanteRepository` | A base do emissor na língua do domínio. Cada `salvar()` é uma transação local, com o seu `COMMIT`. |
| `gravacao/client/GravadorClient` | A chamada HTTP para o gravador, com tempo máximo de espera (`gravador.timeout-ms`) |
| `gravacao/GravacaoAdapter` | Esconde o HTTP do resto do código. Na aula de fila, só ele muda. |
| `gravacao/GravacaoFalhouException` | Vira `503` |
| `EmissaoService` | Os passos da SAGA, com o `try/catch` da compensação. **Sem `@Transactional`, de propósito**: o rollback apagaria o `INVALIDADO`. |

**Gravador**: ganhou a "porta da frente", o `GravadorController` (`POST /interno/comprovantes`). Se o id já existe, não grava de novo e responde `200`. Também passou a ler o JSON em `snake_case`, igual ao emissor.

**Consulta**: não muda.

**Testes** (`mvn test`, 14 no total):
- `EmissaoSagaTest`: o caminho feliz e a compensação, com a base de verdade do emissor e o gravador de mentira (Mockito). Confere o que ficou no banco.
- `ComprovanteControllerTest`: `202`, `400` e agora `503`.
- `DominioTest`: os de antes, mais "invalidar duas vezes dá no mesmo" e "comprovante invalidado não pode ser confirmado".
- `GravadorIdempotenciaTest`: mandar o mesmo comando duas vezes grava uma vez só.
