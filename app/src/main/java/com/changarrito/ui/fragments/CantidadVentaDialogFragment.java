package com.changarrito.ui.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.changarrito.R;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;

import java.util.Locale;

/**
 * Captura cuánto se vende de un producto. Para granel (KG/LT/GR) permite además
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

    public static CantidadVentaDialogFragment newInstance(ProductoEntity producto, Listener listener) {
        CantidadVentaDialogFragment fragment = new CantidadVentaDialogFragment();
        fragment.producto = producto;
        fragment.listener = listener;
        return fragment;
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
                double importe = Double.parseDouble(textoImporte);
                double cantidad = listener.calcularCantidadPorImporte(producto, importe);
                etCantidad.setText(String.format(Locale.getDefault(), "%.3f", cantidad));
            } catch (NumberFormatException ignored) {
                // importe mal escrito: no truena, solo no calcula nada
            }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();

        view.findViewById(R.id.btnAgregarAlCarrito).setOnClickListener(v -> {
            String texto = etCantidad.getText().toString().trim();
            if (texto.isEmpty()) {
                etCantidad.setError("Captura una cantidad");
                return;
            }
            try {
                double cantidad = Double.parseDouble(texto);
                listener.onCantidadConfirmada(producto, cantidad);
                dialog.dismiss();
            } catch (NumberFormatException e) {
                etCantidad.setError("Cantidad inválida");
            }
        });

        view.findViewById(R.id.btnCancelar).setOnClickListener(v -> dialog.dismiss());

        return dialog;
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
