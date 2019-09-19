FROM openjdk:11

RUN mkdir /gecko
WORKDIR /gecko
COPY ./target/GECko.jar .
COPY ./target/lib ./lib
RUN mkdir data
ENTRYPOINT ["java", "-jar", "GECko.jar"]
