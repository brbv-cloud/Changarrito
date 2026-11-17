package com.changarrito.ui.secciones;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.dao.AlertaConProducto;
import com.changarrito.database.entity.TipoAlerta;
import com.changarrito.ui.adapters.AlertaAdapter;
import com.changarrito.utils.WhatsAppIntentHelper;
import com.changarrito.viewmodel.AlertaViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Alertas activas (existencia y caducidad). Primero las urgentes (agotado, caducado),
 * luego las demas; se pueden filtrar por tipo. Las de existencia permiten pedir al
 * proveedor por WhatsApp con el mensaje ya escrito.
 */
public class AlertasFragment extends SeccionFragment {

    private AlertaViewModel viewModel;
    private AlertaAdapter adapter;
    private List<AlertaConProducto> todas = new ArrayList<>();
    private boolean cargado;

    private ChipGroup chipsFiltro;
    private TextView tvResumen, tvVacioTitulo, tvSinAlertas;
    private View vacio;

    public AlertasFragment() {
        super(R.layout.fragment_alertas);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AlertaViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tvResumen = vista(R.id.tvResumen);
        tvVacioTitulo = vista(R.id.tvVacioTitulo);
        tvSinAlertas = vista(R.id.tvSinAlertas);
        vacio = vista(R.id.vacioAlertas);
        chipsFiltro = vista(R.id.chipsFiltro);
        chipsFiltro.setOnCheckedStateChangeListener((g, ids) -> mostrar());

        RecyclerView rvAlertas = vista(R.id.rvAlertas);
        rvAlertas.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AlertaAdapter();
        adapter.setListener(new AlertaAdapter.Listener() {
            @Override
            public void onContactarProveedor(AlertaConProducto alerta) {
                abrirWhatsApp(alerta);
            }

            @Override
            public void onResolver(AlertaConProducto alerta) {
                viewModel.marcarComoResuelta(alerta.id);
                avisar(R.string.alerta_resuelta);
            }
        });
        rvAlertas.setAdapter(adapter);

        viewModel.getAlertasActivas().observe(getViewLifecycleOwner(), alertas -> {
            todas = alertas != null ? alertas : new ArrayList<>();
            cargado = true;
            mostrar();
        });
    }

    @Override
    protected void alMostrar() {
        // "Caduca en 3 dias" depende de la fecha de hoy.
        mostrar();
    }

    private void mostrar() {
        if (getView() == null || !cargado) return;
        long ahora = System.currentTimeMillis();

        // Urgentes primero, conservando el orden (mas recientes primero) dentro de cada grupo.
        List<AlertaConProducto> ordenadas = new ArrayList<>();
        int urgentes = 0, existencia = 0, caducidad = 0;
        for (AlertaConProducto a : todas) {
            if (AlertaAdapter.esUrgente(a, ahora)) {
                ordenadas.add(urgentes, a);
                urgentes++;
            } else {
                ordenadas.add(a);
            }
            if (a.tipoAlerta != TipoAlerta.PROXIMO_VENCIMIENTO) existencia++;
            else caducidad++;
        }

        ((Chip) vista(R.id.chipTodas)).setText(getString(R.string.alertas_filtro_todas, todas.size()));
        ((Chip) vista(R.id.chipExistencia)).setText(getString(R.string.alertas_filtro_stock, existencia));
        ((Chip) vista(R.id.chipCaducidad)).setText(getString(R.string.alertas_filtro_caducidad, caducidad));

        if (todas.isEmpty()) {
            tvResumen.setText(R.string.alertas_resumen_vacio);
        } else {
            String activas = getResources().getQuantityString(R.plurals.alertas_activas_n, todas.size(), todas.size());
            tvResumen.setText(urgentes > 0
                    ? getResources().getQuantityString(R.plurals.alertas_urgentes_n, urgentes, activas, urgentes)
                    : activas);
        }

        int filtro = chipsFiltro.getCheckedChipId();
        List<AlertaConProducto> visibles = new ArrayList<>();
        for (AlertaConProducto a : ordenadas) {
            boolean deExistencia = a.tipoAlerta != TipoAlerta.PROXIMO_VENCIMIENTO;
            if (filtro == R.id.chipExistencia && !deExistencia) continue;
            if (filtro == R.id.chipCaducidad && deExistencia) continue;
            visibles.add(a);
        }
        adapter.setAlertas(visibles);

        boolean sinNada = visibles.isEmpty();
        vacio.setVisibility(sinNada ? View.VISIBLE : View.GONE);
        if (sinNada) {
            boolean porFiltro = !todas.isEmpty();
            tvVacioTitulo.setVisibility(porFiltro ? View.GONE : View.VISIBLE);
            tvSinAlertas.setText(porFiltro ? R.string.alertas_vacio_filtro : R.string.alertas_vacio_texto);
        }
    }

    private void abrirWhatsApp(AlertaConProducto alerta) {
        if (alerta.telefonoProveedor == null || alerta.telefonoProveedor.trim().isEmpty()) {
            avisar(R.string.alerta_sin_telefono_toast);
            return;
        }
        String mensaje = WhatsAppIntentHelper.construirMensaje(
                alerta.nombreProducto, alerta.cantidadDisponible, alerta.unidadMedida);
        String url = WhatsAppIntentHelper.construirUrl(alerta.telefonoProveedor, mensaje);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            avisar(R.string.alerta_sin_whatsapp);
        }
    }
}
