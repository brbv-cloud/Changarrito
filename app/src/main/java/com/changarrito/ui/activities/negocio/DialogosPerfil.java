package com.changarrito.ui.activities.negocio;

import android.app.Activity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.changarrito.R;
import com.changarrito.utils.ClaveUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Diálogos con el formulario dialog_usuario: pedir clave, crear clave, crear trabajador. */
final class DialogosPerfil {

    interface AlConfirmar {
        /** Devuelve un mensaje de error para mostrar en el campo de clave, o null para cerrar. */
        void confirmar(String nombre, String clave, Respuesta respuesta);
    }

    interface Respuesta {
        void error(String mensaje);

        void listo();
    }

    private DialogosPerfil() {
    }

    /**
     * @param pideNombre  muestra el campo de nombre (alta de trabajador)
     * @param confirma    pide repetir la clave (claves nuevas)
     */
    static void mostrar(Activity activity, String titulo, String ayuda, boolean pideNombre,
                        boolean confirma, String textoBoton, AlConfirmar alConfirmar) {
        View v = activity.getLayoutInflater().inflate(R.layout.dialog_usuario, null);
        TextView tvAyuda = v.findViewById(R.id.tvAyuda);
        TextInputLayout tilNombre = v.findViewById(R.id.tilNombre);
        TextInputLayout tilClave = v.findViewById(R.id.tilClave);
        TextInputLayout tilConfirmar = v.findViewById(R.id.tilConfirmar);
        TextInputEditText etNombre = v.findViewById(R.id.etNombre);
        TextInputEditText etClave = v.findViewById(R.id.etClave);
        TextInputEditText etConfirmar = v.findViewById(R.id.etConfirmar);

        if (ayuda != null) {
            tvAyuda.setText(ayuda);
            tvAyuda.setVisibility(View.VISIBLE);
        }
        tilNombre.setVisibility(pideNombre ? View.VISIBLE : View.GONE);
        tilConfirmar.setVisibility(confirma ? View.VISIBLE : View.GONE);
        if (!confirma) tilClave.setHint(R.string.turno_clave_pista);

        AlertDialog dialogo = new MaterialAlertDialogBuilder(activity)
                .setTitle(titulo)
                .setView(v)
                .setPositiveButton(textoBoton, null)
                .setNegativeButton(R.string.clave_cancelar, null)
                .create();
        dialogo.setOnShowListener(d -> {
            (pideNombre ? etNombre : etClave).requestFocus();
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
                tilNombre.setError(null);
                tilClave.setError(null);
                tilConfirmar.setError(null);
                String nombre = texto(etNombre);
                String clave = texto(etClave);
                if (pideNombre && nombre.trim().length() < 2) {
                    tilNombre.setError(activity.getString(R.string.trabajador_nombre_error));
                    return;
                }
                if (!ClaveUtil.esClaveValida(clave)) {
                    tilClave.setError(activity.getString(R.string.clave_invalida));
                    return;
                }
                if (confirma && !clave.equals(texto(etConfirmar))) {
                    tilConfirmar.setError(activity.getString(R.string.clave_no_coincide));
                    return;
                }
                b.setEnabled(false);
                alConfirmar.confirmar(nombre.trim(), clave, new Respuesta() {
                    @Override
                    public void error(String mensaje) {
                        b.setEnabled(true);
                        etClave.setText("");
                        tilClave.setError(mensaje);
                    }

                    @Override
                    public void listo() {
                        dialogo.dismiss();
                    }
                });
            });
        });
        dialogo.show();
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    private static String texto(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
}
