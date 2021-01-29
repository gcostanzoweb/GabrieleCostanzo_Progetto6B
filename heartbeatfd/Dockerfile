FROM maven:3.5.2-jdk-8-alpine as mymaven
COPY heartbeatfd/src /home/app/src
COPY heartbeatfd/pom.xml /home/app
RUN mvn -f /home/app/pom.xml package

FROM openjdk:8-jdk-alpine
WORKDIR /home
COPY --from=mymaven /home/app/target/heartbeatfd-1.0.jar ./heartbeatfd.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/home/heartbeatfd.jar"]
