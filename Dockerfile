# Build (Public ECR = espejo oficial; evita CDN docker-images-prod de Docker Hub en redes restrictivas)
FROM public.ecr.aws/docker/library/maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

# Run
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -g 1000 app && adduser -u 1000 -G app -D app
COPY --from=build /src/target/workflow-engine-*.jar /app/app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
