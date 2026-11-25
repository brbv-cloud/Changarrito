package com.changarrito.ui.activities.acceso;

import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Utilidades compartidas por las pantallas de acceso. */
abstract class BaseAccesoActivity extends AppCompatActivity {

    protected static String texto(TextInputEditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }

    /** Muestra el error bajo el campo (o lo quita si es null). Devuelve true si es válido. */
    protected static boolean marcar(TextInputLayout til, String error) {
        til.setError(error);
        til.setErrorEnabled(error != null);
        return error == null;
    }

    /** Quita el error del campo en cuanto el usuario vuelve a escribir. */
    protected static void limpiarAlEscribir(TextInputLayout til, TextInputEditText campo) {
        campo.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (til.getError() != null) marcar(til, null);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });
    }

    protected static void mostrarError(TextView tvError, String mensaje) {
        if (mensaje == null) {
            tvError.setVisibility(View.GONE);
        } else {
            tvError.setText(mensaje);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    /** Desactiva el botón mientras se espera a Firebase, para que no se mande dos veces. */
    protected static void ocupado(MaterialButton boton, boolean ocupado, int textoNormal, int textoOcupado) {
        boton.setEnabled(!ocupado);
        boton.setText(ocupado ? textoOcupado : textoNormal);
    }
}
