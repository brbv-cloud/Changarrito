package com.changarrito.ui.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.changarrito.R;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;

import java.util.Locale;

/**
 * Captura cuánto se vende de un producto. Para granel (KG/GR/M) permite además
 * capturar un importe fijo ("el cliente trae $50") y calcular la cantidad que le
 * corresponde, en vez de forzar al usuario a hacer la división a mano.
 */
public class CantidadVentaDialogFragment extends DialogFragment {

    public interface Listener {
        void onCantidadConfirmada(ProductoEntity producto, double cantidad);

        /** Se llama para pedir el cálculo cantidad = importe / precio. */
        double calcularCantidadPorImporte(ProductoEntity producto, double importe);
    }

    private ProductoEntity producto;
    private Listener listener;

    private EditText etCantidad;
    private EditText etImporte;
    private TextView tvVistaPrevia;

    public static CantidadVentaDialogFragment newInstance(ProductoEntity producto, Listener listener) {
        CantidadVentaDialogFragment fragment = new CantidadVentaDialogFragment();
        fragment.producto = producto;
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Si Android recrea el dialogo, se recupera a quien avisar: la seccion que lo abrio.
        if (listener == null && getParentFragment() instanceof Listener) {
            listener = (Listener) getParentFragment();
        } else if (listener == null && getActivity() instanceof Listener) {
            listener = (Listener) getActivity();
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_cantidad_venta, null);

        TextView tvProductoInfo = view.findViewById(R.id.tvProductoInfo);
        etCantidad = view.findViewById(R.id.etCantidad);
        LinearLayout layoutImporte = view.findViewById(R.id.layoutImporte);
        etImporte = view.findViewById(R.id.etImporte);
        tvVistaPrevia = view.findViewById(R.id.tvVistaPrevia);

        boolean esGranel = producto.unidadMedida != null && producto.unidadMedida != UnidadMedida.PZA;
        layoutImporte.setVisibility(esGranel ? View.VISIBLE : View.GONE);
        etCantidad.setInputType(esGranel
                ? android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                : android.text.InputType.TYPE_CLASS_NUMBER);

        tvProductoInfo.setText(String.format(Locale.getDefault(),
                "%s\n$%.2f por %s — disponible: %s",
                producto.nombre, producto.precio,
                producto.unidadMedida != null ? producto.unidadMedida.name() : "",
                formatearCantidad(producto.cantidadDisponible, producto.unidadMedida)));

        view.findViewById(R.id.btnCalcularPorImporte).setOnClickListener(v -> {
            String textoImporte = etImporte.getText().toString().trim();
            if (textoImporte.isEmpty()) return;
            try {
                double importe = Double.parseDouble(textoImporte.replace(',', '.'));
                double cantidad = listener.calcularCantidadPorImporte(producto, importe);
                etCantidad.setText(String.format(Locale.getDefault(), "%.3f", cantidad));
            } catch (NumberFormatException ignored) {
                // importe mal escrito: no truena, solo no calcula nada
            }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();

        // El teclado sale solo y el dialogo se acomoda encima de el, sin taparlo.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        etCantidad.requestFocus();

        etCantidad.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                actualizarVistaPrevia();
            }
        });
        actualizarVistaPrevia();

        view.findViewById(R.id.btnAgregarAlCarrito).setOnClickListener(v -> confirmar(dialog));

        // "Realiz." / "Listo" del teclado = Agregar al carrito.
        etCantidad.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                confirmar(dialog);
                return true;
            }
            return false;
        });

        view.findViewById(R.id.btnCancelar).setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    private void confirmar(AlertDialog dialog) {
        String texto = etCantidad.getText().toString().trim();
        if (texto.isEmpty()) {
            etCantidad.setError("Captura una cantidad");
            return;
        }
        Double cantidad = leerNumero(texto);
        if (cantidad == null) {
            etCantidad.setError("Cantidad inválida");
            return;
        }
        listener.onCantidadConfirmada(producto, cantidad);
        dialog.dismiss();
    }

    /** Muestra en grande "2 PZA × $1300.00 = $2600.00" mientras se escribe. */
    private void actualizarVistaPrevia() {
        Double cantidad = leerNumero(etCantidad.getText().toString().trim());
        if (cantidad == null || cantidad <= 0) {
            tvVistaPrevia.setText(String.format(Locale.getDefault(), "$%.2f por %s",
                    producto.precio, producto.unidadMedida != null ? producto.unidadMedida.name() : ""));
            tvVistaPrevia.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_slate_medium));
            return;
        }
        if (cantidad > producto.cantidadDisponible + 0.0001) {
            tvVistaPrevia.setText("Solo hay " + formatearCantidad(producto.cantidadDisponible, producto.unidadMedida));
            tvVistaPrevia.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_rojo));
            return;
        }
        tvVistaPrevia.setText(String.format(Locale.getDefault(), "%s × $%.2f = $%.2f",
                formatearCantidad(cantidad, producto.unidadMedida), producto.precio, cantidad * producto.precio));
        tvVistaPrevia.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_primary_variant));
    }

    /** Acepta punto o coma decimal (el teclado de Samsung trae los dos). null si no es numero. */
    private static Double leerNumero(String texto) {
        if (texto == null || texto.isEmpty()) return null;
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Al cerrar el dialogo se esconde el teclado, para que no se quede abierto en Venta. */
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        android.content.Context ctx = getContext();
        InputMethodManager imm = ctx != null
                ? (InputMethodManager) ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                : null;
        if (imm != null) {
            if (etCantidad != null) imm.hideSoftInputFromWindow(etCantidad.getWindowToken(), 0);
            if (getActivity() != null) {
                imm.hideSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0);
            }
        }
        super.onDismiss(dialog);
    }

    private String formatearCantidad(double cantidad, UnidadMedida unidad) {
        String u = unidad != null ? unidad.name() : "";
        if (cantidad == Math.floor(cantidad)) {
            return String.format(Locale.getDefault(), "%.0f %s", cantidad, u);
        }
        String texto = String.format(Locale.getDefault(), "%.3f", cantidad)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
        return texto + " " + u;
    }
}
