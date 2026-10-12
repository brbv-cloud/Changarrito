package com.changarrito.ui.activities.negocio;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.bitacora.DescripcionBitacora;
import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.BitacoraConUsuario;
import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.utils.FechaUtil;
import com.changarrito.utils.InsetsUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Bitácora de cambios (solo dueño): las últimas 300 entradas, agrupadas por día. */
public class BitacoraActivity extends AppCompatActivity {

    private static final int LIMITE = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitacora);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootBitacora), true, true);
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvBitacora);
        TextView tvVacio = findViewById(R.id.tvVacio);
        Adaptador adaptador = new Adaptador();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adaptador);

        AppDatabase.getInstance(this).bitacoraDao().getRecientesConUsuario(LIMITE).observe(this, lista -> {
            adaptador.poner(lista);
            tvVacio.setVisibility(lista == null || lista.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private class Adaptador extends RecyclerView.Adapter<Adaptador.Fila> {
        private final List<BitacoraConUsuario> datos = new ArrayList<>();
        private final SimpleDateFormat hora = new SimpleDateFormat("HH:mm", Locale.getDefault());
        private final SimpleDateFormat dia = new SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-MX"));

        void poner(List<BitacoraConUsuario> nuevos) {
            datos.clear();
            if (nuevos != null) datos.addAll(nuevos);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Fila onCreateViewHolder(@NonNull ViewGroup padre, int tipo) {
            return new Fila(LayoutInflater.from(padre.getContext()).inflate(R.layout.item_bitacora, padre, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Fila f, int i) {
            BitacoraEntity b = datos.get(i).entrada;
            long inicioDia = FechaUtil.inicioDelDia(b.timestampMs);
            boolean primeraDelDia = i == 0 || FechaUtil.inicioDelDia(datos.get(i - 1).entrada.timestampMs) != inicioDia;
            f.tvDia.setVisibility(primeraDelDia ? View.VISIBLE : View.GONE);
            if (primeraDelDia) f.tvDia.setText(nombreDia(b.timestampMs));
            f.tvHora.setText(hora.format(new Date(b.timestampMs)));
            f.tvTexto.setText(DescripcionBitacora.texto(datos.get(i).nombreUsuario, b.accion, b.campo,
                    b.nombreProducto, b.valorAnterior, b.valorNuevo));
        }

        private String nombreDia(long ms) {
            if (DateUtils.isToday(ms)) return getString(R.string.bitacora_hoy);
            if (DateUtils.isToday(ms + DateUtils.DAY_IN_MILLIS)) return getString(R.string.bitacora_ayer);
            String s = dia.format(new Date(ms));
            return s.substring(0, 1).toUpperCase(Locale.getDefault()) + s.substring(1);
        }

        @Override
        public int getItemCount() {
            return datos.size();
        }

        class Fila extends RecyclerView.ViewHolder {
            final TextView tvDia, tvHora, tvTexto;

            Fila(View v) {
                super(v);
                tvDia = v.findViewById(R.id.tvDia);
                tvHora = v.findViewById(R.id.tvHora);
                tvTexto = v.findViewById(R.id.tvTexto);
            }
        }
    }
}
