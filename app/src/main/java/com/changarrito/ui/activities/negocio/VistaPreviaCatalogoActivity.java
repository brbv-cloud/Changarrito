package com.changarrito.ui.activities.negocio;

import android.content.res.ColorStateList;
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
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Muestra el catálogo sugerido del giro agrupado por categoría. El dueño marca las
 * categorías que vende (todas vienen marcadas) o abre una para escoger producto por
 * producto, y lo importa, o empieza vacío.
 */
public class VistaPreviaCatalogoActivity extends AppCompatActivity {

    private final Map<String, CheckBox> casillas = new LinkedHashMap<>();
    private final Map<String, List<CheckBox>> casillasProducto = new LinkedHashMap<>();
    private final Map<String, TextView> conteos = new java.util.HashMap<>();
    private final Map<CheckBox, ProductoEntity> productoDe = new java.util.HashMap<>();
    /** true mientras se marcan varias casillas a la vez (para no recalcular en cada una). */
    private boolean sincronizando;
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

        // Si se equivoco de giro o de nombre, puede regresar (boton de arriba o "atras").
        findViewById(R.id.tvCambiarGiro).setOnClickListener(v -> regresarAGiro());
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                regresarAGiro();
            }
        });
    }

    private void regresarAGiro() {
        if (!btnImportar.isEnabled()) return; // ya esta importando
        startActivity(new android.content.Intent(this, ElegirGiroActivity.class));
        finish();
    }

    private void mostrar(ConfiguracionNegocioEntity config) {
        catalogo = CatalogoGiro.cargar(this, config.giro);
        ((TextView) findViewById(R.id.tvSubtitulo)).setText(getString(R.string.catalogo_subtitulo,
                catalogo.total(), config.giro.nombreVisible.toLowerCase(Locale.getDefault())));

        LinearLayout contenedor = findViewById(R.id.contenedorCategorias);
        LayoutInflater inflater = LayoutInflater.from(this);
        int alto48 = (int) (48 * getResources().getDisplayMetrics().density);
        ColorStateList verde = ColorStateList.valueOf(getColor(R.color.marca_verde));
        for (Map.Entry<String, List<ProductoEntity>> grupo : catalogo.porCategoria.entrySet()) {
            String categoria = grupo.getKey();
            View tarjeta = inflater.inflate(R.layout.item_categoria_catalogo, contenedor, false);
            MaterialCheckBox cb = tarjeta.findViewById(R.id.cbCategoria);
            ImageView ivExpandir = tarjeta.findViewById(R.id.ivExpandir);
            LinearLayout productos = tarjeta.findViewById(R.id.contenedorProductos);

            ((TextView) tarjeta.findViewById(R.id.tvCategoria)).setText(categoria);
            conteos.put(categoria, tarjeta.findViewById(R.id.tvConteo));
            cb.setContentDescription(categoria);
            ivExpandir.setContentDescription(getString(R.string.catalogo_ver, categoria));

            // Cada producto con su propia casilla: se puede quedar con 5 de 7, por ejemplo.
            List<CheckBox> hijos = new ArrayList<>();
            for (ProductoEntity p : grupo.getValue()) {
                MaterialCheckBox renglon = new MaterialCheckBox(this);
                renglon.setText(String.format(Locale.getDefault(), "%s · $%.2f / %s",
                        p.nombre, p.precio, p.unidadMedida.name()));
                renglon.setTextColor(getColor(R.color.acceso_texto));
                renglon.setTextSize(14);
                renglon.setMinHeight(alto48);
                renglon.setButtonTintList(verde);
                renglon.setChecked(true);
                renglon.setOnCheckedChangeListener((b, marcado) -> {
                    if (!sincronizando) actualizarCategoria(categoria);
                });
                productos.addView(renglon);
                hijos.add(renglon);
                productoDe.put(renglon, p);
            }
            casillasProducto.put(categoria, hijos);

            // La casilla de la categoria marca o desmarca todos sus productos de un jalon.
            cb.setOnClickListener(v -> marcarCategoria(categoria, cuantosMarcados(categoria) < hijos.size()));
            // Tocar la fila despliega los productos para escoger uno por uno.
            tarjeta.findViewById(R.id.filaCategoria).setOnClickListener(v -> {
                boolean abrir = productos.getVisibility() != View.VISIBLE;
                productos.setVisibility(abrir ? View.VISIBLE : View.GONE);
                ivExpandir.animate().rotation(abrir ? 180 : 0).setDuration(150).start();
            });

            casillas.put(categoria, cb);
            contenedor.addView(tarjeta);
            actualizarCategoria(categoria);
        }
        actualizarBoton();
    }

    private int cuantosMarcados(String categoria) {
        int n = 0;
        for (CheckBox c : casillasProducto.get(categoria)) if (c.isChecked()) n++;
        return n;
    }

    private void marcarCategoria(String categoria, boolean marcar) {
        sincronizando = true;
        for (CheckBox c : casillasProducto.get(categoria)) c.setChecked(marcar);
        sincronizando = false;
        actualizarCategoria(categoria);
    }

    /** Casilla de la categoria: palomita si van todos, rayita si solo algunos, vacia si ninguno. */
    private void actualizarCategoria(String categoria) {
        int total = casillasProducto.get(categoria).size();
        int marcados = cuantosMarcados(categoria);
        MaterialCheckBox cb = (MaterialCheckBox) casillas.get(categoria);
        if (cb != null) {
            cb.setCheckedState(marcados == 0 ? MaterialCheckBox.STATE_UNCHECKED
                    : marcados == total ? MaterialCheckBox.STATE_CHECKED : MaterialCheckBox.STATE_INDETERMINATE);
        }
        TextView conteo = conteos.get(categoria);
        if (conteo != null) {
            conteo.setText(marcados == total || marcados == 0
                    ? getString(R.string.catalogo_productos, total)
                    : getString(R.string.catalogo_de, marcados, total));
        }
        actualizarBoton();
    }

    private void marcarTodas(boolean marcar) {
        for (String categoria : casillasProducto.keySet()) marcarCategoria(categoria, marcar);
    }

    private List<ProductoEntity> seleccionados() {
        List<ProductoEntity> lista = new ArrayList<>();
        if (catalogo == null) return lista;
        for (List<CheckBox> hijos : casillasProducto.values()) {
            for (CheckBox c : hijos) if (c.isChecked()) lista.add(productoDe.get(c));
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
