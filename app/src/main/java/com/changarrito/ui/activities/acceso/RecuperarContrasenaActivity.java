package com.changarrito.ui.activities.acceso;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.changarrito.R;
import com.changarrito.auth.AuthRepository;
import com.changarrito.auth.ValidadorAcceso;
import com.changarrito.utils.InsetsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RecuperarContrasenaActivity extends BaseAccesoActivity {

    private AuthRepository auth;
    private TextInputLayout tilCorreo;
    private TextInputEditText etCorreo;
    private TextView tvError, tvEnviado;
    private MaterialButton btnEnviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_contrasena);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootRecuperar), true, true);
        auth = new AuthRepository();

        tilCorreo = findViewById(R.id.tilCorreo);
        etCorreo = findViewById(R.id.etCorreo);
        tvError = findViewById(R.id.tvError);
        tvEnviado = findViewById(R.id.tvEnviado);
        btnEnviar = findViewById(R.id.btnEnviar);
        limpiarAlEscribir(tilCorreo, etCorreo);

        String correo = getIntent().getStringExtra(LoginActivity.EXTRA_CORREO);
        if (correo != null) etCorreo.setText(correo);

        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        findViewById(R.id.tvIniciaSesion).setOnClickListener(v -> finish());
        btnEnviar.setOnClickListener(v -> enviar());
    }

    private void enviar() {
        mostrarError(tvError, null);
        tvEnviado.setVisibility(View.GONE);
        String correo = texto(etCorreo);
        if (!marcar(tilCorreo, ValidadorAcceso.validarCorreo(correo))) return;

        ocupado(btnEnviar, true, R.string.recuperar_enviar, R.string.recuperar_enviando);
        auth.enviarRecuperacion(correo, new AuthRepository.Callback() {
            @Override
            public void onExito() {
                ocupado(btnEnviar, false, R.string.recuperar_enviar, R.string.recuperar_enviando);
                tvEnviado.setText(R.string.recuperar_enviado);
                tvEnviado.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String mensaje) {
                ocupado(btnEnviar, false, R.string.recuperar_enviar, R.string.recuperar_enviando);
                mostrarError(tvError, mensaje);
            }
        });
    }
}
