package com.changarrito.ui.activities.acceso;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.changarrito.R;
import com.changarrito.auth.AuthRepository;
import com.changarrito.auth.GoogleSignInHelper;
import com.changarrito.auth.ValidadorAcceso;
import com.changarrito.utils.InsetsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegistroActivity extends BaseAccesoActivity {

    private AuthRepository auth;
    private GoogleSignInHelper google;

    private TextInputLayout tilNombre, tilCorreo, tilContrasena, tilConfirmar;
    private TextInputEditText etNombre, etCorreo, etContrasena, etConfirmar;
    private MaterialCheckBox cbTerminos;
    private TextView tvError;
    private MaterialButton btnCrearCuenta, btnGoogle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootRegistro), true, true);
        auth = new AuthRepository();
        google = new GoogleSignInHelper(this);

        tilNombre = findViewById(R.id.tilNombre);
        tilCorreo = findViewById(R.id.tilCorreo);
        tilContrasena = findViewById(R.id.tilContrasena);
        tilConfirmar = findViewById(R.id.tilConfirmar);
        etNombre = findViewById(R.id.etNombre);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        etConfirmar = findViewById(R.id.etConfirmar);
        cbTerminos = findViewById(R.id.cbTerminos);
        tvError = findViewById(R.id.tvError);
        btnCrearCuenta = findViewById(R.id.btnCrearCuenta);
        btnGoogle = findViewById(R.id.btnGoogle);

        limpiarAlEscribir(tilNombre, etNombre);
        limpiarAlEscribir(tilCorreo, etCorreo);
        limpiarAlEscribir(tilContrasena, etContrasena);
        limpiarAlEscribir(tilConfirmar, etConfirmar);
        prepararEnlacesLegales(findViewById(R.id.tvTerminos));

        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        findViewById(R.id.tvIniciaSesion).setOnClickListener(v -> finish());
        btnCrearCuenta.setOnClickListener(v -> crearCuenta());
        btnGoogle.setOnClickListener(v -> registrarseConGoogle());
    }

    private void crearCuenta() {
        mostrarError(tvError, null);
        String nombre = texto(etNombre);
        String correo = texto(etCorreo);
        String contrasena = texto(etContrasena);

        boolean ok = marcar(tilNombre, ValidadorAcceso.validarNombre(nombre));
        ok &= marcar(tilCorreo, ValidadorAcceso.validarCorreo(correo));
        ok &= marcar(tilContrasena, ValidadorAcceso.validarContrasenaNueva(contrasena));
        ok &= marcar(tilConfirmar, ValidadorAcceso.validarConfirmacion(contrasena, texto(etConfirmar)));
        if (!ok) return;
        if (!cbTerminos.isChecked()) {
            mostrarError(tvError, "Acepta los términos y el aviso de privacidad para continuar.");
            return;
        }

        ocupado(btnCrearCuenta, true, R.string.registro_crear, R.string.registro_creando);
        auth.registrar(nombre, correo, contrasena, new AuthRepository.Callback() {
            @Override
            public void onExito() {
                NavegacionAcceso.irAlDestino(RegistroActivity.this, auth);
            }

            @Override
            public void onError(String mensaje) {
                ocupado(btnCrearCuenta, false, R.string.registro_crear, R.string.registro_creando);
                mostrarError(tvError, mensaje);
            }
        });
    }

    /**
     * Con Google la cuenta se crea sola la primera vez. Aun así pedimos aceptar el
     * aviso de privacidad, porque igual guardamos su nombre y correo.
     */
    private void registrarseConGoogle() {
        mostrarError(tvError, null);
        if (!cbTerminos.isChecked()) {
            mostrarError(tvError, "Acepta los términos y el aviso de privacidad para continuar.");
            return;
        }
        btnGoogle.setEnabled(false);
        google.iniciar(new GoogleSignInHelper.Callback() {
            @Override
            public void onIdToken(String idToken) {
                auth.entrarConGoogle(idToken, new AuthRepository.Callback() {
                    @Override
                    public void onExito() {
                        NavegacionAcceso.irAlDestino(RegistroActivity.this, auth);
                    }

                    @Override
                    public void onError(String mensaje) {
                        btnGoogle.setEnabled(true);
                        mostrarError(tvError, mensaje);
                    }
                });
            }

            @Override
            public void onError(String mensaje) {
                btnGoogle.setEnabled(true);
                mostrarError(tvError, mensaje);
            }
        });
    }

    /** Convierte "términos" y "aviso de privacidad" en enlaces que abren su texto. */
    private void prepararEnlacesLegales(TextView tv) {
        // getText (no getString) conserva los <a href> del recurso como URLSpan.
        SpannableString texto = new SpannableString(getText(R.string.registro_acepto_terminos));
        for (URLSpan url : texto.getSpans(0, texto.length(), URLSpan.class)) {
            int inicio = texto.getSpanStart(url);
            int fin = texto.getSpanEnd(url);
            boolean esPrivacidad = "privacidad".equals(url.getURL());
            texto.removeSpan(url);
            texto.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    mostrarTextoLegal(esPrivacidad);
                }
            }, inicio, fin, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tv.setText(texto);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void mostrarTextoLegal(boolean privacidad) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(privacidad ? R.string.legal_privacidad_titulo : R.string.legal_terminos_titulo)
                .setMessage(privacidad ? R.string.legal_privacidad_texto : R.string.legal_terminos_texto)
                .setPositiveButton(R.string.legal_entendido, null)
                .show();
    }
}
