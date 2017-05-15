FROM openjdk:8-jdk

ADD build/dist/edustor-assembler.jar .

CMD java -jar edustor-assembler.jar