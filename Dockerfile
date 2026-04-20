FROM eclipse-temurin:21-jre-noble AS builder
WORKDIR /builder
COPY target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-noble
COPY --from=builder /builder/extracted/dependencies/ /app/
COPY --from=builder /builder/extracted/snapshot-dependencies/ /app/
COPY --from=builder /builder/extracted/application/ /app/
WORKDIR /data
ENTRYPOINT ["java", "-jar", "/app/app.jar"]