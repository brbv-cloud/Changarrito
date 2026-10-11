package com.changarrito.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.dao.IngresoDia;
import com.changarrito.ui.adapters.CierreHistorialAdapter;
import com.changarrito.ui.adapters.ProductoTopAdapter;
import com.changarrito.utils.InsetsUtil;
import com.changarrito.viewmodel.CierreCajaViewModel;
import com.changarrito.viewmodel.ReportesViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pantalla de Reportes: 4 secciones que se muestran/ocultan con un toggle de
 * botones (en vez de TabLayout+ViewPager2, para no sumar otra dependencia):
 * Resumen de hoy, grafica de ingresos de 7 dias, top 5 productos y Cierre de Caja.
 *
 * La "grafica" de barras (ingresos por dia y top productos) se arma con Views
 * planos coloreados en vez de una libreria de graficas o Canvas: mas simple y
 * sin riesgo de romper el build por una dependencia externa (ver ProductoTopAdapter
 * y construirGraficaIngresos()).
 */
public class ReportesActivity extends AppCompatActivity {

    /**
     * true = el que abre la pantalla es un trabajador: solo ve el cierre de caja y
     * cuenta "a ciegas", sin ver cuánto efectivo dice el sistema ni el resultado.
     */
    public static final String EXTRA_CIERRE_CIEGO = "cierre_ciego";

    private boolean cierreCiego;

    private MaterialButtonToggleGroup toggleTabs;
    private View seccionHoy, seccionSieteDias, seccionTop, seccionCaja;
    private TextView tvIngresosHoy, tvTransaccionesHoy, tvProductoTopHoy;
    private LinearLayout llGraficaIngresos;
    private RecyclerView rvProductosTop, rvHistorialCierres;
    private TextView tvSinVentasTop;
    private TextView tvEfectivoSistema, tvResultadoCierre;
    private EditText etEfectivoContado, etNotasCierre;
    private MaterialButton btnHacerCierre;

    private ProductoTopAdapter productoTopAdapter;
    private CierreHistorialAdapter cierreHistorialAdapter;

