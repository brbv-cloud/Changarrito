package com.changarrito.ui.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.changarrito.R;
import com.changarrito.database.entity.ProductoEntity;

public class ProductoDialogFragment extends DialogFragment {

    private EditText etNombre, etPrecio, etCantidad, etUmbral, etTelefono;
    private ProductoDialogListener listener;
    private ProductoEntity productoEditando;

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

        android.view.View view = requireActivity().getLayoutInflater().inflate(R.layout.dialog_producto, null);

        etNombre = view.findViewById(R.id.etNombre);
        etPrecio = view.findViewById(R.id.etPrecio);
        etCantidad = view.findViewById(R.id.etCantidad);
        etUmbral = view.findViewById(R.id.etUmbral);
        etTelefono = view.findViewById(R.id.etTelefono);

        // Si es edición, llenar campos
        if (productoEditando != null) {
            etNombre.setText(productoEditando.nombre);
            etPrecio.setText(String.valueOf(productoEditando.precio));
            etCantidad.setText(String.valueOf(productoEditando.cantidadDisponible));
            etUmbral.setText(String.valueOf(productoEditando.umbralStockBajo));
            etTelefono.setText(productoEditando.telefonoProveedor);
        }

        builder.setView(view);

        AlertDialog dialog = builder.create();

        // Botón Guardar
        view.findViewById(R.id.btnGuardar).setOnClickListener(v -> {
            if (validar()) {
                ProductoEntity producto = new ProductoEntity();
                if (productoEditando != null) {
                    producto.id = productoEditando.id;
                }
                producto.nombre = etNombre.getText().toString();
                producto.precio = Double.parseDouble(etPrecio.getText().toString());
                producto.cantidadDisponible = Integer.parseInt(etCantidad.getText().toString());
                producto.umbralStockBajo = Integer.parseInt(etUmbral.getText().toString());
                producto.telefonoProveedor = etTelefono.getText().toString();
                producto.timestampCreacionMs = System.currentTimeMillis();
                producto.timestampActualizacionMs = System.currentTimeMillis();

                listener.onProductoGuardado(producto);
                dialog.dismiss();
            }
        });

        // Botón Cancelar
        view.findViewById(R.id.btnCancelar).setOnClickListener(v -> dialog.dismiss());

        return dialog;
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
        return true;
    }
}