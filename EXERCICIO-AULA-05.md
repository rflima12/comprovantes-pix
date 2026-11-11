# Exercício da Aula 5: a fila da gravação

O negócio avisou: **"a emissão não pode cair junto com o gravador. E nenhum comprovante pode se perder."**

Você vai colocar uma fila de verdade, no RabbitMQ, entre o emissor e o gravador. No caminho, vai ver acontecer, na sua máquina, as palavras da aula: **exchange, binding, ack, reentrega, pelo menos uma vez, consumidor idempotente, falha passageira, falha permanente, retry com backoff, mensagem envenenada e DLQ**.

Ninguém precisa saber muito Java: o único arquivo Java do exercício está pronto para copiar. O trabalho é **rodar, olhar e explicar**.

## Como trabalhar

- **Individual**: cada pessoa faz o seu, na sua máquina.
- Cada passo tem quatro partes: **fazer**, **rodar**, **olhar** (o log do gravador, o painel do RabbitMQ ou o banco) e **responder**.
- As respostas vão **neste arquivo**, embaixo de cada pergunta, no lugar do *✍️ escreva aqui*. Com as suas palavras: não precisa ser bonito, precisa estar certo.
- No fim de cada passo, faça um **commit**. A correção abre a próxima aula: traga a sua branch.

## Antes de começar

**0. O que precisa estar instalado:** o Git e o Docker Desktop, **aberto**. O [README](README.md), na seção "Comece aqui", explica. **Este exercício precisa do Docker**: o RabbitMQ roda nele.

**1. Atualizar o projeto e criar a sua branch** (uma vez só). Se sobrou alguma coisa da Aula 4 sem commit, faça o commit antes.

```bash
docker compose down
git fetch
git switch aula-05
git switch -c exercicio-05-<seu-nome>
```

O `docker compose down` desliga o que sobrou da Aula 4. O `git fetch` traz do GitHub a branch nova.

**2. Abrir dois terminais**, os dois na pasta do projeto. No Windows, use o **Git Bash**: os comandos desta folha não funcionam no PowerShell.

| Terminal | Para quê |
|---|---|
| 1 | o **gravador**. É aqui que você vai olhar o log |
| 2 | **todo o resto**: os outros serviços, os `curl` e o `git` |

Suba o RabbitMQ, o emissor, a consulta e o Redis primeiro, no terminal 2:

```bash
docker compose up -d --build rabbitmq emissor consulta redis
```

Depois, o gravador, no terminal 1. Espere aparecer `Started GravadorApplication`:

```bash
docker compose up --build gravador
```

A primeira vez demora uns minutos: o Docker baixa o RabbitMQ e compila os serviços. Depois, fica rápido.

**3. Abrir o painel do RabbitMQ**, no navegador: `http://localhost:15672`, usuário `guest`, senha `guest`. As duas abas que você vai usar:

| Aba | O que mostra |
|---|---|
| **Exchanges** | os balcões. Clique no nome de um para ver os **bindings** dele (as plaquinhas) |
| **Queues and Streams** | as filas. A coluna **Ready** mostra quantas mensagens estão esperando. Clique no nome de uma fila para abrir |

O painel se atualiza sozinho a cada 5 segundos. Se um número parecer atrasado, espere um pouco.

**4. Mandar um PIX e guardar o id**, no terminal 2:

```bash
ID=$(curl -s -w "%{stderr}%{http_code} em %{time_total}s\n" -X POST localhost:8081/comprovantes -H "Content-Type: application/json" -d "@pix.json" | grep -o '[0-9a-f-]\{36\}' | head -1)
echo $ID
```

A primeira linha manda o PIX (o mesmo `pix.json` de sempre), mostra o **status** e o **tempo** da resposta (por exemplo `202 em 0.21s`) e guarda o id do comprovante na variável `ID`. A segunda mostra o id.

**5. Consultar o comprovante**, no terminal 2:

```bash
curl -s -w "\n%{http_code} em %{time_total}s\n" localhost:8083/comprovantes/$ID
```

