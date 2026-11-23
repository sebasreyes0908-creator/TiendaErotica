package co.tienda.vista;

import co.tienda.modelo.Categoria;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;

/** Utilidades para mostrar imágenes de productos. */
final class Imagenes {

    private Imagenes() { }

    /**
     * Muestra la imagen llenando todo el espacio (como "object-fit: cover" en CSS):
     * se recorta lo que sobra en vez de deformarla.
     */
    static ImageView cubrir(Image img, double ancho, double alto) {
        ImageView vista = new ImageView(img);
        vista.setFitWidth(ancho);
        vista.setFitHeight(alto);
        vista.setSmooth(true);

        Runnable ajustar = () -> {
            double iw = img.getWidth(), ih = img.getHeight();
            if (iw <= 0 || ih <= 0) return;
            double escala = Math.max(ancho / iw, alto / ih);
            double vw = ancho / escala, vh = alto / escala;
            vista.setViewport(new Rectangle2D((iw - vw) / 2, (ih - vh) / 2, vw, vh));
        };
        if (img.getProgress() >= 1) {
            ajustar.run();
        } else {
            img.progressProperty().addListener((obs, a, b) -> {
                if (b.doubleValue() >= 1) ajustar.run();
            });
        }
        return vista;
    }

    /**
     * Imagen por defecto para productos sin foto: degradado con los colores de
     * la categoría, un corazón de fondo y las iniciales del producto.
     */
    static StackPane porDefecto(String nombre, Categoria categoria, double ancho, double alto) {
        String c1 = categoria == null ? "#8e1b3e" : categoria.colorA();
        String c2 = categoria == null ? "#2a0c18" : categoria.colorB();

        StackPane fondo = new StackPane();
        fondo.setMinSize(ancho, alto);
        fondo.setPrefSize(ancho, alto);
        fondo.setMaxSize(ancho, alto);
        fondo.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web(c1)), new Stop(1, Color.web(c2))),
                CornerRadii.EMPTY, javafx.geometry.Insets.EMPTY)));

        Node corazon = Iconos.crear(Iconos.CORAZON, alto * 0.95, "icono-fondo");
        corazon.setRotate(-14);
        corazon.setTranslateX(ancho * 0.22);
        corazon.setTranslateY(alto * 0.12);

        Label iniciales = new Label(iniciales(nombre));
        iniciales.getStyleClass().add("iniciales");
        iniciales.setStyle("-fx-font-size: " + Math.round(alto * 0.3) + "px;");

        fondo.getChildren().addAll(corazon, iniciales);
        return fondo;
    }

    private static String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) return "";
        String[] partes = nombre.trim().split("\\s+");
        String r = partes[0].substring(0, 1);
        if (partes.length > 1) r += partes[1].substring(0, 1);
        return r.toUpperCase();
    }
}
