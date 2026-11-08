# Exercício da Aula 4: o cache da consulta

O negócio avisou: **"a consulta de comprovante é a operação mais chamada do PIX. Todo cliente abre o comprovante logo depois do PIX, e mais de uma vez. O banco do gravador está apanhando."**

Você vai colocar um cache na consulta. No caminho, vai ver acontecer, na sua máquina, as palavras da aula: **fonte da verdade, cache, hit, miss, cache-aside, chave, TTL, invalidação, cache local, cache distribuído e Redis**.

Ninguém precisa saber muito Java: todo código deste exercício está pronto para copiar. O trabalho é **rodar, olhar e explicar**.

## Como trabalhar

- **Individual**: cada pessoa faz o seu, na sua máquina.
- Cada passo tem quatro partes: **fazer**, **rodar**, **olhar** (o log, a resposta do `curl` ou o Redis) e **responder**.
- As respostas vão **neste arquivo**, embaixo de cada pergunta, no lugar do *✍️ escreva aqui*. Com as suas palavras: não precisa ser bonito, precisa estar certo.
- No fim de cada passo, faça um **commit**. A correção abre a próxima aula: traga a sua branch.

## Antes de começar

**0. O que precisa estar instalado:** o Git e o Docker Desktop, **aberto**. O [README](README.md), na seção "Comece aqui", explica. **Este exercício precisa do Docker**: o Redis roda nele.

**1. Atualizar o projeto e criar a sua branch** (uma vez só). Se sobrou alguma coisa da Aula 3 sem commit, faça o commit antes.

```bash
docker compose down
git fetch
git switch aula-04
git switch -c exercicio-04-<seu-nome>
```

O `docker compose down` desliga o que sobrou da Aula 3. O `git fetch` traz do GitHub a branch nova.

**2. Abrir dois terminais**, os dois na pasta do projeto. No Windows, use o **Git Bash**: os comandos desta folha não funcionam no PowerShell.

| Terminal | Para quê |
|---|---|
| 1 | a **consulta**. É aqui que você vai olhar o log |
| 2 | **todo o resto**: os outros serviços, os `curl`, o Redis e o `git` |

Suba o gravador, o emissor e o Redis primeiro, no terminal 2:

```bash
docker compose up -d --build gravador emissor redis
```

Depois, a consulta, no terminal 1. Espere aparecer `Started ConsultaApplication`:

```bash
docker compose up --build consulta
```

A primeira vez demora uns minutos: o Docker baixa o Redis e compila a consulta. Depois, fica rápido.

**3. Mandar um PIX e guardar o id**, no terminal 2:

```bash
ID=$(curl -s -X POST localhost:8081/comprovantes -H "Content-Type: application/json" -d "@pix.json" | grep -o '[0-9a-f-]\{36\}')
echo $ID
```

A primeira linha manda o PIX (o mesmo `pix.json` da Aula 3) e guarda o id do comprovante na variável `ID`. A segunda mostra o id.

**4. Consultar o comprovante** (você vai usar muito), no terminal 2:

```bash
curl -s -w "\n%{http_code} em %{time_total}s\n" localhost:8083/comprovantes/$ID
```

Aparece o comprovante, em JSON. Na última linha, aparecem o **status** e o **tempo** que a consulta levou, por exemplo `200 em 1.054449s`.

**5. Olhar dentro do Redis.** Crie um atalho, uma vez, no terminal 2:

```bash
alias redis-cli='docker compose exec -T redis redis-cli'
```

Depois, é só usar:

| Para... | Comando |
|---|---|
| ver todas as chaves | `redis-cli KEYS '*'` |
| ver o que está guardado numa chave | `redis-cli GET "comprovantes::$ID"` |
| ver quantos segundos faltam para a chave expirar | `redis-cli TTL "comprovantes::$ID"` |
| apagar uma chave | `redis-cli DEL "comprovantes::$ID"` |
| apagar tudo | `redis-cli FLUSHALL` |

