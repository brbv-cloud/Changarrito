package com.changarrito.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.entity.CierreCajaEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CierreHistorialAdapter extends RecyclerView.Adapter<CierreHistorialAdapter.ViewHolder> {

    private static final double EPSILON = 0.01;

    private List<CierreCajaEntity> cierres = new ArrayList<>();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault());

    public void setCierres(List<CierreCajaEntity> cierres) {
        this.cierres = cierres != null ? cierres : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cierre_historial, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CierreCajaEntity cierre = cierres.get(position);
        // Hora real del cierre (puede haber varios el mismo dia, ej. uno por turno).
        long cuando = cierre.timestampCreacionMs > 0 ? cierre.timestampCreacionMs : cierre.fechaMs;
        holder.tvFecha.setText(formatoFecha.format(new Date(cuando)));
        holder.tvMontos.setText(String.format(Locale.getDefault(), "Sistema $%.2f · Contado $%.2f",
                cierre.efectivoSistema, cierre.efectivoFisicoContado));

        int color;
        String texto;
        if (Math.abs(cierre.diferencia) < EPSILON) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_verde);
            texto = "Cuadre exacto";
        } else if (cierre.diferencia > 0) {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_verde);
            texto = String.format(Locale.getDefault(), "+$%.2f sobrante", cierre.diferencia);
        } else {
            color = ContextCompat.getColor(holder.itemView.getContext(), R.color.status_rojo);
            texto = String.format(Locale.getDefault(), "-$%.2f faltante", Math.abs(cierre.diferencia));
        }
        holder.tvDiferencia.setText(texto);
        holder.tvDiferencia.setTextColor(color);

        if (cierre.notas != null && !cierre.notas.trim().isEmpty()) {
            holder.tvNotas.setVisibility(View.VISIBLE);
            holder.tvNotas.setText(cierre.notas);
        } else {
            holder.tvNotas.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return cierres.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvDiferencia, tvMontos, tvNotas;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            tvDiferencia = itemView.findViewById(R.id.tvDiferencia);
            tvMontos = itemView.findViewById(R.id.tvMontos);
            tvNotas = itemView.findViewById(R.id.tvNotas);
        }
    }
}
