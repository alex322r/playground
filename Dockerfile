# Usa una imagen de base de Java
FROM openjdk:22

# Copia el código en el contenedor
COPY Main.java /usr/src/playground/Main.java

# Cambia al directorio donde está el código
WORKDIR /usr/src/playground

# Compila el archivo Main.java
RUN javac Main.java

# Ejecuta el programa compilado
CMD ["java", "Main"]

