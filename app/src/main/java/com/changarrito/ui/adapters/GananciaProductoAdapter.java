package com.changarrito.ui.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.utils.CalculoPrecio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Fila por producto: cuanto se vendio, cuanto dejo y sus dos porcentajes. */
public class GananciaProductoAdapter extends RecyclerView.Adapter<GananciaProductoAdapter.ViewHolder> {

    private List<GananciaProducto> productos = new ArrayList<>();
    private final int maximo;

    /** @param maximo cuantas filas mostrar como mucho (la lista viene ordenada). */
    public GananciaProductoAdapter(int maximo) {
        this.maximo = maximo;
    }

    public void setProductos(List<GananciaProducto> lista) {
        productos = lista != null ? lista : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ganancia_producto, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        GananciaProducto p = productos.get(position);
        Context c = h.itemView.getContext();

        h.tvNombre.setText(p.nombre);
        h.tvDetalle.setText(c.getString(R.string.ganancia_vendiste,
                cantidad(p.cantidad, p.unidadMedida), CalculoGanancias.dinero(p.ventas)));

        int color;
        if (p.tieneCosto()) {
            double ganancia = p.ganancia();
            h.tvGanancia.setText(CalculoGanancias.dinero(ganancia));
            h.tvPorcentajes.setText(c.getString(R.string.ganancia_porcentajes_corto,
                    CalculoGanancias.porcentaje(CalculoGanancias.pctSobreCosto(p)),
                    CalculoGanancias.porcentaje(CalculoGanancias.pctDelPrecio(p))));
            CalculoPrecio.Semaforo s = CalculoGanancias.semaforo(p);
            color = s == CalculoPrecio.Semaforo.ROJO ? R.color.status_rojo
                    : s == CalculoPrecio.Semaforo.AMARILLO ? R.color.status_ambar : R.color.status_verde;
            h.tvGanancia.setTextColor(ContextCompat.getColor(c,
                    ganancia < 0 ? R.color.status_rojo : R.color.color_primary_variant));
        } else {
            h.tvGanancia.setText(R.string.ganancia_sin_costo);
            h.tvPorcentajes.setText(R.string.ganancia_sin_costo_ayuda);
            h.tvGanancia.setTextColor(ContextCompat.getColor(c, R.color.color_slate_medium));
            color = R.color.color_card_stroke;
        }
        ViewCompat.setBackgroundTintList(h.vPunto, ColorStateList.valueOf(ContextCompat.getColor(c, color)));
    }

    private static String cantidad(double valor, UnidadMedida unidad) {
        String u = unidad != null ? unidad.name() : "";
        if (unidad == UnidadMedida.PZA || Math.abs(valor - Math.rint(valor)) < 0.0005) {
            return String.format(Locale.getDefault(), "%.0f %s", valor, u);
        }
        return String.format(Locale.getDefault(), "%.3f %s", valor, u);
    }

    @Override
    public int getItemCount() {
        return Math.min(productos.size(), maximo);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View vPunto;
        final TextView tvNombre, tvDetalle, tvGanancia, tvPorcentajes;

        ViewHolder(@NonNull View v) {
            super(v);
            vPunto = v.findViewById(R.id.vPunto);
            tvNombre = v.findViewById(R.id.tvNombre);
            tvDetalle = v.findViewById(R.id.tvDetalle);
            tvGanancia = v.findViewById(R.id.tvGanancia);
            tvPorcentajes = v.findViewById(R.id.tvPorcentajes);
        }
    }
}
