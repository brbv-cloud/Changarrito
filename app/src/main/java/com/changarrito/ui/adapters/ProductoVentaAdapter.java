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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Lista de productos seleccionables para vender. Mantiene la lista completa aparte
 * de la lista filtrada (búsqueda) para no perder productos al filtrar y volver a
 * limpiar el buscador.
 */
public class ProductoVentaAdapter extends RecyclerView.Adapter<ProductoVentaAdapter.ViewHolder> {

    public interface OnAgregarClickListener {
        void onAgregar(ProductoEntity producto);
    }

    private final List<ProductoEntity> todos = new ArrayList<>();
    private List<ProductoEntity> filtrados = new ArrayList<>();
    private OnAgregarClickListener listener;

    public void setOnAgregarClickListener(OnAgregarClickListener listener) {
        this.listener = listener;
    }

    public void setProductos(List<ProductoEntity> productos) {
        todos.clear();
        if (productos != null) {
            // solo tiene sentido ofrecer para venta lo que sí tiene stock
            for (ProductoEntity p : productos) {
                if (p.cantidadDisponible > 0) todos.add(p);
            }
        }
        filtrar(ultimoFiltro);
    }

    private String ultimoFiltro = "";

    public void filtrar(String texto) {
        ultimoFiltro = texto != null ? texto.trim().toLowerCase(Locale.getDefault()) : "";
        filtrados = new ArrayList<>();
        for (ProductoEntity p : todos) {
            if (ultimoFiltro.isEmpty()
                    || (p.nombre != null && p.nombre.toLowerCase(Locale.getDefault()).contains(ultimoFiltro))) {
                filtrados.add(p);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_producto_venta, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductoEntity producto = filtrados.get(position);
        holder.tvNombre.setText(producto.nombre);
        holder.tvPrecio.setText(String.format(Locale.getDefault(), "$%.2f", producto.precio));
        holder.tvDisponible.setText("Disponible: " + formatearCantidad(producto));

        View.OnClickListener abrir = v -> {
            if (listener != null) listener.onAgregar(producto);
        };
        holder.itemView.setOnClickListener(abrir);
        holder.btnAgregar.setOnClickListener(abrir);
    }

    private String formatearCantidad(ProductoEntity producto) {
        String unidad = producto.unidadMedida != null ? producto.unidadMedida.name() : "";
        double cantidad = producto.cantidadDisponible;
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
        return filtrados.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvPrecio, tvDisponible;
        Button btnAgregar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvDisponible = itemView.findViewById(R.id.tvDisponible);
            btnAgregar = itemView.findViewById(R.id.btnAgregar);
        }
    }
}
