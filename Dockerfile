FROM openjdk:17-slim as build

RUN apt-get update && apt-get install -y wget curl unzip git && rm -rf /var/lib/apt/lists/*

RUN wget https://github.com/sbt/sbt/releases/download/v1.10.7/sbt-1.10.7.zip -O sbt.zip

RUN unzip sbt.zip -d /opt && rm sbt.zip

ENV PATH="/opt/sbt/bin:${PATH}"

WORKDIR /app

COPY . /app

RUN sbt update && sbt assembly

COPY wait-for-it.sh /app/wait-for-it.sh
RUN chmod +x /app/wait-for-it.sh

FROM openjdk:17-slim

COPY --from=build /app/target/scala-2.13/project-assembly-0.1.0-SNAPSHOT.jar /app/app.jar
COPY --from=build /app/wait-for-it.sh /app/wait-for-it.sh

WORKDIR /app

CMD ["./wait-for-it.sh", "db:5432", "--", "java", "-jar", "/app/app.jar"]