Quando o Redis está vazio, o `KEYS` não mostra nada. O `TTL` responde `-2` quando a chave não existe.

> ⚠️ A variável `ID` e o atalho valem **só no terminal onde você criou**. Se abrir outro terminal, crie de novo.

**6. Reiniciar a consulta depois de mudar o código:** `Ctrl+C` no terminal 1 e o mesmo comando de novo, `docker compose up --build consulta`. O `--build` compila o código novo: leva uns 20 segundos.

**7. Ligar e desligar os serviços**, no terminal 2:

| Para... | Comando |
|---|---|
| desligar o Redis | `docker compose stop redis` |
| ligar o Redis de novo | `docker compose start redis` |
| reiniciar o gravador (apaga o banco dele) | `docker compose restart gravador` |

**8. Olhar o banco do gravador:** o console fica em `http://localhost:8082/h2-console`. JDBC URL: `jdbc:h2:mem:gravador`. Usuário `sa`, senha em branco. O banco fica na memória: **reiniciar o gravador apaga o banco dele**.

No fim da aula, `docker compose down` desliga tudo.

---

## Passo 1: a consulta sem cache (sem código, ~8 min)

**Faça:** mande um PIX e guarde o id (item 3). Consulte o comprovante **três vezes** (item 4). Olhe o tempo de cada consulta e o log da consulta (terminal 1).

**Dica:** o gravador demora 1 segundo para ler cada comprovante, **de propósito**: é o banco cheio no horário de pico (`demo.leitura-lenta-ms`, no `application.yml` do gravador). Num banco de verdade, a lentidão vem da carga.

**1.1** Quanto tempo levou cada consulta?

> ✍️ *escreva aqui*

**1.2** No log da consulta, quantas vezes aparece *"Indo ao gravador buscar o comprovante"*? Uma para cada consulta?

> ✍️ *escreva aqui*

**1.3** O comprovante mudou de uma consulta para a outra? Então, por que a consulta vai ao gravador toda vez?

> ✍️ *escreva aqui*

**1.4** Quem é a **fonte da verdade** do comprovante: a consulta ou o gravador? Por quê?

> ✍️ *escreva aqui*

**1.5** Imagine um milhão de clientes, cada um abrindo o comprovante três vezes logo depois do PIX. Quantas leituras chegam ao banco do gravador? O que pode acontecer com ele?

> ✍️ *escreva aqui*

---

## Passo 2: ligar o cache (~10 min)

**Faça:** abra `comprovante-consulta/src/main/java/tech/ada/pix/consulta/ConsultaService.java`. Junto dos outros `import`, coloque:

```java
import org.springframework.cache.annotation.Cacheable;
```

E, **na linha de cima** do método `buscar`, coloque o `@Cacheable`:

```java
    @Cacheable("comprovantes")
    public Optional<ComprovanteView> buscar(UUID id) {
```

O `@Cacheable` diz ao Spring: *"antes de rodar este método, procure o resultado no cache, usando o id como chave"*. O resto (o Redis e o tempo de validade) já está configurado no projeto, no `CacheConfig` e no `application.yml` da consulta.

Reinicie a consulta (item 6). Consulte o **mesmo** comprovante três vezes (a variável `ID` continua valendo). Olhe o tempo e o log.

**2.1** Quanto tempo levou cada consulta agora?

> ✍️ *escreva aqui*

**2.2** Quantas vezes a consulta foi ao gravador?

> ✍️ *escreva aqui*

**2.3** Qual das três consultas foi um **miss**? Quais foram **hit**? Como você sabe?

> ✍️ *escreva aqui*

**2.4** Na primeira consulta, quem foi buscar o comprovante no gravador e guardou no cache: o Redis ou a consulta? Por que esse jeito se chama **cache-aside** ("o cache ao lado")?

> ✍️ *escreva aqui*

Faça um commit.

---

## Passo 3: dentro do Redis (~10 min)

**Faça:**

