package com.changarrito.ui.secciones;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
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
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.scanner.ScannerActivity;
import com.changarrito.ui.adapters.CarritoAdapter;
import com.changarrito.ui.adapters.ProductoVentaAdapter;
import com.changarrito.ui.fragments.CantidadVentaDialogFragment;
import com.changarrito.utils.Cobro;
import com.changarrito.viewmodel.VentaViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Vender. Arriba se buscan o escanean productos y se tocan para agregarlos (por pieza
 * agrega 1 directo; a granel pide la cantidad). Abajo vive la hoja del carrito:
 *  - cerrada es una barra con el total y el boton "Cobrar";
 *  - abierta muestra cada linea con − 1 + y la parte de cobro: con cuanto paga el
 *    cliente (con botones de billetes), el cambio en grande y con que darlo.
 * Como la seccion solo se esconde al cambiar de pestana, el carrito sigue ahi al regresar.
 */
public class VenderFragment extends SeccionFragment implements CantidadVentaDialogFragment.Listener {

    private VentaViewModel viewModel;
    private ProductoVentaAdapter productoVentaAdapter;
    private CarritoAdapter carritoAdapter;
    private ActivityResultLauncher<Intent> scannerLauncher;

    private BottomSheetBehavior<View> hoja;
    private TextView tvArticulos, tvTotal, tvSinProductos;
    private TextView tvCambioEtiqueta, tvCambio, tvDesglose;
    private MaterialCardView cardCambio;
    private MaterialButton btnVerCarrito, btnRegistrarVenta;
    private EditText etPagoCliente, etBuscarProducto;
    private ChipGroup chipsPago;

    private List<VentaViewModel.ItemCarrito> items = new ArrayList<>();
    private long totalCentavosChips = -1;

