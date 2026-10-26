package com.changarrito.ui.secciones;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import com.changarrito.scanner.OpenFoodFactsClient;
import com.changarrito.scanner.ScannerActivity;
import com.changarrito.ui.adapters.ProductoAdapter;
import com.changarrito.ui.fragments.ProductoDialogFragment;
import com.changarrito.utils.EstadoProducto;
import com.changarrito.utils.EstadoStock;
import com.changarrito.viewmodel.ProductoViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Inventario: productos en tarjetas, buscador (nombre, categoria o codigo), filtros por
 * estado (stock bajo, agotados, por caducar), alta/edicion y alta por escaneo de codigo.
 */
public class InventarioFragment extends SeccionFragment implements ProductoDialogFragment.ProductoDialogListener {

    private ProductoAdapter adapter;
    private ProductoViewModel viewModel;
    private ActivityResultLauncher<Intent> scannerLauncher;
    private ExecutorService catalogoExecutor;
    private final Handler hiloPrincipal = new Handler(Looper.getMainLooper());

    private List<ProductoEntity> todos = new ArrayList<>();
    private boolean cargado; // hasta que llega la lista de la BD no se dice "no hay productos"
    private String busqueda = "";
    private ChipGroup chipsFiltro;
    private TextView tvResumen, tvVacio;
    private View btnLimpiar;

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
                confirmarQuitar(producto);
            }
        });
        rvProductos.setAdapter(adapter);

        tvResumen = vista(R.id.tvResumen);
        tvVacio = vista(R.id.tvVacio);
        chipsFiltro = vista(R.id.chipsFiltro);
        chipsFiltro.setOnCheckedStateChangeListener((grupo, ids) -> aplicarFiltro());

        EditText etBuscar = vista(R.id.etBuscar);
        btnLimpiar = vista(R.id.btnLimpiarBusqueda);
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                busqueda = s.toString();
                btnLimpiar.setVisibility(busqueda.isEmpty() ? View.GONE : View.VISIBLE);
                aplicarFiltro();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        btnLimpiar.setOnClickListener(v -> etBuscar.setText(""));
        // La lista ya se filtra mientras se escribe: la tecla "buscar" solo cierra el teclado.
        etBuscar.setOnEditorActionListener((v, accion, evento) -> {
            if (accion != EditorInfo.IME_ACTION_SEARCH) return false;
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            v.clearFocus();
            return true;
        });

        viewModel.getAllProductos().observe(getViewLifecycleOwner(), productos -> {
            todos = productos != null ? productos : new ArrayList<>();
            cargado = true;
            aplicarFiltro();
        });
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });

        vista(R.id.fabAgregarProducto).setOnClickListener(v -> abrirDialogo(null));
        vista(R.id.btnEscanear).setOnClickListener(v ->
                scannerLauncher.launch(new Intent(requireContext(), ScannerActivity.class)));
    }

    @Override
    protected void alMostrar() {
        // "Vence en 3 dias" depende de la fecha de hoy: se recalcula cada vez que se ve la seccion.
        aplicarFiltro();
    }

    // ------------------------------------------------------------------ buscador y filtros

    /** Cuenta cada estado, pone los numeros en los filtros y muestra solo lo que coincide. */
    private void aplicarFiltro() {
        if (getView() == null || !cargado) return;
        long ahora = System.currentTimeMillis();
        int bajos = 0, agotados = 0, porCaducar = 0, porAtender = 0;
        for (ProductoEntity p : todos) {
            EstadoStock.Nivel nivel = EstadoStock.calcular(p);
            boolean caduca = EstadoProducto.caducidadConAviso(p.fechaCaducidadMs, ahora);
            if (nivel == EstadoStock.Nivel.BAJO) bajos++;
            if (nivel == EstadoStock.Nivel.AGOTADO) agotados++;
            if (caduca) porCaducar++;
            if (nivel != EstadoStock.Nivel.NORMAL || caduca) porAtender++;
        }

        ((Chip) vista(R.id.chipTodos)).setText(getString(R.string.inv_filtro_todos, todos.size()));
        ((Chip) vista(R.id.chipBajo)).setText(getString(R.string.inv_filtro_bajo, bajos));
        ((Chip) vista(R.id.chipAgotados)).setText(getString(R.string.inv_filtro_agotados, agotados));
        ((Chip) vista(R.id.chipCaducar)).setText(getString(R.string.inv_filtro_caducar, porCaducar));

        String cuantos = getResources().getQuantityString(R.plurals.inv_productos_n, todos.size(), todos.size());
        tvResumen.setText(porAtender > 0 ? getString(R.string.inv_por_atender, cuantos, porAtender) : cuantos);

        int filtro = chipsFiltro.getCheckedChipId();
        List<ProductoEntity> visibles = new ArrayList<>();
        for (ProductoEntity p : todos) {
            if (pasaFiltro(p, filtro, ahora)
                    && EstadoProducto.coincide(busqueda, p.nombre, p.categoria, p.barcode)) {
                visibles.add(p);
            }
        }
        adapter.setProductos(visibles);

        if (!visibles.isEmpty()) {
            tvVacio.setVisibility(View.GONE);
        } else {
            tvVacio.setVisibility(View.VISIBLE);
            if (todos.isEmpty()) tvVacio.setText(R.string.inv_vacio_sin_productos);
            else if (!busqueda.trim().isEmpty()) tvVacio.setText(getString(R.string.inv_vacio_busqueda, busqueda.trim()));
            else tvVacio.setText(R.string.inv_vacio_filtro);
        }
    }

    private static boolean pasaFiltro(ProductoEntity p, int filtro, long ahora) {
        if (filtro == R.id.chipBajo) return EstadoStock.calcular(p) == EstadoStock.Nivel.BAJO;
        if (filtro == R.id.chipAgotados) return EstadoStock.calcular(p) == EstadoStock.Nivel.AGOTADO;
        if (filtro == R.id.chipCaducar) return EstadoProducto.caducidadConAviso(p.fechaCaducidadMs, ahora);
        return true;
    }

    /** Quitar es con un toque del bote: se pregunta antes para que no pase por accidente. */
    private void confirmarQuitar(ProductoEntity producto) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.inv_quitar_titulo, producto.nombre))
                .setMessage(R.string.inv_quitar_texto)
                .setPositiveButton(R.string.inv_quitar_si, (d, w) -> viewModel.eliminarProducto(producto))
                .setNegativeButton(R.string.inv_cancelar, null)
                .show();
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
