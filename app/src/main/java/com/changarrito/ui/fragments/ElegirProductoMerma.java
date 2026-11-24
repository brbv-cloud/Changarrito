package com.changarrito.ui.fragments;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.AppDatabase;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.repository.MermaRepository;
import com.changarrito.utils.EstadoProducto;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * "Registrar merma" desde Inicio o Reportes: primero se elige el producto (con buscador)
 * y luego sale el mismo cuadro de merma que en Editar producto.
 * Solo aparecen los productos que tienen existencia: de lo que no hay, no se puede perder.
 */
public final class ElegirProductoMerma {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private ElegirProductoMerma() {
    }

    public static void mostrar(Activity a) {
        if (a == null || a.isFinishing()) return;
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_elegir_producto, null, false);
        EditText etBuscar = v.findViewById(R.id.etBuscarProducto);
        RecyclerView rv = v.findViewById(R.id.rvElegirProducto);
        TextView tvVacio = v.findViewById(R.id.tvElegirVacio);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.merma_elegir_titulo)
                .setView(v)
                .setNegativeButton(R.string.producto_cancelar, null)
                .create();

        Adaptador adaptador = new Adaptador(p -> {
            dialogo.dismiss();
            MermaDialog.mostrar(a, p, null);
        });
        rv.setLayoutManager(new LinearLayoutManager(a));
        rv.setAdapter(adaptador);

        Runnable actualizarVacio = () -> {
            boolean vacio = adaptador.cargado && adaptador.getItemCount() == 0;
            tvVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
            tvVacio.setText(adaptador.todos.isEmpty() ? R.string.merma_elegir_sin_productos : R.string.merma_elegir_vacio);
        };
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                adaptador.filtrar(s.toString());
                actualizarVacio.run();
            }
        });
        dialogo.show();

        Application app = a.getApplication();
        Handler principal = new Handler(Looper.getMainLooper());
        EXECUTOR.execute(() -> {
            List<ProductoEntity> conExistencia = new ArrayList<>();
            for (ProductoEntity p : AppDatabase.getInstance(app).productoDao().getAllProductosSync()) {
                if (p.cantidadDisponible > 0.0001) conExistencia.add(p);
            }
            Collator orden = Collator.getInstance(Locale.forLanguageTag("es-MX"));
            orden.setStrength(Collator.PRIMARY);
            Collections.sort(conExistencia, (x, y) -> orden.compare(
                    x.nombre != null ? x.nombre : "", y.nombre != null ? y.nombre : ""));
            principal.post(() -> {
                if (!dialogo.isShowing()) return;
                adaptador.cargar(conExistencia, etBuscar.getText().toString());
                actualizarVacio.run();
            });
        });
    }

    // ------------------------------------------------------------------ lista

    private interface AlElegir {
        void elegido(ProductoEntity p);
    }

    private static final class Adaptador extends RecyclerView.Adapter<Adaptador.Fila> {
        final List<ProductoEntity> todos = new ArrayList<>();
        private final List<ProductoEntity> visibles = new ArrayList<>();
        private final AlElegir alElegir;
        boolean cargado;
        private String busqueda = "";

        Adaptador(AlElegir alElegir) {
            this.alElegir = alElegir;
        }

        void cargar(List<ProductoEntity> lista, String texto) {
            todos.clear();
            todos.addAll(lista);
            cargado = true;
            filtrar(texto);
        }

        void filtrar(String texto) {
            busqueda = texto != null ? texto : "";
            visibles.clear();
            for (ProductoEntity p : todos) {
                if (EstadoProducto.coincide(busqueda, p.nombre, p.categoria, p.barcode)) visibles.add(p);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Fila onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Fila(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_elegir_producto, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Fila h, int position) {
            ProductoEntity p = visibles.get(position);
            String nombre = p.nombre != null ? p.nombre.trim() : "";
            h.inicial.setText(nombre.isEmpty() ? "?" : nombre.substring(0, 1).toUpperCase(Locale.forLanguageTag("es-MX")));
            h.nombre.setText(nombre);
            h.existencia.setText(h.itemView.getContext().getString(R.string.merma_elegir_hay,
                    MermaRepository.cantidadTexto(p.cantidadDisponible, p.unidadMedida)));
            h.itemView.setOnClickListener(x -> alElegir.elegido(p));
        }

        @Override
        public int getItemCount() {
            return visibles.size();
        }

        static final class Fila extends RecyclerView.ViewHolder {
            final TextView inicial, nombre, existencia;

            Fila(View v) {
                super(v);
                inicial = v.findViewById(R.id.tvElegirInicial);
                nombre = v.findViewById(R.id.tvElegirNombre);
                existencia = v.findViewById(R.id.tvElegirExistencia);
            }
        }
    }
}