    /** "Atras" con el carrito abierto lo cierra en vez de salir de Vender. */
    private final OnBackPressedCallback cerrarCarrito = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (hoja != null) hoja.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    };

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
        configurarProductos();
        configurarHoja();
        configurarCobro();

        viewModel.getCarrito().observe(getViewLifecycleOwner(), lista -> {
            items = lista != null ? lista : new ArrayList<>();
            carritoAdapter.setItems(items);
            Map<Integer, Double> enCarrito = new HashMap<>();
            for (VentaViewModel.ItemCarrito i : items) enCarrito.put(i.producto.id, i.cantidad);
            productoVentaAdapter.setEnCarrito(enCarrito);
            actualizarCarrito();
        });
        viewModel.getMensaje().observe(getViewLifecycleOwner(), mensaje -> {
            if (mensaje != null && !mensaje.isEmpty()) {
                Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show();
            }
        });
        viewModel.getVentaRegistrada().observe(getViewLifecycleOwner(), registrada -> {
            if (registrada != null && registrada) {
                Toast.makeText(requireContext(), R.string.venta_registrada, Toast.LENGTH_SHORT).show();
                etPagoCliente.setText("");
                esconderTeclado();
            }
        });
    }

    // ------------------------------------------------------------------ productos

    private void configurarProductos() {
        tvSinProductos = vista(R.id.tvSinProductos);
        RecyclerView rvProductos = vista(R.id.rvProductosDisponibles);
        productoVentaAdapter = new ProductoVentaAdapter();
        productoVentaAdapter.setListener(new ProductoVentaAdapter.Listener() {
            @Override
            public void onAgregar(ProductoEntity producto) {
                agregarRapido(producto);
            }

            @Override
            public void onElegirCantidad(ProductoEntity producto) {
                abrirDialogoCantidad(producto);
            }
        });
        rvProductos.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvProductos.setAdapter(productoVentaAdapter);

        etBuscarProducto = vista(R.id.etBuscarProducto);
        View btnLimpiar = vista(R.id.btnLimpiarBusqueda);
        etBuscarProducto.addTextChangedListener(new Vigilante() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnLimpiar.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
                productoVentaAdapter.filtrar(s.toString());
                actualizarSinProductos();
            }
        });
        btnLimpiar.setOnClickListener(v -> etBuscarProducto.setText(""));
        etBuscarProducto.setOnEditorActionListener((v, accion, e) -> {
            if (accion != EditorInfo.IME_ACTION_SEARCH) return false;
            esconderTeclado();
            return true;
        });
        vista(R.id.btnEscanearVenta).setOnClickListener(v ->
                scannerLauncher.launch(new Intent(requireContext(), ScannerActivity.class)));

        viewModel.getProductosDisponibles().observe(getViewLifecycleOwner(), productos -> {
            productoVentaAdapter.setProductos(productos);
            actualizarSinProductos();
        });
    }

    private void actualizarSinProductos() {
        boolean vacio = productoVentaAdapter.getItemCount() == 0;
        tvSinProductos.setVisibility(vacio ? View.VISIBLE : View.GONE);
        if (!vacio) return;
        String busqueda = etBuscarProducto.getText().toString().trim();
        if (!productoVentaAdapter.hayProductos() || busqueda.isEmpty()) {
            tvSinProductos.setText(R.string.venta_sin_productos);
        } else {
            tvSinProductos.setText(getString(R.string.venta_sin_coincidencias, busqueda));
        }
    }

    /** Por pieza se agrega 1 de un toque (lo mas comun en la tienda); a granel hay que pesar. */
    private void agregarRapido(ProductoEntity producto) {
        if (producto.unidadMedida == null || producto.unidadMedida == UnidadMedida.PZA) {
            viewModel.agregarAlCarrito(producto, 1);
        } else {
            abrirDialogoCantidad(producto);
        }
    }

    private void abrirDialogoCantidad(ProductoEntity producto) {
        CantidadVentaDialogFragment.newInstance(producto, this)
                .show(getChildFragmentManager(), "CantidadVentaDialog");
    }

    /**
     * Busca el codigo escaneado en el inventario (aqui solo se vende lo que ya existe,
     * a diferencia del escaneo en Inventario, que si da de alta productos nuevos).
     */
    private void manejarCodigoEscaneado(String codigo) {
        LiveData<ProductoEntity> busqueda = viewModel.buscarProductoPorBarcode(codigo);
        busqueda.observe(this, new Observer<ProductoEntity>() {
            @Override
            public void onChanged(ProductoEntity producto) {
                busqueda.removeObserver(this);
                if (producto == null) {
                    Toast.makeText(requireContext(), R.string.venta_no_registrado, Toast.LENGTH_LONG).show();
                } else if (producto.cantidadDisponible <= 0) {
                    Toast.makeText(requireContext(), getString(R.string.venta_sin_stock, producto.nombre),
                            Toast.LENGTH_LONG).show();
                } else {
                    agregarRapido(producto);
                }
            }
        });
    }

    // ------------------------------------------------------------------ hoja del carrito

    private void configurarHoja() {
        View vistaHoja = vista(R.id.hojaCarrito);
        hoja = BottomSheetBehavior.from(vistaHoja);
        tvArticulos = vista(R.id.tvArticulos);
        tvTotal = vista(R.id.tvTotal);
        btnVerCarrito = vista(R.id.btnVerCarrito);

        RecyclerView rvCarrito = vista(R.id.rvCarrito);
        carritoAdapter = new CarritoAdapter();
        carritoAdapter.setListener(new CarritoAdapter.Listener() {
            @Override
            public void onMenos(VentaViewModel.ItemCarrito item) {
                viewModel.cambiarCantidad(item.producto.id, item.cantidad - 1);
            }

            @Override
            public void onMas(VentaViewModel.ItemCarrito item) {
                viewModel.cambiarCantidad(item.producto.id, item.cantidad + 1);
            }

            @Override
            public void onCambiarCantidad(VentaViewModel.ItemCarrito item) {
                CantidadVentaDialogFragment.paraCambiar(item.producto, item.cantidad, VenderFragment.this)
                        .show(getChildFragmentManager(), "CantidadVentaDialog");
            }

            @Override
            public void onQuitar(VentaViewModel.ItemCarrito item) {
                viewModel.quitarDelCarrito(item);
            }
        });
        rvCarrito.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCarrito.setAdapter(carritoAdapter);

        View.OnClickListener alternar = v -> hoja.setState(
                hoja.getState() == BottomSheetBehavior.STATE_EXPANDED
                        ? BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_EXPANDED);
        vista(R.id.barraCarrito).setOnClickListener(alternar);
        btnVerCarrito.setOnClickListener(alternar);

        hoja.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int estado) {
                actualizarEstadoHoja();
                if (estado == BottomSheetBehavior.STATE_COLLAPSED) esconderTeclado();
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float desplazamiento) {
            }
        });
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), cerrarCarrito);
        // Con el carrito vacio la hoja arranca escondida (sin parpadear la barra).
        List<VentaViewModel.ItemCarrito> actuales = viewModel.getCarrito().getValue();
        if (actuales == null || actuales.isEmpty()) hoja.setState(BottomSheetBehavior.STATE_HIDDEN);
        actualizarEstadoHoja();
    }

    private void actualizarEstadoHoja() {
        if (hoja == null || btnVerCarrito == null) return;
        boolean abierta = hoja.getState() == BottomSheetBehavior.STATE_EXPANDED;
        btnVerCarrito.setText(abierta ? R.string.venta_seguir : R.string.venta_ver_cobrar);
        cerrarCarrito.setEnabled(abierta && !isHidden());
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        actualizarEstadoHoja();
    }

    /** Barra del carrito: aparece con el primer producto y se va cuando queda vacio. */
    private void actualizarCarrito() {
        double total = total();
        tvTotal.setText(CalculoGanancias.dinero(total));
        tvArticulos.setText(getResources().getQuantityString(R.plurals.venta_productos_carrito, items.size(), items.size()));
        btnRegistrarVenta.setText(getString(R.string.venta_cobrar_total, CalculoGanancias.dinero(total)));

        if (items.isEmpty()) {
            hoja.setHideable(true);
            hoja.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            // Al dejar de poder esconderse, si estaba escondida sube sola a la barra.
            hoja.setHideable(false);
        }
        actualizarBilletes(total);
        actualizarCambio();
    }

    private double total() {
        long centavos = 0;
        for (VentaViewModel.ItemCarrito i : items) centavos += Cobro.aCentavos(i.getSubtotal());
        return centavos / 100.0;
    }

    // ------------------------------------------------------------------ cobro

    private void configurarCobro() {
        totalCentavosChips = -1; // vista nueva: los botones de billetes se vuelven a crear
        etPagoCliente = vista(R.id.etPagoCliente);
        chipsPago = vista(R.id.chipsPago);
        cardCambio = vista(R.id.cardCambio);
        tvCambioEtiqueta = vista(R.id.tvCambioEtiqueta);
        tvCambio = vista(R.id.tvCambio);
        tvDesglose = vista(R.id.tvDesglose);
        btnRegistrarVenta = vista(R.id.btnRegistrarVenta);
        View btnBorrarPago = vista(R.id.btnBorrarPago);

        etPagoCliente.addTextChangedListener(new Vigilante() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnBorrarPago.setVisibility(s.length() == 0 ? View.GONE : View.VISIBLE);
                actualizarCambio();
            }
        });
        etPagoCliente.setOnEditorActionListener((v, accion, e) -> {
            if (accion != EditorInfo.IME_ACTION_DONE) return false;
            esconderTeclado();
            return true;
        });
        btnBorrarPago.setOnClickListener(v -> etPagoCliente.setText(""));
        btnRegistrarVenta.setOnClickListener(v -> viewModel.registrarVenta());
    }

    /** Botones rapidos: "Exacto" y los billetes con los que probablemente paguen. */
    private void actualizarBilletes(double total) {
        long centavos = Cobro.aCentavos(total);
        if (centavos == totalCentavosChips) return; // mismo total: no se rehacen
        totalCentavosChips = centavos;
        chipsPago.removeAllViews();
        if (centavos <= 0) return;
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        agregarBillete(inflater, getString(R.string.venta_exacto), total);
        for (double monto : Cobro.sugerenciasPago(total)) {
            agregarBillete(inflater, CalculoGanancias.dinero(monto).replace(".00", ""), monto);
        }
    }

    private void agregarBillete(LayoutInflater inflater, String texto, double monto) {
        Chip chip = (Chip) inflater.inflate(R.layout.chip_billete, chipsPago, false);
        chip.setText(texto);
        chip.setOnClickListener(v -> {
            String valor = Math.abs(monto - Math.rint(monto)) < 0.005
                    ? String.format(Locale.US, "%.0f", monto) : String.format(Locale.US, "%.2f", monto);
            etPagoCliente.setText(valor);
            etPagoCliente.setSelection(valor.length());
            esconderTeclado();
        });
        chipsPago.addView(chip);
    }

    /**
     * La tarjeta del cambio:
     *  - sin pago escrito: gris, con la pista de que escribir;
     *  - no alcanza: roja, "Faltan $X" (y no deja cobrar);
     *  - alcanza: verde, el cambio en grande y con que billetes y monedas darlo.
     */
    private void actualizarCambio() {
        if (etPagoCliente == null) return;
        long total = Cobro.aCentavos(total());
        String texto = etPagoCliente.getText().toString().trim().replace(',', '.');

        if (texto.isEmpty()) {
            pintarCambio(R.color.chip_gris_fondo, R.color.chip_gris_texto,
                    getString(R.string.venta_cambio), "—", getString(R.string.venta_pista_pago));
            btnRegistrarVenta.setEnabled(total > 0);
            return;
        }
        long pago;
        try {
            pago = Cobro.aCentavos(Double.parseDouble(texto));
        } catch (NumberFormatException e) {
            pintarCambio(R.color.semaforo_rojo_fondo, R.color.semaforo_rojo_texto,
                    getString(R.string.venta_pago_invalido), "—", "");
            btnRegistrarVenta.setEnabled(false);
            return;
        }

        long diferencia = pago - total;
        if (diferencia < 0) {
            pintarCambio(R.color.semaforo_rojo_fondo, R.color.semaforo_rojo_texto,
                    getString(R.string.venta_faltan), CalculoGanancias.dinero(-diferencia / 100.0),
                    getString(R.string.venta_no_alcanza));
            btnRegistrarVenta.setEnabled(false);
        } else if (diferencia == 0) {
            pintarCambio(R.color.semaforo_verde_fondo, R.color.semaforo_verde_texto,
                    getString(R.string.venta_cambio), CalculoGanancias.dinero(0),
                    getString(R.string.venta_pago_exacto));
            btnRegistrarVenta.setEnabled(total > 0);
        } else {
            double cambio = diferencia / 100.0;
            pintarCambio(R.color.semaforo_verde_fondo, R.color.semaforo_verde_texto,
                    getString(R.string.venta_cambio_entregar), CalculoGanancias.dinero(cambio),
                    getString(R.string.venta_entrega, Cobro.textoDesglose(cambio)));
            btnRegistrarVenta.setEnabled(total > 0);
        }
    }

    private void pintarCambio(@ColorRes int fondo, @ColorRes int color, String etiqueta, String monto, String detalle) {
        Context ctx = requireContext();
        int c = ContextCompat.getColor(ctx, color);
        cardCambio.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(ctx, fondo)));
        tvCambioEtiqueta.setText(etiqueta);
        tvCambioEtiqueta.setTextColor(c);
        tvCambio.setText(monto);
        tvCambio.setTextColor(c);
        tvDesglose.setText(detalle);
        tvDesglose.setTextColor(c);
        tvDesglose.setVisibility(detalle.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void esconderTeclado() {
        View v = getView();
        if (v == null) return;
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        View foco = v.findFocus();
        if (foco != null) foco.clearFocus();
    }

    // ------------------------------------------------------------------ dialogo de cantidad

    @Override
    public void onCantidadConfirmada(ProductoEntity producto, double cantidad) {
        viewModel.agregarAlCarrito(producto, cantidad);
    }

    @Override
    public void onCantidadCambiada(ProductoEntity producto, double cantidad) {
        viewModel.cambiarCantidad(producto.id, cantidad);
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
