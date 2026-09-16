package com.changarrito.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.dao.AlertaConProducto;
import com.changarrito.ui.adapters.AlertaAdapter;
import com.changarrito.utils.InsetsUtil;
import com.changarrito.utils.WhatsAppIntentHelper;
import com.changarrito.viewmodel.AlertaViewModel;

public class AlertasActivity extends AppCompatActivity {

    private RecyclerView rvAlertas;
    private TextView tvSinAlertas;
    private AlertaAdapter adapter;
    private AlertaViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertas);

        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootAlertas), true, true);

        rvAlertas = findViewById(R.id.rvAlertas);
        tvSinAlertas = findViewById(R.id.tvSinAlertas);

        viewModel = new ViewModelProvider(this).get(AlertaViewModel.class);

        rvAlertas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlertaAdapter();
        adapter.setListener(new AlertaAdapter.Listener() {
            @Override
            public void onContactarProveedor(AlertaConProducto alerta) {
                abrirWhatsApp(alerta);
            }

            @Override
            public void onResolver(AlertaConProducto alerta) {
                viewModel.marcarComoResuelta(alerta.id);
                Toast.makeText(AlertasActivity.this, "Alerta resuelta", Toast.LENGTH_SHORT).show();
            }
        });
        rvAlertas.setAdapter(adapter);

        viewModel.getAlertasActivas().observe(this, alertas -> {
            adapter.setAlertas(alertas);
            boolean vacio = alertas == null || alertas.isEmpty();
            tvSinAlertas.setVisibility(vacio ? View.VISIBLE : View.GONE);
        });
    }

    private void abrirWhatsApp(AlertaConProducto alerta) {
        if (alerta.telefonoProveedor == null || alerta.telefonoProveedor.trim().isEmpty()) {
            Toast.makeText(this,
                    "Este producto no tiene telefono de proveedor capturado",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String mensaje = WhatsAppIntentHelper.construirMensaje(
                alerta.nombreProducto, alerta.cantidadDisponible, alerta.unidadMedida);
        String url = WhatsAppIntentHelper.construirUrl(alerta.telefonoProveedor, mensaje);

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo abrir WhatsApp. ¿Esta instalado?", Toast.LENGTH_LONG).show();
        }
    }
}
