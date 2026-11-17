package com.changarrito.ui.activities.acceso;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;

import com.changarrito.R;
import com.changarrito.auth.AuthRepository;
import com.changarrito.negocio.CambioDeNegocio;
import com.changarrito.negocio.NegocioRepository;
import com.changarrito.utils.ClaveUtil;
import com.changarrito.utils.InsetsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entro una cuenta distinta a la del negocio que ya tiene este celular.
 *
 * En vez de solo cerrarle la sesion, se le explica que hay en el celular y cuando se
 * respaldo por ultima vez, y se le deja quitar ese negocio para usar su cuenta:
 *  - con el respaldo al dia, basta con confirmar (los datos ya estan a salvo en la nube);
 *  - con datos sin respaldar, se pide la clave del dueno de ese negocio (o, si no tiene
 *    clave, marcar "Entiendo que se borran"), para que nadie pierda ventas ajenas por
 *    solo iniciar sesion con su cuenta. Despues sigue el flujo normal de una cuenta nueva: elegir giro, o
 * recuperar su propio respaldo de la nube si ya tenia uno.
 */
public class CambiarNegocioActivity extends BaseAccesoActivity {

    private final ExecutorService fondo = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    private AuthRepository auth;
    private CambioDeNegocio.Resumen resumen;
    private boolean pedirClave, pedirCasilla;

    private TextView tvSubtitulo, tvRespaldo, tvError;
    private TextInputLayout tilClave;
    private TextInputEditText etClave;
    private MaterialCheckBox cbEntiendo;
    private MaterialButton btnBorrar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = new AuthRepository();
        FirebaseUser usuario = auth.getUsuarioActual();
        if (usuario == null) {
            NavegacionAcceso.irAlDestino(this, auth);
            return;
        }

