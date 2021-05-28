FROM adoptopenjdk/openjdk11:alpine-jre

RUN apk add dumb-init
MAINTAINER igor.khokhriakov@hzg.de

ENV TANGO_HOST=""
ENV MONGODB_HOST=""

ARG JAR_FILE
ADD target/${JAR_FILE} /usr/share/hq.jar

RUN addgroup --system javauser && adduser -S -s /bin/false -G javauser javauser

USER javauser

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD java -jar -server -DTANGO_HOST=${TANGO_HOST} /usr/share/hq.jar dev
