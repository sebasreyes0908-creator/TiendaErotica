package co.tienda.vista;

import co.tienda.modelo.Categoria;
import co.tienda.modelo.Producto;
import co.tienda.persistencia.GestorImagenes;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.util.List;
import java.util.function.Function;

/**
 * Panel lateral para crear o editar un producto. Incluye la zona de imagen,
 * que acepta clic (abre el explorador de archivos) o arrastrar y soltar.
 */
public class PanelEdicion extends VBox {

    public static final double ANCHO = 410;
    private static final double ANCHO_IMG = 362;
    private static final double ALTO_IMG = 200;

    private final Function<String, Image> cargadorImagen;

    private final Label lblTitulo = new Label();
    private final StackPane zonaImagen = new StackPane();
    private final Button btnQuitarImagen = new Button("Quitar imagen");
    private final TextField txtCodigo = new TextField();
    private final TextField txtNombre = new TextField();
    private final ComboBox<Categoria> cmbCategoria = new ComboBox<>();
    private final TextField txtMarca = new TextField();
    private final TextField txtPrecio = new TextField();
    private final Spinner<Integer> spnStock = new Spinner<>(0, 99999, 0);
    private final CheckBox chkDiscreto = new CheckBox("Enviar en empaque discreto");
    private final Button btnEliminar = new Button("Eliminar");
    private final Button btnGuardar = new Button("Guardar");

    private Producto original;      // null = producto nuevo
    private File imagenNueva;       // imagen elegida pero aún no guardada
    private boolean quitarImagen;

    private Runnable alGuardar = () -> { };
    private Runnable alEliminar = () -> { };
    private Runnable alCerrar = () -> { };

    public PanelEdicion(Function<String, Image> cargadorImagen) {
        this.cargadorImagen = cargadorImagen;
        getStyleClass().add("panel-edicion");
        setPrefWidth(ANCHO);
        setMaxWidth(ANCHO);
        setMinWidth(ANCHO);

        // ---------- Encabezado ----------
        lblTitulo.getStyleClass().add("panel-titulo");
        Button btnCerrar = new Button();
        btnCerrar.setGraphic(Iconos.crear(Iconos.CERRAR, 18, "icono-suave"));
        btnCerrar.getStyleClass().add("btn-icono");
        btnCerrar.setOnAction(e -> alCerrar.run());
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);
        HBox encabezado = new HBox(lblTitulo, esp, btnCerrar);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        encabezado.setPadding(new Insets(22, 20, 14, 24));

        // ---------- Imagen ----------
        configurarZonaImagen();
        Button btnElegir = new Button("Elegir imagen…");
        btnElegir.setGraphic(Iconos.crear(Iconos.IMAGEN, 15, "icono-rosa"));
        btnElegir.getStyleClass().add("btn-secundario");
        btnElegir.setOnAction(e -> elegirImagen());
        btnQuitarImagen.getStyleClass().add("btn-texto");
        btnQuitarImagen.setOnAction(e -> {
            imagenNueva = null;
            quitarImagen = true;
            actualizarVistaImagen(true);
        });
        HBox filaBotonesImg = new HBox(10, btnElegir, btnQuitarImagen);
        filaBotonesImg.setAlignment(Pos.CENTER_LEFT);

        // ---------- Campos ----------
        txtCodigo.setPromptText("VB-010");
        txtNombre.setPromptText("Nombre del producto");
        txtMarca.setPromptText("Marca (opcional)");
        txtPrecio.setPromptText("89900");
        cmbCategoria.getItems().setAll(Categoria.values());
        cmbCategoria.setPromptText("Elegir…");
        cmbCategoria.setMaxWidth(Double.MAX_VALUE);
        spnStock.setEditable(true);
        spnStock.setMaxWidth(Double.MAX_VALUE);
        // Que el spinner acepte lo escrito al perder el foco
        spnStock.focusedProperty().addListener((o, a, enfocado) -> {
            if (!enfocado) confirmarStockEscrito();
        });

