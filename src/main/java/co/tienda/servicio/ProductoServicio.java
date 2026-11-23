package co.tienda.servicio;

import co.tienda.modelo.Producto;
import co.tienda.persistencia.ArchivoProductos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lógica del CRUD. Mantiene la lista en memoria y la persiste en el archivo
 * después de cada operación que la modifica.
 */
public class ProductoServicio {

    private final ArchivoProductos archivo;
    private final List<Producto> productos;

    public ProductoServicio(ArchivoProductos archivo) throws IOException {
        this.archivo = archivo;
        this.productos = new ArrayList<>(archivo.cargar());
    }

    // ---------- CREATE ----------
    public void crear(Producto p) throws IOException {
        if (buscar(p.codigo()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un producto con el código " + p.codigo());
        }
        productos.add(p);
        archivo.guardar(productos);
    }

    // ---------- READ ----------
    public List<Producto> listar() {
        return List.copyOf(productos);
    }

    public Optional<Producto> buscar(String codigo) {
        return productos.stream()
                .filter(p -> p.codigo().equalsIgnoreCase(codigo.trim()))
                .findFirst();
    }

    // ---------- UPDATE ----------
    /** Como el record es inmutable, se reemplaza el objeto viejo por uno nuevo. */
    public void actualizar(String codigoOriginal, Producto nuevo) throws IOException {
        int i = indiceDe(codigoOriginal);
        if (i < 0) {
            throw new IllegalArgumentException("No existe el producto " + codigoOriginal);
        }
        if (!nuevo.codigo().equalsIgnoreCase(codigoOriginal) && buscar(nuevo.codigo()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un producto con el código " + nuevo.codigo());
        }
        productos.set(i, nuevo);
        archivo.guardar(productos);
    }

    // ---------- DELETE ----------
    /** Elimina el producto y lo devuelve (útil para poder "deshacer"). */
    public Producto eliminar(String codigo) throws IOException {
        int i = indiceDe(codigo);
        if (i < 0) {
            throw new IllegalArgumentException("No existe el producto " + codigo);
        }
        Producto eliminado = productos.remove(i);
        archivo.guardar(productos);
        return eliminado;
    }

    // ---------- Estadísticas ----------
    public double valorTotalInventario() {
        return productos.stream().mapToDouble(Producto::valorInventario).sum();
    }

    public int unidadesTotales() {
        return productos.stream().mapToInt(Producto::stock).sum();
    }

    /** Productos agotados o con stock bajo. */
    public long productosPorReabastecer() {
        return productos.stream().filter(p -> p.agotado() || p.stockBajo()).count();
    }

    private int indiceDe(String codigo) {
        for (int i = 0; i < productos.size(); i++) {
            if (productos.get(i).codigo().equalsIgnoreCase(codigo.trim())) return i;
        }
        return -1;
    }
}
