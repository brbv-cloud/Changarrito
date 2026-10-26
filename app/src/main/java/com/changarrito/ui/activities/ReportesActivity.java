package com.changarrito.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.MermaConUsuario;
import com.changarrito.database.dao.MermaPorMotivo;
import com.changarrito.database.dao.ProductoHueso;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentasPorCategoria;
import com.changarrito.database.dao.VentasPorHora;
import com.changarrito.database.entity.MotivoMerma;
import com.changarrito.pdf.CompartirArchivo;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.reportes.DiaReporte;
import com.changarrito.reportes.EscalaGrafica;
import com.changarrito.reportes.RangoReporte;
import com.changarrito.reportes.Rankings;
import com.changarrito.reportes.ReparticionPesos;
import com.changarrito.repository.EstadoCuentaRepository;
import com.changarrito.repository.MermaRepository;
import com.changarrito.ui.adapters.BarraHorizontalAdapter;
import com.changarrito.ui.adapters.CierreHistorialAdapter;
import com.changarrito.ui.adapters.PersonaReporteAdapter;
import com.changarrito.ui.graficas.BarraCienView;
import com.changarrito.ui.graficas.DonaView;
import com.changarrito.ui.graficas.GraficaBarrasView;
import com.changarrito.utils.InsetsUtil;
import com.changarrito.viewmodel.CierreCajaViewModel;
import com.changarrito.viewmodel.GananciasViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Reportes del dueno, por pestanas: Resumen, Productos, Horarios, Mermas y Caja.
 * Cada pestana es corta y con graficas sencillas para entenderse de un vistazo;
 * el detalle completo va en el PDF del estado de cuenta.
 *
 * Un trabajador entra solo a la pestana Caja y en modo "cierre a ciegas": cuenta
 * el efectivo sin ver cuanto dice el sistema (EXTRA_CIERRE_CIEGO). En ese modo
 * no se consulta ningun reporte.
 */
public class ReportesActivity extends AppCompatActivity {

    public static final String EXTRA_CIERRE_CIEGO = "cierre_ciego";
    private static final Locale MX = Locale.forLanguageTag("es-MX");
    private static final int MAX_CATEGORIAS = 6;
    private static final int MAX_MERMAS_LISTA = 20;

    private boolean cierreCiego;
    private GananciasViewModel vm;
    private CierreCajaViewModel cierreCajaViewModel;

    private MaterialButtonToggleGroup toggleTabs, toggleRango, toggleTop, toggleHuesos;
    private View seccionResumen, seccionProductos, seccionHorarios, seccionMermas, seccionCaja;

    // Resumen
    private TextView tvVendiste, tvTeQuedo, tvPctQuedo, tvTicket, tvTicketAyuda, tvCompras, tvComprasAyuda;
    private TextView tvExplicaCien, tvDetalleVentasDia, tvAvisoSinCosto;
    private View cardCien, cardVentasDia;
    private BarraCienView barraCien;
    private GraficaBarrasView graficaVentasDia;
    private MaterialButton btnEstadoCuenta;

    // Productos
    private TextView tvTopAyuda, tvTopVacio, tvCategoriasVacio, tvHuesosVacio;
    private DonaView donaCategorias;
    private RecyclerView rvTop, rvCategorias, rvHuesos;

    // Horarios
    private TextView tvHoraPico, tvDetalleHora, tvPorPersonaVacio;
    private GraficaBarrasView graficaHoras;
    private RecyclerView rvPorPersona;

    // Mermas
    private TextView tvMermaTotal, tvMermaResumen, tvMermasVacio;
    private RecyclerView rvMermaMotivos, rvMermas;

    // Caja
    private TextView tvEfectivoSistema, tvResultadoCierre;
    private EditText etEfectivoContado, etNotasCierre;
    private MaterialButton btnHacerCierre;
    private RecyclerView rvHistorialCierres;
    private CierreHistorialAdapter cierreHistorialAdapter;

    private final BarraHorizontalAdapter topAdapter = new BarraHorizontalAdapter();
    private final BarraHorizontalAdapter categoriasAdapter = new BarraHorizontalAdapter();
    private final BarraHorizontalAdapter huesosAdapter = new BarraHorizontalAdapter();
    private final BarraHorizontalAdapter motivosAdapter = new BarraHorizontalAdapter();
    private final BarraHorizontalAdapter mermasAdapter = new BarraHorizontalAdapter();
    private final PersonaReporteAdapter personasAdapter = new PersonaReporteAdapter();

