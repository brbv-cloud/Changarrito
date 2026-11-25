package com.changarrito.ui.activities.acceso;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;
import android.widget.Toast;

import com.changarrito.R;
import com.changarrito.auth.AuthRepository;
import com.changarrito.utils.InsetsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

/**
 * Pantalla para quien se registró con correo y todavía no abre el enlace de
 * verificación. No venía en el diseño: sigue su mismo estilo.
 */
public class VerificarCorreoActivity extends BaseAccesoActivity {

    /** Firebase limita los reenvíos; esperamos un minuto entre uno y otro. */
    private static final long ESPERA_REENVIO_MS = 60_000;

    private AuthRepository auth;
    private TextView tvError;
    private MaterialButton btnYaVerifique, btnReenviar;
    private CountDownTimer cuentaRegresiva;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = new AuthRepository();
        FirebaseUser usuario = auth.getUsuarioActual();
        if (usuario == null) {
            NavegacionAcceso.irAlDestino(this, auth);
            return;
        }

        setContentView(R.layout.activity_verificar_correo);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootVerificar), true, true);

        TextView tvSubtitulo = findViewById(R.id.tvSubtitulo);
        tvSubtitulo.setText(getString(R.string.verificar_subtitulo, usuario.getEmail()));
        tvError = findViewById(R.id.tvError);
        btnYaVerifique = findViewById(R.id.btnYaVerifique);
        btnReenviar = findViewById(R.id.btnReenviar);

        btnYaVerifique.setOnClickListener(v -> comprobar());
        btnReenviar.setOnClickListener(v -> reenviar());
        findViewById(R.id.tvOtraCuenta).setOnClickListener(v -> {
            String correo = usuario.getEmail();
            auth.cerrarSesion();
            Intent i = new Intent(this, LoginActivity.class);
            i.putExtra(LoginActivity.EXTRA_CORREO, correo);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });

        // Recién registrado: el correo acaba de salir, así que arrancamos esperando.
        iniciarEspera();
    }

    private void comprobar() {
        mostrarError(tvError, null);
        ocupado(btnYaVerifique, true, R.string.verificar_ya_verifique, R.string.verificar_comprobando);
        auth.comprobarVerificacion(new AuthRepository.Callback() {
            @Override
            public void onExito() {
                NavegacionAcceso.irAlDestino(VerificarCorreoActivity.this, auth);
            }

            @Override
            public void onError(String mensaje) {
                ocupado(btnYaVerifique, false, R.string.verificar_ya_verifique, R.string.verificar_comprobando);
                mostrarError(tvError, mensaje != null ? mensaje : getString(R.string.verificar_aun_no));
            }
        });
    }

    private void reenviar() {
        mostrarError(tvError, null);
        btnReenviar.setEnabled(false);
        auth.reenviarVerificacion(new AuthRepository.Callback() {
            @Override
            public void onExito() {
                Toast.makeText(VerificarCorreoActivity.this, R.string.verificar_reenviado, Toast.LENGTH_SHORT).show();
                iniciarEspera();
            }

            @Override
            public void onError(String mensaje) {
                btnReenviar.setEnabled(true);
                mostrarError(tvError, mensaje);
            }
        });
    }

    private void iniciarEspera() {
        btnReenviar.setEnabled(false);
        if (cuentaRegresiva != null) cuentaRegresiva.cancel();
        cuentaRegresiva = new CountDownTimer(ESPERA_REENVIO_MS, 1000) {
            @Override
            public void onTick(long restante) {
                btnReenviar.setText(getString(R.string.verificar_reenviar_en, (int) (restante / 1000) + 1));
            }

            @Override
            public void onFinish() {
                btnReenviar.setText(R.string.verificar_reenviar);
                btnReenviar.setEnabled(true);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if (cuentaRegresiva != null) cuentaRegresiva.cancel();
        super.onDestroy();
    }
}