> ⚠️ A variável `ID` vale **só no terminal onde você criou**. Se abrir outro terminal, mande um PIX de novo.

**6. Ligar e desligar o gravador:**

| Para... | Comando | Onde |
|---|---|---|
| desligar o gravador | `docker compose stop gravador` | terminal 2 |
| ligar o gravador | `docker compose up gravador` | terminal 1 |
| ligar o gravador depois de mudar o código dele | `docker compose up --build gravador` | terminal 1 |

Quando o gravador desliga, o terminal 1 volta para o prompt com a linha `gravador-1 exited with code 137` (ou `143`). É normal: foi você que desligou. O banco do gravador fica na memória: **ligar o gravador de novo apaga o banco dele**.

**7. Olhar o log do emissor**, no terminal 2: `docker compose logs --tail 10 emissor`.

**8. Olhar o banco do gravador:** o console fica em `http://localhost:8082/h2-console`. JDBC URL: `jdbc:h2:mem:gravador`. Usuário `sa`, senha em branco. Para ver os comprovantes gravados:

```sql
SELECT ID, NOME FROM COMPROVANTE_ENTITY;
```

Se o gravador reiniciou, conecte de novo.

No fim da aula, `docker compose down` desliga tudo.

---

## Passo 1: o balcão, a plaquinha e a dor de hoje (sem código, ~10 min)

**Faça:**

- **(a)** No painel, abra a aba **Queues and Streams**. Olhe as filas e a coluna **Ready**.
- **(b)** Abra a aba **Exchanges** e clique em `gravacao.ex`. Olhe a parte **Bindings**.
- **(c)** Desligue o gravador (item 6) e mande um PIX (item 4). Olhe o status, o tempo e o log do emissor (item 7).
- **(d)** Volte à aba **Queues and Streams** e olhe a `gravacao.q`.

**1.1** Que filas aparecem com o nome começando por `gravacao`? Quantas mensagens tem em cada uma?

> ✍️ *escreva aqui*

**1.2** Na exchange `gravacao.ex`, para qual fila vai a mensagem com a etiqueta (a routing key) `gravacao`? Onde o painel mostra isso?

> ✍️ *escreva aqui*

**1.3** Com o gravador desligado, o que o PIX respondeu? Em quanto tempo? O que o log do emissor diz?

> ✍️ *escreva aqui*

**1.4** Por que o gravador desligado derrubou a emissão?

> ✍️ *escreva aqui*

**1.5** Quantas mensagens apareceram na `gravacao.q`? Por quê?

> ✍️ *escreva aqui*

**Deixe o gravador desligado**: o passo 2 começa assim. Faça um commit.

---

## Passo 2: trocar o HTTP pela fila (~15 min)

**Faça:**

- **(a)** Abra `comprovante-emissor/src/main/java/tech/ada/pix/emissor/gravacao/GravacaoAdapter.java`. Apague **todo** o conteúdo e cole este:

  ```java
  package tech.ada.pix.emissor.gravacao;

  import org.springframework.amqp.AmqpException;
  import org.springframework.amqp.rabbit.core.RabbitTemplate;
  import org.springframework.stereotype.Component;
  import tech.ada.pix.contracts.ComprovanteRequest;
  import tech.ada.pix.contracts.FilaDeGravacao;
  import tech.ada.pix.contracts.GravarComprovanteCommand;
  import tech.ada.pix.emissor.dominio.Comprovante;

  /**
   * A "tomada" entre a emissão e a gravação.
   *
   * O EmissaoService só sabe "gravar"; não sabe o que existe do outro lado.
   * Aula 5: em vez de chamar o gravador por HTTP e esperar, este adapter publica o pedido de
   * gravação no RabbitMQ e volta na hora. O EmissaoService nem percebeu a troca.
   */
  @Component
  public class GravacaoAdapter {

      private final RabbitTemplate rabbit;

      public GravacaoAdapter(RabbitTemplate rabbit) {
          this.rabbit = rabbit;
      }

      public void gravar(Comprovante comprovante, ComprovanteRequest dados) {
          try {
              // Entrega no balcão (a exchange), com a etiqueta (a routing key).
              // Quem escolhe a fila é o RabbitMQ, pelo binding.
              rabbit.convertAndSend(FilaDeGravacao.EXCHANGE, FilaDeGravacao.ROUTING_KEY,
                      new GravarComprovanteCommand(comprovante.id(), dados));
          } catch (AmqpException falha) {
              // O RabbitMQ está fora do ar: a SAGA compensa, como antes (503 e INVALIDADO).
              throw new GravacaoFalhouException(comprovante.id(), falha);
          }
      }
  }
  ```

