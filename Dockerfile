FROM openjdk/java:11
VOLUME /tmp
COPY target/*.jar /app/marketing-service.jar
EXPOSE 16671
CMD ["java", "-jar", "/app/marketing-service.jar"]