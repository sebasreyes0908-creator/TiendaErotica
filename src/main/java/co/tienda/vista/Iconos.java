package co.tienda.vista;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

/** Íconos vectoriales (SVG de 24x24) para no depender de archivos de imagen. */
final class Iconos {

    static final String MAS = "M11 5h2v14h-2zM5 11h14v2H5z";
    static final String MENOS = "M5 11h14v2H5z";
    static final String BUSCAR = "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";
    static final String CERRAR = "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z";
    static final String CHECK = "M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z";
    static final String ALERTA = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";
    static final String INFO = "M11 7h2v2h-2zm0 4h2v6h-2zm1-9C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z";
    static final String BASURA = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
    static final String IMAGEN = "M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z";
    static final String CORAZON = "M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z";
    static final String CANDADO = "M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1s3.1 1.39 3.1 3.1v2z";
    static final String EDITAR = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";

    private Iconos() { }

    /** Crea un ícono del tamaño indicado (en px). El color se define por CSS. */
    static Node crear(String ruta, double tamano, String claseCss) {
        SVGPath svg = new SVGPath();
        svg.setContent(ruta);
        svg.getStyleClass().add("icono");
        if (claseCss != null) svg.getStyleClass().add(claseCss);

        // Caja invisible de 24x24 para que todos los íconos ocupen lo mismo
        Rectangle caja = new Rectangle(24, 24, Color.TRANSPARENT);
        Group interno = new Group(caja, svg);
        double escala = tamano / 24.0;
        interno.setScaleX(escala);
        interno.setScaleY(escala);
        return new Group(interno);
    }
}