- **(b)** Suba o emissor com o código novo, no terminal 2:
  ```bash
  docker compose up -d --build emissor
  ```
  Leva uns 30 segundos. O emissor está pronto quando `docker compose logs --tail 1 emissor` mostra `Started EmissorApplication`.
- **(c)** **Com o gravador desligado**, mande um PIX (item 4). Olhe o status, o tempo e o log do emissor (item 7).
- **(d)** No painel, olhe a `gravacao.q`.
- **(e)** Derrube o emissor de um jeito feio, como se o servidor saísse da tomada, e olhe a `gravacao.q` de novo:
  ```bash
  docker compose kill emissor
  ```
- **(f)** Ligue o gravador (item 6) e olhe o log dele, no terminal 1. Olhe a `gravacao.q` de novo e consulte o comprovante (item 5).
- **(g)** Ligue o emissor de novo, para o próximo passo: `docker compose up -d emissor`.

**2.1** Com o gravador desligado, o que o PIX respondeu agora? Em quanto tempo? Compare com o passo 1.

> ✍️ *escreva aqui*

**2.2** Onde está o pedido de gravação agora? Como você sabe?

> ✍️ *escreva aqui*

**2.3** Que arquivo você mudou? E o `EmissaoService` (a SAGA), mudou? Por que não precisou?

> ✍️ *escreva aqui*

**2.4** O log do emissor diz *"SAGA concluída: comprovante … GRAVADO"*, e até *"SMS enviado"*. O comprovante já estava gravado? Em quem você confia: no emissor ou no gravador? E o que o `202` quer dizer agora?

> ✍️ *escreva aqui*

**2.5** Depois do `kill`, o pedido de gravação continuou na fila? Por quê? Compare com o passo 5 da Aula 3 (o evento que sumiu).

> ✍️ *escreva aqui*

**2.6** Quando o gravador ligou, o que apareceu no log dele? O que aconteceu com a fila? E a consulta?

> ✍️ *escreva aqui*

Faça um commit.

---

## Passo 3: gravou e caiu antes do "pronto" (sem código, ~12 min)

**Faça:**

- **(a)** Desligue o gravador (item 6). Ligue de novo, com a chave da demo, no terminal 1:
  ```bash
  DEMO_FALHAR_ANTES_DO_ACK=true docker compose up gravador
  ```
  Com essa chave, o gravador grava o comprovante e "cai" logo depois, **antes do ack** (antes de dizer "pronto" ao RabbitMQ). Isso só acontece quando ele acabou de gravar: se o comprovante já estava gravado, ele não cai.
- **(b)** Mande um PIX (item 4). Olhe o log do gravador.
- **(c)** Olhe o banco do gravador (item 8).
- **(d)** Desligue a chave: desligue o gravador (item 6) e ligue de novo **sem** nada na frente, `docker compose up gravador`.

**3.1** Quantas vezes o gravador recebeu a mesma mensagem? O que ele fez em cada vez?

> ✍️ *escreva aqui*

**3.2** Por que o RabbitMQ entregou a mensagem de novo?

> ✍️ *escreva aqui*

**3.3** Quantas linhas tem o comprovante no banco do gravador? Por que não duas?

> ✍️ *escreva aqui*

**3.4** O RabbitMQ sabia que, na primeira vez, o comprovante tinha sido gravado? Por que se diz que a fila entrega **"pelo menos uma vez"**, e não "exatamente uma vez"?

> ✍️ *escreva aqui*