        // La imagen por defecto se actualiza en vivo al escribir el nombre o cambiar la categoría
        txtNombre.textProperty().addListener((o, a, b) -> { if (sinImagen()) actualizarVistaImagen(false); });
        cmbCategoria.valueProperty().addListener((o, a, b) -> { if (sinImagen()) actualizarVistaImagen(false); });

        // Al escribir en un campo marcado con error, se quita la marca
        for (Control c : List.of(txtCodigo, txtNombre, txtPrecio, cmbCategoria)) {
            c.focusedProperty().addListener((o, a, b) -> { if (b) c.getStyleClass().remove("error"); });
        }

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        grid.getColumnConstraints().addAll(col, col);
        grid.add(campo("Código *", txtCodigo), 0, 0);
        grid.add(campo("Categoría *", cmbCategoria), 1, 0);
        grid.add(campo("Nombre *", txtNombre), 0, 1, 2, 1);
        grid.add(campo("Marca", txtMarca), 0, 2, 2, 1);
        grid.add(campo("Precio (COP) *", txtPrecio), 0, 3);
        grid.add(campo("Stock", spnStock), 1, 3);
        grid.add(chkDiscreto, 0, 4, 2, 1);

        VBox cuerpo = new VBox(14, zonaImagen, filaBotonesImg, grid);
        cuerpo.setPadding(new Insets(0, 24, 20, 24));
        ScrollPane scroll = new ScrollPane(cuerpo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // ---------- Pie ----------
        btnEliminar.getStyleClass().add("btn-peligro");
        btnEliminar.setGraphic(Iconos.crear(Iconos.BASURA, 15, "icono-blanco"));
        btnEliminar.setOnAction(e -> alEliminar.run());
        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().add("btn-secundario");
        btnCancelar.setOnAction(e -> alCerrar.run());
        btnGuardar.getStyleClass().add("btn-primario");
        btnGuardar.setGraphic(Iconos.crear(Iconos.CHECK, 15, "icono-blanco"));
        btnGuardar.setDefaultButton(true);
        btnGuardar.setOnAction(e -> alGuardar.run());
        Region esp2 = new Region();
        HBox.setHgrow(esp2, Priority.ALWAYS);
        HBox pie = new HBox(10, btnEliminar, esp2, btnCancelar, btnGuardar);
        pie.setAlignment(Pos.CENTER_LEFT);
        pie.getStyleClass().add("panel-pie");

        getChildren().addAll(encabezado, scroll, pie);
    }

    // ------------------------------------------------------------ API

    public void setAlGuardar(Runnable r) { alGuardar = r; }
    public void setAlEliminar(Runnable r) { alEliminar = r; }
    public void setAlCerrar(Runnable r) { alCerrar = r; }

    public Producto getOriginal() { return original; }
    public File getImagenNueva() { return imagenNueva; }
    public boolean isQuitarImagen() { return quitarImagen; }
    public String getCodigo() { return txtCodigo.getText().trim(); }

    /** Prepara el panel para crear un producto nuevo. */
    public void nuevo() {
        original = null;
        imagenNueva = null;
        quitarImagen = false;
        lblTitulo.setText("Nuevo producto");
        btnGuardar.setText("Crear producto");
        txtCodigo.clear();
        txtNombre.clear();
        cmbCategoria.setValue(null);
        txtMarca.clear();
        txtPrecio.clear();
        spnStock.getValueFactory().setValue(1);
        chkDiscreto.setSelected(true);
        btnEliminar.setVisible(false);
        btnEliminar.setManaged(false);
        limpiarErrores();
        actualizarVistaImagen(true);
    }

