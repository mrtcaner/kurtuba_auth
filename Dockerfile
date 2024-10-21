FROM --platform=linux/arm/v7 eclipse-temurin:17-jre-jammy
EXPOSE 8080
COPY target/${project.name}-${project.version}.jar /usr/local/app.jar
CMD java -jar -Dspring.profiles.active=dev /usr/local/app.jar
