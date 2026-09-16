package com.changarrito.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.dao.AlertaConProducto;
import com.changarrito.database.entity.TipoAlerta;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AlertaAdapter extends RecyclerView.Adapter<AlertaAdapter.ViewHolder> {

    public interface Listener {
        void onContactarProveedor(AlertaConProducto alerta);
        void onResolver(AlertaConProducto alerta);
    }

    private List<AlertaConProducto> alertas = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setAlertas(List<AlertaConProducto> alertas) {
        this.alertas = alertas != null ? alertas : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alerta, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AlertaConProducto alerta = alertas.get(position);
        boolean esStockBajo = alerta.tipoAlerta == TipoAlerta.STOCK_BAJO;

        holder.tvNombreProducto.setText(alerta.nombreProducto);

        int colorAcento = ContextCompat.getColor(holder.itemView.getContext(),
                esStockBajo ? R.color.status_ambar : R.color.status_rojo);
        holder.card.setStrokeColor(colorAcento);
        holder.ivIcono.setColorFilter(colorAcento);

        if (esStockBajo) {
            holder.tvMensaje.setText("Stock bajo: quedan " + formatearCantidad(alerta));
        } else {
            long diasRestantes = TimeUnit.MILLISECONDS.toDays(
                    Math.max(0, alerta.fechaCaducidadMs - System.currentTimeMillis()));
            holder.tvMensaje.setText(diasRestantes <= 0
                    ? "Vence hoy"
                    : "Vence en " + diasRestantes + " dia(s)");
        }

        boolean tieneTelefono = alerta.telefonoProveedor != null
                && !alerta.telefonoProveedor.trim().isEmpty();
        holder.btnContactar.setVisibility(esStockBajo ? View.VISIBLE : View.GONE);
        holder.btnContactar.setEnabled(tieneTelefono);

        holder.btnContactar.setOnClickListener(v -> {
            if (listener != null) listener.onContactarProveedor(alerta);
        });
        holder.btnResolver.setOnClickListener(v -> {
            if (listener != null) listener.onResolver(alerta);
        });
    }

    private String formatearCantidad(AlertaConProducto alerta) {
        String unidad = alerta.unidadMedida != null ? alerta.unidadMedida.name() : "";
        double cantidad = alerta.cantidadDisponible;
        if (cantidad == Math.floor(cantidad)) {
            return String.format(Locale.getDefault(), "%.0f %s", cantidad, unidad);
        }
        String texto = String.format(Locale.getDefault(), "%.3f", cantidad)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
        return texto + " " + unidad;
    }

    @Override
    public int getItemCount() {
        return alertas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView ivIcono;
        TextView tvNombreProducto, tvMensaje;
        MaterialButton btnContactar, btnResolver;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            ivIcono = itemView.findViewById(R.id.ivIconoAlerta);
            tvNombreProducto = itemView.findViewById(R.id.tvNombreProducto);
            tvMensaje = itemView.findViewById(R.id.tvMensajeAlerta);
            btnContactar = itemView.findViewById(R.id.btnContactarProveedor);
            btnResolver = itemView.findViewById(R.id.btnResolver);
        }
    }
}
