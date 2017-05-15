FROM openjdk:8-jdk

ADD build/dist/edustor-assembler.jar .

HEALTHCHECK CMD curl -f http://localhost:8080/version
CMD java -jar edustor-assembler.jar