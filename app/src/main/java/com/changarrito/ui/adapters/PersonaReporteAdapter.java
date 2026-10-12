package com.changarrito.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.reportes.CalculoGanancias;

import java.util.ArrayList;
import java.util.List;

/** Fila por persona: ventas que hizo en su turno y diferencias en sus cierres. */
public class PersonaReporteAdapter extends RecyclerView.Adapter<PersonaReporteAdapter.ViewHolder> {

    private static final double CENTAVO = 0.005;

    private List<CalculoGanancias.FilaPersona> filas = new ArrayList<>();

    public void setFilas(List<CalculoGanancias.FilaPersona> lista) {
        filas = lista != null ? lista : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_persona_reporte, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        CalculoGanancias.FilaPersona f = filas.get(position);
        Context c = h.itemView.getContext();

        h.tvNombre.setText(f.nombre != null ? f.nombre : c.getString(R.string.ganancia_persona_sin_nombre));
        h.tvVentas.setText(CalculoGanancias.dinero(f.ventas));
        h.tvTransacciones.setText(c.getResources().getQuantityString(
                R.plurals.ganancia_ventas, f.transacciones, f.transacciones));

        if (f.cierres == 0) {
            h.tvCierres.setText(R.string.ganancia_persona_sin_cierres);
            h.tvCierres.setTextColor(ContextCompat.getColor(c, R.color.color_slate_medium));
            return;
        }
        String cuantos = c.getResources().getQuantityString(R.plurals.ganancia_cierres, f.cierres, f.cierres);
        int color;
        String detalle;
        if (f.faltante < -CENTAVO) {
            detalle = c.getString(R.string.ganancia_persona_falto, CalculoGanancias.dinero(-f.faltante));
            color = R.color.status_rojo;
        } else if (f.sobrante > CENTAVO) {
            detalle = c.getString(R.string.ganancia_persona_sobro, CalculoGanancias.dinero(f.sobrante));
            color = R.color.status_ambar;
        } else {
            detalle = c.getString(R.string.ganancia_persona_cuadro);
            color = R.color.status_verde;
        }
        // Si hubo faltante y sobrante en distintos cierres, se muestran los dos.
        if (f.faltante < -CENTAVO && f.sobrante > CENTAVO) {
            detalle += " · " + c.getString(R.string.ganancia_persona_sobro, CalculoGanancias.dinero(f.sobrante));
        }
        h.tvCierres.setText(cuantos + " · " + detalle);
        h.tvCierres.setTextColor(ContextCompat.getColor(c, color));
    }

    @Override
    public int getItemCount() {
        return filas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvNombre, tvVentas, tvTransacciones, tvCierres;

        ViewHolder(@NonNull View v) {
            super(v);
            tvNombre = v.findViewById(R.id.tvNombre);
            tvVentas = v.findViewById(R.id.tvVentas);
            tvTransacciones = v.findViewById(R.id.tvTransacciones);
            tvCierres = v.findViewById(R.id.tvCierres);
        }
    }
}
