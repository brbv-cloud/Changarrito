package com.changarrito.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.repository.MermaRepository;
import com.changarrito.viewmodel.VentaViewModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Lineas del carrito. Por pieza: control − 1 + (con 1 pieza el "−" se vuelve bote, para
 * quitar la linea sin un boton extra; tocar el numero deja escribir la cantidad). A granel: la cantidad es un boton que abre el
 * dialogo para cambiarla, y el bote la quita.
 */
public class CarritoAdapter extends RecyclerView.Adapter<CarritoAdapter.ViewHolder> {

    public interface Listener {
        void onMenos(VentaViewModel.ItemCarrito item);

        void onMas(VentaViewModel.ItemCarrito item);

        void onCambiarCantidad(VentaViewModel.ItemCarrito item);

        void onQuitar(VentaViewModel.ItemCarrito item);
    }

    private List<VentaViewModel.ItemCarrito> items = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<VentaViewModel.ItemCarrito> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_carrito, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        VentaViewModel.ItemCarrito item = items.get(position);
        Context ctx = h.itemView.getContext();
        UnidadMedida unidad = item.producto.unidadMedida;

        h.tvNombre.setText(item.producto.nombre);
        h.tvPrecioUnitario.setText(ctx.getString(R.string.inv_precio_unidad,
                CalculoGanancias.dinero(item.producto.precio), unidad != null ? unidad.name() : ""));
        h.tvSubtotal.setText(CalculoGanancias.dinero(item.getSubtotal()));

        boolean porPieza = unidad == null || unidad == UnidadMedida.PZA;
        h.stepper.setVisibility(porPieza ? View.VISIBLE : View.GONE);
        h.btnCantidadGranel.setVisibility(porPieza ? View.GONE : View.VISIBLE);
        h.btnQuitar.setVisibility(porPieza ? View.GONE : View.VISIBLE);

        if (porPieza) {
            h.tvCantidad.setText(MermaRepository.cantidadTexto(item.cantidad, null));
            boolean ultima = item.cantidad <= 1.0005;
            h.btnMenos.setImageResource(ultima ? R.drawable.ic_eliminar : R.drawable.ic_menos);
            h.btnMenos.setContentDescription(ctx.getString(ultima ? R.string.venta_quitar_linea : R.string.venta_menos));
            h.btnMenos.setOnClickListener(v -> {
                if (listener != null) listener.onMenos(item);
            });
            h.btnMas.setOnClickListener(v -> {
                if (listener != null) listener.onMas(item);
            });
            // Tocar el numero abre el teclado para escribir la cantidad (ej. 24 refrescos).
            h.tvCantidad.setOnClickListener(v -> {
                if (listener != null) listener.onCambiarCantidad(item);
            });
        } else {
            h.btnCantidadGranel.setText(MermaRepository.cantidadTexto(item.cantidad, unidad));
            h.btnCantidadGranel.setOnClickListener(v -> {
                if (listener != null) listener.onCambiarCantidad(item);
            });
            h.btnQuitar.setOnClickListener(v -> {
                if (listener != null) listener.onQuitar(item);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvNombre, tvPrecioUnitario, tvSubtotal, tvCantidad;
        final View stepper, btnMas, btnQuitar;
        final ImageButton btnMenos;
        final MaterialButton btnCantidadGranel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvPrecioUnitario = itemView.findViewById(R.id.tvPrecioUnitario);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
            tvCantidad = itemView.findViewById(R.id.tvCantidad);
            stepper = itemView.findViewById(R.id.stepper);
            btnMenos = itemView.findViewById(R.id.btnMenos);
            btnMas = itemView.findViewById(R.id.btnMas);
            btnQuitar = itemView.findViewById(R.id.btnQuitar);
            btnCantidadGranel = itemView.findViewById(R.id.btnCantidadGranel);
        }
    }
}
