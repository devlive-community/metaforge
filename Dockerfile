FROM eclipse-temurin:11-jdk-focal

LABEL maintainer="community@devlive.org" \
      description="MetaForge Server Image"

# Add metaforge
RUN mkdir -p /opt/app
ADD --chown=root:root dist/metaforge-server-*.tar.gz /opt/app/
RUN cd /opt/app && \
    mv metaforge-server-* metaforge

WORKDIR /opt/app/metaforge

EXPOSE 9096

ENTRYPOINT ["sh", "./bin/debug.sh"]