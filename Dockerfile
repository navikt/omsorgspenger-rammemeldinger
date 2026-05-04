FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.04.30.1354Z
LABEL org.opencontainers.image.source=https://github.com/navikt/omsorgspenger-rammemeldinger

COPY build/libs/app.jar /app/app.jar
WORKDIR /app

USER nonroot

CMD [ "-jar", "app.jar" ]
