package com.changarrito.ui.secciones;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.dao.AlertaConProducto;
import com.changarrito.ui.adapters.AlertaAdapter;
import com.changarrito.utils.WhatsAppIntentHelper;
import com.changarrito.viewmodel.AlertaViewModel;

/** Alertas activas (stock bajo y por vencer), con atajo a WhatsApp del proveedor. */
public class AlertasFragment extends SeccionFragment {

    private AlertaViewModel viewModel;

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
        RecyclerView rvAlertas = vista(R.id.rvAlertas);
        View tvSinAlertas = vista(R.id.tvSinAlertas);

        rvAlertas.setLayoutManager(new LinearLayoutManager(requireContext()));
        AlertaAdapter adapter = new AlertaAdapter();
        adapter.setListener(new AlertaAdapter.Listener() {
            @Override
            public void onContactarProveedor(AlertaConProducto alerta) {
                abrirWhatsApp(alerta);
            }

            @Override
            public void onResolver(AlertaConProducto alerta) {
                viewModel.marcarComoResuelta(alerta.id);
                Toast.makeText(requireContext(), "Alerta resuelta", Toast.LENGTH_SHORT).show();
            }
        });
        rvAlertas.setAdapter(adapter);

        viewModel.getAlertasActivas().observe(getViewLifecycleOwner(), alertas -> {
            adapter.setAlertas(alertas);
            boolean vacio = alertas == null || alertas.isEmpty();
            tvSinAlertas.setVisibility(vacio ? View.VISIBLE : View.GONE);
        });
    }

    private void abrirWhatsApp(AlertaConProducto alerta) {
        if (alerta.telefonoProveedor == null || alerta.telefonoProveedor.trim().isEmpty()) {
            Toast.makeText(requireContext(), "Este producto no tiene telefono de proveedor capturado",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String mensaje = WhatsAppIntentHelper.construirMensaje(
                alerta.nombreProducto, alerta.cantidadDisponible, alerta.unidadMedida);
        String url = WhatsAppIntentHelper.construirUrl(alerta.telefonoProveedor, mensaje);
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No se pudo abrir WhatsApp. ¿Esta instalado?", Toast.LENGTH_LONG).show();
        }
    }
}
