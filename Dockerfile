FROM eclipse-temurin:21-jre-noble AS builder
WORKDIR /builder
COPY target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-noble
# the ICMP health check of the SDK runs the system ping, which the base image lacks
RUN apt-get update \
    && apt-get install -y --no-install-recommends iputils-ping \
    && rm -rf /var/lib/apt/lists/*
COPY --from=builder /builder/extracted/dependencies/ /app/
COPY --from=builder /builder/extracted/snapshot-dependencies/ /app/
COPY --from=builder /builder/extracted/application/ /app/
WORKDIR /data
ENTRYPOINT ["java", "-jar", "/app/app.jar"]