# syntax=docker/dockerfile:1
#
# Uma imagem por serviço. Quem escolhe o serviço é o docker-compose.yml, com MODULO=emissor,
# MODULO=gravador, MODULO=consulta ou MODULO=reacoes. Com isso, ninguém precisa instalar Java nem Maven na máquina.

# 1º estágio: compila o projeto com Maven e Java 21.
FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG MODULO
WORKDIR /projeto
COPY . .
# O cache guarda as dependências que o Maven baixa. A primeira vez demora uns minutos;
# depois, recompilar leva poucos segundos.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -pl comprovante-${MODULO} -am package -DskipTests

# 2º estágio: só o Java, para rodar o .jar que o 1º estágio gerou.
FROM eclipse-temurin:21-jre
ARG MODULO
WORKDIR /app
COPY --from=build /projeto/comprovante-${MODULO}/target/comprovante-${MODULO}-*.jar app.jar
# Desliga o cache de nomes do Java. Sem isso, depois de religar o gravador, o emissor
# continuaria achando, por mais alguns segundos, que o nome "gravador" não existe.
RUN printf 'networkaddress.cache.ttl=0\nnetworkaddress.cache.negative.ttl=0\n' > sem-cache-de-nomes.properties
ENTRYPOINT ["java", "-Djava.security.properties=/app/sem-cache-de-nomes.properties", "-jar", "app.jar"]
