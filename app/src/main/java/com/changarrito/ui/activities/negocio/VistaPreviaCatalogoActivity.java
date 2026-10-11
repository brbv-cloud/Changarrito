package com.changarrito.ui.activities.negocio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.changarrito.R;
import com.changarrito.auth.AuthRepository;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.negocio.CatalogoGiro;
import com.changarrito.negocio.NegocioRepository;
import com.changarrito.ui.activities.acceso.NavegacionAcceso;
import com.changarrito.utils.InsetsUtil;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Muestra el catálogo sugerido del giro agrupado por categoría. El dueño marca las
 * categorías que vende (todas vienen marcadas) y lo importa, o empieza vacío.
 */
public class VistaPreviaCatalogoActivity extends AppCompatActivity {

    private final Map<String, CheckBox> casillas = new LinkedHashMap<>();
    private CatalogoGiro catalogo;
    private NegocioRepository repo;
    private MaterialButton btnImportar, btnVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vista_previa_catalogo);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootCatalogo), true, true);
        repo = new NegocioRepository(getApplication());
        btnImportar = findViewById(R.id.btnImportar);
        btnVacio = findViewById(R.id.btnVacio);

        repo.getConfiguracion().observe(this, config -> {
            if (config == null || catalogo != null) return;
            mostrar(config);
        });

        findViewById(R.id.tvTodas).setOnClickListener(v -> marcarTodas(true));
        findViewById(R.id.tvNinguna).setOnClickListener(v -> marcarTodas(false));
        btnImportar.setOnClickListener(v -> importar(seleccionados()));
        btnVacio.setOnClickListener(v -> importar(new ArrayList<>()));
    }

    private void mostrar(ConfiguracionNegocioEntity config) {
        catalogo = CatalogoGiro.cargar(this, config.giro);
        ((TextView) findViewById(R.id.tvSubtitulo)).setText(getString(R.string.catalogo_subtitulo,
                catalogo.total(), config.giro.nombreVisible.toLowerCase(Locale.getDefault())));

        LinearLayout contenedor = findViewById(R.id.contenedorCategorias);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Map.Entry<String, List<ProductoEntity>> grupo : catalogo.porCategoria.entrySet()) {
            View tarjeta = inflater.inflate(R.layout.item_categoria_catalogo, contenedor, false);
            CheckBox cb = tarjeta.findViewById(R.id.cbCategoria);
            ImageView ivExpandir = tarjeta.findViewById(R.id.ivExpandir);
            LinearLayout productos = tarjeta.findViewById(R.id.contenedorProductos);

            ((TextView) tarjeta.findViewById(R.id.tvCategoria)).setText(grupo.getKey());
            ((TextView) tarjeta.findViewById(R.id.tvConteo)).setText(
                    getString(R.string.catalogo_productos, grupo.getValue().size()));
            cb.setContentDescription(grupo.getKey());
            cb.setOnCheckedChangeListener((b, marcado) -> actualizarBoton());
            ivExpandir.setContentDescription(getString(R.string.catalogo_ver, grupo.getKey()));

            for (ProductoEntity p : grupo.getValue()) {
                TextView renglon = new TextView(this);
                renglon.setText(String.format(Locale.getDefault(), "%s · $%.2f / %s",
                        p.nombre, p.precio, p.unidadMedida.name()));
                renglon.setTextColor(getColor(R.color.acceso_texto_secundario));
                renglon.setTextSize(14);
                renglon.setPadding(0, 6, 0, 6);
                productos.addView(renglon);
            }
            // Tocar la fila despliega los productos; la casilla marca o desmarca la categoría.
            tarjeta.findViewById(R.id.filaCategoria).setOnClickListener(v -> {
                boolean abrir = productos.getVisibility() != View.VISIBLE;
                productos.setVisibility(abrir ? View.VISIBLE : View.GONE);
                ivExpandir.animate().rotation(abrir ? 180 : 0).setDuration(150).start();
            });

            casillas.put(grupo.getKey(), cb);
            contenedor.addView(tarjeta);
        }
        actualizarBoton();
    }

    private void marcarTodas(boolean marcar) {
        for (CheckBox cb : casillas.values()) cb.setChecked(marcar);
    }

    private List<ProductoEntity> seleccionados() {
        List<ProductoEntity> lista = new ArrayList<>();
        if (catalogo == null) return lista;
        for (Map.Entry<String, CheckBox> e : casillas.entrySet()) {
            if (e.getValue().isChecked()) lista.addAll(catalogo.porCategoria.get(e.getKey()));
        }
        return lista;
    }

    private void actualizarBoton() {
        int n = seleccionados().size();
        btnImportar.setText(getString(R.string.catalogo_importar, n));
        btnImportar.setEnabled(n > 0);
    }

    private void importar(List<ProductoEntity> productos) {
        btnImportar.setEnabled(false);
        btnVacio.setEnabled(false);
        if (!productos.isEmpty()) btnImportar.setText(R.string.catalogo_importando);
        repo.importarCatalogo(productos, n -> {
            if (n > 0) Toast.makeText(this, getString(R.string.catalogo_importados, n), Toast.LENGTH_LONG).show();
            NavegacionAcceso.irAlDestino(this, new AuthRepository());
        });
    }
}
