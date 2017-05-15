FROM openjdk:8-jdk

WORKDIR /code
ADD . /code/src

RUN cd src && ./gradlew build

RUN mv ./src/build/dist/edustor-assembler.jar .

RUN rm -rf src /root/.gradle

CMD java -jar edustor-assembler.jar