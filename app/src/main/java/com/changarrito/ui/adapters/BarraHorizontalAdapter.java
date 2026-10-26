package com.changarrito.ui.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista con barra horizontal: "1. Coca-Cola ███████ 45 PZA". Sirve para el top 10,
 * las categorias (como leyenda de la dona), los motivos de merma y los huesos.
 */
public class BarraHorizontalAdapter extends RecyclerView.Adapter<BarraHorizontalAdapter.ViewHolder> {

    public static class Fila {
        public String numero;      // "1", "2"... o null
        public String titulo;
        public String subtitulo;   // opcional
        public String valor;
        public float proporcion;   // 0 a 1 (largo de la barra); negativo = sin barra
        public int color;

        public Fila(String numero, String titulo, String subtitulo, String valor, float proporcion, int color) {
            this.numero = numero;
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.valor = valor;
            this.proporcion = proporcion;
            this.color = color;
        }
    }

    private List<Fila> filas = new ArrayList<>();

    public void setFilas(List<Fila> nuevas) {
        filas = nuevas != null ? nuevas : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_barra_horizontal, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Fila f = filas.get(position);
        h.tvNumero.setVisibility(f.numero != null ? View.VISIBLE : View.GONE);
        h.tvNumero.setText(f.numero);
        h.tvTitulo.setText(f.titulo);
        h.tvValor.setText(f.valor);
        h.tvSubtitulo.setVisibility(f.subtitulo != null ? View.VISIBLE : View.GONE);
        h.tvSubtitulo.setText(f.subtitulo);

        boolean conBarra = f.proporcion >= 0;
        h.contenedorBarra.setVisibility(conBarra ? View.VISIBLE : View.GONE);
        if (conBarra) {
            float p = Math.max(0.03f, Math.min(1f, f.proporcion));
            LinearLayout.LayoutParams lb = (LinearLayout.LayoutParams) h.vBarra.getLayoutParams();
            lb.weight = p;
            h.vBarra.setLayoutParams(lb);
            LinearLayout.LayoutParams le = (LinearLayout.LayoutParams) h.vResto.getLayoutParams();
            le.weight = 1f - p;
            h.vResto.setLayoutParams(le);
            ViewCompat.setBackgroundTintList(h.vBarra, ColorStateList.valueOf(f.color));
        }
    }

    @Override
    public int getItemCount() {
        return filas.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvNumero, tvTitulo, tvSubtitulo, tvValor;
        final View contenedorBarra, vBarra, vResto;

        ViewHolder(@NonNull View v) {
            super(v);
            tvNumero = v.findViewById(R.id.tvNumero);
            tvTitulo = v.findViewById(R.id.tvTitulo);
            tvSubtitulo = v.findViewById(R.id.tvSubtitulo);
            tvValor = v.findViewById(R.id.tvValor);
            contenedorBarra = v.findViewById(R.id.contenedorBarra);
            vBarra = v.findViewById(R.id.vBarra);
            vResto = v.findViewById(R.id.vResto);
        }
    }
}
