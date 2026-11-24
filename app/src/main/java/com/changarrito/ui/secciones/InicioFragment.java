package com.changarrito.ui.secciones;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.changarrito.R;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.negocio.SesionTurno;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.ui.Ayuda;
import com.changarrito.ui.activities.PrincipalActivity;
import com.changarrito.ui.activities.negocio.AjustesActivity;
import com.changarrito.ui.fragments.ProductoDialogFragment;
import com.changarrito.ui.guia.Bienvenida;
import com.changarrito.ui.guia.GuiaOverlay;
import com.changarrito.viewmodel.InicioViewModel;
import com.changarrito.viewmodel.ProductoViewModel;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Locale;

/**
 * Inicio: encabezado verde con lo vendido hoy (y como va contra un dia normal del negocio)
 * y botones grandes de colores para ir a cada parte. El trabajador no ve dinero: ve
 * cuantas compras van, y no tiene el boton de Reportes.
 *
 * La primera vez sale la bienvenida, que ofrece el recorrido con "reflector".
 * El boton "?" abre la guia con puntos.
 */
public class InicioFragment extends SeccionFragment implements ProductoDialogFragment.ProductoDialogListener {

    private static final String CLAVE_BIENVENIDA = "bienvenida";

    private InicioViewModel vm;
    private ProductoViewModel productoVm;
    private boolean esDueno;
    @Nullable
    private ResumenGanancia hoy;
    @Nullable
    private Double promedioDiario;
    /** Pedido desde Ajustes ("Ver el tutorial de bienvenida"). */
    private boolean recorridoPendiente;

