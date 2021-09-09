FROM gradle:7.2.0-jdk11-openj9
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

CMD ["./gradlew", "run"]