    /** Carga un producto existente para editarlo. */
    public void editar(Producto p) {
        original = p;
        imagenNueva = null;
        quitarImagen = false;
        lblTitulo.setText("Editar producto");
        btnGuardar.setText("Guardar cambios");
        txtCodigo.setText(p.codigo());
        txtNombre.setText(p.nombre());
        cmbCategoria.setValue(p.categoria());
        txtMarca.setText(p.marca());
        txtPrecio.setText(p.precio() == Math.floor(p.precio())
                ? String.valueOf((long) p.precio()) : String.valueOf(p.precio()));
        spnStock.getValueFactory().setValue(p.stock());
        chkDiscreto.setSelected(p.discreto());
        btnEliminar.setVisible(true);
        btnEliminar.setManaged(true);
        limpiarErrores();
        actualizarVistaImagen(true);
    }

    public void enfocarPrimerCampo() {
        (original == null ? txtCodigo : txtNombre).requestFocus();
    }

    /** Revisa los campos y marca en rojo los que tienen problemas. Devuelve el primer error, o null si todo está bien. */
    public String validar() {
        limpiarErrores();
        confirmarStockEscrito();
        String primerError = null;
        if (txtCodigo.getText().isBlank()) {
            marcar(txtCodigo);
            primerError = "El código es obligatorio";
        }
        if (cmbCategoria.getValue() == null) {
            marcar(cmbCategoria);
            if (primerError == null) primerError = "Elige una categoría";
        }
        if (txtNombre.getText().isBlank()) {
            marcar(txtNombre);
            if (primerError == null) primerError = "El nombre es obligatorio";
        }
        Double precio = leerPrecio();
        if (precio == null || precio <= 0) {
            marcar(txtPrecio);
            if (primerError == null) primerError = "El precio debe ser un número mayor que 0";
        }
        return primerError;
    }

    /** Crea el record con los datos del formulario. */
    public Producto construir(String nombreImagen) {
        Double precio = leerPrecio();
        return new Producto(txtCodigo.getText(), txtNombre.getText(), cmbCategoria.getValue(),
                txtMarca.getText(), precio == null ? 0 : precio, spnStock.getValue(),
                chkDiscreto.isSelected(), nombreImagen);
    }

    /** Sacude el panel para indicar un error. */
    public void sacudir() {
        TranslateTransition t = new TranslateTransition(Duration.millis(55), this);
        t.setFromX(0);
        t.setByX(10);
        t.setCycleCount(6);
        t.setAutoReverse(true);
        t.setOnFinished(e -> setTranslateX(0));
        t.play();
    }

    // ------------------------------------------------------------ Imagen

    private void configurarZonaImagen() {
        zonaImagen.getStyleClass().add("zona-imagen");
        zonaImagen.setMinSize(ANCHO_IMG, ALTO_IMG);
        zonaImagen.setPrefSize(ANCHO_IMG, ALTO_IMG);
        zonaImagen.setMaxSize(ANCHO_IMG, ALTO_IMG);
        Rectangle recorte = new Rectangle(ANCHO_IMG, ALTO_IMG);
        recorte.setArcWidth(26);
        recorte.setArcHeight(26);
        zonaImagen.setClip(recorte);
        zonaImagen.setOnMouseClicked(e -> elegirImagen());

        // Arrastrar y soltar
        zonaImagen.setOnDragOver(e -> {
            if (archivoArrastrado(e.getDragboard()) != null) {
                e.acceptTransferModes(TransferMode.COPY);
                if (!zonaImagen.getStyleClass().contains("drop-activo"))
                    zonaImagen.getStyleClass().add("drop-activo");
            }
            e.consume();
        });
        zonaImagen.setOnDragExited(e -> zonaImagen.getStyleClass().remove("drop-activo"));
        zonaImagen.setOnDragDropped(e -> {
            File f = archivoArrastrado(e.getDragboard());
            if (f != null) usarImagen(f);
            e.setDropCompleted(f != null);
            e.consume();
        });
    }

