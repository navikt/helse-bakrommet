FROM gcr.io/distroless/java21-debian12:nonroot

ENV TZ="Europe/Oslo"
ENV JAVA_OPTS='-XX:MaxRAMPercentage=90'

WORKDIR /app

COPY bakrommet-bootstrap/build/deps/*.jar /app/
COPY bakrommet-bootstrap/build/libs/*.jar /app/

CMD ["app.jar"]
