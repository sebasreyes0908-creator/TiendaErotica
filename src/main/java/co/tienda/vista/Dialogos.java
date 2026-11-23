package co.tienda.vista;

import javafx.animation.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * Diálogo de confirmación propio (en lugar del Alert gris de JavaFX).
 * Aparece con un pequeño "rebote" y desenfoca lo que hay detrás.
 */
public class Dialogos {

    private final StackPane capa;
    private final Node[] fondos;
    private StackPane actual;

    /**
     * @param capa   donde se dibuja el diálogo
     * @param fondos nodos que se desenfocan mientras el diálogo está abierto
     */
    public Dialogos(StackPane capa, Node... fondos) {
        this.capa = capa;
        this.fondos = fondos;
    }

    public boolean estaAbierto() {
        return actual != null;
    }

    public void confirmar(String titulo, String mensaje, String textoAceptar, Runnable alAceptar) {
        if (actual != null) return;

        StackPane icono = new StackPane(Iconos.crear(Iconos.BASURA, 30, "dialogo-icono-svg"));
        icono.getStyleClass().add("dialogo-icono");

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("dialogo-titulo");
        Label lblMensaje = new Label(mensaje);
        lblMensaje.getStyleClass().add("dialogo-mensaje");
        lblMensaje.setWrapText(true);
        lblMensaje.setTextAlignment(TextAlignment.CENTER);

        Button cancelar = new Button("Cancelar");
        cancelar.getStyleClass().add("btn-secundario");
        Button aceptar = new Button(textoAceptar);
        aceptar.getStyleClass().add("btn-peligro");
        HBox botones = new HBox(12, cancelar, aceptar);
        botones.setAlignment(Pos.CENTER);

        VBox caja = new VBox(14, icono, lblTitulo, lblMensaje, botones);
        caja.setAlignment(Pos.CENTER);
        caja.getStyleClass().add("dialogo");
        caja.setPrefWidth(390);
        caja.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane fondo = new StackPane(caja);
        fondo.getStyleClass().add("modal-fondo");
        fondo.setOnMouseClicked(e -> { if (e.getTarget() == fondo) cerrar(); });

        cancelar.setOnAction(e -> cerrar());
        aceptar.setOnAction(e -> { cerrar(); alAceptar.run(); });

        capa.getChildren().add(fondo);
        actual = fondo;
        // El foco va a la caja y no a un botón, para que un Enter "sobrante"
        // (por ejemplo, el del menú contextual) no active nada por accidente
        caja.setFocusTraversable(true);
        caja.requestFocus();

        // Animación: fondo aparece, la caja crece con rebote y el resto se desenfoca
        fondo.setOpacity(0);
        FadeTransition aparece = new FadeTransition(Duration.millis(200), fondo);
        aparece.setToValue(1);

        caja.setScaleX(0.7);
        caja.setScaleY(0.7);
        ScaleTransition crece = new ScaleTransition(Duration.millis(180), caja);
        crece.setToX(1.05);
        crece.setToY(1.05);
        ScaleTransition asienta = new ScaleTransition(Duration.millis(110), caja);
        asienta.setToX(1);
        asienta.setToY(1);

        new ParallelTransition(aparece, new SequentialTransition(crece, asienta), desenfoque(0, 8)).play();
    }

    public void cerrar() {
        if (actual == null) return;
        StackPane fondo = actual;
        actual = null;
        Node caja = fondo.getChildren().get(0);

        FadeTransition desaparece = new FadeTransition(Duration.millis(160), fondo);
        desaparece.setToValue(0);
        ScaleTransition encoge = new ScaleTransition(Duration.millis(160), caja);
        encoge.setToX(0.9);
        encoge.setToY(0.9);
        ParallelTransition salida = new ParallelTransition(desaparece, encoge, desenfoque(8, 0));
        salida.setOnFinished(e -> {
            capa.getChildren().remove(fondo);
            for (Node n : fondos) n.setEffect(null);
        });
        salida.play();
    }

    private Timeline desenfoque(double desde, double hasta) {
        Timeline t = new Timeline();
        for (Node n : fondos) {
            GaussianBlur blur = n.getEffect() instanceof GaussianBlur g ? g : new GaussianBlur(desde);
            n.setEffect(blur);
            t.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO, new KeyValue(blur.radiusProperty(), desde)),
                    new KeyFrame(Duration.millis(200), new KeyValue(blur.radiusProperty(), hasta)));
        }
        return t;
    }
}