    private File archivoArrastrado(Dragboard db) {
        if (!db.hasFiles()) return null;
        return db.getFiles().stream().filter(GestorImagenes::esImagen).findFirst().orElse(null);
    }

    private void elegirImagen() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Elegir imagen del producto");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File f = fc.showOpenDialog(getScene().getWindow());
        if (f != null) usarImagen(f);
    }

    private void usarImagen(File f) {
        imagenNueva = f;
        quitarImagen = false;
        actualizarVistaImagen(true);
    }

    private boolean sinImagen() {
        return imagenNueva == null && (quitarImagen || original == null || original.imagen() == null);
    }

    private void actualizarVistaImagen(boolean animar) {
        Image img = null;
        if (imagenNueva != null) {
            img = new Image(imagenNueva.toURI().toString(), 900, 0, true, true, false);
        } else if (!quitarImagen && original != null && original.imagen() != null) {
            img = cargadorImagen.apply(original.imagen());
        }

        Node contenido;
        StackPane capaHover = new StackPane();
        capaHover.getStyleClass().add("zona-hover");
        Label textoHover = new Label();
        textoHover.getStyleClass().add("zona-hover-texto");
        textoHover.setGraphic(Iconos.crear(Iconos.IMAGEN, 18, "icono-blanco"));
        capaHover.getChildren().add(textoHover);

        if (img != null && !img.isError()) {
            contenido = Imagenes.cubrir(img, ANCHO_IMG, ALTO_IMG);
            textoHover.setText("Cambiar imagen");
            capaHover.setOpacity(0);
            StackPane.setAlignment(textoHover, Pos.CENTER);
            btnQuitarImagen.setDisable(false);
        } else {
            contenido = Imagenes.porDefecto(txtNombre.getText(), cmbCategoria.getValue(), ANCHO_IMG, ALTO_IMG);
            textoHover.setText("Haz clic o arrastra una imagen aquí");
            textoHover.getStyleClass().add("zona-hint-chip");
            capaHover.getStyleClass().setAll("zona-hint");
            capaHover.setOpacity(0.85);
            StackPane.setAlignment(textoHover, Pos.BOTTOM_CENTER);
            StackPane.setMargin(textoHover, new Insets(0, 0, 14, 0));
            btnQuitarImagen.setDisable(true);
        }
        // La capa de ayuda se intensifica al pasar el mouse
        double opacidadBase = capaHover.getOpacity();
        zonaImagen.setOnMouseEntered(e -> fade(capaHover, 1));
        zonaImagen.setOnMouseExited(e -> fade(capaHover, opacidadBase));

        zonaImagen.getChildren().setAll(contenido, capaHover);
        if (animar) {
            contenido.setOpacity(0);
            fade(contenido, 1);
        }
    }

    private static void fade(Node n, double hasta) {
        FadeTransition f = new FadeTransition(Duration.millis(220), n);
        f.setToValue(hasta);
        f.play();
    }

    // ------------------------------------------------------------ Utilidades

    private VBox campo(String etiqueta, Control control) {
        Label l = new Label(etiqueta);
        l.getStyleClass().add("campo-etiqueta");
        control.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, l, control);
    }

    private Double leerPrecio() {
        try {
            return Double.parseDouble(txtPrecio.getText().trim().replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void confirmarStockEscrito() {
        try {
            int v = Integer.parseInt(spnStock.getEditor().getText().trim());
            spnStock.getValueFactory().setValue(Math.max(0, v));
        } catch (NumberFormatException e) {
            spnStock.getEditor().setText(String.valueOf(spnStock.getValue()));
        }
    }

    private void marcar(Control c) {
        if (!c.getStyleClass().contains("error")) c.getStyleClass().add("error");
    }

    private void limpiarErrores() {
        for (Control c : List.of(txtCodigo, txtNombre, txtPrecio, cmbCategoria)) {
            c.getStyleClass().remove("error");
        }
    }
}
