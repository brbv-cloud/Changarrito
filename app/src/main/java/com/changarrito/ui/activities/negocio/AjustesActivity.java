package com.changarrito.ui.activities.negocio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.changarrito.R;
import com.changarrito.auth.AuthRepository;
import com.changarrito.auth.GoogleSignInHelper;
import com.changarrito.database.entity.Rol;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.negocio.NegocioRepository;
import com.changarrito.respaldo.RespaldoNube;
import com.changarrito.respaldo.RespaldoRepository;
import com.changarrito.workers.RespaldoWorker;
import com.changarrito.negocio.SesionTurno;
import com.changarrito.ui.activities.acceso.LoginActivity;
import com.changarrito.utils.InsetsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

/**
 * Ajustes. El dueño ve todo (negocio, equipo, cerrar sesión). Un trabajador solo ve
 * su turno y "Cambiar de turno": así no puede dar de alta gente ni sacar al dueño.
 */
public class AjustesActivity extends AppCompatActivity {

    private NegocioRepository repo;
    private AuthRepository auth;
    private LinearLayout contenedorEquipo;
    private UsuarioEntity dueno;
    private int trabajadoresActivos;
    private RespaldoRepository respaldo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootAjustes), true, true);
        repo = new NegocioRepository(getApplication());
        auth = new AuthRepository();
        contenedorEquipo = findViewById(R.id.contenedorEquipo);

        boolean esDueno = SesionTurno.esDueno(this);
        String nombreTurno = SesionTurno.getNombre(this);
        ((TextView) findViewById(R.id.tvInicialTurno)).setText(GiroUi.inicial(nombreTurno));
        ((TextView) findViewById(R.id.tvTurnoActual)).setText(getString(R.string.ajustes_turno_actual, nombreTurno));
        ((TextView) findViewById(R.id.tvRolTurno)).setText(esDueno ? R.string.rol_dueno : R.string.rol_trabajador);
        findViewById(R.id.seccionNegocio).setVisibility(esDueno ? View.VISIBLE : View.GONE);

        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        findViewById(R.id.btnCambiarTurno).setOnClickListener(v -> cambiarTurno());
        findViewById(R.id.tvAcerca).setOnClickListener(v -> mostrarAcerca());

        if (!esDueno) return;

        FirebaseUser cuenta = auth.getUsuarioActual();
        repo.getConfiguracion().observe(this, config -> {
            if (config == null) return;
            ((TextView) findViewById(R.id.tvNombreNegocio)).setText(config.nombreNegocio);
            ((ImageView) findViewById(R.id.ivGiro)).setImageResource(GiroUi.icono(config.giro));
            String correo = cuenta != null && cuenta.getEmail() != null ? cuenta.getEmail() : "";
            ((TextView) findViewById(R.id.tvGiroCuenta)).setText(config.giro.nombreVisible + " · " + correo);
        });
        repo.getUsuariosActivos().observe(this, this::mostrarEquipo);

        findViewById(R.id.btnBitacora).setOnClickListener(v ->
                startActivity(new Intent(this, BitacoraActivity.class)));

        respaldo = new RespaldoRepository(getApplication());
        mostrarEstadoRespaldo();
        findViewById(R.id.btnRespaldarAhora).setOnClickListener(v -> respaldarAhora());
        findViewById(R.id.btnRestaurar).setOnClickListener(v -> buscarParaRestaurar());
        findViewById(R.id.btnAgregarTrabajador).setOnClickListener(v -> agregarTrabajador());
        findViewById(R.id.btnMiClave).setOnClickListener(v -> crearClaveDueno(null));
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> confirmarCerrarSesion());
    }

    // ---------- Respaldo en la nube ----------

    private void mostrarEstadoRespaldo() {
        TextView tv = findViewById(R.id.tvEstadoRespaldo);
        long ms = respaldo.getUltimoRespaldoMs();
        if (ms == 0) {
            tv.setText(R.string.respaldo_nunca);
            return;
        }
        String resumen = respaldo.getUltimoResumen();
        tv.setText(getString(R.string.respaldo_ultimo, DialogosRespaldo.cuando(this, ms),
                resumen != null ? resumen : ""));
    }

    private void botonesRespaldo(boolean activos, int textoRespaldar, int textoRestaurar) {
        MaterialButton respaldar = findViewById(R.id.btnRespaldarAhora);
        MaterialButton restaurar = findViewById(R.id.btnRestaurar);
        respaldar.setEnabled(activos);
        restaurar.setEnabled(activos);
        respaldar.setText(textoRespaldar);
        restaurar.setText(textoRestaurar);
    }

    private void respaldarAhora() {
        botonesRespaldo(false, R.string.respaldo_en_proceso, R.string.respaldo_restaurar);
        respaldo.respaldar(new RespaldoRepository.Callback<RespaldoNube.InfoRespaldo>() {
            @Override
            public void onExito(RespaldoNube.InfoRespaldo info) {
                if (isFinishing() || isDestroyed()) return;
                botonesRespaldo(true, R.string.respaldo_ahora, R.string.respaldo_restaurar);
                mostrarEstadoRespaldo();
                Toast.makeText(AjustesActivity.this, R.string.respaldo_listo, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String mensaje) {
                if (isFinishing() || isDestroyed()) return;
                botonesRespaldo(true, R.string.respaldo_ahora, R.string.respaldo_restaurar);
                DialogosRespaldo.mostrarError(AjustesActivity.this, mensaje);
            }
        });
    }

    private void buscarParaRestaurar() {
        botonesRespaldo(false, R.string.respaldo_ahora, R.string.respaldo_buscando);
        respaldo.consultar(new RespaldoRepository.Callback<RespaldoNube.InfoRespaldo>() {
            @Override
            public void onExito(RespaldoNube.InfoRespaldo info) {
                if (isFinishing() || isDestroyed()) return;
                botonesRespaldo(true, R.string.respaldo_ahora, R.string.respaldo_restaurar);
                if (info == null) {
                    DialogosRespaldo.mostrarError(AjustesActivity.this, getString(R.string.respaldo_error_sin_respaldo));
                } else {
                    DialogosRespaldo.confirmarReemplazo(AjustesActivity.this, info);
                }
            }

            @Override
            public void onError(String mensaje) {
                if (isFinishing() || isDestroyed()) return;
                botonesRespaldo(true, R.string.respaldo_ahora, R.string.respaldo_restaurar);
                DialogosRespaldo.mostrarError(AjustesActivity.this, mensaje);
            }
        });
    }

    private void mostrarEquipo(List<UsuarioEntity> usuarios) {
        contenedorEquipo.removeAllViews();
        trabajadoresActivos = 0;
        if (usuarios == null) return;
        LayoutInflater inflater = LayoutInflater.from(this);
        for (UsuarioEntity u : usuarios) {
            if (u.rol == Rol.DUENO) {
                dueno = u;
            } else {
                trabajadoresActivos++;
            }
            View renglon = GiroUi.renglonPerfil(inflater, contenedorEquipo, u);
            if (u.rol == Rol.TRABAJADOR) {
                TextView btnBaja = renglon.findViewById(R.id.btnAccion);
                btnBaja.setText(R.string.ajustes_dar_de_baja);
                btnBaja.setVisibility(View.VISIBLE);
                btnBaja.setOnClickListener(v -> confirmarBaja(u));
            }
            contenedorEquipo.addView(renglon);
        }
        // "Cambiar de turno" solo tiene sentido con más de una persona.
        findViewById(R.id.btnCambiarTurno).setVisibility(trabajadoresActivos > 0 ? View.VISIBLE : View.GONE);
    }

    private void agregarTrabajador() {
        // Con trabajadores, el dueño también entra con clave: si no tiene, la crea primero.
        if (dueno != null && dueno.claveHash == null) {
            crearClaveDueno(this::pedirDatosTrabajador);
        } else {
            pedirDatosTrabajador();
        }
    }

    private void pedirDatosTrabajador() {
        DialogosPerfil.mostrar(this, getString(R.string.trabajador_nuevo_titulo), null, true, true,
                getString(R.string.clave_guardar),
                (nombre, clave, respuesta) -> repo.crearTrabajador(nombre, clave, id -> {
                    respuesta.listo();
                    Toast.makeText(this, getString(R.string.trabajador_agregado, nombre), Toast.LENGTH_LONG).show();
                }));
    }

    private void crearClaveDueno(Runnable despues) {
        if (dueno == null) return;
        String ayuda = despues != null ? getString(R.string.clave_dueno_ayuda) : null;
        DialogosPerfil.mostrar(this, getString(R.string.clave_dueno_titulo), ayuda, false, true,
                getString(R.string.clave_guardar),
                (nombre, clave, respuesta) -> repo.establecerClave(dueno.id, clave, ok -> {
                    respuesta.listo();
                    Toast.makeText(this, R.string.clave_guardada, Toast.LENGTH_SHORT).show();
                    if (despues != null) despues.run();
                }));
    }

    private void confirmarBaja(UsuarioEntity u) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ajustes_dar_de_baja)
                .setMessage(getString(R.string.ajustes_dar_de_baja_confirmar, u.nombre))
                .setPositiveButton(R.string.ajustes_dar_de_baja, (d, w) -> repo.darDeBaja(u.id, null))
                .setNegativeButton(R.string.clave_cancelar, null)
                .show();
    }

    private void cambiarTurno() {
        SesionTurno.cerrar(this);
        Intent i = new Intent(this, SeleccionarTurnoActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void confirmarCerrarSesion() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ajustes_cerrar_sesion)
                .setMessage(R.string.ajustes_cerrar_sesion_confirmar)
                .setPositiveButton(R.string.ajustes_cerrar_sesion, (d, w) -> cerrarSesion())
                .setNegativeButton(R.string.clave_cancelar, null)
                .show();
    }

    private void cerrarSesion() {
        RespaldoWorker.cancelar(this);
        SesionTurno.cerrar(this);
        auth.cerrarSesion();
        GoogleSignInHelper.olvidarCuentaElegida(this);
        Intent i = new Intent(this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void mostrarAcerca() {
        String version;
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            version = "1.0";
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ajustes_acerca)
                .setMessage(getString(R.string.ajustes_acerca_texto, version))
                .setPositiveButton(R.string.legal_entendido, null)
                .show();
    }
}
