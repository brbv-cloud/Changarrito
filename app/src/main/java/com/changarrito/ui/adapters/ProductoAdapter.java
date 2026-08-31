package com.changarrito.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;

import java.util.List;
import java.util.Locale;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder> {

    private List<ProductoEntity> productos;
    private OnProductoClickListener listener;

    public interface OnProductoClickListener {
        void onEditar(ProductoEntity producto);
        void onEliminar(ProductoEntity producto);
    }

    public ProductoAdapter(List<ProductoEntity> productos) {
        this.productos = productos;
    }

    public void setOnProductoClickListener(OnProductoClickListener listener) {
        this.listener = listener;
    }

    public void setProductos(List<ProductoEntity> productos) {
        this.productos = productos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_producto, parent, false);
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        ProductoEntity producto = productos.get(position);

        holder.tvNombre.setText(producto.nombre);
        holder.tvPrecio.setText(String.format(Locale.getDefault(), "$%.2f", producto.precio));
        holder.tvCantidad.setText("Cantidad: " + formatearCantidad(producto));

        holder.btnEditar.setOnClickListener(v -> {
            if (listener != null) listener.onEditar(producto);
        });

        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) listener.onEliminar(producto);
        });
    }

    /**
     * Formatea la cantidad según su unidad:
     * PZA muestra entero ("12 PZA"), granel muestra decimales ("3.5 KG").
     */
    private String formatearCantidad(ProductoEntity producto) {
        String unidad = producto.unidadMedida != null
                ? producto.unidadMedida.name()
                : "";

        if (producto.unidadMedida == UnidadMedida.PZA) {
            return String.format(Locale.getDefault(), "%.0f %s", producto.cantidadDisponible, unidad);
        }
        return String.format(Locale.getDefault(), "%.3f %s", producto.cantidadDisponible, unidad);
    }

    @Override
    public int getItemCount() {
        return productos != null ? productos.size() : 0;
    }

    public static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvPrecio, tvCantidad;
        Button btnEditar, btnEliminar;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}