package com.changarrito.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.changarrito.R;
import com.changarrito.database.AppDatabase;
import com.changarrito.database.entity.EstadoAlerta;
import com.changarrito.negocio.SesionTurno;
import com.changarrito.ui.secciones.AlertasFragment;
import com.changarrito.ui.secciones.InicioFragment;
import com.changarrito.ui.secciones.InventarioFragment;
import com.changarrito.ui.secciones.ReportesFragment;
import com.changarrito.ui.secciones.VenderFragment;
import com.changarrito.utils.InsetsUtil;
import com.changarrito.viewmodel.AlertaViewModel;
import com.changarrito.workers.AlertaCheckWorker;
import com.changarrito.workers.RespaldoWorker;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.TimeUnit;

/**
 * La app ya adentro: una sola pantalla con la barra de abajo
 * (Inicio · Inventario · Vender · Alertas · Reportes) y las cinco secciones como fragments.
 *
 * Antes cada pestana era su propia Activity y al cambiar se notaba la transicion de
 * ventana (parecia que la app "regresaba" o se trababa). Ahora la pantalla es siempre la
 * misma: solo se esconde la seccion actual y se muestra la otra con un fundido corto.
 * Cada seccion se crea la primera vez que se visita y despues se conserva tal cual.
 *
 * Reglas:
 *  - Para el trabajador "Reportes" se llama "Caja" y abre solo el cierre a ciegas.
 *  - "Atras" en cualquier seccion regresa a Inicio; en Inicio sale de la app.
 *  - Mientras el teclado esta abierto la barra se esconde (deja espacio para escribir).
 *  - La pestana Alertas muestra cuantas hay activas.
 *  - EXTRA_SECCION abre directo una seccion (la notificacion de alertas lo usa).
 */
public class PrincipalActivity extends AppCompatActivity {

    public static final String EXTRA_SECCION = "seccion";
    public static final String SECCION_ALERTAS = "alertas";

    private static final String ESTADO_SECCION = "seccion_actual";
    private static final int[] SECCIONES = {
            R.id.nav_inicio, R.id.nav_inventario, R.id.nav_vender, R.id.nav_alertas, R.id.nav_reportes};