        setContentView(R.layout.activity_cambiar_negocio);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootCambio), true, true);

        tvSubtitulo = findViewById(R.id.tvSubtitulo);
        tvRespaldo = findViewById(R.id.tvRespaldo);
        tvError = findViewById(R.id.tvError);
        tilClave = findViewById(R.id.tilClave);
        etClave = findViewById(R.id.etClave);
        cbEntiendo = findViewById(R.id.cbEntiendo);
        btnBorrar = findViewById(R.id.btnBorrar);
        limpiarAlEscribir(tilClave, etClave);

        btnBorrar.setOnClickListener(v -> intentarBorrar());
        findViewById(R.id.tvOtraCuenta).setOnClickListener(v -> volverAlLogin());

        fondo.execute(() -> {
            CambioDeNegocio.Resumen r = CambioDeNegocio.leerSync(getApplication());
            principal.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                mostrar(r, usuario.getEmail());
            });
        });
    }

    private String nombreNegocio() {
        return resumen != null && resumen.nombreNegocio != null && !resumen.nombreNegocio.trim().isEmpty()
                ? resumen.nombreNegocio : getString(R.string.cambio_sin_nombre);
    }

    private void mostrar(CambioDeNegocio.Resumen r, String correo) {
        resumen = r;
        String contenido = getResources().getQuantityString(R.plurals.cambio_productos, r.productos, r.productos)
                + " · " + getResources().getQuantityString(R.plurals.cambio_ventas, r.ventas, r.ventas);
        tvSubtitulo.setText(getString(R.string.cambio_subtitulo, nombreNegocio(), contenido,
                correo != null ? correo : ""));

        // Estado del respaldo: rojo si nunca, ambar si hubo cambios despues, verde si esta al dia.
        int fondoColor, textoColor;
        String texto;
        String fecha = r.ultimoRespaldoMs > 0
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-MX")).format(new Date(r.ultimoRespaldoMs))
                : null;
        if (fecha == null) {
            texto = getString(R.string.cambio_respaldo_nunca);
            fondoColor = R.color.semaforo_rojo_fondo;
            textoColor = R.color.semaforo_rojo_texto;
        } else if (r.hayCambiosSinRespaldar()) {
            texto = getString(R.string.cambio_respaldo_viejo, fecha);
            fondoColor = R.color.semaforo_amarillo_fondo;
            textoColor = R.color.semaforo_amarillo_texto;
        } else {
            texto = getString(R.string.cambio_respaldo_ok, fecha);
            fondoColor = R.color.semaforo_verde_fondo;
            textoColor = R.color.semaforo_verde_texto;
        }
        tvRespaldo.setText(texto);
        tvRespaldo.setTextColor(ContextCompat.getColor(this, textoColor));
        tvRespaldo.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, fondoColor)));
        tvRespaldo.setVisibility(android.view.View.VISIBLE);
        findViewById(R.id.cajaConsejo).setVisibility(
                r.hayCambiosSinRespaldar() ? android.view.View.VISIBLE : android.view.View.GONE);

        pedirClave = r.hayCambiosSinRespaldar() && r.duenoConClave();
        pedirCasilla = r.hayCambiosSinRespaldar() && !r.duenoConClave();
        if (pedirClave) {
            ((TextView) findViewById(R.id.tvEtiquetaClave)).setText(getString(R.string.cambio_clave, nombreNegocio()));
            findViewById(R.id.tvEtiquetaClave).setVisibility(android.view.View.VISIBLE);
            findViewById(R.id.tvAyudaClave).setVisibility(android.view.View.VISIBLE);
            tilClave.setVisibility(android.view.View.VISIBLE);
        } else if (pedirCasilla) {
            cbEntiendo.setVisibility(android.view.View.VISIBLE);
        } else {
            // Respaldo al dia: nada que perder, boton verde y sin clave.
            btnBorrar.setText(R.string.cambio_usar_mi_cuenta);
            btnBorrar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.marca_verde)));
        }
    }

    private void intentarBorrar() {
        if (resumen == null) return; // todavia leyendo
        mostrarError(tvError, null);
        if (pedirClave) {
            String clave = texto(etClave);
            if (!ClaveUtil.esClaveValida(clave)) {
                marcar(tilClave, getString(R.string.cambio_falta_clave));
                return;
            }
        } else if (pedirCasilla && !cbEntiendo.isChecked()) {
            mostrarError(tvError, getString(R.string.cambio_falta_entiendo));
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.cambio_confirmar_titulo, nombreNegocio()))
                .setMessage(pedirClave || pedirCasilla ? R.string.cambio_confirmar_texto : R.string.cambio_confirmar_texto_ok)
                .setPositiveButton(R.string.cambio_confirmar_si, (d, w) -> verificarYBorrar())
                .setNegativeButton(R.string.cambio_cancelar, null)
                .show();
    }

    private void verificarYBorrar() {
        ocupado(btnBorrar, true, textoBoton(), R.string.cambio_borrando);
        if (!pedirClave) {
            borrar();
            return;
        }
        // Misma verificacion que al abrir turno: cuenta intentos y bloquea tras 5 fallos.
        new NegocioRepository(getApplication()).verificarClave(resumen.dueno.id, texto(etClave), res -> {
            NegocioRepository.ResultadoClave r = (NegocioRepository.ResultadoClave) res[0];
            if (r == NegocioRepository.ResultadoClave.CORRECTA) {
                borrar();
                return;
            }
            ocupado(btnBorrar, false, textoBoton(), R.string.cambio_borrando);
            if (r == NegocioRepository.ResultadoClave.BLOQUEADO) {
                mostrarError(tvError, getString(R.string.cambio_bloqueado));
            } else {
                etClave.setText("");
                mostrarError(tvError, getString(R.string.cambio_clave_mal, ((Long) res[1]).intValue()));
            }
        });
    }

    private void borrar() {
        fondo.execute(() -> {
            try {
                CambioDeNegocio.borrarSync(getApplication());
                // Celular limpio: sigue como cuenta nueva (elegir giro o recuperar su respaldo).
                principal.post(() -> NavegacionAcceso.irAlDestino(this, auth));
            } catch (Exception e) {
                principal.post(() -> {
                    ocupado(btnBorrar, false, textoBoton(), R.string.cambio_borrando);
                    mostrarError(tvError, getString(R.string.cambio_error, String.valueOf(e.getMessage())));
                });
            }
        });
    }

    private int textoBoton() {
        return pedirClave || pedirCasilla ? R.string.cambio_borrar : R.string.cambio_usar_mi_cuenta;
    }

    private void volverAlLogin() {
        auth.cerrarSesion();
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fondo.shutdown();
    }
}