**3.5** E se, além de gravar, o gravador mandasse um SMS ao cliente, sem olhar se já tinha mandado?

> ✍️ *escreva aqui*

Faça um commit.

---

## Passo 4: a pizza de picolé (sem código, ~8 min)

**Faça:**

- **(a)** Abra o arquivo `envenenada.json`, na raiz do projeto. É um pedido de gravação **sem os dados do comprovante** (`"dados":null`).
- **(b)** Publique esse pedido direto na exchange, pelo painel do RabbitMQ, no terminal 2. A resposta deve ser `{"routed":true}`:
  ```bash
  curl -s -u guest:guest -X POST localhost:15672/api/exchanges/%2F/gravacao.ex/publish -H "Content-Type: application/json" -d "@envenenada.json"
  ```
  Olhe o log do gravador por uns segundos.
- **(c)** Desligue o gravador (item 6). Espere uns 5 segundos e olhe a `gravacao.q` no painel.
- **(d)** Conte quantas vezes o gravador recebeu uma mensagem pela fila:
  ```bash
  docker compose logs gravador | grep -c "Recebi pela fila"
  ```

**4.1** O que aparece no log do gravador? Para sozinho?

> ✍️ *escreva aqui*

**4.2** Quantas vezes o gravador recebeu a mensagem? Em quantos segundos, mais ou menos?

> ✍️ *escreva aqui*

**4.3** Por que a mensagem volta sem parar?

> ✍️ *escreva aqui*

**4.4** Tentar de novo vai resolver um dia? Que tipo de falha é essa: passageira ou permanente?

> ✍️ *escreva aqui*

**4.5** Com o gravador desligado, a mensagem sumiu? Onde ela está? O que vai acontecer quando o gravador ligar?

> ✍️ *escreva aqui*

Faça um commit.

---

## Passo 5: tentar de novo, com limite, e a caixinha do gerente (~13 min)

**Faça:**

- **(a)** Abra `comprovante-gravador/src/main/resources/application.yml`. No bloco `retry`, troque `enabled: false` por `enabled: true`:
  ```yaml
          retry:
            enabled: true          # ← passo 5: troque para true
  ```
  Só troque a palavra: não mexa nos espaços do começo da linha. Leia os comentários das linhas de baixo: 3 tentativas ao todo, esperando 1 segundo antes da 2ª e 2 segundos antes da 3ª.
- **(b)** Ligue o gravador com a mudança (item 6, com `--build`). A envenenada do passo 4 está esperando na fila. Olhe o log, e repare na **hora** de cada linha *"Recebi pela fila"*. No fim, aparece um aviso comprido, que começa com `Retries exhausted`: é o Spring desistindo.
- **(c)** No painel, abra a fila `gravacao.dlq` (aba **Queues and Streams**, clique no nome). Na parte **Get messages**, clique no botão **Get Message(s)**. Olhe o **Payload** e os **headers**, principalmente o `x-death`.
- **(d)** Publique a envenenada de novo (o mesmo `curl` do passo 4, item b) e olhe o log.
- **(e)** Mande um PIX bom (item 4) e consulte (item 5).

**5.1** Quantas vezes o gravador tentou a envenenada? Quanto tempo passou entre uma tentativa e outra? Por que esperar cada vez mais?

> ✍️ *escreva aqui*

**5.2** Depois da terceira tentativa, para onde foi a mensagem? Ela voltou para a `gravacao.q`?

> ✍️ *escreva aqui*

**5.3** O que o painel mostra da mensagem na DLQ? Dá para descobrir de onde ela veio e por que foi parar lá?

> ✍️ *escreva aqui*

**5.4** Na envenenada nova, do item (d), as três tentativas mostram *"reentrega: não"*. Então, quem tentou de novo: o RabbitMQ ou o gravador?

> ✍️ *escreva aqui*

**5.5** O PIX bom passou? Por que foi bom tirar a envenenada do caminho?

> ✍️ *escreva aqui*

**5.6** Por que mandar a mensagem para a DLQ, e não jogar fora?

> ✍️ *escreva aqui*

