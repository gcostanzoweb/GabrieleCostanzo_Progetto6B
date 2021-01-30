FROM maven:3.5.2-jdk-8-alpine as mymaven
COPY heartbeatfd/src /home/app/src
COPY heartbeatfd/pom.xml /home/app
ARG TASK_DELAY
ARG PERIOD
ENV TASK_DELAY=$TASK_DELAY
ENV PERIOD=$PERIOD
RUN mvn -f /home/app/pom.xml clean install
RUN mvn -f /home/app/pom.xml clean package

FROM openjdk:8-jdk-alpine
WORKDIR /home
COPY --from=mymaven /home/app/target/heartbeatfd-1.0.jar ./heartbeatfd.jar
ARG TASK_DELAY
ARG PERIOD
ENV TASK_DELAY=$TASK_DELAY
ENV PERIOD=$PERIOD
ENTRYPOINT ["java","-jar","/home/heartbeatfd.jar"]
