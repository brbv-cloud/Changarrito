package com.changarrito.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.viewmodel.VentaViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.ViewHolder> {

    public interface OnQuitarClickListener {
        void onQuitar(VentaViewModel.ItemCarrito item);
    }

    private List<VentaViewModel.ItemCarrito> items = new ArrayList<>();
    private OnQuitarClickListener listener;

    public void setOnQuitarClickListener(OnQuitarClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<VentaViewModel.ItemCarrito> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carrito, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VentaViewModel.ItemCarrito item = items.get(position);
        holder.tvNombre.setText(item.producto.nombre);
        holder.tvCantidad.setText(formatearCantidad(item) + " x $"
                + String.format(Locale.getDefault(), "%.2f", item.producto.precio));
        holder.tvSubtotal.setText(String.format(Locale.getDefault(), "$%.2f", item.getSubtotal()));
        holder.btnQuitar.setOnClickListener(v -> {
            if (listener != null) listener.onQuitar(item);
        });
    }

    private String formatearCantidad(VentaViewModel.ItemCarrito item) {
        String unidad = item.producto.unidadMedida != null ? item.producto.unidadMedida.name() : "";
        double cantidad = item.cantidad;
        if (cantidad == Math.floor(cantidad)) {
            return String.format(Locale.getDefault(), "%.0f %s", cantidad, unidad);
        }
        String texto = String.format(Locale.getDefault(), "%.3f", cantidad)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
        return texto + " " + unidad;
    }

    public double getTotal() {
        double total = 0;
        for (VentaViewModel.ItemCarrito item : items) {
            total += item.getSubtotal();
        }
        return total;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvCantidad, tvSubtotal;
        Button btnQuitar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
            btnQuitar = itemView.findViewById(R.id.btnQuitar);
        }
    }
}
