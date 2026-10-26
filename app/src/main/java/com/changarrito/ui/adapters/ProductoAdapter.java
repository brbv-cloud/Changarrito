package com.changarrito.ui.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
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

import java.util.List;
import java.util.Locale;

/**
 * Tarjetas de Inventario. Cada una dice de un vistazo lo importante con etiquetas de
 * color: cuanto hay (gris si va bien, ambar si queda poco, rojo si se acabo) y, solo
 * cuando importa, la caducidad ("Vence en 3 dias", "Caducado"). Si algo requiere
 * atencion, el borde de la tarjeta toma ese color.
 */
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto, parent, false);
        return new ProductoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder h, int position) {
        ProductoEntity p = productos.get(position);
        Context ctx = h.itemView.getContext();

        h.tvInicial.setText(inicial(p.nombre));
        h.tvNombre.setText(p.nombre);
        String unidad = p.unidadMedida != null ? p.unidadMedida.name() : "";
        h.tvPrecio.setText(ctx.getString(R.string.inv_precio_unidad, CalculoGanancias.dinero(p.precio), unidad));
        boolean conCategoria = p.categoria != null && !p.categoria.trim().isEmpty();
        h.tvCategoria.setVisibility(conCategoria ? View.VISIBLE : View.GONE);
        if (conCategoria) h.tvCategoria.setText("· " + p.categoria.trim());

        // Etiqueta de cantidad
        EstadoStock.Nivel nivel = EstadoStock.calcular(p);
        String cantidad = MermaRepository.cantidadTexto(p.cantidadDisponible, p.unidadMedida);
        int colorBorde = 0;
        switch (nivel) {
            case AGOTADO:
                etiqueta(h.tvChipStock, ctx.getString(R.string.inv_chip_agotado),
                        R.color.semaforo_rojo_fondo, R.color.semaforo_rojo_texto);
                colorBorde = R.color.status_rojo;
                break;
            case BAJO:
                etiqueta(h.tvChipStock, ctx.getString(R.string.inv_chip_bajo, cantidad),
                        R.color.semaforo_amarillo_fondo, R.color.semaforo_amarillo_texto);
                colorBorde = R.color.status_ambar;
                break;
            default:
                etiqueta(h.tvChipStock, cantidad, R.color.chip_gris_fondo, R.color.chip_gris_texto);
        }

        // Etiqueta de caducidad: solo si ya caduco o le quedan 7 dias o menos.
        long ahora = System.currentTimeMillis();
        EstadoProducto.Caducidad cad = EstadoProducto.caducidad(p.fechaCaducidadMs, ahora);
        if (cad == EstadoProducto.Caducidad.CADUCADO) {
            etiqueta(h.tvChipCaducidad, ctx.getString(R.string.inv_chip_caducado),
                    R.color.semaforo_rojo_fondo, R.color.semaforo_rojo_texto);
            h.tvChipCaducidad.setVisibility(View.VISIBLE);
            colorBorde = R.color.status_rojo;
        } else if (cad == EstadoProducto.Caducidad.PROXIMA) {
            long dias = EstadoProducto.diasParaCaducar(p.fechaCaducidadMs, ahora);
            String texto = dias == 0 ? ctx.getString(R.string.inv_chip_vence_hoy)
                    : dias == 1 ? ctx.getString(R.string.inv_chip_vence_manana)
                    : ctx.getString(R.string.inv_chip_vence_en, (int) dias);
            etiqueta(h.tvChipCaducidad, texto, R.color.semaforo_amarillo_fondo, R.color.semaforo_amarillo_texto);
            h.tvChipCaducidad.setVisibility(View.VISIBLE);
            if (colorBorde == 0) colorBorde = R.color.status_ambar;
        } else {
            h.tvChipCaducidad.setVisibility(View.GONE);
        }

        // Borde: neutro si todo va bien; del color del aviso mas fuerte si no.
        h.card.setStrokeColor(ContextCompat.getColor(ctx, colorBorde != 0 ? colorBorde : R.color.color_card_stroke));
        h.card.setStrokeWidth(ctx.getResources().getDimensionPixelSize(
                colorBorde != 0 ? R.dimen.card_stroke_width_alerta : R.dimen.card_stroke_width));

        h.card.setOnClickListener(v -> {
            if (listener != null) listener.onEditar(p);
        });
        h.btnEditar.setOnClickListener(v -> {
            if (listener != null) listener.onEditar(p);
        });
        h.btnEliminar.setOnClickListener(v -> {
            if (listener != null) listener.onEliminar(p);
        });
    }

    private static void etiqueta(TextView tv, String texto, @ColorRes int fondo, @ColorRes int color) {
        Context ctx = tv.getContext();
        tv.setText(texto);
        tv.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ctx, fondo)));
        tv.setTextColor(ContextCompat.getColor(ctx, color));
    }

    private static String inicial(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "?";
        return nombre.trim().substring(0, 1).toUpperCase(Locale.forLanguageTag("es-MX"));
    }

    @Override
    public int getItemCount() {
        return productos != null ? productos.size() : 0;
    }

    public static class ProductoViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvInicial, tvNombre, tvPrecio, tvCategoria, tvChipStock, tvChipCaducidad;
        final View btnEditar, btnEliminar;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvInicial = itemView.findViewById(R.id.tvInicial);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvCategoria = itemView.findViewById(R.id.tvCategoria);
            tvChipStock = itemView.findViewById(R.id.tvChipStock);
            tvChipCaducidad = itemView.findViewById(R.id.tvChipCaducidad);
            btnEditar = itemView.findViewById(R.id.btnEditar);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
