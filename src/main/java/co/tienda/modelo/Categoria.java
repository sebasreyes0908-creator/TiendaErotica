package co.tienda.modelo;

/**
 * Categorías de productos. Cada una tiene dos colores que se usan para
 * dibujar la imagen por defecto cuando el producto no tiene foto.
 */
public enum Categoria {
    JUGUETES("Juguetes", "#d81b60", "#6a1b9a"),
    LENCERIA("Lencería", "#880e4f", "#1a0a10"),
    LUBRICANTES("Lubricantes", "#00838f", "#283593"),
    ACEITES_MASAJE("Aceites de masaje", "#ef6c00", "#ad1457"),
    JUEGOS_PAREJA("Juegos de pareja", "#8e24aa", "#e91e63"),
    PRESERVATIVOS("Preservativos", "#1565c0", "#6a1b9a"),
    ACCESORIOS("Accesorios", "#b71c1c", "#3e1a2b");

    private final String etiqueta;
    private final String colorA;
    private final String colorB;

    Categoria(String etiqueta, String colorA, String colorB) {
        this.etiqueta = etiqueta;
        this.colorA = colorA;
        this.colorB = colorB;
    }

    public String colorA() { return colorA; }
    public String colorB() { return colorB; }

    @Override
    public String toString() {
        return etiqueta;
    }
}
