FROM registry.redhat.io/ubi10 AS ubi-micro-build

ARG INFINISPAN_VERSION=15.2.4.Final
ARG INFINISPAN_DIST=https://github.com/infinispan/infinispan/releases/download/$INFINISPAN_VERSION/infinispan-server-$INFINISPAN_VERSION.zip

RUN dnf install -y unzip

ADD $INFINISPAN_DIST /tmp/infinispan/
ADD https://dl.fedoraproject.org/pub/epel/epel-release-latest-10.noarch.rpm /tmp/infinispan

RUN (cd /tmp/infinispan && \
    unzip /tmp/infinispan/infinispan-*.zip && \
    rm /tmp/infinispan/infinispan-*.zip) || true

RUN mv /tmp/infinispan/infinispan-* /opt/infinispan && mkdir -p /opt/infinispan/data
RUN chmod -R g+rwX /opt/infinispan

ADD ubi-null.sh /tmp/
#RUN bash /tmp/ubi-null.sh java-latest-openjdk-headless glibc-langpack-en findutils
RUN bash /tmp/ubi-null.sh glibc-langpack-en findutils grep sed

FROM registry.redhat.io/ubi10-micro
ENV LANG en_US.UTF-8

# Flag for determining app is running in container
ENV KC_RUN_IN_CONTAINER true

COPY --from=ubi-micro-build /tmp/null/rootfs/ /
COPY --from=ubi-micro-build --chown=1000:0 /opt/infinispan /opt/infinispan

RUN echo "infinispan:x:0:root" >> /etc/group && \
    echo "infinispan:x:1000:0:infinispan user:/opt/infinispan:/sbin/nologin" >> /etc/passwd

USER 1000

EXPOSE 11222 11221 11223 2157 46655 57600 7800 7900 8080 8443

ENTRYPOINT [ "/opt/infinispan/bin/server.sh" ]

# common labels
ARG INFINISPAN_VERSION
ARG INFINISPAN_URL="https://infinispan.org/"
ARG INFINISPAN_TAGS="infinispan datagrid cache"
ARG INFINISPAN_MAINTAINER=${INFINISPAN_URL}
ARG INFINISPAN_VENDOR=${INFINISPAN_MAINTAINER}

LABEL maintainer=${INFINISPAN_MAINTAINER} \
      vendor=${INFINISPAN_VENDOR} \
      version=${INFINISPAN_VERSION} \
      url=${INFINISPAN_URL} \
      io.openshift.tags=${INFINISPAN_TAGS} \
      release="" \
      vcs-ref="" \
      com.redhat.build-host="" \
      com.redhat.component="" \
      com.redhat.license_terms=""

# server specific
ARG INFINISPAN_SERVER_DISPLAY_NAME="Infinispan Server"
ARG INFINISPAN_SERVER_IMAGE_NAME="infinispan"
ARG INFINISPAN_SERVER_DESCRIPTION="${INFINISPAN_SERVER_DISPLAY_NAME} Image"

LABEL name=${INFINISPAN_SERVER_IMAGE_NAME} \
      description=${INFINISPAN_SERVER_DESCRIPTION} \
      summary=${INFINISPAN_SERVER_DESCRIPTION} \
      io.k8s.display-name=${INFINISPAN_SERVER_DISPLAY_NAME} \
      io.k8s.description=${INFINISPAN_SERVER_DESCRIPTION}

# oci
ARG INFINISPAN_SOURCE="https://github.com/infinispan/infinispan"
ARG INFINISPAN_DOCS=${INFINISPAN_URL}documentation

LABEL org.opencontainers.image.title=${INFINISPAN_SERVER_DISPLAY_NAME} \
      org.opencontainers.image.url=${INFINISPAN_URL} \
      org.opencontainers.image.source=${INFINISPAN_SOURCE} \
      org.opencontainers.image.description=${INFINISPAN_DESCRIPTION} \
      org.opencontainers.image.documentation=${INFINISPAN_DOCS}