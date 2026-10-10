package com.changarrito.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.dao.ProductoConTotal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lista de "productos mas vendidos" con una barra horizontal proporcional al
 * monto de cada uno (el mas vendido llena toda la barra, los demas a escala).
 *
 * La barra se arma con dos Views dentro de un LinearLayout usando layout_weight
 * en vez de una libreria de graficas o un Canvas: mas simple, cero dependencias
 * nuevas, y funciona igual de bien para 5 productos.
 */
public class ProductoTopAdapter extends RecyclerView.Adapter<ProductoTopAdapter.ViewHolder> {

    private List<ProductoConTotal> productos = new ArrayList<>();
    private double maxTotal = 0;

    public void setProductos(List<ProductoConTotal> productos) {
        this.productos = productos != null ? productos : new ArrayList<>();
        maxTotal = 0;
        for (ProductoConTotal p : this.productos) {
            maxTotal = Math.max(maxTotal, p.totalIngresos);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_producto_top, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductoConTotal producto = productos.get(position);
        holder.tvNombre.setText(producto.nombre);
        holder.tvTotal.setText(String.format(Locale.getDefault(), "$%.0f", producto.totalIngresos));

        float proporcion = maxTotal > 0 ? (float) (producto.totalIngresos / maxTotal) : 0f;
        // Minimo 4%: hasta el producto mas chico se ve como una rayita, no desaparece.
        proporcion = Math.max(proporcion, 0.04f);

        LinearLayout.LayoutParams paramsBarra = (LinearLayout.LayoutParams) holder.vBarra.getLayoutParams();
        paramsBarra.weight = proporcion;
        holder.vBarra.setLayoutParams(paramsBarra);

        LinearLayout.LayoutParams paramsEspacio = (LinearLayout.LayoutParams) holder.vEspacio.getLayoutParams();
        paramsEspacio.weight = 1f - proporcion;
        holder.vEspacio.setLayoutParams(paramsEspacio);
    }

    @Override
    public int getItemCount() {
        return productos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvTotal;
        View vBarra, vEspacio;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            vBarra = itemView.findViewById(R.id.vBarra);
            vEspacio = itemView.findViewById(R.id.vEspacio);
        }
    }
}
