package co.tienda.persistencia;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

/**
 * Maneja las imágenes de los productos.
 *
 * Cuando el usuario elige una imagen, se COPIA a la carpeta "imagenes" del
 * programa y en el record solo se guarda el nombre del archivo. Así, si el
 * usuario borra o mueve la foto original, el producto no pierde su imagen.
 */
public class GestorImagenes {

    private static final Set<String> EXTENSIONES = Set.of(".png", ".jpg", ".jpeg", ".gif", ".bmp");

    private final Path carpeta;

    public GestorImagenes(String carpeta) throws IOException {
        this.carpeta = Path.of(carpeta);
        Files.createDirectories(this.carpeta);
    }

    /** Copia la imagen a la carpeta del programa y devuelve el nombre con que quedó. */
    public String importar(File origen, String codigoProducto) throws IOException {
        if (!esImagen(origen)) {
            throw new IllegalArgumentException("El archivo no es una imagen válida (png, jpg, gif o bmp)");
        }
        String base = codigoProducto.trim().toUpperCase().replaceAll("[^A-Z0-9-]", "_");
        String nombre = base + "_" + System.currentTimeMillis() + extension(origen.getName());
        Files.copy(origen.toPath(), carpeta.resolve(nombre), StandardCopyOption.REPLACE_EXISTING);
        return nombre;
    }

    public Path ruta(String nombre) {
        return nombre == null ? null : carpeta.resolve(nombre);
    }

    public boolean existe(String nombre) {
        return nombre != null && Files.exists(carpeta.resolve(nombre));
    }

    /** Borra la imagen de la carpeta (si existe). */
    public void eliminar(String nombre) {
        if (nombre == null) return;
        try {
            Files.deleteIfExists(carpeta.resolve(nombre));
        } catch (IOException ignorada) {
            // Si no se puede borrar, solo queda un archivo huérfano; no es grave.
        }
    }

    public static boolean esImagen(File f) {
        return f != null && f.isFile() && EXTENSIONES.contains(extension(f.getName()));
    }

    private static String extension(String nombre) {
        int i = nombre.lastIndexOf('.');
        return i < 0 ? "" : nombre.substring(i).toLowerCase(Locale.ROOT);
    }
}
