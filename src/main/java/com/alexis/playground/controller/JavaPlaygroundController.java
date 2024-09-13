package com.alexis.playground.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class JavaPlaygroundController {
    @CrossOrigin(origins = "*")
    @PostMapping("/execute")
    public ResponseEntity<String> ejecutarCodigo(@RequestBody Map<String, String> request) {
        String codigo = request.get("codigo");

        // Guardar el código en un archivo

        String nombreArchivo = "Main.java";
        try (FileWriter writer = new FileWriter(nombreArchivo)) {
            writer.write(codigo);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al guardar el código");
        }

        // Verificar la sintaxis del código con javac antes de intentar compilar en Docker
        try {
            ProcessBuilder syntaxCheckPb = new ProcessBuilder("javac", nombreArchivo);
            Process syntaxCheckProcess = syntaxCheckPb.start();
            int syntaxExitCode = syntaxCheckProcess.waitFor();

            // Leer la salida del verificador de sintaxis
            StringBuilder syntaxErrors = new StringBuilder();
            try (BufferedReader syntaxErrorReader = new BufferedReader(new InputStreamReader(syntaxCheckProcess.getErrorStream()))) {
                String linea;
                while ((linea = syntaxErrorReader.readLine()) != null) {
                    syntaxErrors.append(linea).append("\n");
                }
            }

            if (syntaxExitCode != 0) {
                System.out.println("Errores de sintaxis encontrados:");
                System.out.println(syntaxErrors.toString());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Errores de sintaxis: " + syntaxErrors.toString());
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al verificar la sintaxis: " + e.getMessage());
        }

        // Construir y ejecutar el contenedor Docker
        try {
            // Compilar y ejecutar el código en Docker
            ProcessBuilder pb = new ProcessBuilder("docker", "build", "-t", "java-sandbox", ".");
            Process buildProcess = pb.start();
            int buildExitCode = buildProcess.waitFor();

            // Capturar la salida y los errores durante la construcción
            BufferedReader buildErrorReader = new BufferedReader(new InputStreamReader(buildProcess.getErrorStream()));
            StringBuilder buildErrors = new StringBuilder();
            String linea;
            while ((linea = buildErrorReader.readLine()) != null) {
                buildErrors.append(linea).append("\n");
            }

            if (buildExitCode != 0) {
                System.out.println("Error en la construcción del contenedor Docker:");
                System.out.println(buildErrors.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al construir el contenedor Docker: " + buildErrors.toString());
            }

// Continuar con la ejecución del contenedor si la construcción fue exitosa
            ProcessBuilder runPb = new ProcessBuilder("docker", "run", "--rm", "--memory=256m", "--cpus=1.0", "java-sandbox");
            Process runProcess = runPb.start();
            int runExitCode = runProcess.waitFor();

            // Leer la salida del proceso en ejecución
            BufferedReader reader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
            StringBuilder salida = new StringBuilder();
            while ((linea = reader.readLine()) != null) {
                salida.append(linea).append("\n");
            }

            if (runExitCode != 0) {
                BufferedReader runErrorReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                StringBuilder runErrors = new StringBuilder();
                while ((linea = runErrorReader.readLine()) != null) {
                    runErrors.append(linea).append("\n");
                }
                System.out.println("Error durante la ejecución del contenedor Docker:");
                System.out.println(runErrors.toString());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error durante la ejecución: " + runErrors.toString());
            }

// Retornar la salida si todo es exitoso
            return ResponseEntity.ok(salida.toString());


        } catch (Exception e) {
            System.out.println("erro");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al ejecutar el código");
        }
    }
}

