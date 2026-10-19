package com.a5.reuse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reutilizado de programa3a: centenoProgram3.OutFile; solo cambia el paquete. */
public class OutFile {
    public void escribir(String rutaArchivo, String contenido) throws IOException {
        escribir(Path.of(rutaArchivo), contenido);
    }

    public void escribir(Path rutaArchivo, String contenido) throws IOException {
        Files.writeString(rutaArchivo, contenido);
    }
}
