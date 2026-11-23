package co.tienda.modelo;

import java.io.Serial;
import java.io.Serializable;

/**
 * Producto de la tienda, modelado como record.
 *
 * Los records son inmutables: para "actualizar" un producto se crea uno nuevo
 * con los datos cambiados (ver los métodos conStock / conImagen) y se
 * reemplaza en la lista.
 *
 * Al implementar Serializable, Java puede escribir el record en un archivo con
 * ObjectOutputStream. Al deserializar, Java llama al constructor canónico, así
 * que las validaciones del constructor compacto también se aplican al leer.
 *
 * @param imagen nombre del archivo de imagen dentro de la carpeta "imagenes"
 *               (null si el producto no tiene imagen). Se guarda solo el nombre
 *               y no la imagen, para que el archivo .dat no crezca.
 */
public record Producto(
        String codigo,
        String nombre,
        Categoria categoria,
        String marca,
        double precio,
        int stock,
        boolean discreto,
        String imagen
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int STOCK_MINIMO = 3;

    /** Constructor compacto: valida y normaliza los datos. */
    public Producto {
        if (codigo == null || codigo.isBlank())
            throw new IllegalArgumentException("El código es obligatorio");
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("El nombre es obligatorio");
        if (categoria == null)
            throw new IllegalArgumentException("Seleccione una categoría");
        if (precio <= 0)
            throw new IllegalArgumentException("El precio debe ser mayor que 0");
        if (stock < 0)
            throw new IllegalArgumentException("El stock no puede ser negativo");

        codigo = codigo.trim().toUpperCase();
        nombre = nombre.trim();
        marca = (marca == null || marca.isBlank()) ? "Genérica" : marca.trim();
        imagen = (imagen == null || imagen.isBlank()) ? null : imagen.trim();
    }

    /** "Wither": devuelve una copia con otro stock (el record no tiene setters). */
    public Producto conStock(int nuevoStock) {
        return new Producto(codigo, nombre, categoria, marca, precio, nuevoStock, discreto, imagen);
    }

    /** Copia con otra imagen. */
    public Producto conImagen(String nuevaImagen) {
        return new Producto(codigo, nombre, categoria, marca, precio, stock, discreto, nuevaImagen);
    }

    public boolean agotado() {
        return stock == 0;
    }

    public boolean stockBajo() {
        return stock > 0 && stock <= STOCK_MINIMO;
    }

    public double valorInventario() {
        return precio * stock;
    }
}
