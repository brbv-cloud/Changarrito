package com.changarrito.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
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
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.scanner.ScannerActivity;
import com.changarrito.ui.adapters.CarritoAdapter;
import com.changarrito.ui.adapters.ProductoVentaAdapter;
import com.changarrito.ui.fragments.CantidadVentaDialogFragment;
import com.changarrito.utils.InsetsUtil;
import com.changarrito.viewmodel.VentaViewModel;

import java.util.Locale;

public class VentaActivity extends AppCompatActivity implements CantidadVentaDialogFragment.Listener {

    private VentaViewModel viewModel;
    private ProductoVentaAdapter productoVentaAdapter;
    private CarritoAdapter carritoAdapter;
    private TextView tvTotal;
    private ActivityResultLauncher<Intent> scannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venta);
        setTitle("Nueva venta");

        // El buscador + botón de escaneo viven pegados al borde superior: sin esto,
        // en equipos con edge-to-edge (Android 15+) quedan debajo de la barra de
        // notificaciones y no se alcanzan a tocar.
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootVenta), true, true);

        viewModel = new ViewModelProvider(this).get(VentaViewModel.class);

        EditText etBuscarProducto = findViewById(R.id.etBuscarProducto);
        ImageButton btnEscanearVenta = findViewById(R.id.btnEscanearVenta);
        RecyclerView rvProductosDisponibles = findViewById(R.id.rvProductosDisponibles);
        RecyclerView rvCarrito = findViewById(R.id.rvCarrito);
        tvTotal = findViewById(R.id.tvTotal);
        Button btnRegistrarVenta = findViewById(R.id.btnRegistrarVenta);

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

        btnEscanearVenta.setOnClickListener(v ->
                scannerLauncher.launch(new Intent(this, ScannerActivity.class))
        );

        productoVentaAdapter = new ProductoVentaAdapter();
        productoVentaAdapter.setOnAgregarClickListener(this::abrirDialogoCantidad);
        rvProductosDisponibles.setLayoutManager(new LinearLayoutManager(this));
        rvProductosDisponibles.setAdapter(productoVentaAdapter);

        carritoAdapter = new CarritoAdapter();
        carritoAdapter.setOnQuitarClickListener(item -> viewModel.quitarDelCarrito(item));
        rvCarrito.setLayoutManager(new LinearLayoutManager(this));
        rvCarrito.setAdapter(carritoAdapter);

        etBuscarProducto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                productoVentaAdapter.filtrar(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        viewModel.getProductosDisponibles().observe(this, productos ->
                productoVentaAdapter.setProductos(productos));

        viewModel.getCarrito().observe(this, items -> {
            carritoAdapter.setItems(items);
            actualizarTotal();
        });

        viewModel.getMensaje().observe(this, mensaje -> {
            if (mensaje != null && !mensaje.isEmpty()) {
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getVentaRegistrada().observe(this, registrada -> {
            if (registrada != null && registrada) {
                Toast.makeText(this, "Venta registrada. Inventario actualizado.", Toast.LENGTH_SHORT).show();
            }
        });

        btnRegistrarVenta.setOnClickListener(v -> viewModel.registrarVenta());
    }

    private void abrirDialogoCantidad(ProductoEntity producto) {
        CantidadVentaDialogFragment dialog = CantidadVentaDialogFragment.newInstance(producto, this);
        dialog.show(getSupportFragmentManager(), "CantidadVentaDialog");
    }

    /**
     * Busca el código escaneado en el inventario (aquí solo se vende lo que ya existe,
     * a diferencia del escaneo en Inventario que sí da de alta productos nuevos).
     * Si lo encuentra y tiene stock, abre directo el diálogo de cantidad.
     */
    private void manejarCodigoEscaneado(String codigo) {
        LiveData<ProductoEntity> busqueda = viewModel.buscarProductoPorBarcode(codigo);

        busqueda.observe(this, new Observer<ProductoEntity>() {
            @Override
            public void onChanged(ProductoEntity producto) {
                busqueda.removeObserver(this);

                if (producto == null) {
                    Toast.makeText(VentaActivity.this,
                            "Ese código no está registrado en el inventario.",
                            Toast.LENGTH_LONG).show();
                } else if (producto.cantidadDisponible <= 0) {
                    Toast.makeText(VentaActivity.this,
                            producto.nombre + " no tiene stock disponible.",
                            Toast.LENGTH_LONG).show();
                } else {
                    abrirDialogoCantidad(producto);
                }
            }
        });
    }

    private void actualizarTotal() {
        tvTotal.setText(String.format(Locale.getDefault(), "Total: $%.2f", carritoAdapter.getTotal()));
    }

    @Override
    public void onCantidadConfirmada(ProductoEntity producto, double cantidad) {
        viewModel.agregarAlCarrito(producto, cantidad);
    }

    @Override
    public double calcularCantidadPorImporte(ProductoEntity producto, double importe) {
        return viewModel.calcularCantidadPorImporte(producto, importe);
    }
}