- **(a)** Apague tudo do cache, para começar do zero, e consulte o comprovante:
  ```bash
  redis-cli FLUSHALL
  curl -s -w "\n%{http_code} em %{time_total}s\n" localhost:8083/comprovantes/$ID
  ```
  **Logo em seguida**, reinicie a consulta: `Ctrl+C` no terminal 1 e `docker compose up consulta`. Assim que aparecer `Started ConsultaApplication`, consulte de novo e olhe o log.
- **(b)** Olhe dentro do Redis: rode `redis-cli KEYS '*'`, depois `redis-cli GET "comprovantes::$ID"`, e depois `redis-cli TTL "comprovantes::$ID"` algumas vezes.
- **(c)** Continue rodando o `TTL` até aparecer `-2` (leva menos de um minuto). Consulte de novo e olhe o tempo e o log. Rode o `TTL` mais uma vez.

**3.1** Em (a), depois de reiniciar a consulta, ela foi ao gravador? Por que o cache não se perdeu?

> ✍️ *escreva aqui*

**3.2** Se o cache fosse um `HashMap` dentro da consulta, o que aconteceria ao reiniciar? E se tivéssemos três consultas atrás de um balanceador de carga, cada uma com o seu `HashMap`?

> ✍️ *escreva aqui*

**3.3** Qual é a **chave** do comprovante no Redis? O que ela tem a ver com o comprovante?

> ✍️ *escreva aqui*

**3.4** O que está guardado na chave? Compare com a resposta da consulta.

> ✍️ *escreva aqui*

**3.5** O que os números do `TTL` mostram? O que aconteceu com a chave quando o `TTL` chegou a `-2`? E com a consulta seguinte?

> ✍️ *escreva aqui*

Faça um commit.

---

## Passo 4: o cache mente (sem código, ~10 min)

**Faça:**

- **(a)** Abra o console do H2 do **gravador** (item 8) e conecte. Deixe a página aberta.
- **(b)** No terminal 2, apague o cache e consulte o comprovante. Olhe o `nome`:
  ```bash
  redis-cli FLUSHALL
  curl -s -w "\n%{http_code} em %{time_total}s\n" localhost:8083/comprovantes/$ID
  ```
- **(c)** **Logo em seguida**, no console do H2, rode (alguém corrigiu o nome do cliente direto no banco):
  ```sql
  UPDATE COMPROVANTE_ENTITY SET NOME = 'Giovanna';
  SELECT ID, NOME FROM COMPROVANTE_ENTITY;
  ```
- **(d)** Consulte o comprovante de novo. Olhe o `nome`.
- **(e)** Apague a chave do comprovante e consulte de novo. Olhe o `nome` e o tempo:
  ```bash
  redis-cli DEL "comprovantes::$ID"
  curl -s -w "\n%{http_code} em %{time_total}s\n" localhost:8083/comprovantes/$ID
  ```

**4.1** Depois do `UPDATE`, que nome está no banco do gravador? E que nome a consulta mostrou em (d)? Por quê?

> ✍️ *escreva aqui*

**4.2** Quem estava certo em (d): o banco ou o cache?

> ✍️ *escreva aqui*

**4.3** Em (e), o que o `DEL` fez? Por que a consulta seguinte levou 1 segundo?

> ✍️ *escreva aqui*

**4.4** E se ninguém rodasse o `DEL`? Por quanto tempo, no máximo, a consulta mostraria o nome errado? Por quê?

> ✍️ *escreva aqui*

**4.5** Na vida real, um comprovante de PIX muda depois de gravado? Que TTL você escolheria para ele: 60 segundos, 1 hora ou 1 dia? Por quê?

> ✍️ *escreva aqui*

---

## Passo 5: o 404 no cache (~12 min)

O comprovante `44444444-4444-4444-4444-444444444444` **ainda não existe**. Ele está no arquivo `gravar.json`, pronto para ser gravado direto no gravador, como se a gravação terminasse agora. Guarde o id numa variável, no terminal 2:

