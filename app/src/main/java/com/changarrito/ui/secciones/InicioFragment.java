package com.changarrito.ui.secciones;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.changarrito.R;
import com.changarrito.negocio.SesionTurno;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.ui.activities.negocio.AjustesActivity;
import com.changarrito.viewmodel.InicioViewModel;

import java.util.Locale;

/**
 * Inicio (lo primero despues de entrar): saludo, tres numeros del dia y accesos grandes
 * a lo que mas se usa. El trabajador no ve dinero: en lugar de "Ventas de hoy" ve
 * cuantas compras van, y "Reportes" es "Cerrar caja".
 */
public class InicioFragment extends SeccionFragment {

    private InicioViewModel vm;
    private boolean esDueno;

    public InicioFragment() {
        super(R.layout.fragment_inicio);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(this).get(InicioViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        esDueno = SesionTurno.esDueno(requireContext());

        ((TextView) vista(R.id.tvEtiquetaVentas)).setText(
                esDueno ? R.string.inicio_ventas_hoy : R.string.inicio_compras_hoy);
        ((TextView) vista(R.id.tvAccesoReportes)).setText(
                esDueno ? R.string.inicio_ver_reportes : R.string.inicio_cerrar_caja);
        ((ImageView) vista(R.id.ivAccesoReportes)).setImageResource(
                esDueno ? R.drawable.ic_nav_reportes : R.drawable.ic_nav_caja);

        vm.getConfiguracion().observe(getViewLifecycleOwner(), c -> {
            if (c != null && c.nombreNegocio != null) {
                ((TextView) vista(R.id.tvNegocio)).setText(c.nombreNegocio);
            }
        });
        vm.getProductos().observe(getViewLifecycleOwner(), n ->
                ((TextView) vista(R.id.tvNumProductos)).setText(String.valueOf(n != null ? n : 0)));
        vm.getHoy().observe(getViewLifecycleOwner(), r -> {
            TextView tv = vista(R.id.tvVentasHoy);
            if (esDueno) tv.setText(CalculoGanancias.dinero(r != null ? r.ventas : 0));
            else tv.setText(String.valueOf(r != null ? r.tickets : 0));
        });
        vm.getAlertas().observe(getViewLifecycleOwner(), n -> {
            int cantidad = n != null ? n : 0;
            TextView tv = vista(R.id.tvNumAlertas);
            tv.setText(String.valueOf(cantidad));
            tv.setTextColor(ContextCompat.getColor(requireContext(),
                    cantidad > 0 ? R.color.status_rojo : R.color.color_slate_dark));
            ((TextView) vista(R.id.tvAlertasAyuda)).setText(
                    cantidad > 0 ? R.string.inicio_con_alertas : R.string.inicio_sin_alertas);
        });

        vista(R.id.btnAjustes).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AjustesActivity.class)));
        vista(R.id.accesoInventario).setOnClickListener(v -> irA(R.id.nav_inventario));
        vista(R.id.accesoVender).setOnClickListener(v -> irA(R.id.nav_vender));
        vista(R.id.accesoAlertas).setOnClickListener(v -> irA(R.id.nav_alertas));
        vista(R.id.accesoReportes).setOnClickListener(v -> irA(R.id.nav_reportes));
        vista(R.id.tarjetaProductos).setOnClickListener(v -> irA(R.id.nav_inventario));
        vista(R.id.tarjetaAlertas).setOnClickListener(v -> irA(R.id.nav_alertas));
    }

    @Override
    protected void alMostrar() {
        // Las ventas de hoy no se recalculan solas: se piden cada vez que se ve Inicio.
        vm.refrescarHoy();
        String nombre = SesionTurno.getNombre(requireContext());
        ((TextView) vista(R.id.tvSaludo)).setText(getString(R.string.inicio_hola, primerNombre(nombre)));
        ((TextView) vista(R.id.tvTurno)).setText(getString(R.string.inicio_atiende, nombre));
    }

    /** "BRUNO RODRIGO BECERRIL" → "Bruno". */
    static String primerNombre(String completo) {
        if (completo == null || completo.trim().isEmpty()) return "";
        String p = completo.trim().split("\\s+")[0];
        Locale mx = Locale.forLanguageTag("es-MX");
        return p.substring(0, 1).toUpperCase(mx) + p.substring(1).toLowerCase(mx);
    }
}
