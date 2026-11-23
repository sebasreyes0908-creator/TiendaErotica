package co.tienda.servicio;

import co.tienda.modelo.Categoria;
import co.tienda.modelo.Producto;

import java.io.IOException;
import java.util.List;

/** Productos de ejemplo que se cargan la primera vez que se abre el programa. */
public final class DatosEjemplo {

    private DatosEjemplo() { }

    public static void cargar(ProductoServicio servicio) throws IOException {
        List<Producto> ejemplos = List.of(
                new Producto("VB-001", "Vibrador clásico recargable", Categoria.JUGUETES, "Lelo", 189900, 10, true, null),
                new Producto("VB-002", "Gel lubricante base agua 200 ml", Categoria.LUBRICANTES, "Sico", 49900, 8, true, null),
                new Producto("VB-003", "Conjunto de encaje negro", Categoria.LENCERIA, "Leonisa", 129900, 6, true, null),
                new Producto("VB-004", "Aceite de masaje de vainilla", Categoria.ACEITES_MASAJE, "Velvet", 42000, 3, true, null),
                new Producto("VB-005", "Preservativos ultra delgados x12", Categoria.PRESERVATIVOS, "Durex", 28500, 40, false, null),
                new Producto("VB-006", "Antifaz de satín", Categoria.ACCESORIOS, "Velvet", 19900, 12, false, null),
                new Producto("VB-007", "Dados para parejas", Categoria.JUEGOS_PAREJA, "Velvet", 24900, 0, false, null),
                new Producto("VB-008", "Vela de masaje aroma canela", Categoria.ACEITES_MASAJE, "Velvet", 54900, 2, true, null)
        );
        for (Producto p : ejemplos) {
            servicio.crear(p);
        }
    }
}
