package co.tienda.vista;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Notificaciones tipo "toast" que bajan deslizándose en la parte superior
 * central de la ventana y se cierran solas. Tienen una barra que muestra el tiempo que les
 * queda; si se pone el mouse encima, el tiempo se pausa.
 */
public class Notificaciones {

    public enum Tipo {
        EXITO(Iconos.CHECK), ERROR(Iconos.CERRAR), ADVERTENCIA(Iconos.ALERTA), INFO(Iconos.INFO);

        final String icono;
        Tipo(String icono) { this.icono = icono; }
    }

    private static final double ANCHO = 380;
    private static final int MAXIMO_VISIBLES = 4;
    private static final String AL_CERRAR = "alCerrar";

    private final VBox contenedor = new VBox(10);

    public Notificaciones(StackPane capa) {
        contenedor.setPadding(new Insets(20));
        contenedor.setAlignment(Pos.TOP_CENTER);
        contenedor.setPickOnBounds(false);
        contenedor.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(contenedor, Pos.TOP_CENTER);
        capa.getChildren().add(contenedor);
    }

    public void mostrar(Tipo tipo, String titulo, String mensaje) {
        mostrar(tipo, titulo, mensaje, null, null, null);
    }

    /**
     * @param textoAccion texto de un botón opcional (por ejemplo "Deshacer")
     * @param accion      qué hacer si se pulsa ese botón
     * @param alCerrar    se ejecuta una sola vez cuando la notificación desaparece
     */
    public void mostrar(Tipo tipo, String titulo, String mensaje,
                        String textoAccion, Runnable accion, Runnable alCerrar) {
        // Si hay demasiadas, se quita la más vieja
        while (contenedor.getChildren().size() >= MAXIMO_VISIBLES) {
            Node vieja = contenedor.getChildren().remove(0);
            ejecutarAlCerrar(vieja);
        }

        StackPane icono = new StackPane(Iconos.crear(tipo.icono, 18, "toast-icono-svg"));
        icono.getStyleClass().add("toast-icono");

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("toast-titulo");
        lblTitulo.setWrapText(true);
        Label lblMensaje = new Label(mensaje);
        lblMensaje.getStyleClass().add("toast-mensaje");
        lblMensaje.setWrapText(true);
        VBox textos = new VBox(2, lblTitulo, lblMensaje);
        HBox.setHgrow(textos, Priority.ALWAYS);

        Button btnCerrar = new Button();
        btnCerrar.setGraphic(Iconos.crear(Iconos.CERRAR, 14, "toast-cerrar-svg"));
        btnCerrar.getStyleClass().add("toast-cerrar");
        btnCerrar.setMinWidth(Region.USE_PREF_SIZE);

        HBox fila = new HBox(12, icono, textos);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(14, 10, 14, 14));

        Button btnAccion = null;
        if (textoAccion != null && accion != null) {
            btnAccion = new Button(textoAccion);
            btnAccion.getStyleClass().add("toast-accion");
            btnAccion.setMinWidth(Region.USE_PREF_SIZE);
            fila.getChildren().add(btnAccion);
        }
        fila.getChildren().add(btnCerrar);

        Rectangle barra = new Rectangle(ANCHO, 3);
        barra.getStyleClass().add("toast-barra");

        VBox toast = new VBox(fila, barra);
        toast.getStyleClass().addAll("toast", "toast-" + tipo.name().toLowerCase());
        toast.setPrefWidth(ANCHO);
        toast.setMaxWidth(ANCHO);
        Rectangle recorte = new Rectangle();
        recorte.setArcWidth(24);
        recorte.setArcHeight(24);
        recorte.widthProperty().bind(toast.widthProperty());
        recorte.heightProperty().bind(toast.heightProperty());
        toast.setClip(recorte);
        toast.getProperties().put(AL_CERRAR, alCerrar);

        contenedor.getChildren().add(toast);

        // Animación de entrada: baja desde arriba y aparece
        toast.setTranslateY(-90);
        toast.setOpacity(0);
        TranslateTransition entra = new TranslateTransition(Duration.millis(380), toast);
        entra.setToY(0);
        entra.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.3, 1));
        FadeTransition aparece = new FadeTransition(Duration.millis(250), toast);
        aparece.setToValue(1);
        new ParallelTransition(entra, aparece).play();

        // Tiempo de vida: la barra se va vaciando
        Duration duracion = Duration.seconds(btnAccion != null ? 6 : 4);
        Timeline vida = new Timeline(new KeyFrame(duracion, new KeyValue(barra.widthProperty(), 0)));
        vida.setOnFinished(e -> cerrar(toast));
        vida.play();

        toast.setOnMouseEntered(e -> vida.pause());
        toast.setOnMouseExited(e -> vida.play());
        btnCerrar.setOnAction(e -> { vida.stop(); cerrar(toast); });
        if (btnAccion != null) {
            btnAccion.setOnAction(e -> { vida.stop(); accion.run(); cerrar(toast); });
        }
    }

    private void cerrar(VBox toast) {
        if (toast.getProperties().containsKey("cerrando")) return;
        toast.getProperties().put("cerrando", true);
        ejecutarAlCerrar(toast);

        TranslateTransition sale = new TranslateTransition(Duration.millis(260), toast);
        sale.setToX(ANCHO + 40);   // sale hacia la derecha
        sale.setInterpolator(Interpolator.EASE_IN);
        FadeTransition desaparece = new FadeTransition(Duration.millis(260), toast);
        desaparece.setToValue(0);
        ParallelTransition salida = new ParallelTransition(sale, desaparece);
        salida.setOnFinished(e -> contenedor.getChildren().remove(toast));
        salida.play();
    }

    private void ejecutarAlCerrar(Node toast) {
        Object r = toast.getProperties().remove(AL_CERRAR);
        if (r instanceof Runnable accion) accion.run();
    }
}