```bash
NOVO=44444444-4444-4444-4444-444444444444
```

**Faça:**

- **(a)** Consulte o comprovante novo, que ainda não existe:
  ```bash
  curl -s -w "\n%{http_code} em %{time_total}s\n" localhost:8083/comprovantes/$NOVO
  ```
- **(b)** Olhe o Redis: `redis-cli KEYS '*'` e `redis-cli GET "comprovantes::$NOVO"`.
- **(c)** Grave o comprovante direto no gravador (a resposta deve ser `201`):
  ```bash
  curl -i -X POST localhost:8082/interno/comprovantes -H "Content-Type: application/json" -d "@gravar.json"
  ```
- **(d)** Consulte o comprovante novo mais duas vezes.
- **(e)** Conserte: no `ConsultaService`, troque `@Cacheable("comprovantes")` por
  ```java
      @Cacheable(value = "comprovantes", unless = "#result == null")
  ```
  Reinicie a consulta. Depois, para começar do zero, apague o banco do gravador e o cache:
  ```bash
  docker compose restart gravador
  redis-cli FLUSHALL
  ```
  Espere uns 15 segundos (o gravador está subindo) e repita (a), (b), (c) e (d).

**5.1** Em (a), qual foi o status? E o que apareceu no Redis em (b)?

> ✍️ *escreva aqui*

**5.2** Em (d), o comprovante já estava gravado. O que a consulta respondeu? Por quê? Até quando ela ia responder isso?

> ✍️ *escreva aqui*

**5.3** Depois do conserto, o que mudou em (b) e em (d)? O que o `unless = "#result == null"` quer dizer?

> ✍️ *escreva aqui*

**5.4** Na aula de fila, a gravação vai acontecer **depois** do `202`: o cliente pode consultar antes de o gravador terminar. Com o 404 guardado no cache e um TTL de 1 hora, o que o cliente veria?

> ✍️ *escreva aqui*

**5.5** Complete a regra: *"no cache, guarde ______, nunca ______"*.

> ✍️ *escreva aqui*

Faça um commit.

---

## Passo 6: o Redis fora do ar (~10 min)

Use o comprovante novo (`$NOVO`): o do passo 1 sumiu quando o gravador reiniciou.

**Faça:**

- **(a)** Desligue o Redis (`docker compose stop redis`) e consulte o comprovante novo. Anote o status e olhe o log.
- **(b)** Ligue o Redis de novo (`docker compose start redis`). Crie o arquivo `comprovante-consulta/src/main/java/tech/ada/pix/consulta/CacheTolerante.java` com o conteúdo abaixo. Reinicie a consulta e consulte uma vez.

  ```java
  package tech.ada.pix.consulta;

  import org.slf4j.Logger;
  import org.slf4j.LoggerFactory;
  import org.springframework.cache.Cache;
  import org.springframework.cache.annotation.CachingConfigurer;
  import org.springframework.cache.interceptor.CacheErrorHandler;
  import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
  import org.springframework.context.annotation.Configuration;

  /**
   * Exercício da Aula 4: o cache não pode derrubar a consulta.
   *
   * Se o Redis não responder, em vez de devolver erro para o cliente, a consulta segue sem cache
   * e vai direto ao gravador. Fica mais lento, mas não erra: o cache é só uma cópia.
   */
  @Configuration
  public class CacheTolerante implements CachingConfigurer {

      private static final Logger log = LoggerFactory.getLogger(CacheTolerante.class);

      @Override
      public CacheErrorHandler errorHandler() {
          return new SimpleCacheErrorHandler() {

              @Override
              public void handleCacheGetError(RuntimeException erro, Cache cache, Object chave) {
                  log.warn("Cache fora do ar. Vou direto ao gravador (comprovante {})", chave);
              }

              @Override
              public void handleCachePutError(RuntimeException erro, Cache cache, Object chave, Object valor) {
                  log.warn("Cache fora do ar. Não deu para guardar o comprovante {}", chave);
              }
          };
      }
  }
  ```