**5.7** Se o banco do gravador piscasse por 1 segundo (uma falha passageira), o retry ajudava?

> ✍️ *escreva aqui*

Faça um commit.

---

## Desafio extra (quem terminar antes, ou em casa)

Até agora, a DLQ só recebeu mensagem com defeito. Será que ela também recebe mensagem **boa**?

**Faça:**

- **(a)** Desligue o gravador (item 6) e ligue de novo com o banco "piscando" nas **2** primeiras tentativas de cada comprovante:
  ```bash
  DEMO_FALHAS_PASSAGEIRAS=2 docker compose up gravador
  ```
  Mande um PIX (item 4), olhe o log e consulte (item 5).
- **(b)** Desligue o gravador e ligue de novo, agora com o banco piscando nas **5** primeiras tentativas:
  ```bash
  DEMO_FALHAS_PASSAGEIRAS=5 docker compose up gravador
  ```
  Mande um PIX, olhe o log e a `gravacao.dlq` no painel, e consulte.
- **(c)** Alguém consertou o banco. Desligue o gravador. Crie o arquivo `comprovante-gravador/src/main/java/tech/ada/pix/gravador/ReprocessadorDaDlq.java` com o conteúdo abaixo. Depois, ligue o gravador sem a falha (item 6, com `--build`).

  ```java
  package tech.ada.pix.gravador;

  import com.rabbitmq.client.Channel;
  import com.rabbitmq.client.GetResponse;
  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.amqp.rabbit.core.RabbitTemplate;
  import org.springframework.web.bind.annotation.PostMapping;
  import org.springframework.web.bind.annotation.RestController;
  import tech.ada.pix.contracts.FilaDeGravacao;

  import java.io.IOException;

  /**
   * Desafio extra da Aula 5: alguém olhou a DLQ, consertou a causa e agora quer reprocessar.
   *
   * POST /interno/dlq/reprocessar devolve as mensagens da DLQ para a fila de gravação.
   * Para cada mensagem: pega da DLQ SEM confirmar, publica de volta no balcão (a exchange) e só
   * então confirma (o ack). Se o gravador cair no meio, a mensagem continua na DLQ: nada se perde.
   */
  @RestController
  public class ReprocessadorDaDlq {

      private static final Logger log = LoggerFactory.getLogger(ReprocessadorDaDlq.class);

      private final RabbitTemplate rabbit;

      public ReprocessadorDaDlq(RabbitTemplate rabbit) {
          this.rabbit = rabbit;
      }

      @PostMapping("/interno/dlq/reprocessar")
      public String reprocessar() {
          // Só as que estão na DLQ agora. Se alguma voltar para lá durante o reprocesso, fica para a próxima vez.
          long naDlq = rabbit.execute(canal -> canal.messageCount(FilaDeGravacao.DLQ));
          int devolvidas = 0;
          for (long i = 0; i < naDlq; i++) {
              if (Boolean.TRUE.equals(rabbit.execute(this::devolverUma))) {
                  devolvidas++;
              }
          }
          log.info("Devolvi {} mensagem(ns) da DLQ para a fila {}", devolvidas, FilaDeGravacao.FILA);
          return devolvidas + " mensagem(ns) devolvida(s) para a fila " + FilaDeGravacao.FILA + "\n";
      }

      private boolean devolverUma(Channel canal) throws IOException {
          GetResponse mensagem = canal.basicGet(FilaDeGravacao.DLQ, false);   // false: pega, mas ainda NÃO confirma
          if (mensagem == null) {
              return false;                                                    // a DLQ ficou vazia
          }
          canal.basicPublish(FilaDeGravacao.EXCHANGE, FilaDeGravacao.ROUTING_KEY,
                  mensagem.getProps(), mensagem.getBody());                    // de volta ao balcão, igualzinha
          canal.basicAck(mensagem.getEnvelope().getDeliveryTag(), false);      // agora sim: sai da DLQ
          return true;
      }
  }
  ```

- **(d)** Reprocesse a DLQ, no terminal 2. Olhe o log do gravador e o painel, e consulte o comprovante do item (b) (a variável `ID` ainda é a dele):
  ```bash
  curl -X POST localhost:8082/interno/dlq/reprocessar
  ```

