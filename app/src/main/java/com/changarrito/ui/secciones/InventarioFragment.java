package com.changarrito.ui.secciones;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.changarrito.R;
import com.changarrito.database.CatalogoBarcodesHelper;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.negocio.NegocioRepository;
import com.changarrito.negocio.SesionTurno;
import com.changarrito.scanner.OpenFoodFactsClient;
import com.changarrito.scanner.ScannerActivity;
import com.changarrito.ui.activities.negocio.AjustesActivity;
import com.changarrito.ui.adapters.ProductoAdapter;
import com.changarrito.ui.fragments.ProductoDialogFragment;
import com.changarrito.viewmodel.ProductoViewModel;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Inventario: lista de productos, alta/edicion y alta por escaneo de codigo. */
public class InventarioFragment extends SeccionFragment implements ProductoDialogFragment.ProductoDialogListener {

    private ProductoAdapter adapter;
    private ProductoViewModel viewModel;
    private ActivityResultLauncher<Intent> scannerLauncher;
    private ExecutorService catalogoExecutor;
    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());

    public InventarioFragment() {
        super(R.layout.fragment_inventario);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProductoViewModel.class);
        catalogoExecutor = Executors.newSingleThreadExecutor();
        scannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String codigo = result.getData().getStringExtra(ScannerActivity.EXTRA_BARCODE);
                        if (codigo != null) manejarCodigoEscaneado(codigo);
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView rvProductos = vista(R.id.rvProductos);
        rvProductos.setLayoutManager(new LinearLayoutManager(requireContext()));
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

        viewModel.getAllProductos().observe(getViewLifecycleOwner(), productos -> adapter.setProductos(productos));
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        vista(R.id.fabAgregarProducto).setOnClickListener(v -> abrirDialogo(null));
        vista(R.id.fabEscanear).setOnClickListener(v ->
                scannerLauncher.launch(new Intent(requireContext(), ScannerActivity.class)));
        vista(R.id.btnAjustes).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AjustesActivity.class)));

        new NegocioRepository(requireActivity().getApplication()).getConfiguracion()
                .observe(getViewLifecycleOwner(), config -> {
                    if (config != null && config.nombreNegocio != null) {
                        ((TextView) vista(R.id.tvNegocio)).setText(config.nombreNegocio);
                    }
                });
    }

    @Override
    protected void alMostrar() {
        // El turno puede cambiar en Ajustes: se refresca cada vez que se ve esta seccion.
        ((TextView) vista(R.id.tvTurno)).setText(
                getString(R.string.ajustes_turno_actual, SesionTurno.getNombre(requireContext())));
    }

    /**
     * 1. Busca el codigo en el inventario (offline, instantaneo).
     * 2. Si no esta, en el catalogo offline precargado (~15,000 productos).
     * 3. Si tampoco, intenta el nombre en Open Food Facts (unico paso con internet).
     * 4. Si nada lo identifica, abre el formulario solo con el codigo.
     */
    private void manejarCodigoEscaneado(String codigo) {
        LiveData<ProductoEntity> busqueda = viewModel.getProductoByBarcode(codigo);
        busqueda.observe(this, new Observer<ProductoEntity>() {
            @Override
            public void onChanged(ProductoEntity producto) {
                // Solo interesa el primer resultado.
                busqueda.removeObserver(this);
                if (producto != null) {
                    Toast.makeText(requireContext(), "Producto encontrado: " + producto.nombre,
                            Toast.LENGTH_SHORT).show();
                    abrirDialogo(producto);
                } else {
                    buscarEnCatalogoOfflineYAbrir(codigo);
                }
            }
        });
    }

    private void buscarEnCatalogoOfflineYAbrir(String codigo) {
        Context app = requireContext().getApplicationContext();
        catalogoExecutor.execute(() -> {
            String nombre = CatalogoBarcodesHelper.getInstance(app).buscarNombre(codigo);
            hiloPrincipal.post(() -> {
                if (!isAdded()) return;
                if (nombre != null) {
                    ProductoEntity nuevo = new ProductoEntity();
                    nuevo.barcode = codigo;
                    nuevo.nombre = nombre;
                    Toast.makeText(requireContext(), "Identificado (catálogo offline): " + nombre,
                            Toast.LENGTH_SHORT).show();
                    abrirDialogo(nuevo);
                } else {
                    buscarEnLineaYAbrir(codigo);
                }
            });
        });
    }

    private void buscarEnLineaYAbrir(String codigo) {
        Toast.makeText(requireContext(), "Buscando producto en línea...", Toast.LENGTH_SHORT).show();
        OpenFoodFactsClient.buscarNombre(codigo, nombre -> {
            if (!isAdded()) return;
            ProductoEntity nuevo = new ProductoEntity();
            nuevo.barcode = codigo;
            if (nombre != null) {
                nuevo.nombre = nombre;
                Toast.makeText(requireContext(), "Identificado: " + nombre, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "No se identificó, captura el nombre a mano",
                        Toast.LENGTH_SHORT).show();
            }
            abrirDialogo(nuevo);
        });
    }

    private void abrirDialogo(ProductoEntity producto) {
        // Con getChildFragmentManager el formulario sabe a quien avisar al guardar,
        // incluso si Android lo vuelve a crear (por ejemplo al girar la pantalla).
        ProductoDialogFragment.newInstance(producto, this).show(getChildFragmentManager(), "ProductoDialog");
    }

    @Override
    public void onProductoGuardado(ProductoEntity producto) {
        viewModel.guardarProducto(producto);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (catalogoExecutor != null) catalogoExecutor.shutdown();
    }
}
