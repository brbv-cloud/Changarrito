package com.changarrito.ui.activities.negocio;

import com.changarrito.ui.Ayuda;
import com.changarrito.ui.guia.GuiaOverlay;
import com.changarrito.utils.Conexion;
import java.util.ArrayList;

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
        findViewById(R.id.btnVerTutorial).setOnClickListener(v -> {
            // Regresa a Inicio (la pantalla que ya estaba abierta) y arranca el recorrido.
            Intent i = new Intent(this, com.changarrito.ui.activities.PrincipalActivity.class)
                    .putExtra(com.changarrito.ui.activities.PrincipalActivity.EXTRA_TUTORIAL, true)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        });
        findViewById(R.id.tvReiniciarAyudas).setOnClickListener(v -> {
            com.changarrito.ui.Ayuda.reiniciar(this);
            android.widget.Toast.makeText(this, R.string.ayuda_reiniciadas, android.widget.Toast.LENGTH_LONG).show();
        });
        // Guia: boton "?" y, la primera vez con esta cuenta, sola.
        findViewById(R.id.btnGuia).setOnClickListener(v -> mostrarGuia());
        if (!Ayuda.yaVista(this, "guia_ajustes")) {
            Ayuda.marcarVista(this, "guia_ajustes");
            findViewById(R.id.rootAjustes).postDelayed(() -> {
                if (!isFinishing()) mostrarGuia();
            }, 700);
        }

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
        // Solo el dueno puede corregir el nombre del negocio.
        findViewById(R.id.ivEditarNombre).setVisibility(View.VISIBLE);
        findViewById(R.id.tarjetaNegocio).setOnClickListener(v -> editarNombre());

        findViewById(R.id.btnBitacora).setOnClickListener(v ->
                startActivity(new Intent(this, BitacoraActivity.class)));

        respaldo = new RespaldoRepository(getApplication());
        mostrarEstadoRespaldo();
        findViewById(R.id.btnRespaldarAhora).setOnClickListener(v -> respaldarAhora());
        // Solo en la version de desarrollo: dejar presionado prueba el recordatorio de las 9 pm.
        if ((getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            findViewById(R.id.btnRespaldarAhora).setOnLongClickListener(v -> {
                com.changarrito.workers.RecordatorioRespaldoWorker.probarPronto(this, 30);
                Toast.makeText(this, "Prueba: el recordatorio de las 9 pm corre en 30 segundos. "
                        + "Sal de la app con el botón de Inicio.", Toast.LENGTH_LONG).show();
                return true;
            });
        }
        findViewById(R.id.btnRestaurar).setOnClickListener(v -> buscarParaRestaurar());
        findViewById(R.id.btnAgregarTrabajador).setOnClickListener(v -> agregarTrabajador());
        findViewById(R.id.btnMiClave).setOnClickListener(v -> crearClaveDueno(null));
        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> confirmarCerrarSesion());
    }

    // ---------- Guia ----------

    /** Paso por paso (la pantalla es larga: cada parte se trae a la vista al explicarla). */
    private void mostrarGuia() {
        List<GuiaOverlay.Paso> p = new ArrayList<>();
        paso(p, R.id.btnCambiarTurno, R.drawable.ic_cambiar_turno, R.string.g_aj_turno_t, R.string.g_aj_turno);
        paso(p, R.id.btnVerTutorial, R.drawable.ic_info, R.string.g_aj_tutorial_t, R.string.g_aj_tutorial);
        paso(p, R.id.tarjetaNegocio, R.drawable.ic_editar, R.string.g_aj_negocio_t, R.string.g_aj_negocio);
        paso(p, R.id.btnBitacora, R.drawable.ic_reloj, R.string.g_aj_bitacora_t, R.string.g_aj_bitacora);
        paso(p, R.id.btnRespaldarAhora, R.drawable.ic_nube_subir, R.string.g_aj_respaldo_t, R.string.g_aj_respaldo);
        paso(p, R.id.btnRestaurar, R.drawable.ic_nube_bajar, R.string.g_aj_restaurar_t, R.string.g_aj_restaurar);
        paso(p, R.id.btnAgregarTrabajador, R.drawable.ic_persona_mas, R.string.g_aj_equipo_t, R.string.g_aj_equipo);
        paso(p, R.id.btnMiClave, R.drawable.ic_llave, R.string.g_aj_clave_t, R.string.g_aj_clave);
        paso(p, R.id.btnCerrarSesion, R.drawable.ic_salir, R.string.g_aj_salir_t, R.string.g_aj_salir);
        GuiaOverlay.mostrar(this, p, GuiaOverlay.Modo.RECORRIDO, null);
    }

    /** Solo lo que se ve (el trabajador no tiene la parte del negocio). */
    private void paso(List<GuiaOverlay.Paso> lista, int id, int icono, int titulo, int texto) {
        View v = findViewById(id);
        if (v != null && v.isShown()) lista.add(new GuiaOverlay.Paso(v, icono, getText(titulo), getText(texto)));
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
        if (!Conexion.hayInternet(this)) tv.append("\n" + getString(R.string.respaldo_sin_internet));
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

    /** Corregir el nombre del negocio (por si se escribio mal al empezar). */
    private void editarNombre() {
        View vista = getLayoutInflater().inflate(R.layout.dialogo_nombre_negocio, null);
        com.google.android.material.textfield.TextInputLayout til = vista.findViewById(R.id.tilNombre);
        com.google.android.material.textfield.TextInputEditText et = vista.findViewById(R.id.etNombre);
        et.setText(((TextView) findViewById(R.id.tvNombreNegocio)).getText());
        et.setSelection(et.getText() != null ? et.getText().length() : 0);
        androidx.appcompat.app.AlertDialog dialogo = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.ajustes_nombre_titulo)
                .setView(vista)
                .setPositiveButton(R.string.ajustes_nombre_guardar, null)
                .setNegativeButton(R.string.ajustes_nombre_cancelar, null)
                .create();
        dialogo.setOnShowListener(d -> dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(b -> {
                    String nombre = et.getText() != null ? et.getText().toString().trim() : "";
                    if (nombre.isEmpty()) {
                        til.setError(getString(R.string.ajustes_nombre_vacio));
                        return;
                    }
                    repo.cambiarNombreNegocio(nombre, ok -> {
                        if (ok) android.widget.Toast.makeText(this, R.string.ajustes_nombre_guardado,
                                android.widget.Toast.LENGTH_SHORT).show();
                    });
                    dialogo.dismiss();
                }));
        dialogo.show();
    }
}
