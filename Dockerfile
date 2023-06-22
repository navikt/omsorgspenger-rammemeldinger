FROM cgr.dev/chainguard/jre:openjdk17
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-rammemeldinger

COPY build/libs/app.jar /app/app.jar
WORKDIR /app
CMD [ "-jar", "app.jar" ]