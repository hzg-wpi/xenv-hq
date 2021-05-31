FROM adoptopenjdk/openjdk11:alpine-jre

RUN apk add dumb-init
MAINTAINER igor.khokhriakov@hzg.de

ARG TANGO_HOST=""
ARG MONGODB_HOST=""
ENV TANGO_HOST=$TANGO_HOST
ENV MONGODB_HOST=$MONGODB_HOST

ARG JAR_FILE
ADD target/${JAR_FILE} /app/bin/hq.jar

ADD config /app/config

RUN addgroup --system javauser && adduser -S -s /bin/false -G javauser javauser
RUN chown -R javauser /app

USER javauser

WORKDIR /app

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD java -jar -server -DTANGO_HOST=$TANGO_HOST -Dmongodb.host=$MONGODB_HOST /app/bin/hq.jar dev
