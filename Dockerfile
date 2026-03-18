FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.03.16.1330Z
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-rammemeldinger

COPY build/libs/app.jar /app/app.jar
WORKDIR /app

USER nonroot

CMD [ "-jar", "app.jar" ]
