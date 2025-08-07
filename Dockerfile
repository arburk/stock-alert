FROM eclipse-temurin:21-jre-alpine

COPY target/stock-alert-*.jar /stock-alert.jar

ENTRYPOINT ["java", "-jar", "/stock-alert.jar"]
