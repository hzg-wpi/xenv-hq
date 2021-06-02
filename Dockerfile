FROM openjdk:11

RUN apt update && apt install -y dumb-init wait-for-it
MAINTAINER igor.khokhriakov@hzg.de

ARG JAR_FILE
ADD target/${JAR_FILE} /app/bin/hq.jar

ADD config /app/config

RUN addgroup --system javauser && adduser --disabled-password --no-create-home --shell /bin/false --ingroup javauser --gecos "" javauser
RUN chown -R javauser /app

USER javauser

WORKDIR /app

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["/bin/bash", "-c", "/usr/bin/wait-for-it $TANGO_HOST --strict --timeout=30 -- java -jar -server -DTANGO_HOST=$TANGO_HOST -Dmongodb.host=$MONGODB_HOST /app/bin/hq.jar dev"]
