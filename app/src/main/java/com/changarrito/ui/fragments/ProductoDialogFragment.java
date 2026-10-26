package com.changarrito.ui.fragments;

import android.app.Application;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.changarrito.R;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;
import com.changarrito.database.entity.ModoCaducidad;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.negocio.CatalogoGiro;
import com.changarrito.negocio.NegocioRepository;
import com.changarrito.negocio.SesionTurno;
import com.changarrito.repository.ProductoRepository;
import com.changarrito.utils.CalculoPrecio;
import com.changarrito.utils.InsetsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Alta y edición de producto a pantalla completa (Sprint 7).
 *
 * Costo, % de ganancia y precio se mueven juntos (fórmula sobre el costo, ver CalculoPrecio):
 *  - cambiar el costo o el % recalcula el precio sugerido;
 *  - escribir el precio a mano recalcula el %.
 * El dueño ve el semáforo de ganancia. Un trabajador captura el costo solo al dar de
 * alta un producto y nunca ve el % ni la ganancia: el precio se le sugiere con el
 * margen por defecto del negocio.
 */
public class ProductoDialogFragment extends DialogFragment {

    public interface ProductoDialogListener {
        void onProductoGuardado(ProductoEntity producto);
    }

    private ProductoDialogListener listener;
    private ProductoEntity productoEditando;

    private boolean esDueno;
    private boolean esNuevo;
    private boolean actualizando; // evita que los TextWatcher se llamen entre sí en cadena
    private double margenPorDefecto = 30;
    private ModoCaducidad modoCaducidad = ModoCaducidad.OPCIONAL;
    private long fechaCaducidadMs;
    private boolean configAplicada;

    private TextInputLayout tilNombre, tilPrecio, tilCosto, tilCantidad;
    private TextInputEditText etNombre, etCosto, etPrecio, etCantidad, etUmbral, etTelefono, etBarcode;
    private MaterialAutoCompleteTextView etCategoria;
    private MaterialButtonToggleGroup grupoUnidad;
    private Slider sliderMargen;
    private TextView tvMargenValor, tvGanancia, tvPorcentajes, tvSemaforo, tvFechaCaducidad,
            tvQuitarFecha, tvEtiquetaCaducidad, tvEtiquetaCosto, tvEtiquetaPrecio, tvError;
    private View tarjetaGanancia, seccionCaducidad;
    private MaterialCheckBox cbRedondear;

    private final List<UnidadMedida> unidades = new ArrayList<>();
    private final Set<String> categorias = new LinkedHashSet<>();

    public ProductoDialogFragment() {
    }

    /** producto null = alta nueva; con id 0 = alta prellenada (ej. desde el escáner). */
    public static ProductoDialogFragment newInstance(ProductoEntity producto, ProductoDialogListener listener) {
        ProductoDialogFragment fragment = new ProductoDialogFragment();
        fragment.productoEditando = producto;
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_Changarrito_Acceso_PantallaCompleta);
        if (listener == null && getActivity() instanceof ProductoDialogListener) {
            listener = (ProductoDialogListener) getActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_producto, container, false);
        InsetsUtil.aplicarPaddingBarrasDelSistema(v.findViewById(R.id.rootProducto), true, true);

        esDueno = SesionTurno.esDueno(requireContext());
        esNuevo = productoEditando == null || productoEditando.id == 0;

