# --- construcción ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -q -B package -DskipTests
# --- ejecución ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# El usuario del servicio es dueño de la carpeta de los PDF: un volumen nuevo hereda ese dueño al montarse
RUN useradd --system --uid 10001 appuser \
 && mkdir -p /datos/roles-pago \
 && chown appuser /datos/roles-pago
COPY --from=build /app/target/*.jar app.jar
# Certificado intermedio (RapidSSL/DigiCert) con el que Java verifica al AD y a la API de correo: es PÚBLICO, puede viajar dentro de la imagen. Vence el 2/11/2027
COPY certs/ca-ad.crt /app/certs/ca-ad.crt
USER appuser
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
