package co.tienda.vista;

import co.tienda.modelo.Producto;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Tarjeta visual de un producto: imagen, etiquetas, precio y botones +/- para
 * el stock. Al pasar el mouse se eleva y la imagen hace zoom; clic para editar;
 * clic derecho para un menú contextual.
 */
public class TarjetaProducto extends VBox {

    public static final double ANCHO = 228;
    public static final double ALTO_IMG = 170;

    private final Function<String, Image> cargadorImagen;
    private final NumberFormat formatoPrecio;

    private final StackPane contenedorImagen = new StackPane();
    private final Label chipCategoria = new Label();
    private final Label chipDiscreto = new Label("Discreto");
    private final Label badgeStock = new Label();
    private final Label lblNombre = new Label();
    private final Label lblMarca = new Label();
    private final Label lblPrecio = new Label();
    private final Label lblStock = new Label();

    private Producto producto;

    public TarjetaProducto(Producto producto,
                           Function<String, Image> cargadorImagen,
                           NumberFormat formatoPrecio,
                           Consumer<Producto> alEditar,
                           BiConsumer<Producto, Integer> alCambiarStock,
                           Consumer<Producto> alEliminar) {
        this.cargadorImagen = cargadorImagen;
        this.formatoPrecio = formatoPrecio;

        getStyleClass().add("tarjeta");
        setMinWidth(ANCHO);
        setPrefWidth(ANCHO);
        setMaxWidth(ANCHO);

        // ---------- Zona de imagen ----------
        StackPane zonaImagen = new StackPane(contenedorImagen, chipCategoria, chipDiscreto, badgeStock);
        zonaImagen.setMinSize(ANCHO, ALTO_IMG);
        zonaImagen.setPrefSize(ANCHO, ALTO_IMG);
        zonaImagen.setMaxSize(ANCHO, ALTO_IMG);
        // Recorte: esquinas superiores redondeadas, inferiores rectas
        Rectangle redondeado = new Rectangle(ANCHO, ALTO_IMG);
        redondeado.setArcWidth(32);
        redondeado.setArcHeight(32);
        Shape recorte = Shape.union(redondeado, new Rectangle(0, ALTO_IMG / 2, ANCHO, ALTO_IMG / 2));
        zonaImagen.setClip(recorte);

        chipCategoria.getStyleClass().add("chip-categoria");
        chipDiscreto.getStyleClass().add("chip-discreto");
        chipDiscreto.setGraphic(Iconos.crear(Iconos.CANDADO, 11, "icono-blanco"));
        badgeStock.getStyleClass().add("badge-stock");
        StackPane.setAlignment(chipCategoria, Pos.TOP_LEFT);
        StackPane.setAlignment(chipDiscreto, Pos.TOP_RIGHT);
        StackPane.setAlignment(badgeStock, Pos.BOTTOM_LEFT);
        StackPane.setMargin(chipCategoria, new Insets(10));
        StackPane.setMargin(chipDiscreto, new Insets(10));
        StackPane.setMargin(badgeStock, new Insets(10));

        // ---------- Información ----------
        lblNombre.getStyleClass().add("tarjeta-nombre");
        lblNombre.setWrapText(true);
        lblNombre.setMinHeight(40);
        lblNombre.setPrefHeight(40);
        lblNombre.setAlignment(Pos.TOP_LEFT);
        lblMarca.getStyleClass().add("tarjeta-marca");
        lblPrecio.getStyleClass().add("tarjeta-precio");
        lblStock.getStyleClass().add("stock-valor");

        Button menos = botonStock(Iconos.MENOS, -1, alCambiarStock, "Restar una unidad");
        Button mas = botonStock(Iconos.MAS, +1, alCambiarStock, "Sumar una unidad");
        HBox controles = new HBox(4, menos, lblStock, mas);
        controles.setAlignment(Pos.CENTER);

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox filaPrecio = new HBox(lblPrecio, espacio, controles);
        filaPrecio.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(filaPrecio, new Insets(8, 0, 0, 0));

        VBox info = new VBox(2, lblNombre, lblMarca, filaPrecio);
        info.setPadding(new Insets(12, 14, 14, 14));

        getChildren().addAll(zonaImagen, info);

        // ---------- Interacción ----------
        configurarHover();
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) alEditar.accept(this.producto);
        });

        MenuItem editar = new MenuItem("Editar", Iconos.crear(Iconos.EDITAR, 14, "icono-menu"));
        editar.setOnAction(e -> alEditar.accept(this.producto));
        MenuItem sumar = new MenuItem("Sumar 5 unidades", Iconos.crear(Iconos.MAS, 14, "icono-menu"));
        sumar.setOnAction(e -> alCambiarStock.accept(this.producto, 5));
        MenuItem eliminar = new MenuItem("Eliminar", Iconos.crear(Iconos.BASURA, 14, "icono-menu-peligro"));
        eliminar.setOnAction(e -> alEliminar.accept(this.producto));
        ContextMenu menu = new ContextMenu(editar, sumar, new SeparatorMenuItem(), eliminar);
        setOnContextMenuRequested(e -> menu.show(this, e.getScreenX(), e.getScreenY()));

        setProducto(producto);
    }

    public Producto getProducto() {
        return producto;
    }

    /** Actualiza la tarjeta con los datos de un producto (por ejemplo, tras cambiar el stock). */
    public void setProducto(Producto p) {
        boolean recargarImagen = producto == null
                || !Objects.equals(producto.imagen(), p.imagen())
                || (p.imagen() == null && (!producto.nombre().equals(p.nombre())
                                           || producto.categoria() != p.categoria()));
        producto = p;

        chipCategoria.setText(p.categoria().toString());
        chipDiscreto.setVisible(p.discreto());
        lblNombre.setText(p.nombre());
        lblMarca.setText(p.marca() + "  ·  " + p.codigo());
        lblPrecio.setText(formatoPrecio.format(p.precio()));
        lblStock.setText(String.valueOf(p.stock()));

        badgeStock.getStyleClass().removeAll("badge-ok", "badge-bajo", "badge-agotado");
        if (p.agotado()) {
            badgeStock.setText("Agotado");
            badgeStock.getStyleClass().add("badge-agotado");
        } else if (p.stockBajo()) {
            badgeStock.setText("¡Quedan " + p.stock() + "!");
            badgeStock.getStyleClass().add("badge-bajo");
        } else {
            badgeStock.setText(p.stock() + " en stock");
            badgeStock.getStyleClass().add("badge-ok");
        }
        contenedorImagen.setOpacity(p.agotado() ? 0.45 : 1);

        if (recargarImagen) {
            Image img = cargadorImagen.apply(p.imagen());
            Node vista = (img != null && !img.isError())
                    ? Imagenes.cubrir(img, ANCHO, ALTO_IMG)
                    : Imagenes.porDefecto(p.nombre(), p.categoria(), ANCHO, ALTO_IMG);
            contenedorImagen.getChildren().setAll(vista);
        }
    }

    /** Pequeño "latido" del número de stock cuando cambia. */
    public void pulsoStock() {
        ScaleTransition pulso = new ScaleTransition(Duration.millis(120), lblStock);
        pulso.setFromX(1);
        pulso.setFromY(1);
        pulso.setToX(1.55);
        pulso.setToY(1.55);
        pulso.setAutoReverse(true);
        pulso.setCycleCount(2);
        pulso.play();
    }

    /** Animación de entrada (se usa al cargar la cuadrícula, con un retraso escalonado). */
    public void animarEntrada(int orden) {
        setOpacity(0);
        setTranslateY(24);
        FadeTransition fade = new FadeTransition(Duration.millis(320), this);
        fade.setToValue(1);
        TranslateTransition sube = new TranslateTransition(Duration.millis(320), this);
        sube.setToY(0);
        sube.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.3, 1));
        ParallelTransition entrada = new ParallelTransition(fade, sube);
        entrada.setDelay(Duration.millis(Math.min(orden, 16) * 40));
        entrada.play();
    }

    /** Animación de salida (al eliminar). */
    public void animarSalida(Runnable alTerminar) {
        ScaleTransition encoge = new ScaleTransition(Duration.millis(240), this);
        encoge.setToX(0.6);
        encoge.setToY(0.6);
        FadeTransition fade = new FadeTransition(Duration.millis(240), this);
        fade.setToValue(0);
        ParallelTransition salida = new ParallelTransition(encoge, fade);
        salida.setOnFinished(e -> alTerminar.run());
        salida.play();
    }

    // ------------------------------------------------------------------

    private Button botonStock(String icono, int delta, BiConsumer<Producto, Integer> alCambiar, String ayuda) {
        Button b = new Button();
        b.setGraphic(Iconos.crear(icono, 14, null));
        b.getStyleClass().add("btn-stock");
        b.setTooltip(new Tooltip(ayuda));
        b.setOnAction(e -> alCambiar.accept(producto, delta));
        // Evita que el clic en el botón también abra el editor
        b.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);
        return b;
    }

    private void configurarHover() {
        ScaleTransition zoom = new ScaleTransition(Duration.millis(400), contenedorImagen);
        TranslateTransition eleva = new TranslateTransition(Duration.millis(200), this);

        setOnMouseEntered(e -> {
            zoom.stop();
            zoom.setToX(1.1);
            zoom.setToY(1.1);
            zoom.play();
            eleva.stop();
            eleva.setToY(-6);
            eleva.play();
        });
        setOnMouseExited(e -> {
            zoom.stop();
            zoom.setToX(1);
            zoom.setToY(1);
            zoom.play();
            eleva.stop();
            eleva.setToY(0);
            eleva.play();
        });
    }
}