        tilNombre = v.findViewById(R.id.tilNombre);
        tilCosto = v.findViewById(R.id.tilCosto);
        tilPrecio = v.findViewById(R.id.tilPrecio);
        tilCantidad = v.findViewById(R.id.tilCantidad);
        etNombre = v.findViewById(R.id.etNombre);
        etCategoria = v.findViewById(R.id.etCategoria);
        etCosto = v.findViewById(R.id.etCosto);
        etPrecio = v.findViewById(R.id.etPrecio);
        etCantidad = v.findViewById(R.id.etCantidad);
        etUmbral = v.findViewById(R.id.etUmbral);
        etTelefono = v.findViewById(R.id.etTelefono);
        etBarcode = v.findViewById(R.id.etBarcode);
        grupoUnidad = v.findViewById(R.id.grupoUnidad);
        sliderMargen = v.findViewById(R.id.sliderMargen);
        tvMargenValor = v.findViewById(R.id.tvMargenValor);
        tvGanancia = v.findViewById(R.id.tvGanancia);
        tvPorcentajes = v.findViewById(R.id.tvPorcentajes);
        tvSemaforo = v.findViewById(R.id.tvSemaforo);
        tvFechaCaducidad = v.findViewById(R.id.tvFechaCaducidad);
        tvQuitarFecha = v.findViewById(R.id.tvQuitarFecha);
        tvEtiquetaCaducidad = v.findViewById(R.id.tvEtiquetaCaducidad);
        tvEtiquetaCosto = v.findViewById(R.id.tvEtiquetaCosto);
        tvEtiquetaPrecio = v.findViewById(R.id.tvEtiquetaPrecio);
        tvError = v.findViewById(R.id.tvError);
        tarjetaGanancia = v.findViewById(R.id.tarjetaGanancia);
        seccionCaducidad = v.findViewById(R.id.seccionCaducidad);
        cbRedondear = v.findViewById(R.id.cbRedondear);

        ((TextView) v.findViewById(R.id.tvTitulo)).setText(esNuevo ? R.string.producto_nuevo : R.string.producto_editar);
        v.findViewById(R.id.btnCancelar).setOnClickListener(x -> dismiss());
        v.findViewById(R.id.btnGuardar).setOnClickListener(x -> guardar());

        // Merma (danado, caducado, robo): dueno y trabajadores, solo en productos ya guardados.
        View btnMerma = v.findViewById(R.id.btnMerma);
        btnMerma.setVisibility(esNuevo ? View.GONE : View.VISIBLE);
        btnMerma.setOnClickListener(x -> MermaDialog.mostrar(requireActivity(), productoEditando, this::dismiss));

        // Lo que el trabajador no ve: el % de ganancia y el semáforo siempre; el costo,
        // salvo al dar de alta (lo necesita para que se sugiera el precio).
        v.findViewById(R.id.seccionMargen).setVisibility(esDueno ? View.VISIBLE : View.GONE);
        tarjetaGanancia.setVisibility(esDueno ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.seccionCosto).setVisibility(esDueno || esNuevo ? View.VISIBLE : View.GONE);

        llenarConProducto();
        conectarCalculos(v);
        tvFechaCaducidad.setOnClickListener(x -> elegirFecha());
        tvQuitarFecha.setOnClickListener(x -> {
            fechaCaducidadMs = 0;
            mostrarFecha();
        });