    private ReportesViewModel reportesViewModel;
    private CierreCajaViewModel cierreCajaViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reportes);

        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootReportes), true, true);

        vincularVistas();
        configurarListas();

        reportesViewModel = new ViewModelProvider(this).get(ReportesViewModel.class);
        cierreCajaViewModel = new ViewModelProvider(this).get(CierreCajaViewModel.class);

        configurarTabs();
        cierreCiego = getIntent().getBooleanExtra(EXTRA_CIERRE_CIEGO, false);
        if (cierreCiego) aplicarModoCierreCiego();
        observarReportes();
        observarCierreCaja();

        cierreCajaViewModel.actualizarEfectivoPendiente();

        btnHacerCierre.setOnClickListener(v -> intentarHacerCierre());
    }

    private void vincularVistas() {
        toggleTabs = findViewById(R.id.toggleTabs);
        seccionHoy = findViewById(R.id.seccionHoy);
        seccionSieteDias = findViewById(R.id.seccion7Dias);
        seccionTop = findViewById(R.id.seccionTop);
        seccionCaja = findViewById(R.id.seccionCaja);

        tvIngresosHoy = findViewById(R.id.tvIngresosHoy);
        tvTransaccionesHoy = findViewById(R.id.tvTransaccionesHoy);
        tvProductoTopHoy = findViewById(R.id.tvProductoTopHoy);
        llGraficaIngresos = findViewById(R.id.llGraficaIngresos);
        rvProductosTop = findViewById(R.id.rvProductosTop);
        tvSinVentasTop = findViewById(R.id.tvSinVentasTop);
        rvHistorialCierres = findViewById(R.id.rvHistorialCierres);

        tvEfectivoSistema = findViewById(R.id.tvEfectivoSistema);
        etEfectivoContado = findViewById(R.id.etEfectivoContado);
        etNotasCierre = findViewById(R.id.etNotasCierre);
        btnHacerCierre = findViewById(R.id.btnHacerCierre);
        tvResultadoCierre = findViewById(R.id.tvResultadoCierre);
    }

    private void configurarTabs() {
        toggleTabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            seccionHoy.setVisibility(checkedId == R.id.btnTabHoy ? View.VISIBLE : View.GONE);
            seccionSieteDias.setVisibility(checkedId == R.id.btnTab7Dias ? View.VISIBLE : View.GONE);
            seccionTop.setVisibility(checkedId == R.id.btnTabTop ? View.VISIBLE : View.GONE);
            seccionCaja.setVisibility(checkedId == R.id.btnTabCaja ? View.VISIBLE : View.GONE);

            if (checkedId == R.id.btnTabCaja) {
                cierreCajaViewModel.actualizarEfectivoPendiente();
            }
        });
    }

    private void configurarListas() {
        productoTopAdapter = new ProductoTopAdapter();
        rvProductosTop.setLayoutManager(new LinearLayoutManager(this));
        rvProductosTop.setAdapter(productoTopAdapter);

        cierreHistorialAdapter = new CierreHistorialAdapter();
        rvHistorialCierres.setLayoutManager(new LinearLayoutManager(this));
        rvHistorialCierres.setAdapter(cierreHistorialAdapter);
    }

    private void observarReportes() {
        reportesViewModel.getIngresosHoy().observe(this, ingresos -> {
            double valor = ingresos != null ? ingresos : 0;
            tvIngresosHoy.setText(String.format(Locale.getDefault(), "$%.2f", valor));
        });

        reportesViewModel.getTransaccionesHoy().observe(this, conteo -> {
            int cantidad = conteo != null ? conteo : 0;
            tvTransaccionesHoy.setText(cantidad + (cantidad == 1 ? " transaccion" : " transacciones"));
        });

        reportesViewModel.getProductoTopHoy().observe(this, producto ->
                tvProductoTopHoy.setText(producto != null ? producto.nombre : "Sin ventas todavia"));

        reportesViewModel.getIngresosUltimos7Dias().observe(this, this::construirGraficaIngresos);

        reportesViewModel.getProductosMasVendidos().observe(this, productos -> {
            productoTopAdapter.setProductos(productos);
            boolean vacio = productos == null || productos.isEmpty();
            tvSinVentasTop.setVisibility(vacio ? View.VISIBLE : View.GONE);
            rvProductosTop.setVisibility(vacio ? View.GONE : View.VISIBLE);
        });
    }

    private void observarCierreCaja() {
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
    }

    private void intentarHacerCierre() {
        String textoContado = etEfectivoContado.getText().toString().trim();
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

        String notas = etNotasCierre.getText().toString().trim();
        cierreCajaViewModel.hacerCierre(contado, notas);
    }

    private void aplicarModoCierreCiego() {
        toggleTabs.check(R.id.btnTabCaja);
        findViewById(R.id.scrollTabs).setVisibility(View.GONE);
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

    /**
     * Arma la grafica de barras de ingresos de los ultimos 7 dias con Views planos
     * (LinearLayout + View coloreado): cada columna es un LinearLayout vertical con
     * una barra de altura proporcional al maximo de la semana, y una etiqueta con
     * el dia debajo. Rellena con 0 los dias sin ventas para no dejar huecos.
     */
    private void construirGraficaIngresos(List<IngresoDia> datos) {
        Map<String, Double> porFecha = new HashMap<>();
        if (datos != null) {
            for (IngresoDia d : datos) {
                porFecha.put(d.fecha, d.total);
            }
        }

        SimpleDateFormat claveFormato = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat etiquetaFormato = new SimpleDateFormat("dd/MM", Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        List<Double> valores = new ArrayList<>();
        List<String> etiquetas = new ArrayList<>();
        double maxValor = 0;

        for (int i = 0; i < 7; i++) {
            String clave = claveFormato.format(cal.getTime());
            Double valor = porFecha.get(clave);
            double valorFinal = valor != null ? valor : 0.0;
            valores.add(valorFinal);
            etiquetas.add(etiquetaFormato.format(cal.getTime()));
            maxValor = Math.max(maxValor, valorFinal);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        llGraficaIngresos.removeAllViews();
        int alturaMaximaPx = dpAPx(110);

        for (int i = 0; i < 7; i++) {
            double valor = valores.get(i);
            int alturaPx = maxValor > 0
                    ? Math.max(dpAPx(4), (int) (alturaMaximaPx * (valor / maxValor)))
                    : dpAPx(4);

            LinearLayout columna = new LinearLayout(this);
            columna.setOrientation(LinearLayout.VERTICAL);
            columna.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            columna.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

            View barra = new View(this);
            barra.setLayoutParams(new LinearLayout.LayoutParams(dpAPx(20), alturaPx));
            barra.setBackgroundColor(ContextCompat.getColor(this, R.color.color_primary));

            TextView tvEtiqueta = new TextView(this);
            tvEtiqueta.setText(etiquetas.get(i));
            tvEtiqueta.setTextSize(10);
            tvEtiqueta.setTextColor(ContextCompat.getColor(this, R.color.color_slate_medium));
            tvEtiqueta.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams paramsEtiqueta = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            paramsEtiqueta.topMargin = dpAPx(4);
            tvEtiqueta.setLayoutParams(paramsEtiqueta);

            columna.addView(barra);
            columna.addView(tvEtiqueta);
            llGraficaIngresos.addView(columna);
        }
    }

    private int dpAPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
