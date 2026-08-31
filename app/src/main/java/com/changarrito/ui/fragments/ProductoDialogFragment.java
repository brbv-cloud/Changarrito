package com.changarrito.ui.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.changarrito.R;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProductoDialogFragment extends DialogFragment {

    private EditText etNombre, etPrecio, etCantidad, etUmbral, etTelefono, etBarcode;
    private Spinner spUnidadMedida;
    private TextView tvFechaCaducidad;
    private ProductoDialogListener listener;
    private ProductoEntity productoEditando;
    private long fechaCaducidadMs = 0;

    public interface ProductoDialogListener {
        void onProductoGuardado(ProductoEntity producto);
    }

    public ProductoDialogFragment() {}

    public static ProductoDialogFragment newInstance(ProductoEntity producto, ProductoDialogListener listener) {
        ProductoDialogFragment fragment = new ProductoDialogFragment();
        fragment.productoEditando = producto;
        fragment.listener = listener;
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_producto, null);

        etNombre = view.findViewById(R.id.etNombre);
        etPrecio = view.findViewById(R.id.etPrecio);
        etCantidad = view.findViewById(R.id.etCantidad);
        etUmbral = view.findViewById(R.id.etUmbral);
        etTelefono = view.findViewById(R.id.etTelefono);
        etBarcode = view.findViewById(R.id.etBarcode);
        spUnidadMedida = view.findViewById(R.id.spUnidadMedida);
        tvFechaCaducidad = view.findViewById(R.id.tvFechaCaducidad);

        ArrayAdapter<UnidadMedida> unidadAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                UnidadMedida.values()
        );
        unidadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnidadMedida.setAdapter(unidadAdapter);

        // Por defecto PZA: casi todo lo que trae código de barras se vende por pieza.
        // El granel (KG/LT/GR) rara vez viene empaquetado con código.
        spUnidadMedida.setSelection(unidadAdapter.getPosition(UnidadMedida.PZA));

        tvFechaCaducidad.setOnClickListener(v -> mostrarDatePicker());

        if (productoEditando != null) {
            if (productoEditando.nombre != null) {
                etNombre.setText(productoEditando.nombre);
            }
            if (productoEditando.barcode != null) {
                etBarcode.setText(productoEditando.barcode);
            }

            boolean esEdicionReal = productoEditando.id != 0;

            if (esEdicionReal) {
                etPrecio.setText(String.valueOf(productoEditando.precio));
                etCantidad.setText(String.valueOf(productoEditando.cantidadDisponible));
                etUmbral.setText(String.valueOf(productoEditando.umbralStockBajo));
                etTelefono.setText(productoEditando.telefonoProveedor);

                if (productoEditando.unidadMedida != null) {
                    spUnidadMedida.setSelection(unidadAdapter.getPosition(productoEditando.unidadMedida));
                }

                fechaCaducidadMs = productoEditando.fechaCaducidadMs;
                actualizarTextoFecha();
            }
        }

        builder.setView(view);
        AlertDialog dialog = builder.create();

        view.findViewById(R.id.btnGuardar).setOnClickListener(v -> {
            if (!validar()) return;

            ProductoEntity producto = new ProductoEntity();
            if (productoEditando != null && productoEditando.id != 0) {
                producto.id = productoEditando.id;
                producto.timestampCreacionMs = productoEditando.timestampCreacionMs;
            }

            producto.nombre = etNombre.getText().toString().trim();
            producto.precio = parseDouble(etPrecio);
            producto.cantidadDisponible = parseDouble(etCantidad);
            producto.umbralStockBajo = parseDouble(etUmbral);
            producto.unidadMedida = (UnidadMedida) spUnidadMedida.getSelectedItem();
            producto.telefonoProveedor = etTelefono.getText().toString().trim();
            producto.barcode = etBarcode.getText().toString().trim();
            producto.fechaCaducidadMs = fechaCaducidadMs;

            listener.onProductoGuardado(producto);
            dialog.dismiss();
        });

        view.findViewById(R.id.btnCancelar).setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    private void mostrarDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (fechaCaducidadMs > 0) {
            cal.setTimeInMillis(fechaCaducidadMs);
        }

        DatePickerDialog picker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar seleccion = Calendar.getInstance();
                    seleccion.set(year, month, dayOfMonth, 0, 0, 0);
                    seleccion.set(Calendar.MILLISECOND, 0);
                    fechaCaducidadMs = seleccion.getTimeInMillis();
                    actualizarTextoFecha();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        picker.show();
    }

    private void actualizarTextoFecha() {
        if (fechaCaducidadMs > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            tvFechaCaducidad.setText(sdf.format(new Date(fechaCaducidadMs)));
        } else {
            tvFechaCaducidad.setText("Sin fecha de caducidad (tocar para elegir)");
        }
    }

    private double parseDouble(EditText campo) {
        String texto = campo.getText().toString().trim();
        if (texto.isEmpty()) return 0;
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean validar() {
        if (etNombre.getText().toString().trim().isEmpty()) {
            etNombre.setError("Nombre requerido");
            return false;
        }
        if (etPrecio.getText().toString().trim().isEmpty()) {
            etPrecio.setError("Precio requerido");
            return false;
        }
        if (etCantidad.getText().toString().trim().isEmpty()) {
            etCantidad.setError("Cantidad requerida");
            return false;
        }

        UnidadMedida unidad = (UnidadMedida) spUnidadMedida.getSelectedItem();
        double cantidad = parseDouble(etCantidad);
        if (unidad == UnidadMedida.PZA && cantidad != Math.floor(cantidad)) {
            etCantidad.setError("PZA no acepta decimales");
            return false;
        }
        return true;
    }
}