**E.1** Em (a), o que aconteceu nas três tentativas? O comprovante foi gravado?

> ✍️ *escreva aqui*

**E.2** Em (b), para onde foi o pedido de gravação? A mensagem tinha defeito? O que a consulta respondeu? O que o cliente pensaria?

> ✍️ *escreva aqui*

**E.3** Em (d), o que aconteceu com cada mensagem que estava na DLQ?

> ✍️ *escreva aqui*

**E.4** Então, a DLQ é lixo? O que alguém tem que fazer com ela, e em que ordem?

> ✍️ *escreva aqui*

**E.5** Como escolher o número de tentativas e as esperas?

> ✍️ *escreva aqui*

---

## Se der errado

| O que aparece | O que fazer |
|---|---|
| `Cannot connect to the Docker daemon`, `error during connect` ou `docker: command not found` | O Docker Desktop não está aberto. Abra e espere ele dizer que está rodando (*Engine running*). |
| `port is already allocated` (5672, 15672, 8081, 8082 ou 8083) | Outro programa está usando a porta: talvez um serviço rodando fora do Docker. Feche esse programa. O `docker compose ps` mostra o que está rodando no Docker. |
| O painel do RabbitMQ não abre | O RabbitMQ leva uns 20 segundos para subir. Espere e recarregue. Ele está pronto quando o `docker compose ps` mostra `(healthy)` do lado dele. |
| O login do painel não entra | Usuário `guest`, senha `guest`. |
| `Empty reply from server` ou `Connection refused` | O serviço ainda está subindo. Espere o `Started ...Application` no log e mande de novo. |
| O `echo $ID` mostra uma linha vazia | O PIX não deu certo, ou o emissor ainda estava subindo. Olhe o status que apareceu na linha de cima e o log do emissor (item 7). |
| No passo 2, a `gravacao.q` não mostra nenhuma mensagem esperando | O gravador estava ligado e pegou a mensagem na hora. Desligue o gravador (item 6) e mande outro PIX. Ou o painel ainda não atualizou: espere 5 segundos. |
| No passo 2, o PIX continua respondendo `503` | Você subiu o emissor sem o `--build`, ou colou o código no arquivo errado. Se o log do emissor fala em `Connection refused` com o RabbitMQ, ligue o RabbitMQ: `docker compose up -d rabbitmq`. |
| No passo 2, o `--build` do emissor dá erro de compilação | O código novo tem que substituir **todo** o conteúdo do `GravacaoAdapter.java`, na mesma pasta, com o mesmo `package` na primeira linha. |
| No passo 3, o gravador não cai | A chave não pegou. O `DEMO_FALHAR_ANTES_DO_ACK=true` vai na mesma linha do `docker compose up gravador`, na frente. |
| Depois do passo 3, todo PIX dá erro uma vez | A chave continua ligada. Desligue o gravador e ligue com `docker compose up gravador`, sem nada na frente. |
| No passo 4, o `curl` responde `{"routed":false}` | A exchange ainda não existe. Ligue o gravador uma vez: ele cria a exchange e as filas quando sobe. |
| No passo 4, o log enche a tela e não para | É o passo. Desligue o gravador (item 6). |
| No passo 5, o gravador continua tentando sem parar | Você ligou o gravador sem o `--build`, ou não salvou o `application.yml` com o `enabled: true`. |
| No passo 5, o gravador não sobe depois de mudar o `application.yml` | Os espaços do começo da linha mudaram. Deixe a linha como estava e troque só a palavra `false` por `true`. |
| Cliquei em *Get Message(s)* e a mensagem continua na DLQ | É de propósito: do jeito que vem marcado, o painel só espia a mensagem e devolve para a fila. |
| A consulta responde `500` | O gravador está desligado: a consulta lê do gravador (Aula 4). Ligue o gravador. |
| A consulta responde `404` para um `$ID` antigo | O gravador reiniciou e apagou o banco. Mande um PIX novo (item 4). |
