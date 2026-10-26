package com.changarrito.ui.secciones;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.changarrito.viewmodel.VentaViewModel;

import java.util.Locale;

/**
 * Vender: buscar o escanear productos, armar el carrito y cobrar. Como la seccion solo
 * se esconde al cambiar de pestana, el carrito sigue ahi al regresar.
 */
public class VenderFragment extends SeccionFragment implements CantidadVentaDialogFragment.Listener {

    private VentaViewModel viewModel;
    private ProductoVentaAdapter productoVentaAdapter;
    private CarritoAdapter carritoAdapter;
    private TextView tvTotal;
    private EditText etPagoCliente;
    private TextView tvCambio;
    private ActivityResultLauncher<Intent> scannerLauncher;

    public VenderFragment() {
        super(R.layout.fragment_vender);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(VentaViewModel.class);
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

        EditText etBuscarProducto = vista(R.id.etBuscarProducto);
        RecyclerView rvProductosDisponibles = vista(R.id.rvProductosDisponibles);
        RecyclerView rvCarrito = vista(R.id.rvCarrito);
        tvTotal = vista(R.id.tvTotal);
        etPagoCliente = vista(R.id.etPagoCliente);
        tvCambio = vista(R.id.tvCambio);

        vista(R.id.btnEscanearVenta).setOnClickListener(v ->
                scannerLauncher.launch(new Intent(requireContext(), ScannerActivity.class)));

        productoVentaAdapter = new ProductoVentaAdapter();
        productoVentaAdapter.setOnAgregarClickListener(this::abrirDialogoCantidad);
        rvProductosDisponibles.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvProductosDisponibles.setAdapter(productoVentaAdapter);

        carritoAdapter = new CarritoAdapter();
        carritoAdapter.setOnQuitarClickListener(item -> viewModel.quitarDelCarrito(item));
        rvCarrito.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCarrito.setAdapter(carritoAdapter);

        etBuscarProducto.addTextChangedListener(new Vigilante() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                productoVentaAdapter.filtrar(s.toString());
            }
        });
        etPagoCliente.addTextChangedListener(new Vigilante() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                actualizarCambio();
            }
        });

        viewModel.getProductosDisponibles().observe(getViewLifecycleOwner(), productos ->
                productoVentaAdapter.setProductos(productos));
        viewModel.getCarrito().observe(getViewLifecycleOwner(), items -> {
            carritoAdapter.setItems(items);
            actualizarTotal();
            actualizarCambio();
        });
        viewModel.getMensaje().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje != null && !mensaje.isEmpty()) {
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.getVentaRegistrada().observe(getViewLifecycleOwner(), registrada -> {
            if (registrada != null && registrada) {
                Toast.makeText(requireContext(), "Venta registrada. Inventario actualizado.", Toast.LENGTH_SHORT).show();
                etPagoCliente.setText("");
            }
        });

        vista(R.id.btnRegistrarVenta).setOnClickListener(v -> viewModel.registrarVenta());
    }

    private void abrirDialogoCantidad(ProductoEntity producto) {
        CantidadVentaDialogFragment.newInstance(producto, this)
                .show(getChildFragmentManager(), "CantidadVentaDialog");
    }

    /**
     * Busca el codigo escaneado en el inventario (aqui solo se vende lo que ya existe,
     * a diferencia del escaneo en Inventario que si da de alta productos nuevos).
     * Si lo encuentra y tiene stock, abre directo el dialogo de cantidad.
     */
    private void manejarCodigoEscaneado(String codigo) {
        LiveData<ProductoEntity> busqueda = viewModel.buscarProductoPorBarcode(codigo);
        busqueda.observe(this, new Observer<ProductoEntity>() {
            @Override
            public void onChanged(ProductoEntity producto) {
                busqueda.removeObserver(this);
                if (producto == null) {
                    Toast.makeText(requireContext(), "Ese código no está registrado en el inventario.",
                            Toast.LENGTH_LONG).show();
                } else if (producto.cantidadDisponible <= 0) {
                    Toast.makeText(requireContext(), producto.nombre + " no tiene stock disponible.",
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

    /**
     * Con cuanto paga el cliente vs. el total del carrito: si alcanza, muestra el
     * cambio a devolver; si no alcanza (o el campo esta vacio), avisa cuanto falta
     * en vez de mostrar un "cambio" negativo.
     */
    private void actualizarCambio() {
        double total = carritoAdapter.getTotal();
        String texto = etPagoCliente.getText().toString().trim();

        if (total <= 0) {
            tvCambio.setText("Cambio: $0.00");
            tvCambio.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_verde));
            return;
        }

        double pago;
        try {
            pago = texto.isEmpty() ? 0 : Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            tvCambio.setText("Cantidad invalida");
            tvCambio.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_rojo));
            return;
        }

        double diferencia = pago - total;
        if (texto.isEmpty() || diferencia < 0) {
            tvCambio.setText(String.format(Locale.getDefault(), "Falta: $%.2f", Math.abs(diferencia)));
            tvCambio.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_rojo));
        } else {
            tvCambio.setText(String.format(Locale.getDefault(), "Cambio: $%.2f", diferencia));
            tvCambio.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_verde));
        }
    }

    @Override
    public void onCantidadConfirmada(ProductoEntity producto, double cantidad) {
        viewModel.agregarAlCarrito(producto, cantidad);
    }

    @Override
    public double calcularCantidadPorImporte(ProductoEntity producto, double importe) {
        return viewModel.calcularCantidadPorImporte(producto, importe);
    }

    /** TextWatcher con solo el metodo que interesa. */
    private abstract static class Vigilante implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
