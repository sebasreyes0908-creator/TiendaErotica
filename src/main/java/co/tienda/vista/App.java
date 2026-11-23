package co.tienda.vista;

import co.tienda.modelo.Categoria;
import co.tienda.modelo.Producto;
import co.tienda.persistencia.ArchivoProductos;
import co.tienda.persistencia.GestorImagenes;
import co.tienda.servicio.DatosEjemplo;
import co.tienda.servicio.ProductoServicio;
import co.tienda.vista.Notificaciones.Tipo;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.*;
import java.util.function.Function;

/**
 * Ventana principal de "Velvet Boutique".
 *
 * Capas (de abajo hacia arriba, dentro de un StackPane):
 *   1. base           → barra lateral + cuadrícula de tarjetas
 *   2. capaPanel      → panel lateral de edición (se desliza desde la derecha)
 *   3. capaModal      → diálogos de confirmación
 *   4. capaToasts     → notificaciones
 */
public class App extends Application {

    private static final String ARCHIVO_DATOS = "productos.dat";
    private static final String CARPETA_IMAGENES = "imagenes";
    private static final NumberFormat PESOS = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
    static { PESOS.setMaximumFractionDigits(0); }

    /** Opciones de orden de la cuadrícula. */
    private enum Orden {
        NOMBRE("Nombre (A-Z)", Comparator.comparing((Producto p) -> p.nombre().toLowerCase())),
        PRECIO_ASC("Precio: menor a mayor", Comparator.comparingDouble(Producto::precio)),
        PRECIO_DESC("Precio: mayor a menor", Comparator.comparingDouble(Producto::precio).reversed()),
        STOCK("Stock: menor a mayor", Comparator.comparingInt(Producto::stock));

        final String texto;
        final Comparator<Producto> comparador;
        Orden(String texto, Comparator<Producto> comparador) { this.texto = texto; this.comparador = comparador; }
        @Override public String toString() { return texto; }
    }

    private ProductoServicio servicio;
    private GestorImagenes gestorImagenes;
    private final Map<String, Image> cacheImagenes = new HashMap<>();
    private final Map<String, TarjetaProducto> tarjetas = new LinkedHashMap<>();

    // Capas
    private final StackPane raiz = new StackPane();
    private final BorderPane base = new BorderPane();
    private StackPane capaPanel;
    private Region velo;
    private PanelEdicion panel;
    private boolean panelAbierto;
    private Notificaciones notificaciones;
    private Dialogos dialogos;

    // Controles
    private final FlowPane cuadricula = new FlowPane(20, 20);
    private final VBox estadoVacio = new VBox(10);
    private final TextField txtBuscar = new TextField();
    private final ComboBox<Orden> cmbOrden = new ComboBox<>();
    private final ToggleButton tglReabastecer = new ToggleButton("Por reabastecer");
    private final ToggleGroup grupoCategorias = new ToggleGroup();
    private final Map<Categoria, Label> contadores = new EnumMap<>(Categoria.class);
    private final Label contadorTodas = new Label();
    private final Label lblResultados = new Label();
    private final Label statProductos = new Label("0");
    private final Label statUnidades = new Label("0");
    private final Label statValor = new Label("$ 0");
    private final Label statReabastecer = new Label("0");

    @Override
    public void start(Stage stage) {
        boolean primeraVez;
        try {
            ArchivoProductos archivo = new ArchivoProductos(ARCHIVO_DATOS);
            primeraVez = !archivo.existe();
            servicio = new ProductoServicio(archivo);
            gestorImagenes = new GestorImagenes(CARPETA_IMAGENES);
            if (primeraVez) DatosEjemplo.cargar(servicio);
        } catch (IOException e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "No se pudieron cargar los datos:\n" + e.getMessage());
            a.showAndWait();
            Platform.exit();
            return;
        }

        base.setLeft(crearBarraLateral());
        base.setCenter(crearContenido());
        capaPanel = crearCapaPanel();
        StackPane capaModal = new StackPane();
        capaModal.setPickOnBounds(false);
        StackPane capaToasts = new StackPane();
        capaToasts.setPickOnBounds(false);
        raiz.getChildren().addAll(base, capaPanel, capaModal, capaToasts);