    private List<GananciaProducto> productos = new ArrayList<>();
    private List<DiaReporte> dias = new ArrayList<>();
    private int[] porHora = new int[24];
    private int primeraHora;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootReportes), true, true);

        vincularVistas();
        cierreCajaViewModel = new ViewModelProvider(this).get(CierreCajaViewModel.class);
        configurarCaja();
        configurarTabs();

        cierreCiego = getIntent().getBooleanExtra(EXTRA_CIERRE_CIEGO, false);
        if (cierreCiego) {
            aplicarModoCierreCiego();
        } else {
            configurarReportes();
        }
        cierreCajaViewModel.actualizarEfectivoPendiente();
    }

    private void vincularVistas() {
        toggleTabs = findViewById(R.id.toggleTabs);
        toggleRango = findViewById(R.id.toggleRango);
        toggleTop = findViewById(R.id.toggleTop);
        toggleHuesos = findViewById(R.id.toggleHuesos);
        seccionResumen = findViewById(R.id.seccionResumen);
        seccionProductos = findViewById(R.id.seccionProductos);
        seccionHorarios = findViewById(R.id.seccionHorarios);
        seccionMermas = findViewById(R.id.seccionMermas);
        seccionCaja = findViewById(R.id.seccionCaja);

        tvVendiste = findViewById(R.id.tvVendiste);
        tvTeQuedo = findViewById(R.id.tvTeQuedo);
        tvPctQuedo = findViewById(R.id.tvPctQuedo);
        tvTicket = findViewById(R.id.tvTicket);
        tvTicketAyuda = findViewById(R.id.tvTicketAyuda);
        tvCompras = findViewById(R.id.tvCompras);
        tvComprasAyuda = findViewById(R.id.tvComprasAyuda);
        tvExplicaCien = findViewById(R.id.tvExplicaCien);
        tvDetalleVentasDia = findViewById(R.id.tvDetalleVentasDia);
        tvAvisoSinCosto = findViewById(R.id.tvAvisoSinCosto);
        cardCien = findViewById(R.id.cardCien);
        cardVentasDia = findViewById(R.id.cardVentasDia);
        barraCien = findViewById(R.id.barraCien);
        graficaVentasDia = findViewById(R.id.graficaVentasDia);
        btnEstadoCuenta = findViewById(R.id.btnEstadoCuenta);

        tvTopAyuda = findViewById(R.id.tvTopAyuda);
        tvTopVacio = findViewById(R.id.tvTopVacio);
        tvCategoriasVacio = findViewById(R.id.tvCategoriasVacio);
        tvHuesosVacio = findViewById(R.id.tvHuesosVacio);
        donaCategorias = findViewById(R.id.donaCategorias);
        rvTop = findViewById(R.id.rvTop);
        rvCategorias = findViewById(R.id.rvCategorias);
        rvHuesos = findViewById(R.id.rvHuesos);

        tvHoraPico = findViewById(R.id.tvHoraPico);
        tvDetalleHora = findViewById(R.id.tvDetalleHora);
        tvPorPersonaVacio = findViewById(R.id.tvPorPersonaVacio);
        graficaHoras = findViewById(R.id.graficaHoras);
        rvPorPersona = findViewById(R.id.rvPorPersona);

        tvMermaTotal = findViewById(R.id.tvMermaTotal);
        tvMermaResumen = findViewById(R.id.tvMermaResumen);
        tvMermasVacio = findViewById(R.id.tvMermasVacio);
        rvMermaMotivos = findViewById(R.id.rvMermaMotivos);
        rvMermas = findViewById(R.id.rvMermas);

        tvEfectivoSistema = findViewById(R.id.tvEfectivoSistema);
        tvResultadoCierre = findViewById(R.id.tvResultadoCierre);
        etEfectivoContado = findViewById(R.id.etEfectivoContado);
        etNotasCierre = findViewById(R.id.etNotasCierre);
        btnHacerCierre = findViewById(R.id.btnHacerCierre);
        rvHistorialCierres = findViewById(R.id.rvHistorialCierres);
    }

    private void configurarTabs() {
        toggleTabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            seccionResumen.setVisibility(checkedId == R.id.btnTabResumen ? View.VISIBLE : View.GONE);
            seccionProductos.setVisibility(checkedId == R.id.btnTabProductos ? View.VISIBLE : View.GONE);
            seccionHorarios.setVisibility(checkedId == R.id.btnTabHorarios ? View.VISIBLE : View.GONE);
            seccionMermas.setVisibility(checkedId == R.id.btnTabMermas ? View.VISIBLE : View.GONE);
            seccionCaja.setVisibility(checkedId == R.id.btnTabCaja ? View.VISIBLE : View.GONE);
            // El periodo no aplica a la caja.
            toggleRango.setVisibility(checkedId == R.id.btnTabCaja || cierreCiego ? View.GONE : View.VISIBLE);
            if (checkedId == R.id.btnTabCaja) cierreCajaViewModel.actualizarEfectivoPendiente();
        });
    }

    private static void lista(RecyclerView rv, RecyclerView.Adapter<?> adapter) {
        rv.setLayoutManager(new LinearLayoutManager(rv.getContext()));
        rv.setAdapter(adapter);
    }

    // ================================================================== reportes

    private void configurarReportes() {
        vm = new ViewModelProvider(this).get(GananciasViewModel.class);

        toggleRango.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnRango7) vm.setRango(RangoReporte.SIETE_DIAS);
            else if (checkedId == R.id.btnRango30) vm.setRango(RangoReporte.TREINTA_DIAS);
            else vm.setRango(RangoReporte.HOY);
        });

        lista(rvTop, topAdapter);
        lista(rvCategorias, categoriasAdapter);
        lista(rvHuesos, huesosAdapter);
        lista(rvPorPersona, personasAdapter);
        lista(rvMermaMotivos, motivosAdapter);
        lista(rvMermas, mermasAdapter);

        vm.getResumen().observe(this, this::mostrarResumen);
        vm.getPorDia().observe(this, d -> {
            dias = d != null ? d : new ArrayList<>();
            mostrarVentasPorDia();
        });
        graficaVentasDia.setOnBarraTocada(i -> {
            if (i < dias.size()) {
                tvDetalleVentasDia.setText(getString(R.string.rep_detalle_dia,
                        fechaLarga(dias.get(i).diaMs), CalculoGanancias.dinero(dias.get(i).ventas)));
            }
        });

        vm.getPorProducto().observe(this, p -> {
            productos = p != null ? p : new ArrayList<>();
            mostrarTop();
        });
        toggleTop.addOnButtonCheckedListener((g, id, checked) -> {
            if (checked) mostrarTop();
        });
        vm.getPorCategoria().observe(this, this::mostrarCategorias);
        toggleHuesos.addOnButtonCheckedListener((g, id, checked) -> {
            if (checked) vm.setDiasHueso(id == R.id.btnHuesos60 ? 60 : 30);
        });
        vm.getHuesos().observe(this, this::mostrarHuesos);

        vm.getPorHora().observe(this, this::mostrarHoras);
        graficaHoras.setOnBarraTocada(i -> {
            int h = primeraHora + i;
            if (h >= 0 && h < 24) {
                tvDetalleHora.setText(getString(R.string.rep_detalle_hora, Rankings.etiquetaHora(h),
                        Rankings.etiquetaHora((h + 1) % 24),
                        getResources().getQuantityString(R.plurals.rep_compras_n, porHora[h], porHora[h])));
            }
        });
        vm.getPorPersona().observe(this, filas -> {
            personasAdapter.setFilas(filas);
            boolean vacio = filas == null || filas.isEmpty();
            tvPorPersonaVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
            rvPorPersona.setVisibility(vacio ? View.GONE : View.VISIBLE);
        });

        vm.getMermasPorMotivo().observe(this, this::mostrarMermasPorMotivo);
        vm.getMermas().observe(this, this::mostrarMermas);

        btnEstadoCuenta.setOnClickListener(v -> generarEstadoCuenta(vm.getRangoActual()));
    }

    // ---------------------------------------------------------------- Resumen

    private void mostrarResumen(ResumenGanancia r) {
        tvVendiste.setText(CalculoGanancias.dinero(r != null ? r.ventas : 0));
        double ganancia = CalculoGanancias.ganancia(r);
        tvTeQuedo.setText(CalculoGanancias.dinero(ganancia));
        tvTeQuedo.setTextColor(ContextCompat.getColor(this,
                ganancia < 0 ? R.color.status_rojo : R.color.color_primary_variant));
        // En la app la ganancia se explica solo en pesos ("de cada $100..."); los dos
        // porcentajes (sobre costo y del precio) van en el PDF, con su explicacion.
        tvPctQuedo.setVisibility(View.GONE);
        tvTicket.setText(CalculoGanancias.dinero(Rankings.ticketPromedio(r)));
        tvTicketAyuda.setText(R.string.rep_ticket_ayuda);
        tvCompras.setText(String.valueOf(r != null ? r.tickets : 0));
        tvComprasAyuda.setText(R.string.rep_compras_ayuda);

        double[] reparto = r != null ? ReparticionPesos.deCada100(r.ventasConCosto, r.costo) : null;
        cardCien.setVisibility(reparto != null ? View.VISIBLE : View.GONE);
        if (reparto != null) {
            barraCien.setReparto(reparto);
            tvExplicaCien.setText(reparto[1] < 0
                    ? getString(R.string.grafica_cien_perdida, pesosEnteros(-reparto[1]))
                    : getString(R.string.grafica_cien_explica, pesosEnteros(reparto[0]), pesosEnteros(reparto[1])));
        }

        if (r != null && r.ventasSinCosto > 0) {
            String cuantas = getResources().getQuantityString(R.plurals.ganancia_ventas, r.ventasSinCosto, r.ventasSinCosto);
            tvAvisoSinCosto.setText(getString(R.string.ganancia_aviso_sin_costo,
                    cuantas, CalculoGanancias.dinero(r.importeSinCosto)));
            tvAvisoSinCosto.setVisibility(View.VISIBLE);
        } else {
            tvAvisoSinCosto.setVisibility(View.GONE);
        }
    }

    private void mostrarVentasPorDia() {
        boolean varios = dias.size() > 1;
        cardVentasDia.setVisibility(varios ? View.VISIBLE : View.GONE);
        if (!varios) return;
        int n = dias.size();
        String[] etiquetas = new String[n];
        double[] valores = new double[n];
        SimpleDateFormat fmt = new SimpleDateFormat(n <= 7 ? "EEE d" : "d/M", MX);
        int mejor = -1;
        for (int i = 0; i < n; i++) {
            etiquetas[i] = fmt.format(new Date(dias.get(i).diaMs)).replace(".", "");
            valores[i] = dias.get(i).ventas;
            if (valores[i] > 0 && (mejor < 0 || valores[i] > valores[mejor])) mejor = i;
        }
        graficaVentasDia.setDatos(etiquetas, valores, mejor,
                ContextCompat.getColor(this, R.color.marca_verde), EscalaGrafica::dineroCorto);
        tvDetalleVentasDia.setText(mejor < 0 ? getString(R.string.ganancia_sin_ventas)
                : getString(R.string.rep_mejor_dia, fechaLarga(dias.get(mejor).diaMs),
                CalculoGanancias.dinero(valores[mejor])));
    }

    // ---------------------------------------------------------------- Productos

    private void mostrarTop() {
        boolean rentables = toggleTop.getCheckedButtonId() == R.id.btnTopRentables;
        tvTopAyuda.setText(rentables ? R.string.rep_top_rentables_ayuda : R.string.rep_top_vendidos_ayuda);
        List<GananciaProducto> top = rentables ? Rankings.masRentables(productos, Rankings.TOP)
                : Rankings.masVendidos(productos, Rankings.TOP);
        double max = 0;
        for (GananciaProducto p : top) max = Math.max(max, rentables ? p.ganancia() : p.cantidad);
        int color = ContextCompat.getColor(this, rentables ? R.color.marca_verde : R.color.categoria_2);
        List<BarraHorizontalAdapter.Fila> filas = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            GananciaProducto p = top.get(i);
            String cantidad = MermaRepository.cantidadTexto(p.cantidad, p.unidadMedida);
            filas.add(new BarraHorizontalAdapter.Fila(String.valueOf(i + 1), p.nombre,
                    rentables ? cantidad + " vendidos" : CalculoGanancias.dinero(p.ventas),
                    rentables ? CalculoGanancias.dinero(p.ganancia()) : cantidad,
                    max > 0 ? (float) ((rentables ? p.ganancia() : p.cantidad) / max) : 0, color));
        }
        topAdapter.setFilas(filas);
        tvTopVacio.setVisibility(filas.isEmpty() ? View.VISIBLE : View.GONE);
        rvTop.setVisibility(filas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private int[] coloresCategorias(List<VentasPorCategoria> cats) {
        int[] paleta = {
                ContextCompat.getColor(this, R.color.categoria_1),
                ContextCompat.getColor(this, R.color.categoria_2),
                ContextCompat.getColor(this, R.color.categoria_3),
                ContextCompat.getColor(this, R.color.categoria_4),
                ContextCompat.getColor(this, R.color.categoria_5)};
        int otras = ContextCompat.getColor(this, R.color.categoria_otras);
        int[] colores = new int[cats.size()];
        for (int i = 0; i < cats.size(); i++) {
            colores[i] = Rankings.OTRAS.equals(cats.get(i).categoria) ? otras : paleta[i % paleta.length];
        }
        return colores;
    }

    private void mostrarCategorias(List<VentasPorCategoria> datos) {
        List<VentasPorCategoria> cats = Rankings.categoriasPrincipales(datos, MAX_CATEGORIAS);
        double total = Rankings.total(cats);
        boolean vacio = cats.isEmpty();
        tvCategoriasVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        donaCategorias.setVisibility(vacio ? View.GONE : View.VISIBLE);
        rvCategorias.setVisibility(vacio ? View.GONE : View.VISIBLE);
        if (vacio) return;

        int[] colores = coloresCategorias(cats);
        double[] valores = new double[cats.size()];
        double max = 0;
        for (int i = 0; i < cats.size(); i++) {
            valores[i] = cats.get(i).ventas;
            max = Math.max(max, valores[i]);
        }
        donaCategorias.setDatos(valores, colores, EscalaGrafica.dineroCorto(total), getString(R.string.rep_categorias_centro));
        List<BarraHorizontalAdapter.Fila> filas = new ArrayList<>();
        for (int i = 0; i < cats.size(); i++) {
            VentasPorCategoria c = cats.get(i);
            filas.add(new BarraHorizontalAdapter.Fila(null, c.categoria, null,
                    String.format(MX, "%.0f%%", total > 0 ? c.ventas * 100 / total : 0) + " · "
                            + CalculoGanancias.dinero(c.ventas),
                    max > 0 ? (float) (c.ventas / max) : 0, colores[i]));
        }
        categoriasAdapter.setFilas(filas);
    }

    private void mostrarHuesos(List<ProductoHueso> huesos) {
        List<BarraHorizontalAdapter.Fila> filas = new ArrayList<>();
        long ahora = System.currentTimeMillis();
        if (huesos != null) {
            for (ProductoHueso h : huesos) {
                String quedan = MermaRepository.cantidadTexto(h.existencia, h.unidadMedida);
                String sub = h.ultimaVentaMs != null
                        ? getString(R.string.rep_hueso_sub_venta, Rankings.diasEntre(h.ultimaVentaMs, ahora), quedan)
                        : getString(R.string.rep_hueso_sub_nunca, quedan);
                String valor = h.dineroDetenido() > 0
                        ? getString(R.string.rep_hueso_detenido, CalculoGanancias.dinero(h.dineroDetenido())) : "";
                filas.add(new BarraHorizontalAdapter.Fila(null, h.nombre, sub, valor, -1, 0));
            }
        }
        huesosAdapter.setFilas(filas);
        tvHuesosVacio.setVisibility(filas.isEmpty() ? View.VISIBLE : View.GONE);
        rvHuesos.setVisibility(filas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ---------------------------------------------------------------- Horarios

    private void mostrarHoras(List<VentasPorHora> datos) {
        porHora = Rankings.ticketsPorHora(datos);
        int pico = Rankings.horaPico(porHora);
        graficaHoras.setVisibility(pico < 0 ? View.GONE : View.VISIBLE);
        tvDetalleHora.setText("");
        if (pico < 0) {
            tvHoraPico.setText(R.string.rep_hora_pico_sin);
            return;
        }
        tvHoraPico.setText(getString(R.string.rep_hora_pico, Rankings.etiquetaHora(pico),
                Rankings.etiquetaHora((pico + 1) % 24)));
        int[] visibles = Rankings.horasVisibles(porHora);
        primeraHora = visibles[0];
        int n = visibles[1] - visibles[0] + 1;
        String[] etiquetas = new String[n];
        double[] valores = new double[n];
        for (int i = 0; i < n; i++) {
            etiquetas[i] = Rankings.etiquetaHora(primeraHora + i).replace(" ", "");
            valores[i] = porHora[primeraHora + i];
        }
        graficaHoras.setDatos(etiquetas, valores, pico - primeraHora,
                ContextCompat.getColor(this, R.color.marca_ambar), v -> String.valueOf((long) v));
    }

    // ---------------------------------------------------------------- Mermas

    private void mostrarMermasPorMotivo(List<MermaPorMotivo> datos) {
        double total = 0;
        double max = 0;
        int registros = 0;
        if (datos != null) {
            for (MermaPorMotivo m : datos) {
                total += m.costo;
                registros += m.registros;
                max = Math.max(max, m.costo);
            }
        }
        tvMermaTotal.setText(CalculoGanancias.dinero(total));
        tvMermaResumen.setText(getString(R.string.rep_mermas_resumen, registros));
        int rojo = ContextCompat.getColor(this, R.color.status_rojo);
        List<BarraHorizontalAdapter.Fila> filas = new ArrayList<>();
        if (datos != null) {
            for (MermaPorMotivo m : datos) {
                filas.add(new BarraHorizontalAdapter.Fila(null, MotivoMerma.nombre(m.motivo),
                        m.registros + (m.registros == 1 ? " registro" : " registros"),
                        CalculoGanancias.dinero(m.costo), max > 0 ? (float) (m.costo / max) : 0, rojo));
            }
        }
        motivosAdapter.setFilas(filas);
        rvMermaMotivos.setVisibility(filas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void mostrarMermas(List<MermaConUsuario> datos) {
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", MX);
        List<BarraHorizontalAdapter.Fila> filas = new ArrayList<>();
        if (datos != null) {
            for (int i = 0; i < Math.min(MAX_MERMAS_LISTA, datos.size()); i++) {
                MermaConUsuario mu = datos.get(i);
                String sub = getString(R.string.rep_merma_sub, fmt.format(new Date(mu.merma.timestampMs)),
                        MotivoMerma.nombre(mu.merma.motivo != null ? mu.merma.motivo.name() : null),
                        mu.nombreUsuario != null ? mu.nombreUsuario : getString(R.string.rep_sin_turno));
                if (mu.merma.nota != null) sub += " · " + mu.merma.nota;
                filas.add(new BarraHorizontalAdapter.Fila(null,
                        mu.merma.nombreProducto + " · " + MermaRepository.cantidadTexto(mu.merma.cantidad, mu.merma.unidadMedida),
                        sub, mu.merma.costoTotal() > 0 ? CalculoGanancias.dinero(mu.merma.costoTotal()) : "", -1, 0));
            }
        }
        mermasAdapter.setFilas(filas);
        tvMermasVacio.setVisibility(filas.isEmpty() ? View.VISIBLE : View.GONE);
        rvMermas.setVisibility(filas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ---------------------------------------------------------------- PDF

    private void generarEstadoCuenta(RangoReporte rango) {
        btnEstadoCuenta.setEnabled(false);
        btnEstadoCuenta.setText(R.string.pdf_generando);
        new EstadoCuentaRepository(getApplication()).generar(rango, new EstadoCuentaRepository.Callback() {
            @Override
            public void onListo(File pdf, int paginas) {
                if (isFinishing() || isDestroyed()) return;
                btnEstadoCuenta.setEnabled(true);
                btnEstadoCuenta.setText(R.string.pdf_boton);
                ofrecerPdf(pdf, paginas, rango);
            }

            @Override
            public void onError(String mensaje) {
                if (isFinishing() || isDestroyed()) return;
                btnEstadoCuenta.setEnabled(true);
                btnEstadoCuenta.setText(R.string.pdf_boton);
                Toast.makeText(ReportesActivity.this, getString(R.string.pdf_error, mensaje), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void ofrecerPdf(File pdf, int paginas, RangoReporte rango) {
        long[] r = rango.calcular(System.currentTimeMillis());
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy", MX);
        String ini = f.format(new Date(r[0]));
        String fin = f.format(new Date(r[1]));
        String periodo = ini.equals(fin) ? ini : ini + " – " + fin;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.pdf_listo_titulo)
                .setMessage(getString(R.string.pdf_listo_texto, periodo, paginas))
                .setPositiveButton(R.string.pdf_compartir, (d, w) ->
                        CompartirArchivo.compartir(this, pdf, getString(R.string.pdf_asunto, periodo)))
                .setNegativeButton(R.string.pdf_ver, (d, w) -> {
                    if (!CompartirArchivo.ver(this, pdf)) {
                        Toast.makeText(this, R.string.pdf_sin_visor, Toast.LENGTH_LONG).show();
                    }
                })
                .setNeutralButton(R.string.pdf_guardar, (d, w) -> Toast.makeText(this,
                        CompartirArchivo.guardarEnDescargas(this, pdf) ? R.string.pdf_guardado : R.string.pdf_no_guardado,
                        Toast.LENGTH_LONG).show())
                .show();
    }

    /** "$77": en la app basta con pesos enteros. */
    private static String pesosEnteros(double valor) {
        return String.format(MX, "$%,.0f", valor);
    }

    /** "Sábado 24 de octubre" */
    private static String fechaLarga(long ms) {
        String t = new SimpleDateFormat("EEEE d 'de' MMMM", MX).format(new Date(ms));
        return t.isEmpty() ? t : t.substring(0, 1).toUpperCase(MX) + t.substring(1);
    }

    // ================================================================== caja

    private void configurarCaja() {
        cierreHistorialAdapter = new CierreHistorialAdapter();
        lista(rvHistorialCierres, cierreHistorialAdapter);

        cierreCajaViewModel.getHistorial().observe(this, cierreHistorialAdapter::setCierres);
        cierreCajaViewModel.getEfectivoPendienteDeCierre().observe(this, efectivo -> {
            double valor = efectivo != null ? efectivo : 0;
            tvEfectivoSistema.setText(String.format(Locale.getDefault(), "$%.2f", valor));
        });
        cierreCajaViewModel.getUltimoResultado().observe(this, cierre -> {
            if (cierre == null) return;
            mostrarResultadoCierre(cierre.diferencia);
            etEfectivoContado.setText("");
            etNotasCierre.setText("");
        });
        btnHacerCierre.setOnClickListener(v -> intentarHacerCierre());
    }

    private void intentarHacerCierre() {
        String textoContado = etEfectivoContado.getText().toString().trim().replace(',', '.');
        if (TextUtils.isEmpty(textoContado)) {
            Toast.makeText(this, "Captura el efectivo contado", Toast.LENGTH_SHORT).show();
            return;
        }
        double contado;
        try {
            contado = Double.parseDouble(textoContado);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Cantidad invalida", Toast.LENGTH_SHORT).show();
            return;
        }
        if (contado < 0) {
            Toast.makeText(this, "La cantidad no puede ser negativa", Toast.LENGTH_SHORT).show();
            return;
        }
        cierreCajaViewModel.hacerCierre(contado, etNotasCierre.getText().toString().trim());
    }

    private void aplicarModoCierreCiego() {
        toggleTabs.check(R.id.btnTabCaja);
        findViewById(R.id.scrollTabs).setVisibility(View.GONE);
        toggleRango.setVisibility(View.GONE);
        ((TextView) findViewById(R.id.tvTituloReportes)).setText(R.string.cierre_ciego_titulo);
        ((TextView) findViewById(R.id.tvEtiquetaEfectivo)).setText(R.string.cierre_ciego_ayuda);
        tvEfectivoSistema.setVisibility(View.GONE);
        findViewById(R.id.tvTituloHistorial).setVisibility(View.GONE);
        findViewById(R.id.cardHistorial).setVisibility(View.GONE);
    }

    private void mostrarResultadoCierre(double diferencia) {
        tvResultadoCierre.setVisibility(View.VISIBLE);
        if (cierreCiego) {
            tvResultadoCierre.setText(R.string.cierre_ciego_registrado);
            tvResultadoCierre.setTextColor(ContextCompat.getColor(this, R.color.status_verde));
            return;
        }
        double epsilon = 0.01;
        if (Math.abs(diferencia) < epsilon) {
            tvResultadoCierre.setText("Cuadre exacto");
            tvResultadoCierre.setTextColor(ContextCompat.getColor(this, R.color.status_verde));
        } else if (diferencia > 0) {
            tvResultadoCierre.setText(String.format(Locale.getDefault(), "Sobrante: $%.2f", diferencia));
            tvResultadoCierre.setTextColor(ContextCompat.getColor(this, R.color.status_verde));
        } else {
            tvResultadoCierre.setText(String.format(Locale.getDefault(), "Faltante: $%.2f", Math.abs(diferencia)));
            tvResultadoCierre.setTextColor(ContextCompat.getColor(this, R.color.status_rojo));
        }
    }
}
