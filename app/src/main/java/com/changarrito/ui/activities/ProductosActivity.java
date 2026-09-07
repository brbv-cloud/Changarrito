package com.changarrito.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.CatalogoBarcodesHelper;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.scanner.OpenFoodFactsClient;
import com.changarrito.scanner.ScannerActivity;
import com.changarrito.ui.adapters.ProductoAdapter;
import com.changarrito.ui.fragments.ProductoDialogFragment;
import com.changarrito.utils.InsetsUtil;
import com.changarrito.viewmodel.ProductoViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductosActivity extends AppCompatActivity implements ProductoDialogFragment.ProductoDialogListener {

    private RecyclerView rvProductos;
    private FloatingActionButton fabAgregarProducto, fabEscanear, fabVender;
    private ProductoAdapter adapter;
    private ProductoViewModel viewModel;

    private ActivityResultLauncher<Intent> scannerLauncher;
    private ExecutorService catalogoExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productos);

        // Evita que la lista arranque debajo de la barra de notificaciones y que los
        // FAB queden tapados por la barra de navegación en equipos edge-to-edge (Android 15+).
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootProductos), true, true);

        rvProductos = findViewById(R.id.rvProductos);
        fabAgregarProducto = findViewById(R.id.fabAgregarProducto);
        fabEscanear = findViewById(R.id.fabEscanear);
        fabVender = findViewById(R.id.fabVender);

        viewModel = new ViewModelProvider(this).get(ProductoViewModel.class);
        catalogoExecutor = Executors.newSingleThreadExecutor();

        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String codigo = result.getData().getStringExtra(ScannerActivity.EXTRA_BARCODE);
                        if (codigo != null) {
                            manejarCodigoEscaneado(codigo);
                        }
                    }
                }
        );

        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductoAdapter(new ArrayList<>());
        adapter.setOnProductoClickListener(new ProductoAdapter.OnProductoClickListener() {
            @Override
            public void onEditar(ProductoEntity producto) {
                abrirDialogo(producto);
            }

            @Override
            public void onEliminar(ProductoEntity producto) {
                viewModel.eliminarProducto(producto);
            }
        });
        rvProductos.setAdapter(adapter);

        viewModel.getAllProductos().observe(this, productos -> {
            adapter.setProductos(productos);
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        fabAgregarProducto.setOnClickListener(v -> abrirDialogo(null));

        fabEscanear.setOnClickListener(v ->
                scannerLauncher.launch(new Intent(this, ScannerActivity.class))
        );

        fabVender.setOnClickListener(v ->
                startActivity(new Intent(this, VentaActivity.class))
        );
    }

    /**
     * 1. Busca el código en la BD local de inventario (offline, instantáneo).
     * 2. Si no existe ahí, busca en el catálogo offline precargado (offline, ~15,000 productos).
     * 3. Si tampoco está ahí, intenta enriquecer el nombre desde Open Food Facts en línea
     *    (único paso que requiere internet, y solo como último recurso).
     * 4. Si nada de lo anterior lo identifica, abre el formulario solo con el código.
     */
    private void manejarCodigoEscaneado(String codigo) {
        LiveData<ProductoEntity> busqueda = viewModel.getProductoByBarcode(codigo);

        busqueda.observe(this, new Observer<ProductoEntity>() {
            @Override
            public void onChanged(ProductoEntity producto) {
                // Dejar de observar: solo queremos el primer resultado
                busqueda.removeObserver(this);

                if (producto != null) {
                    Toast.makeText(ProductosActivity.this,
                            "Producto encontrado: " + producto.nombre,
                            Toast.LENGTH_SHORT).show();
                    abrirDialogo(producto);
                } else {
                    buscarEnCatalogoOfflineYAbrir(codigo);
                }
            }
        });
    }

    private void buscarEnCatalogoOfflineYAbrir(String codigo) {
        catalogoExecutor.execute(() -> {
            String nombre = CatalogoBarcodesHelper
                    .getInstance(getApplicationContext())
                    .buscarNombre(codigo);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (nombre != null) {
                    ProductoEntity nuevo = new ProductoEntity();
                    nuevo.barcode = codigo;
                    nuevo.nombre = nombre;
                    Toast.makeText(this, "Identificado (catálogo offline): " + nombre,
                            Toast.LENGTH_SHORT).show();
                    abrirDialogo(nuevo);
                } else {
                    buscarEnLineaYAbrir(codigo);
                }
            });
        });
    }

    private void buscarEnLineaYAbrir(String codigo) {
        Toast.makeText(this, "Buscando producto en línea...", Toast.LENGTH_SHORT).show();

        OpenFoodFactsClient.buscarNombre(codigo, nombre -> {
            ProductoEntity nuevo = new ProductoEntity();
            nuevo.barcode = codigo;

            if (nombre != null) {
                nuevo.nombre = nombre;
                Toast.makeText(this, "Identificado: " + nombre, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "No se identificó, captura el nombre a mano",
                        Toast.LENGTH_SHORT).show();
            }

            abrirDialogo(nuevo);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (catalogoExecutor != null) {
            catalogoExecutor.shutdown();
        }
    }

    private void abrirDialogo(ProductoEntity producto) {
        ProductoDialogFragment dialog = ProductoDialogFragment.newInstance(producto, this);
        dialog.show(getSupportFragmentManager(), "ProductoDialog");
    }

    @Override
    public void onProductoGuardado(ProductoEntity producto) {
        viewModel.guardarProducto(producto);
    }
}