package co.tienda.persistencia;

import co.tienda.modelo.Producto;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Guarda y lee la lista de productos en un archivo binario usando
 * serialización de objetos (ObjectOutputStream / ObjectInputStream).
 */
public class ArchivoProductos {

    private final Path ruta;

    public ArchivoProductos(String nombreArchivo) {
        this.ruta = Path.of(nombreArchivo);
    }

    /** Escribe toda la lista en el archivo (sobrescribe lo anterior). */
    public void guardar(List<Producto> productos) throws IOException {
        // Se escribe primero a un archivo temporal y luego se reemplaza,
        // así si algo falla a mitad de camino no se daña el archivo original.
        Path temporal = Path.of(ruta + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(Files.newOutputStream(temporal)))) {
            out.writeObject(new ArrayList<>(productos));
        }
        Files.move(temporal, ruta, StandardCopyOption.REPLACE_EXISTING);
    }

    /** Lee la lista del archivo. Si no existe todavía, devuelve una lista vacía. */
    @SuppressWarnings("unchecked")
    public List<Producto> cargar() throws IOException {
        if (!existe()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(ruta)))) {
            return (List<Producto>) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException("El archivo tiene un formato desconocido", e);
        }
    }

    public boolean existe() {
        return Files.exists(ruta);
    }

    public Path getRuta() {
        return ruta;
    }
}