    public InicioFragment() {
        super(R.layout.fragment_inicio);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = new ViewModelProvider(this).get(InicioViewModel.class);
        productoVm = new ViewModelProvider(this).get(ProductoViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        esDueno = SesionTurno.esDueno(requireContext());

        ((TextView) vista(R.id.tvEtiquetaVentas)).setText(esDueno ? R.string.ini_vendido_hoy : R.string.ini_compras_hoy);
        vista(R.id.mosaicoReportes).setVisibility(esDueno ? View.VISIBLE : View.GONE);

        vm.getConfiguracion().observe(getViewLifecycleOwner(), c -> {
            if (c != null && c.nombreNegocio != null) ((TextView) vista(R.id.tvNegocio)).setText(c.nombreNegocio);
        });
        vm.getProductos().observe(getViewLifecycleOwner(), n -> {
            int cuantos = n != null ? n : 0;
            ((TextView) vista(R.id.tvInventarioSub)).setText(
                    getResources().getQuantityString(R.plurals.ini_productos, cuantos, cuantos));
        });
        vm.getPendientes().observe(getViewLifecycleOwner(), n -> {
            int cuantos = n != null ? n : 0;
            TextView chip = vista(R.id.tvPorSurtir);
            chip.setVisibility(cuantos > 0 ? View.VISIBLE : View.GONE);
            chip.setText(getString(R.string.ini_por_surtir, cuantos));
        });
        vm.getAlertas().observe(getViewLifecycleOwner(), n -> {
            int cuantas = n != null ? n : 0;
            TextView insignia = vista(R.id.tvInsigniaAlertas);
            insignia.setVisibility(cuantas > 0 ? View.VISIBLE : View.GONE);
            insignia.setText(cuantas > 99 ? "99+" : String.valueOf(cuantas));
            ((TextView) vista(R.id.tvAlertasSub)).setText(cuantas > 0
                    ? getResources().getQuantityString(R.plurals.ini_alertas_sub, cuantas, cuantas)
                    : getString(R.string.ini_alertas_ok));
        });
        vm.getHoy().observe(getViewLifecycleOwner(), r -> {
            hoy = r;
            mostrarResumen();
        });
        vm.getPromedioDiario().observe(getViewLifecycleOwner(), p -> {
            promedioDiario = p;
            mostrarResumen();
        });
        productoVm.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) avisar(error);
        });

        vista(R.id.btnAjustes).setOnClickListener(v -> startActivity(new Intent(requireContext(), AjustesActivity.class)));
        vista(R.id.mosaicoVender).setOnClickListener(v -> irA(R.id.nav_vender));
        vista(R.id.mosaicoInventario).setOnClickListener(v -> irA(R.id.nav_inventario));
        vista(R.id.mosaicoAlertas).setOnClickListener(v -> irA(R.id.nav_alertas));
        vista(R.id.mosaicoReportes).setOnClickListener(v -> irA(R.id.nav_reportes));
        vista(R.id.mosaicoAgregar).setOnClickListener(v ->
                ProductoDialogFragment.newInstance(null, this).show(getChildFragmentManager(), "ProductoDialog"));
        vista(R.id.mosaicoCaja).setOnClickListener(v -> {
            if (getActivity() instanceof PrincipalActivity) ((PrincipalActivity) getActivity()).irACaja();
        });
        vista(R.id.mosaicoMerma).setOnClickListener(v ->
                com.changarrito.ui.fragments.ElegirProductoMerma.mostrar(requireActivity()));
        vista(R.id.mosaicoGasto).setOnClickListener(v ->
                com.changarrito.ui.fragments.GastoDialog.mostrar(requireActivity(), null));
        vista(R.id.tarjetaResumen).setOnClickListener(v -> {
            if (esDueno) irA(R.id.nav_reportes);
        });
    }

    @Override
    protected void alMostrar() {
        // Las ventas de hoy no se recalculan solas: se piden cada vez que se ve Inicio.
        vm.refrescarHoy();
        String nombre = SesionTurno.getNombre(requireContext());
        ((TextView) vista(R.id.tvSaludo)).setText(getString(R.string.inicio_hola, primerNombre(nombre)) + " 👋");
        ((TextView) vista(R.id.tvTurno)).setText(getString(R.string.inicio_atiende, nombre));

        if (recorridoPendiente) {
            // Lo pidio desde Ajustes: directo al recorrido, sin preguntar.
            recorridoPendiente = false;
            Ayuda.marcarVista(requireContext(), CLAVE_BIENVENIDA);
            requireView().postDelayed(() -> {
                if (!isAdded() || isHidden()) return;
                GuiaOverlay.mostrar(requireActivity(), pasosRecorrido(), GuiaOverlay.Modo.RECORRIDO, null);
            }, 450);
            return;
        }
        // Primera vez con esta cuenta: bienvenida y recorrido de 1 minuto.
        if (!Ayuda.yaVista(requireContext(), CLAVE_BIENVENIDA)) {
            Ayuda.marcarVista(requireContext(), CLAVE_BIENVENIDA);
            requireView().postDelayed(() -> {
                if (!isAdded() || isHidden()) return;
                Bienvenida.mostrar(requireActivity(), () -> GuiaOverlay.mostrar(requireActivity(),
                        pasosRecorrido(), GuiaOverlay.Modo.RECORRIDO, null));
            }, 450);
        }
    }

    /**
     * Dueno: lo vendido hoy, ventas y venta promedio, y una barra contra un dia normal
     * (promedio de los ultimos 30 dias con ventas). Trabajador: solo cuantas compras van.
     */
    private void mostrarResumen() {
        if (getView() == null) return;
        TextView tvVentas = vista(R.id.tvVentasHoy);
        TextView tvDetalle = vista(R.id.tvDetalleHoy);
        TextView tvMeta = vista(R.id.tvMetaDia);
        LinearProgressIndicator barra = vista(R.id.barraDia);
        int tickets = hoy != null ? hoy.tickets : 0;
        double ventas = hoy != null ? hoy.ventas : 0;

        if (!esDueno) {
            tvVentas.setText(String.valueOf(tickets));
            tvDetalle.setText(R.string.ini_detalle_trabajador);
            barra.setVisibility(View.GONE);
            tvMeta.setVisibility(View.GONE);
            return;
        }
        tvVentas.setText(CalculoGanancias.dinero(ventas));
        double porVenta = tickets > 0 ? ventas / tickets : 0;
        tvDetalle.setText(getResources().getQuantityString(R.plurals.ini_detalle_dueno, tickets, tickets,
                CalculoGanancias.dinero(porVenta).replace(".00", "")));

        tvMeta.setVisibility(View.VISIBLE);
        if (promedioDiario != null && promedioDiario > 0) {
            int pct = (int) Math.round(ventas * 100 / promedioDiario);
            barra.setVisibility(View.VISIBLE);
            barra.setProgressCompat(Math.min(100, pct), true);
            tvMeta.setText(pct >= 100 ? getString(R.string.ini_meta_superada, pct) : getString(R.string.ini_meta, pct));
        } else {
            // Negocio nuevo: todavia no hay "dia normal" con que comparar.
            barra.setVisibility(View.GONE);
            tvMeta.setText(R.string.ini_meta_sin_historial);
        }
    }

    // ------------------------------------------------------------------ guias

    /** Muestra el recorrido de la primera vez (desde Ajustes). */
    public void verRecorrido() {
        recorridoPendiente = true;
        if (isResumed() && !isHidden() && getView() != null) alMostrar();
    }

    /** Recorrido de la primera vez (reflector, "PASO 1 DE 6"). */
    private java.util.List<GuiaOverlay.Paso> pasosRecorrido() {
        Pasos p = new Pasos();
        p.agregar(vista(R.id.tarjetaResumen), R.drawable.ic_nav_reportes, R.string.g_ini_resumen_t,
                esDueno ? R.string.g_ini_resumen : R.string.g_ini_resumen_trab);
        p.agregar(vista(R.id.mosaicoVender), R.drawable.ic_nav_vender, R.string.g_ini_vender_t, R.string.g_ini_vender);
        p.agregar(vista(R.id.mosaicoInventario), R.drawable.ic_nav_inventario, R.string.g_ini_inv_t, R.string.g_ini_inv);
        p.agregar(vista(R.id.mosaicoAlertas), R.drawable.ic_nav_alertas, R.string.g_ini_ale_t, R.string.g_ini_ale);
        p.agregar(vista(R.id.mosaicoMerma), R.drawable.ic_eliminar, R.string.g_ini_merma_t, R.string.g_ini_merma);
        p.agregar(vista(R.id.mosaicoGasto), R.drawable.ic_billete, R.string.g_ini_gasto_t,
                esDueno ? R.string.g_ini_gasto : R.string.g_ini_gasto_trab);
        p.agregar(vista(R.id.mosaicoCaja), R.drawable.ic_nav_caja, R.string.g_ini_caja_t,
                esDueno ? R.string.g_ini_caja : R.string.g_ini_caja_trab);
        p.agregar(vista(R.id.btnGuia), R.drawable.ic_info, R.string.g_ini_ayuda_t, R.string.g_ini_ayuda);
        return p.lista;
    }

    /** Guia con puntos (boton "?"): todo lo de la pantalla. */
    @Override
    protected void pasosGuia(Pasos p) {
        p.agregar(vista(R.id.tarjetaResumen), R.drawable.ic_nav_reportes, R.string.g_ini_resumen_t,
                esDueno ? R.string.g_ini_resumen : R.string.g_ini_resumen_trab);
        p.agregar(vista(R.id.mosaicoVender), R.drawable.ic_nav_vender, R.string.g_ini_vender_t, R.string.g_ini_vender);
        p.agregar(vista(R.id.mosaicoInventario), R.drawable.ic_nav_inventario, R.string.g_ini_inv_t, R.string.g_ini_inv);
        p.agregar(vista(R.id.mosaicoAlertas), R.drawable.ic_nav_alertas, R.string.g_ini_ale_t, R.string.g_ini_ale);
        if (esDueno) {
            p.agregar(vista(R.id.mosaicoReportes), R.drawable.ic_nav_reportes, R.string.g_ini_rep_t, R.string.g_ini_rep);
        }
        p.agregar(vista(R.id.mosaicoAgregar), R.drawable.ic_mas, R.string.g_ini_agr_t, R.string.g_ini_agr);
        p.agregar(vista(R.id.mosaicoMerma), R.drawable.ic_eliminar, R.string.g_ini_merma_t, R.string.g_ini_merma);
        p.agregar(vista(R.id.mosaicoGasto), R.drawable.ic_billete, R.string.g_ini_gasto_t,
                esDueno ? R.string.g_ini_gasto : R.string.g_ini_gasto_trab);
        p.agregar(vista(R.id.mosaicoCaja), R.drawable.ic_nav_caja, R.string.g_ini_caja_t,
                esDueno ? R.string.g_ini_caja : R.string.g_ini_caja_trab);
    }

    // ------------------------------------------------------------------ agregar producto

    @Override
    public void onProductoGuardado(ProductoEntity producto) {
        productoVm.guardarProducto(producto);
    }

    /** "BRUNO RODRIGO BECERRIL" → "Bruno". */
    static String primerNombre(String completo) {
        if (completo == null || completo.trim().isEmpty()) return "";
        String p = completo.trim().split("\\s+")[0];
        Locale mx = Locale.forLanguageTag("es-MX");
        return p.substring(0, 1).toUpperCase(mx) + p.substring(1).toLowerCase(mx);
    }
}