- **(c)** Desligue o Redis de novo e consulte o comprovante novo **duas vezes**. Anote o status e o tempo, e olhe o log.
- **(d)** Ligue o Redis (`docker compose start redis`) e consulte duas vezes. Olhe os tempos.

**6.1** Em (a), o que a consulta respondeu? O gravador estava de pé?

> ✍️ *escreva aqui*

**6.2** Faz sentido o cliente receber erro porque o cache caiu? O cache é a fonte da verdade ou uma cópia?

> ✍️ *escreva aqui*

**6.3** Em (c), qual foi o status? E o tempo? Por que ficou mais lento até do que sem cache nenhum (passo 1)? **Dica:** cada linha *"Cache fora do ar"* no log é 1 segundo esperando o Redis responder.

> ✍️ *escreva aqui*

**6.4** Com o Redis fora, quem recebe **todas** as leituras? No horário de pico, o que isso pode causar?

> ✍️ *escreva aqui*

**6.5** Em (d), por que a primeira consulta levou 1 segundo e a segunda não?

> ✍️ *escreva aqui*

Faça um commit.

---

## Desafio extra (para quem já sabe Java)

O material da aula (`aula-04-aluno.md`, §7) pede que a consulta, antes de responder `404`, tente **3 vezes** no gravador, com uma pequena espera entre uma tentativa e outra. Hoje, a gravação é síncrona e isso não faz diferença. Na aula de fila, a gravação vai acontecer depois do `202`, e o cliente pode consultar antes de ela terminar.

Faça a consulta tentar 3 vezes antes do `404`, e guardar no cache só quando achar. Prove com um teste: o gravador de mentira responde *"não tenho"* duas vezes e acha na terceira. Para rodar os testes: `docker compose run --rm testes`.

---

## Se der errado

| O que aparece | O que fazer |
|---|---|
| `Cannot connect to the Docker daemon`, `error during connect` ou `docker: command not found` | O Docker Desktop não está aberto. Abra e espere ele dizer que está rodando (*Engine running*). |
| `port is already allocated` (8081, 8082 ou 8083) | Outro programa está usando a porta: talvez um serviço rodando fora do Docker. Feche esse programa. O `docker compose ps` mostra o que está rodando no Docker. |
| `Empty reply from server` ou `Connection refused` | O serviço ainda está subindo. Espere o `Started ...Application` no log e mande de novo. |
| O `echo $ID` mostra uma linha vazia | O PIX não deu certo, ou o emissor ainda estava subindo. Mande o PIX com o comando da Aula 3 para ver a resposta: `curl -i -X POST localhost:8081/comprovantes -H "Content-Type: application/json" -d "@pix.json"`. |
| `redis-cli: command not found` | Falta criar o atalho neste terminal (item 5). |
| `service "redis" is not running` | O Redis está desligado: `docker compose start redis`. |
| A consulta responde `404` para o `$ID` | O gravador reiniciou e apagou o banco. Mande um PIX novo e guarde o id de novo (item 3). |
| No passo 3 ou 4, a chave sumiu antes da hora (o `TTL` deu `-2`) | Passou mais de 60 segundos: a chave expirou. Repita o item, mais rápido. |
| No console do H2: `Table "COMPROVANTE_ENTITY" not found` | A JDBC URL está errada. Use `jdbc:h2:mem:gravador`. |
| `cannot find symbol` com `Cacheable` ao subir a consulta | Faltou o `import org.springframework.cache.annotation.Cacheable;`. O erro aparece no meio do build, numa linha com `[ERROR]`. |
| Mudei o código e nada mudou | Reiniciou a consulta **com o `--build`**? |
| Os comandos com `$ID`, `$NOVO` ou `redis-cli` não funcionam | Use o **Git Bash**. No PowerShell, as variáveis e o atalho são diferentes. |
| `the input device is not a TTY` | Rode o mesmo comando com `-T` depois do `run` (ou do `exec`): `docker compose run --rm -T testes`. |
