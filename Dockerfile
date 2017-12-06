FROM docker.stammgruppe.eu/ubuntu:1

WORKDIR /gecko
COPY ./target/GECkO.jar .
COPY ./target/lib ./lib
RUN mkdir data
VOLUME ["/gecko/data"]
ENTRYPOINT ["java", "-jar", "GECkO.jar"]