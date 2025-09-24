# Imagen base con JDK 21
FROM eclipse-temurin:21-jdk

# Setear el directorio de trabajo
WORKDIR /app

# Copiar el archivo jar generado por Gradle/Maven
COPY build/libs/*.jar app.jar

# Exponer el puerto que usará la app
EXPOSE 8080

# Comando para correr la aplicación
ENTRYPOINT ["java","-jar","app.jar"]
