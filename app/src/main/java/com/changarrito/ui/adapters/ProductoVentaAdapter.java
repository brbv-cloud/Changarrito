package com.changarrito.ui.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.repository.MermaRepository;
import com.changarrito.utils.EstadoProducto;
import com.changarrito.utils.EstadoStock;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Productos para vender (solo los que tienen existencia). Guarda la lista completa
 * aparte de la filtrada para no perder productos al buscar y luego borrar la busqueda.
 * Cada tarjeta dice cuanto hay y, si ya esta en el carrito, cuanto lleva.
 */
public class ProductoVentaAdapter extends RecyclerView.Adapter<ProductoVentaAdapter.ViewHolder> {

    public interface Listener {
        /** Toque normal: por pieza agrega 1; a granel pide la cantidad. */
        void onAgregar(ProductoEntity producto);

        /** Mantener presionado: escribir la cantidad exacta. */
        void onElegirCantidad(ProductoEntity producto);
    }

    private final List<ProductoEntity> todos = new ArrayList<>();
    private List<ProductoEntity> filtrados = new ArrayList<>();
    private Map<Integer, Double> enCarrito = new HashMap<>();
    private String busqueda = "";
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setProductos(List<ProductoEntity> productos) {
        todos.clear();
        if (productos != null) {
            // Solo tiene sentido ofrecer para venta lo que si tiene existencia.
            for (ProductoEntity p : productos) {
                if (EstadoStock.calcular(p) != EstadoStock.Nivel.AGOTADO) todos.add(p);
            }
        }
        filtrar(busqueda);
    }

    /** Cantidad de cada producto que ya va en el carrito (id → cantidad). */
    public void setEnCarrito(Map<Integer, Double> enCarrito) {
        this.enCarrito = enCarrito != null ? enCarrito : new HashMap<>();
        notifyDataSetChanged();
    }

    /** Busca por nombre, categoria o codigo, sin importar acentos ni mayusculas. */
    public void filtrar(String texto) {
        busqueda = texto != null ? texto : "";
        filtrados = new ArrayList<>();
        for (ProductoEntity p : todos) {
            if (EstadoProducto.coincide(busqueda, p.nombre, p.categoria, p.barcode)) filtrados.add(p);
        }
        notifyDataSetChanged();
    }

    public boolean hayProductos() {
        return !todos.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto_venta, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ProductoEntity p = filtrados.get(position);
        Context ctx = h.itemView.getContext();

        h.tvInicial.setText(p.nombre == null || p.nombre.trim().isEmpty() ? "?"
                : p.nombre.trim().substring(0, 1).toUpperCase(Locale.forLanguageTag("es-MX")));
        h.tvNombre.setText(p.nombre);
        String unidad = p.unidadMedida != null ? p.unidadMedida.name() : "";
        h.tvPrecio.setText(ctx.getString(R.string.inv_precio_unidad, CalculoGanancias.dinero(p.precio), unidad));

        String cantidad = MermaRepository.cantidadTexto(p.cantidadDisponible, p.unidadMedida);
        boolean bajo = EstadoStock.calcular(p) == EstadoStock.Nivel.BAJO;
        if (bajo) {
            etiqueta(h.tvChipStock, ctx.getString(R.string.venta_quedan, cantidad),
                    R.color.semaforo_amarillo_fondo, R.color.semaforo_amarillo_texto);
        } else {
            etiqueta(h.tvChipStock, ctx.getString(R.string.venta_hay, cantidad),
                    R.color.chip_gris_fondo, R.color.chip_gris_texto);
        }

        Double llevado = enCarrito.get(p.id);
        boolean enElCarrito = llevado != null && llevado > 0;
        h.tvChipCarrito.setVisibility(enElCarrito ? View.VISIBLE : View.GONE);
        if (enElCarrito) {
            etiqueta(h.tvChipCarrito, ctx.getString(R.string.venta_en_carrito,
                    MermaRepository.cantidadTexto(llevado, p.unidadMedida)),
                    R.color.semaforo_verde_fondo, R.color.semaforo_verde_texto);
        }
        h.card.setStrokeColor(ContextCompat.getColor(ctx, enElCarrito ? R.color.marca_verde : R.color.color_card_stroke));

        View.OnClickListener agregar = v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (listener != null) listener.onAgregar(p);
        };
        h.card.setOnClickListener(agregar);
        h.btnAgregar.setOnClickListener(agregar);
        View.OnLongClickListener elegir = v -> {
            if (listener != null) listener.onElegirCantidad(p);
            return true;
        };
        h.card.setOnLongClickListener(elegir);
        h.btnAgregar.setOnLongClickListener(elegir);
    }

    private static void etiqueta(TextView tv, String texto, @ColorRes int fondo, @ColorRes int color) {
        Context ctx = tv.getContext();
        tv.setText(texto);
        tv.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, fondo)));
        tv.setTextColor(ContextCompat.getColor(ctx, color));
    }

    @Override
    public int getItemCount() {
        return filtrados.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvInicial, tvNombre, tvPrecio, tvChipStock, tvChipCarrito;
        final View btnAgregar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvInicial = itemView.findViewById(R.id.tvInicial);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvChipStock = itemView.findViewById(R.id.tvChipStock);
            tvChipCarrito = itemView.findViewById(R.id.tvChipCarrito);
            btnAgregar = itemView.findViewById(R.id.btnAgregar);
        }
    }
}
