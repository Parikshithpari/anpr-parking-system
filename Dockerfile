FROM openjdk:17
WORKDIR /app
COPY target/ANPR-Parking-System-0.0.1-SNAPSHOT.jar /app/ANPR-Parking-System.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "ANPR-Parking-System.jar"]