        Application app = requireActivity().getApplication();
        new NegocioRepository(app).getConfiguracion().observe(getViewLifecycleOwner(), this::aplicarConfiguracion);
        new ProductoRepository(app).getCategorias().observe(getViewLifecycleOwner(), lista -> {
            if (lista != null) categorias.addAll(lista);
            actualizarSugerenciasCategoria();
        });
        return v;
    }

    private void llenarConProducto() {
        ProductoEntity p = productoEditando;
        if (p != null) {
            if (p.nombre != null) etNombre.setText(p.nombre);
            if (p.barcode != null) etBarcode.setText(p.barcode);
        }
        if (p == null || esNuevo) {
            fechaCaducidadMs = 0;
            mostrarFecha();
            return;
        }
        actualizando = true;
        if (p.categoria != null) etCategoria.setText(p.categoria, false);
        if (p.costoCompra > 0) etCosto.setText(numero(p.costoCompra));
        etPrecio.setText(numero(p.precio));
        etCantidad.setText(numero(p.cantidadDisponible));
        etUmbral.setText(numero(p.umbralStockBajo));
        etTelefono.setText(p.telefonoProveedor);
        actualizando = false;
        // Al editar se respeta el precio que ya tiene: no se redondea solo.
        cbRedondear.setChecked(false);
        double margen = p.costoCompra > 0 ? CalculoPrecio.margenSobreCosto(p.costoCompra, p.precio) : p.margenPorcentaje;
        ponerMargen(margen);
        fechaCaducidadMs = p.fechaCaducidadMs;
        mostrarFecha();
        actualizarSemaforo();
    }

    private void aplicarConfiguracion(ConfiguracionNegocioEntity config) {
        if (config == null || configAplicada) return;
        configAplicada = true;
        margenPorDefecto = config.margenPorDefecto > 0 ? config.margenPorDefecto : 30;
        modoCaducidad = config.modoCaducidad != null ? config.modoCaducidad : ModoCaducidad.OPCIONAL;

        unidades.clear();
        unidades.addAll(config.getUnidades());
        if (productoEditando != null && productoEditando.unidadMedida != null
                && !unidades.contains(productoEditando.unidadMedida)) {
            unidades.add(productoEditando.unidadMedida); // no esconder la unidad que ya tiene
        }
        armarBotonesUnidad();

        if (config.giro != null) {
            categorias.addAll(CatalogoGiro.cargar(requireContext(), config.giro).porCategoria.keySet());
            actualizarSugerenciasCategoria();
        }

        boolean ocultarFecha = modoCaducidad == ModoCaducidad.OCULTA && fechaCaducidadMs == 0;
        seccionCaducidad.setVisibility(ocultarFecha ? View.GONE : View.VISIBLE);
        tvEtiquetaCaducidad.setText(modoCaducidad == ModoCaducidad.OBLIGATORIA
                ? R.string.producto_caducidad_obligatoria : R.string.producto_caducidad);

        if (esNuevo) {
            ponerMargen(margenPorDefecto);
            recalcularPrecio();
        }
    }

    private void armarBotonesUnidad() {
        grupoUnidad.removeAllViews();
        UnidadMedida inicial = productoEditando != null && productoEditando.unidadMedida != null
                ? productoEditando.unidadMedida
                : (unidades.contains(UnidadMedida.PZA) ? UnidadMedida.PZA : unidades.get(0));
        for (UnidadMedida u : unidades) {
            MaterialButton b = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            b.setId(View.generateViewId());
            b.setTag(u);
            b.setText(nombreUnidad(u));
            b.setAllCaps(false);
            grupoUnidad.addView(b, new android.widget.LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (u == inicial) grupoUnidad.check(b.getId());
        }
        grupoUnidad.addOnButtonCheckedListener((g, id, marcado) -> {
            if (marcado) actualizarTextosPorUnidad();
        });
        actualizarTextosPorUnidad();
    }

    private UnidadMedida unidadElegida() {
        int id = grupoUnidad.getCheckedButtonId();
        View b = id == View.NO_ID ? null : grupoUnidad.findViewById(id);
        return b != null ? (UnidadMedida) b.getTag() : UnidadMedida.PZA;
    }

    private String nombreUnidad(UnidadMedida u) {
        switch (u) {
            case KG: return getString(R.string.unidad_kg);
            case GR: return getString(R.string.unidad_gr);
            case M: return getString(R.string.unidad_m);
            default: return getString(R.string.unidad_pza);
        }
    }

    private void actualizarTextosPorUnidad() {
        String u = nombreUnidad(unidadElegida()).toLowerCase(Locale.getDefault());
        tvEtiquetaCosto.setText(getString(R.string.producto_costo_por, u));
        tvEtiquetaPrecio.setText(getString(R.string.producto_precio) + " por " + u);
        actualizarSemaforo();
    }

    private void actualizarSugerenciasCategoria() {
        if (getContext() == null) return;
        etCategoria.setSimpleItems(categorias.toArray(new String[0]));
    }

    // ---------- costo, % y precio ----------

    private void conectarCalculos(View v) {
        etCosto.addTextChangedListener(new Vigilante(this::recalcularPrecio));
        etPrecio.addTextChangedListener(new Vigilante(this::recalcularMargen));
        sliderMargen.addOnChangeListener((s, valor, delUsuario) -> {
            tvMargenValor.setText(getString(R.string.producto_margen_valor, (int) valor));
            if (delUsuario) recalcularPrecio();
        });
        cbRedondear.setOnCheckedChangeListener((b, marcado) -> recalcularPrecio());
        v.findViewById(R.id.btnMargen20).setOnClickListener(x -> elegirMargen(20));
        v.findViewById(R.id.btnMargen30).setOnClickListener(x -> elegirMargen(30));
        v.findViewById(R.id.btnMargen50).setOnClickListener(x -> elegirMargen(50));
    }

    private void elegirMargen(double margen) {
        ponerMargen(margen);
        recalcularPrecio();
    }

    private void ponerMargen(double margen) {
        float valor = (float) Math.max(0, Math.min(100, Math.round(margen)));
        sliderMargen.setValue(valor);
        tvMargenValor.setText(getString(R.string.producto_margen_valor, Math.round(margen)));
    }

    /** El trabajador no ve el slider: para él siempre se usa el margen por defecto del negocio. */
    private double margenActual() {
        return esDueno ? sliderMargen.getValue() : margenPorDefecto;
    }

    private void recalcularPrecio() {
        if (actualizando) return;
        double costo = leer(etCosto);
        if (costo > 0) {
            actualizando = true;
            etPrecio.setText(numero(CalculoPrecio.precioSugerido(costo, margenActual(), cbRedondear.isChecked())));
            actualizando = false;
        }
        actualizarSemaforo();
    }

    private void recalcularMargen() {
        if (actualizando) return;
        double costo = leer(etCosto);
        if (costo > 0 && esDueno) {
            double margen = CalculoPrecio.margenSobreCosto(costo, leer(etPrecio));
            sliderMargen.setValue((float) Math.max(0, Math.min(100, Math.round(margen))));
            tvMargenValor.setText(getString(R.string.producto_margen_valor, Math.round(margen)));
        }
        actualizarSemaforo();
    }

    private void actualizarSemaforo() {
        if (!esDueno || tvGanancia == null) return;
        double costo = leer(etCosto);
        double precio = leer(etPrecio);
        String unidad = nombreUnidad(unidadElegida()).toLowerCase(Locale.getDefault());
        int fondo, texto;
        if (costo <= 0 || precio <= 0) {
            tvGanancia.setText(R.string.producto_sin_costo);
            tvPorcentajes.setVisibility(View.GONE);
            tvSemaforo.setVisibility(View.GONE);
            fondo = R.drawable.bg_tarjeta_info;
            texto = R.color.acceso_texto_secundario;
        } else {
            double ganancia = CalculoPrecio.gananciaPorUnidad(costo, precio);
            tvGanancia.setText(getString(ganancia < 0 ? R.string.producto_pierdes : R.string.producto_ganas,
                    dinero(Math.abs(ganancia)), unidad));
            tvPorcentajes.setText(getString(R.string.producto_porcentajes,
                    porcentaje(CalculoPrecio.margenSobreCosto(costo, precio)),
                    porcentaje(CalculoPrecio.margenSobrePrecio(costo, precio))));
            tvPorcentajes.setVisibility(View.VISIBLE);
            tvSemaforo.setVisibility(View.VISIBLE);
            switch (CalculoPrecio.semaforo(costo, precio)) {
                case ROJO:
                    tvSemaforo.setText(R.string.producto_semaforo_rojo);
                    fondo = R.drawable.bg_semaforo_rojo;
                    texto = R.color.semaforo_rojo_texto;
                    break;
                case AMARILLO:
                    tvSemaforo.setText(R.string.producto_semaforo_amarillo);
                    fondo = R.drawable.bg_semaforo_amarillo;
                    texto = R.color.semaforo_amarillo_texto;
                    break;
                default:
                    tvSemaforo.setText(R.string.producto_semaforo_verde);
                    fondo = R.drawable.bg_semaforo_verde;
                    texto = R.color.semaforo_verde_texto;
            }
        }
        int color = ContextCompat.getColor(requireContext(), texto);
        tarjetaGanancia.setBackgroundResource(fondo);
        tvGanancia.setTextColor(color);
        tvPorcentajes.setTextColor(color);
        tvSemaforo.setTextColor(color);
    }

    // ---------- caducidad ----------

    private void elegirFecha() {
        Calendar cal = Calendar.getInstance();
        if (fechaCaducidadMs > 0) cal.setTimeInMillis(fechaCaducidadMs);
        new DatePickerDialog(requireContext(), (dp, anio, mes, dia) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(anio, mes, dia, 0, 0, 0);
            sel.set(Calendar.MILLISECOND, 0);
            fechaCaducidadMs = sel.getTimeInMillis();
            mostrarFecha();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void mostrarFecha() {
        if (fechaCaducidadMs > 0) {
            tvFechaCaducidad.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(new Date(fechaCaducidadMs)));
            tvFechaCaducidad.setTextColor(ContextCompat.getColor(requireContext(), R.color.acceso_texto));
            tvQuitarFecha.setVisibility(View.VISIBLE);
        } else {
            tvFechaCaducidad.setText(R.string.producto_caducidad_vacia);
            tvFechaCaducidad.setTextColor(ContextCompat.getColor(requireContext(), R.color.acceso_texto_pista));
            tvQuitarFecha.setVisibility(View.GONE);
        }
    }

    // ---------- guardar ----------

    private void guardar() {
        tvError.setVisibility(View.GONE);
        tilNombre.setError(null);
        tilPrecio.setError(null);
        tilCosto.setError(null);
        tilCantidad.setError(null);

        String nombre = texto(etNombre).trim();
        if (nombre.isEmpty()) {
            tilNombre.setError(getString(R.string.producto_error_nombre));
            return;
        }
        Double costo = leerONulo(etCosto);
        Double precio = leerONulo(etPrecio);
        Double cantidad = leerONulo(etCantidad);
        Double umbral = leerONulo(etUmbral);
        if (costo == null) {
            tilCosto.setError(getString(R.string.producto_error_numero));
            return;
        }
        if (precio == null || precio <= 0) {
            tilPrecio.setError(getString(R.string.producto_error_precio));
            return;
        }
        if (cantidad == null || umbral == null) {
            tilCantidad.setError(getString(R.string.producto_error_numero));
            return;
        }
        UnidadMedida unidad = unidadElegida();
        if (unidad == UnidadMedida.PZA && cantidad != Math.floor(cantidad)) {
            tilCantidad.setError(getString(R.string.producto_error_pza));
            return;
        }
        if (modoCaducidad == ModoCaducidad.OBLIGATORIA && fechaCaducidadMs == 0) {
            tvError.setText(R.string.producto_error_caducidad);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        ProductoEntity p = new ProductoEntity();
        if (!esNuevo) {
            p.id = productoEditando.id;
            p.timestampCreacionMs = productoEditando.timestampCreacionMs;
            p.activo = productoEditando.activo;
        }
        // Un trabajador que edita no ve el costo: se conserva el que ya tenía.
        boolean costoVisible = esDueno || esNuevo;
        p.costoCompra = costoVisible ? costo : productoEditando.costoCompra;
        p.nombre = nombre;
        p.categoria = texto(etCategoria).trim();
        p.precio = CalculoPrecio.redondearCentavos(precio);
        p.margenPorcentaje = p.costoCompra > 0
                ? CalculoPrecio.redondearCentavos(CalculoPrecio.margenSobreCosto(p.costoCompra, p.precio)) : 0;
        p.unidadMedida = unidad;
        p.cantidadDisponible = cantidad;
        p.umbralStockBajo = umbral;
        p.fechaCaducidadMs = fechaCaducidadMs;
        p.telefonoProveedor = texto(etTelefono).trim();
        p.barcode = texto(etBarcode).trim();

        if (listener != null) listener.onProductoGuardado(p);
        dismiss();
    }

    // ---------- utilidades ----------

    private static String texto(TextView et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    /** Número del campo, 0 si está vacío o no es válido (para cálculos en vivo). */
    private static double leer(TextView et) {
        Double d = leerONulo(et);
        return d == null ? 0 : d;
    }

    /** Vacío = 0; texto que no es número = null (para marcar el error al guardar). */
    private static Double leerONulo(TextView et) {
        String t = texto(et).trim().replace(",", ".");
        if (t.isEmpty()) return 0.0;
        try {
            double d = Double.parseDouble(t);
            return d < 0 ? null : d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 12.0 → "12", 12.5 → "12.5", 0.755 → "0.755". */
    private static String numero(double d) {
        if (d == Math.rint(d)) return String.valueOf((long) d);
        return String.valueOf(Math.round(d * 1000) / 1000.0);
    }

    private static String dinero(double d) {
        return String.format(Locale.getDefault(), "$%.2f", d);
    }

    private static String porcentaje(double d) {
        return String.format(Locale.getDefault(), "%.1f", d);
    }

    /** TextWatcher que solo avisa cuando cambia el texto. */
    private static class Vigilante implements TextWatcher {
        private final Runnable alCambiar;

        Vigilante(Runnable alCambiar) {
            this.alCambiar = alCambiar;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void onTextChanged(CharSequence s, int a, int b, int c) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            alCambiar.run();
        }
    }
}
