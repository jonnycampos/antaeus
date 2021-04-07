FROM adoptopenjdk/openjdk11:latest

RUN apt-get update

COPY . /anteus
WORKDIR /anteus

# When the container starts: build, test and run the app.
CMD ./gradlew build --settings-file=settings.gradle.scheduler.kts && ./gradlew run --settings-file=settings.gradle.scheduler.kts