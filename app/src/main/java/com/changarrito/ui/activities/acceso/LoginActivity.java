package com.changarrito.ui.activities.acceso;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.changarrito.R;
import com.changarrito.auth.AuthRepository;
import com.changarrito.auth.GoogleSignInHelper;
import com.changarrito.auth.ValidadorAcceso;
import com.changarrito.utils.InsetsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Pantalla de arranque de la app. Si ya hay sesión guardada en el celular, ni se
 * muestra: manda directo al destino (funciona sin internet).
 */
public class LoginActivity extends BaseAccesoActivity {

    public static final String EXTRA_CORREO = "correo";

    private AuthRepository auth;
    private GoogleSignInHelper google;

    private TextInputLayout tilCorreo, tilContrasena;
    private TextInputEditText etCorreo, etContrasena;
    private TextView tvError;
    private MaterialButton btnIniciarSesion, btnGoogle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = new AuthRepository();

        if (auth.getUsuarioActual() != null) {
            NavegacionAcceso.irAlDestino(this, auth);
            return;
        }

        setContentView(R.layout.activity_login);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootLogin), true, true);
        google = new GoogleSignInHelper(this);

        tilCorreo = findViewById(R.id.tilCorreo);
        tilContrasena = findViewById(R.id.tilContrasena);
        etCorreo = findViewById(R.id.etCorreo);
        etContrasena = findViewById(R.id.etContrasena);
        tvError = findViewById(R.id.tvError);
        btnIniciarSesion = findViewById(R.id.btnIniciarSesion);
        btnGoogle = findViewById(R.id.btnGoogle);

        limpiarAlEscribir(tilCorreo, etCorreo);
        limpiarAlEscribir(tilContrasena, etContrasena);

        String correoPrevio = getIntent().getStringExtra(EXTRA_CORREO);
        if (correoPrevio != null) etCorreo.setText(correoPrevio);

        btnIniciarSesion.setOnClickListener(v -> iniciarSesion());
        btnGoogle.setOnClickListener(v -> entrarConGoogle());

        findViewById(R.id.tvOlvidaste).setOnClickListener(v -> {
            Intent i = new Intent(this, RecuperarContrasenaActivity.class);
            i.putExtra(EXTRA_CORREO, texto(etCorreo).trim());
            startActivity(i);
        });
        findViewById(R.id.tvRegistrate).setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));
    }

    private void iniciarSesion() {
        mostrarError(tvError, null);
        String correo = texto(etCorreo);
        String contrasena = texto(etContrasena);
        boolean ok = marcar(tilCorreo, ValidadorAcceso.validarCorreo(correo));
        ok &= marcar(tilContrasena, ValidadorAcceso.validarContrasenaLogin(contrasena));
        if (!ok) return;

        ocupado(btnIniciarSesion, true, R.string.acceso_iniciar_sesion, R.string.acceso_entrando);
        auth.iniciarSesion(correo, contrasena, new AuthRepository.Callback() {
            @Override
            public void onExito() {
                NavegacionAcceso.irAlDestino(LoginActivity.this, auth);
            }

            @Override
            public void onError(String mensaje) {
                ocupado(btnIniciarSesion, false, R.string.acceso_iniciar_sesion, R.string.acceso_entrando);
                mostrarError(tvError, mensaje);
            }
        });
    }

    private void entrarConGoogle() {
        mostrarError(tvError, null);
        btnGoogle.setEnabled(false);
        google.iniciar(new GoogleSignInHelper.Callback() {
            @Override
            public void onIdToken(String idToken) {
                auth.entrarConGoogle(idToken, new AuthRepository.Callback() {
                    @Override
                    public void onExito() {
                        NavegacionAcceso.irAlDestino(LoginActivity.this, auth);
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
}
