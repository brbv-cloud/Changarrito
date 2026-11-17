package com.changarrito.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.dao.AlertaConProducto;
import com.changarrito.database.entity.TipoAlerta;
import com.changarrito.repository.MermaRepository;
import com.changarrito.utils.EstadoProducto;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Tarjetas de Alertas con franja de color a la izquierda:
 *  - roja (urgente): AGOTADO o CADUCADO;
 *  - ambar (pronto): STOCK BAJO o POR CADUCAR.
 * Las de existencia traen "Pedir por WhatsApp" con el mensaje ya escrito al proveedor.
 */
public class AlertaAdapter extends RecyclerView.Adapter<AlertaAdapter.ViewHolder> {

    public interface Listener {
        void onContactarProveedor(AlertaConProducto alerta);

        void onResolver(AlertaConProducto alerta);
    }

    private static final double EPSILON = 0.0001;

    private List<AlertaConProducto> alertas = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setAlertas(List<AlertaConProducto> alertas) {
        this.alertas = alertas != null ? alertas : new ArrayList<>();
        notifyDataSetChanged();
    }

    /** Agotado o ya caducado: va primero y en rojo. */
    public static boolean esUrgente(AlertaConProducto a, long ahora) {
        if (a.tipoAlerta == TipoAlerta.AGOTADO) return true;
        if (a.tipoAlerta == TipoAlerta.STOCK_BAJO) return a.cantidadDisponible <= EPSILON;
        return a.fechaCaducidadMs > 0 && EstadoProducto.diasParaCaducar(a.fechaCaducidadMs, ahora) <= 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alerta, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AlertaConProducto a = alertas.get(position);
        Context ctx = h.itemView.getContext();
        long ahora = System.currentTimeMillis();
        boolean deExistencia = a.tipoAlerta != TipoAlerta.PROXIMO_VENCIMIENTO;
        boolean urgente = esUrgente(a, ahora);

        int color = ContextCompat.getColor(ctx, urgente ? R.color.status_rojo : R.color.status_ambar);
        int colorTexto = ContextCompat.getColor(ctx, urgente ? R.color.semaforo_rojo_texto : R.color.semaforo_amarillo_texto);
        h.franja.setBackgroundColor(color);
        h.ivIcono.setImageResource(deExistencia ? R.drawable.ic_nav_inventario : R.drawable.ic_reloj);
        h.ivIcono.setColorFilter(colorTexto);
        h.tvTipo.setTextColor(colorTexto);
        h.tvNombreProducto.setText(a.nombreProducto);

        if (deExistencia) {
            String minimo = MermaRepository.cantidadTexto(a.umbralStockBajo, a.unidadMedida);
            if (urgente) {
                h.tvTipo.setText(R.string.alerta_tipo_agotado);
                h.tvMensaje.setText(ctx.getString(R.string.alerta_msg_agotado, minimo));
            } else {
                h.tvTipo.setText(R.string.alerta_tipo_bajo);
                h.tvMensaje.setText(ctx.getString(R.string.alerta_msg_bajo,
                        MermaRepository.cantidadTexto(a.cantidadDisponible, a.unidadMedida), minimo));
            }
        } else {
            String fecha = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-MX"))
                    .format(new Date(a.fechaCaducidadMs));
            long dias = EstadoProducto.diasParaCaducar(a.fechaCaducidadMs, ahora);
            h.tvTipo.setText(dias < 0 ? R.string.alerta_tipo_caducado : R.string.alerta_tipo_por_caducar);
            if (dias < 0) h.tvMensaje.setText(ctx.getString(R.string.alerta_msg_caducado, fecha));
            else if (dias == 0) h.tvMensaje.setText(ctx.getString(R.string.alerta_msg_caducado_hoy, fecha));
            else if (dias == 1) h.tvMensaje.setText(ctx.getString(R.string.alerta_msg_manana, fecha));
            else h.tvMensaje.setText(ctx.getString(R.string.alerta_msg_dias, (int) dias, fecha));
        }

        // Pedir al proveedor solo tiene sentido en las de existencia.
        boolean tieneTelefono = a.telefonoProveedor != null && !a.telefonoProveedor.trim().isEmpty();
        h.btnContactar.setVisibility(deExistencia ? View.VISIBLE : View.GONE);
        h.btnContactar.setEnabled(tieneTelefono);
        h.btnContactar.setText(tieneTelefono ? R.string.alerta_pedir : R.string.alerta_sin_telefono);
        h.btnContactar.setOnClickListener(v -> {
            if (listener != null) listener.onContactarProveedor(a);
        });
        h.btnResolver.setOnClickListener(v -> {
            if (listener != null) listener.onResolver(a);
        });
    }

    @Override
    public int getItemCount() {
        return alertas.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final View franja;
        final ImageView ivIcono;
        final TextView tvTipo, tvNombreProducto, tvMensaje;
        final MaterialButton btnContactar, btnResolver;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            franja = itemView.findViewById(R.id.franja);
            ivIcono = itemView.findViewById(R.id.ivIconoAlerta);
            tvTipo = itemView.findViewById(R.id.tvTipo);
            tvNombreProducto = itemView.findViewById(R.id.tvNombreProducto);
            tvMensaje = itemView.findViewById(R.id.tvMensajeAlerta);
            btnContactar = itemView.findViewById(R.id.btnContactarProveedor);
            btnResolver = itemView.findViewById(R.id.btnResolver);
        }
    }
}