    private BottomNavigationView nav;
    @IdRes
    private int actual = R.id.nav_inicio;
    private ActivityResultLauncher<String> permisoNotificaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootPrincipal), true, true);

        permisoNotificaciones = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), concedido -> { /* la app funciona igual */ });

        nav = findViewById(R.id.navInferior);
        configurarBarra();

        if (savedInstanceState != null) {
            actual = savedInstanceState.getInt(ESTADO_SECCION, R.id.nav_inicio);
            mostrar(actual, false);
        } else {
            int pedida = seccionPedida(getIntent());
            mostrar(pedida != 0 ? pedida : R.id.nav_inicio, false);
            tareasDeArranque();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int pedida = seccionPedida(intent);
        if (pedida != 0) mostrar(pedida, false);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_SECCION, actual);
    }

    /** Para los accesos de Inicio ("Vender", "Ver alertas"...). */
    public void irA(@IdRes int itemNavegacion) {
        if (itemNavegacion != actual) mostrar(itemNavegacion, true);
    }

    // ------------------------------------------------------------------ barra

    private void configurarBarra() {
        if (!SesionTurno.esDueno(this)) {
            MenuItem reportes = nav.getMenu().findItem(R.id.nav_reportes);
            reportes.setTitle(R.string.nav_caja);
            reportes.setIcon(R.drawable.ic_nav_caja);
        }
        nav.setOnItemSelectedListener(item -> {
            if (item.getItemId() != actual) mostrar(item.getItemId(), true);
            return true;
        });
        nav.setOnItemReselectedListener(item -> { /* ya estamos aqui */ });

        // El padding de las barras del sistema ya lo pone la vista raiz (InsetsUtil);
        // aqui solo se esconde la barra cuando aparece el teclado.
        ViewCompat.setOnApplyWindowInsetsListener(nav, (v, insets) -> {
            boolean teclado = insets.isVisible(WindowInsetsCompat.Type.ime());
            View barra = (View) v.getParent();
            barra.setVisibility(teclado ? View.GONE : View.VISIBLE);
            return insets;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (actual != R.id.nav_inicio) {
                    mostrar(R.id.nav_inicio, true);
                } else {
                    finish();
                }
            }
        });

        AppDatabase.getInstance(this).alertaDao().contarPorEstado(EstadoAlerta.ACTIVA).observe(this, n -> {
            BadgeDrawable insignia = nav.getOrCreateBadge(R.id.nav_alertas);
            int cantidad = n != null ? n : 0;
            insignia.setVisible(cantidad > 0);
            insignia.setNumber(cantidad);
            insignia.setBackgroundColor(ContextCompat.getColor(this, R.color.status_rojo));
            insignia.setMaxCharacterCount(3);
        });
    }

    // ------------------------------------------------------------------ secciones

    /**
     * Deja visible solo la seccion pedida. Las demas se esconden (no se destruyen), asi
     * al volver todo sigue como estaba. Se hace en el momento (commitNow) para que dos
     * toques rapidos seguidos no dejen dos secciones encimadas.
     */
    private void mostrar(@IdRes int destino, boolean animar) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction t = fm.beginTransaction().setReorderingAllowed(true);
        if (animar) t.setCustomAnimations(R.anim.seccion_entra, R.anim.seccion_sale);
        for (int id : SECCIONES) {
            Fragment f = fm.findFragmentByTag(etiqueta(id));
            if (id == destino) {
                if (f == null) t.add(R.id.contenedorSecciones, crear(id), etiqueta(id));
                else if (f.isHidden()) t.show(f);
            } else if (f != null && !f.isHidden()) {
                t.hide(f);
            }
        }
        t.commitNowAllowingStateLoss();
        actual = destino;
        MenuItem item = nav.getMenu().findItem(destino);
        if (item != null && !item.isChecked()) item.setChecked(true);
    }

    private static Fragment crear(@IdRes int id) {
        if (id == R.id.nav_inventario) return new InventarioFragment();
        if (id == R.id.nav_vender) return new VenderFragment();
        if (id == R.id.nav_alertas) return new AlertasFragment();
        if (id == R.id.nav_reportes) return new ReportesFragment();
        return new InicioFragment();
    }

    /** Etiquetas fijas (no ids de recursos) para encontrar la seccion tras rotar la pantalla. */
    private static String etiqueta(@IdRes int id) {
        if (id == R.id.nav_inventario) return "seccion_inventario";
        if (id == R.id.nav_vender) return "seccion_vender";
        if (id == R.id.nav_alertas) return "seccion_alertas";
        if (id == R.id.nav_reportes) return "seccion_reportes";
        return "seccion_inicio";
    }

    private static int seccionPedida(Intent intent) {
        if (intent == null) return 0;
        String s = intent.getStringExtra(EXTRA_SECCION);
        intent.removeExtra(EXTRA_SECCION); // que no se repita al rotar
        return SECCION_ALERTAS.equals(s) ? R.id.nav_alertas : 0;
    }

    // ------------------------------------------------------------------ arranque

    /** Lo que se hace una vez al entrar a la app. */
    private void tareasDeArranque() {
        // Android 13+: permiso para mostrar las notificaciones de alertas. Si lo niega,
        // las alertas se siguen viendo en su pestana.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            permisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
        // Revisa alertas al abrir, sin esperar a la revision periodica de cada 24 h.
        new ViewModelProvider(this).get(AlertaViewModel.class).verificarAlertasAhora();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("revision_alertas", ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(AlertaCheckWorker.class, 24, TimeUnit.HOURS).build());
        // Respaldo automatico en la nube una vez al dia, cuando haya internet.
        RespaldoWorker.programarDiario(this);
    }
}