        notificaciones = new Notificaciones(capaToasts);
        dialogos = new Dialogos(capaModal, base, capaPanel);

        Scene scene = new Scene(raiz, 1320, 800);
        scene.getStylesheets().add(getClass().getResource("estilos.css").toExternalForm());
        configurarAtajos(scene);

        stage.setTitle("Velvet Boutique · Inventario");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(620);
        stage.show();

        grupoCategorias.selectToggle(grupoCategorias.getToggles().get(0));
        refrescar(true);
        mensajesDeBienvenida(primeraVez);
    }

    // ================================================================ UI

    private VBox crearBarraLateral() {
        Node icono = Iconos.crear(Iconos.CORAZON, 34, "logo-icono");
        animarLatido(icono);
        Label titulo = new Label("Velvet");
        titulo.getStyleClass().add("logo-titulo");
        Label sub = new Label("B O U T I Q U E");
        sub.getStyleClass().add("logo-sub");
        HBox logo = new HBox(12, icono, new VBox(-4, titulo, sub));
        logo.setAlignment(Pos.CENTER_LEFT);
        logo.setPadding(new Insets(0, 0, 10, 6));

        VBox categorias = new VBox(4, botonCategoria(null, "Todas", contadorTodas));
        for (Categoria c : Categoria.values()) {
            Label cnt = new Label();
            contadores.put(c, cnt);
            categorias.getChildren().add(botonCategoria(c, c.toString(), cnt));
        }
        // Siempre debe haber una categoría seleccionada
        grupoCategorias.selectedToggleProperty().addListener((o, viejo, nuevo) -> {
            if (nuevo == null && viejo != null) grupoCategorias.selectToggle(viejo);
        });

        GridPane stats = new GridPane();
        stats.setHgap(8);
        stats.setVgap(8);
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        stats.getColumnConstraints().addAll(col, col);
        stats.add(tarjetaStat("Productos", statProductos, null), 0, 0);
        stats.add(tarjetaStat("Unidades", statUnidades, null), 1, 0);
        stats.add(tarjetaStat("Valor del inventario", statValor, null), 0, 1, 2, 1);
        stats.add(tarjetaStat("Por reabastecer", statReabastecer, "stat-alerta"), 0, 2, 2, 1);

        Region espacio = new Region();
        VBox.setVgrow(espacio, Priority.ALWAYS);

        VBox lateral = new VBox(6, logo, seccion("CATEGORÍAS"), categorias, espacio, seccion("RESUMEN"), stats);
        lateral.getStyleClass().add("lateral");
        lateral.setPrefWidth(250);
        lateral.setMinWidth(250);
        return lateral;
    }

    private ToggleButton botonCategoria(Categoria c, String texto, Label contador) {
        Circle punto = new Circle(5, c == null ? Color.web("#f2a7c0") : Color.web(c.colorA()));
        Label nombre = new Label(texto);
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);
        contador.getStyleClass().add("contador");
        HBox contenido = new HBox(10, punto, nombre, esp, contador);
        contenido.setAlignment(Pos.CENTER_LEFT);

        ToggleButton b = new ToggleButton();
        b.setUserData(c);
        b.setToggleGroup(grupoCategorias);
        b.getStyleClass().add("btn-categoria");
        b.setGraphic(contenido);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.setMaxWidth(Double.MAX_VALUE);
        contenido.prefWidthProperty().bind(b.widthProperty().subtract(28));
        b.setOnAction(e -> refrescar(true));

        // Se desplaza un poco a la derecha al pasar el mouse
        TranslateTransition t = new TranslateTransition(Duration.millis(150), contenido);
        b.setOnMouseEntered(e -> { t.stop(); t.setToX(4); t.play(); });
        b.setOnMouseExited(e -> { t.stop(); t.setToX(0); t.play(); });
        return b;
    }

    private VBox tarjetaStat(String titulo, Label valor, String claseExtra) {
        valor.getStyleClass().add("stat-valor");
        Label lbl = new Label(titulo);
        lbl.getStyleClass().add("stat-titulo");
        VBox v = new VBox(2, valor, lbl);
        v.getStyleClass().add("stat");
        if (claseExtra != null) v.getStyleClass().add(claseExtra);
        return v;
    }

    private Label seccion(String texto) {
        Label l = new Label(texto);
        l.getStyleClass().add("lateral-seccion");
        return l;
    }

    private VBox crearContenido() {
        // Fila 1: título + botón nuevo
        Label h1 = new Label("Inventario");
        h1.getStyleClass().add("h1");
        lblResultados.getStyleClass().add("subtitulo");
        Button btnNuevo = new Button("Nuevo producto");
        btnNuevo.setGraphic(Iconos.crear(Iconos.MAS, 16, "icono-blanco"));
        btnNuevo.getStyleClass().add("btn-primario");
        btnNuevo.setOnAction(e -> abrirNuevo());
        Region esp = new Region();
        HBox.setHgrow(esp, Priority.ALWAYS);
        HBox fila1 = new HBox(new VBox(2, h1, lblResultados), esp, btnNuevo);
        fila1.setAlignment(Pos.CENTER_LEFT);

        // Fila 2: buscador, orden y filtro
        txtBuscar.setPromptText("Buscar por nombre, marca, código o categoría…   (Ctrl+F)");
        txtBuscar.textProperty().addListener((o, a, b) -> refrescar(true));
        HBox.setHgrow(txtBuscar, Priority.ALWAYS);
        Button limpiarBusqueda = new Button();
        limpiarBusqueda.setGraphic(Iconos.crear(Iconos.CERRAR, 14, "icono-suave"));
        limpiarBusqueda.getStyleClass().add("btn-icono");
        limpiarBusqueda.visibleProperty().bind(txtBuscar.textProperty().isNotEmpty());
        limpiarBusqueda.setOnAction(e -> txtBuscar.clear());
        HBox buscador = new HBox(Iconos.crear(Iconos.BUSCAR, 18, "icono-suave"), txtBuscar, limpiarBusqueda);
        buscador.getStyleClass().add("buscador");
        buscador.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(buscador, Priority.ALWAYS);
        txtBuscar.focusedProperty().addListener((o, a, b) -> {
            if (b) buscador.getStyleClass().add("enfocado");
            else buscador.getStyleClass().remove("enfocado");
        });

        cmbOrden.getItems().setAll(Orden.values());
        cmbOrden.setValue(Orden.NOMBRE);
        cmbOrden.setOnAction(e -> refrescar(true));
        tglReabastecer.getStyleClass().add("chip-toggle");
        tglReabastecer.setGraphic(Iconos.crear(Iconos.ALERTA, 14, "icono-alerta"));
        tglReabastecer.setOnAction(e -> refrescar(true));

        HBox fila2 = new HBox(12, buscador, cmbOrden, tglReabastecer);
        fila2.setAlignment(Pos.CENTER_LEFT);

        // Cuadrícula de tarjetas
        cuadricula.setPadding(new Insets(10, 4, 30, 4));
        Label vacioTitulo = new Label("No hay productos que coincidan");
        vacioTitulo.getStyleClass().add("vacio-titulo");
        Label vacioTexto = new Label("Prueba con otra búsqueda o categoría, o agrega un producto nuevo.");
        vacioTexto.getStyleClass().add("subtitulo");
        estadoVacio.getChildren().setAll(Iconos.crear(Iconos.BUSCAR, 56, "icono-vacio"), vacioTitulo, vacioTexto);
        estadoVacio.setAlignment(Pos.CENTER);
        estadoVacio.setPadding(new Insets(80, 0, 0, 0));
        estadoVacio.setVisible(false);
        estadoVacio.setManaged(false);

        ScrollPane scroll = new ScrollPane(new VBox(cuadricula, estadoVacio));
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("scroll-cuadricula");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox contenido = new VBox(18, fila1, fila2, scroll);
        contenido.getStyleClass().add("contenido");
        return contenido;
    }

    private StackPane crearCapaPanel() {
        velo = new Region();
        velo.getStyleClass().add("velo");
        velo.setOnMouseClicked(e -> cerrarPanel());

        panel = new PanelEdicion(this::cargarImagen);
        panel.setAlGuardar(this::guardar);
        panel.setAlEliminar(() -> confirmarEliminacion(panel.getOriginal()));
        panel.setAlCerrar(this::cerrarPanel);

        StackPane capa = new StackPane(velo, panel);
        StackPane.setAlignment(panel, Pos.CENTER_RIGHT);
        capa.setVisible(false);
        return capa;
    }

    private void configurarAtajos(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), this::abrirNuevo);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN), () -> {
            if (!panelAbierto) txtBuscar.requestFocus();
        });
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                // Si hay una lista desplegable abierta, Esc solo debe cerrar esa lista
                if (scene.getFocusOwner() instanceof ComboBoxBase<?> combo && combo.isShowing()) return;
                if (dialogos.estaAbierto()) dialogos.cerrar();
                else if (panelAbierto) cerrarPanel();
                else txtBuscar.clear();
                e.consume();
            }
        });
    }

    // ============================================================ Panel

    private void abrirNuevo() {
        if (dialogos.estaAbierto()) return;
        panel.nuevo();
        abrirPanel();
    }

    private void abrirEditar(Producto p) {
        if (dialogos.estaAbierto()) return;
        panel.editar(p);
        abrirPanel();
    }

    private void abrirPanel() {
        if (panelAbierto) {
            panel.enfocarPrimerCampo();
            return;
        }
        panelAbierto = true;
        capaPanel.setVisible(true);
        velo.setOpacity(0);
        panel.setTranslateX(PanelEdicion.ANCHO + 30);

        FadeTransition f = new FadeTransition(Duration.millis(250), velo);
        f.setToValue(1);
        TranslateTransition t = new TranslateTransition(Duration.millis(340), panel);
        t.setToX(0);
        t.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.3, 1));
        ParallelTransition p = new ParallelTransition(f, t);
        p.setOnFinished(e -> panel.enfocarPrimerCampo());
        p.play();
    }

    private void cerrarPanel() {
        if (!panelAbierto) return;
        panelAbierto = false;
        FadeTransition f = new FadeTransition(Duration.millis(220), velo);
        f.setToValue(0);
        TranslateTransition t = new TranslateTransition(Duration.millis(240), panel);
        t.setToX(PanelEdicion.ANCHO + 30);
        t.setInterpolator(Interpolator.EASE_IN);
        ParallelTransition p = new ParallelTransition(f, t);
        p.setOnFinished(e -> { if (!panelAbierto) capaPanel.setVisible(false); });
        p.play();
    }

    // ========================================================= Acciones CRUD

    private void guardar() {
        String error = panel.validar();
        if (error != null) {
            panel.sacudir();
            notificaciones.mostrar(Tipo.ERROR, "Revisa el formulario", error);
            return;
        }

        Producto original = panel.getOriginal();
        String imagen = original == null ? null : original.imagen();
        String imagenImportada = null;
        try {
            if (panel.getImagenNueva() != null) {
                imagenImportada = gestorImagenes.importar(panel.getImagenNueva(), panel.getCodigo());
                imagen = imagenImportada;
            } else if (panel.isQuitarImagen()) {
                imagen = null;
            }

            Producto p = panel.construir(imagen);   // el record valida aquí
            if (original == null) {
                servicio.crear(p);
                notificaciones.mostrar(Tipo.EXITO, "Producto creado", p.nombre() + " se agregó al inventario");
            } else {
                servicio.actualizar(original.codigo(), p);
                notificaciones.mostrar(Tipo.EXITO, "Cambios guardados", p.nombre() + " se actualizó");
                // Si la imagen cambió, la vieja ya no se necesita
                if (original.imagen() != null && !original.imagen().equals(imagen)) {
                    gestorImagenes.eliminar(original.imagen());
                }
            }
            avisarStock(p);
            cerrarPanel();
            refrescar(true);
        } catch (IllegalArgumentException ex) {
            gestorImagenes.eliminar(imagenImportada);
            panel.sacudir();
            notificaciones.mostrar(Tipo.ERROR, "No se pudo guardar", ex.getMessage());
        } catch (IOException ex) {
            gestorImagenes.eliminar(imagenImportada);
            notificaciones.mostrar(Tipo.ERROR, "Error de archivo", ex.getMessage());
        }
    }

    private void confirmarEliminacion(Producto p) {
        if (p == null) return;
        dialogos.confirmar("¿Eliminar producto?",
                "Se eliminará \"" + p.nombre() + "\" del inventario.",
                "Sí, eliminar", () -> eliminar(p));
    }

    private void eliminar(Producto p) {
        try {
            servicio.eliminar(p.codigo());
        } catch (IOException | IllegalArgumentException ex) {
            notificaciones.mostrar(Tipo.ERROR, "No se pudo eliminar", ex.getMessage());
            return;
        }
        cerrarPanel();

        TarjetaProducto t = tarjetas.get(p.codigo());
        if (t != null) t.animarSalida(() -> refrescar(false));
        else refrescar(false);

        // Se puede deshacer mientras la notificación esté visible.
        // La imagen solo se borra del disco cuando ya no se puede deshacer.
        boolean[] restaurado = {false};
        notificaciones.mostrar(Tipo.INFO, "Producto eliminado", p.nombre(),
                "Deshacer",
                () -> {
                    try {
                        servicio.crear(p);
                        restaurado[0] = true;
                        refrescar(true);
                        notificaciones.mostrar(Tipo.EXITO, "Restaurado", p.nombre() + " volvió al inventario");
                    } catch (IOException | IllegalArgumentException ex) {
                        notificaciones.mostrar(Tipo.ERROR, "No se pudo restaurar", ex.getMessage());
                    }
                },
                () -> { if (!restaurado[0]) gestorImagenes.eliminar(p.imagen()); });
    }

    /** Botones +/- de cada tarjeta. */
    private void cambiarStock(Producto p, int delta) {
        int nuevo = p.stock() + delta;
        if (nuevo < 0) {
            notificaciones.mostrar(Tipo.ADVERTENCIA, "Sin unidades", p.nombre() + " ya está agotado");
            return;
        }
        try {
            Producto actualizado = p.conStock(nuevo);
            servicio.actualizar(p.codigo(), actualizado);
            TarjetaProducto t = tarjetas.get(p.codigo());
            if (t != null) {
                t.setProducto(actualizado);
                t.pulsoStock();
            }
            actualizarEstadisticas();
            actualizarContadores();
            if (delta < 0) avisarStock(actualizado);
            else if (p.agotado() || p.stockBajo()) {
                if (!actualizado.stockBajo())
                    notificaciones.mostrar(Tipo.EXITO, "Reabastecido", actualizado.nombre() + " ya tiene stock suficiente");
            }
        } catch (IOException ex) {
            notificaciones.mostrar(Tipo.ERROR, "Error de archivo", ex.getMessage());
        }
    }

    private void avisarStock(Producto p) {
        if (p.agotado()) {
            notificaciones.mostrar(Tipo.ADVERTENCIA, "Producto agotado", p.nombre() + " se quedó sin unidades");
        } else if (p.stockBajo()) {
            notificaciones.mostrar(Tipo.ADVERTENCIA, "Stock bajo",
                    "Quedan " + p.stock() + (p.stock() == 1 ? " unidad" : " unidades") + " de " + p.nombre());
        }
    }

    // ============================================================ Refrescar

    private void refrescar(boolean animar) {
        Categoria cat = grupoCategorias.getSelectedToggle() == null
                ? null : (Categoria) grupoCategorias.getSelectedToggle().getUserData();
        String texto = txtBuscar.getText() == null ? "" : txtBuscar.getText().toLowerCase().trim();
        boolean soloReabastecer = tglReabastecer.isSelected();

        List<Producto> todos = servicio.listar();
        List<Producto> visibles = todos.stream()
                .filter(p -> cat == null || p.categoria() == cat)
                .filter(p -> !soloReabastecer || p.agotado() || p.stockBajo())
                .filter(p -> texto.isEmpty()
                        || p.nombre().toLowerCase().contains(texto)
                        || p.marca().toLowerCase().contains(texto)
                        || p.codigo().toLowerCase().contains(texto)
                        || p.categoria().toString().toLowerCase().contains(texto))
                .sorted(cmbOrden.getValue().comparador)
                .toList();

        cuadricula.getChildren().clear();
        tarjetas.clear();
        int i = 0;
        for (Producto p : visibles) {
            TarjetaProducto t = new TarjetaProducto(p, this::cargarImagen, PESOS,
                    this::abrirEditar, this::cambiarStock, this::confirmarEliminacion);
            tarjetas.put(p.codigo(), t);
            cuadricula.getChildren().add(t);
            if (animar) t.animarEntrada(i++);
        }

        boolean vacio = visibles.isEmpty();
        estadoVacio.setVisible(vacio);
        estadoVacio.setManaged(vacio);
        if (vacio && animar) {
            estadoVacio.setOpacity(0);
            FadeTransition f = new FadeTransition(Duration.millis(300), estadoVacio);
            f.setToValue(1);
            f.play();
        }

        lblResultados.setText(visibles.size() == todos.size()
                ? todos.size() + " productos en total"
                : "Mostrando " + visibles.size() + " de " + todos.size() + " productos");
        actualizarContadores();
        actualizarEstadisticas();
    }

    private void actualizarContadores() {
        List<Producto> todos = servicio.listar();
        contadorTodas.setText(String.valueOf(todos.size()));
        for (Categoria c : Categoria.values()) {
            long n = todos.stream().filter(p -> p.categoria() == c).count();
            contadores.get(c).setText(String.valueOf(n));
        }
    }

    private void actualizarEstadisticas() {
        animarNumero(statProductos, servicio.listar().size(), v -> String.valueOf(Math.round(v)));
        animarNumero(statUnidades, servicio.unidadesTotales(), v -> String.valueOf(Math.round(v)));
        animarNumero(statValor, servicio.valorTotalInventario(), PESOS::format);
        animarNumero(statReabastecer, servicio.productosPorReabastecer(), v -> String.valueOf(Math.round(v)));
    }

    /** Hace que un número "cuente" desde su valor anterior hasta el nuevo. */
    private void animarNumero(Label lbl, double hasta, Function<Double, String> formato) {
        Object anterior = lbl.getProperties().get("valor");
        double desde = anterior instanceof Double d ? d : 0;
        lbl.getProperties().put("valor", hasta);
        if (desde == hasta) {
            lbl.setText(formato.apply(hasta));
            return;
        }
        Object animAnterior = lbl.getProperties().get("animacion");
        if (animAnterior instanceof Timeline t) t.stop();

        DoubleProperty valor = new SimpleDoubleProperty(desde);
        valor.addListener((o, a, b) -> lbl.setText(formato.apply(b.doubleValue())));
        Timeline anim = new Timeline(new KeyFrame(Duration.millis(700),
                new KeyValue(valor, hasta, Interpolator.EASE_OUT)));
        lbl.getProperties().put("animacion", anim);
        anim.play();
    }

    // ============================================================ Utilidades

    /** Carga (y guarda en caché) la imagen de un producto. Devuelve null si no tiene. */
    private Image cargarImagen(String nombre) {
        if (nombre == null || !gestorImagenes.existe(nombre)) return null;
        return cacheImagenes.computeIfAbsent(nombre, n -> {
            Path ruta = gestorImagenes.ruta(n);
            return new Image(ruta.toUri().toString(), 700, 0, true, true, true);
        });
    }

    private void animarLatido(Node n) {
        ScaleTransition s = new ScaleTransition(Duration.millis(600), n);
        s.setToX(1.15);
        s.setToY(1.15);
        s.setAutoReverse(true);
        s.setCycleCount(Animation.INDEFINITE);
        s.setInterpolator(Interpolator.EASE_BOTH);
        s.play();
    }

    private void mensajesDeBienvenida(boolean primeraVez) {
        PauseTransition espera = new PauseTransition(Duration.millis(700));
        espera.setOnFinished(e -> {
            if (primeraVez) {
                notificaciones.mostrar(Tipo.INFO, "¡Bienvenido!",
                        "Cargamos algunos productos de ejemplo. Haz clic en una tarjeta para editarla.");
            }
            long n = servicio.productosPorReabastecer();
            if (n > 0) {
                notificaciones.mostrar(Tipo.ADVERTENCIA, "Atención al inventario",
                        n + (n == 1 ? " producto necesita" : " productos necesitan") + " reabastecerse",
                        "Ver", () -> { tglReabastecer.setSelected(true); refrescar(true); }, null);
            }
        });
        espera.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
