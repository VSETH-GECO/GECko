FROM openjdk:11

RUN mkdir /gecko
WORKDIR /gecko
COPY ./target/GECkO.jar .
COPY ./target/lib ./lib
RUN mkdir data
ENTRYPOINT ["java", "-jar", "GECkO.jar"]
