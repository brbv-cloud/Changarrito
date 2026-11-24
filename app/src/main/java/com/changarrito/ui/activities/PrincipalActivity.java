package com.changarrito.ui.activities;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.view.ViewGroup;

import android.app.Application;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.annotation.ColorRes;
import com.changarrito.negocio.CambioDeNegocio;
import com.changarrito.utils.Conexion;
import com.changarrito.workers.RecordatorioRespaldoWorker;

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
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.changarrito.R;
import com.changarrito.database.AppDatabase;
import com.changarrito.database.entity.EstadoAlerta;
import com.changarrito.negocio.SesionTurno;
import com.changarrito.repository.AlertaRepository;
import com.changarrito.ui.Avisos;
import com.changarrito.ui.secciones.AlertasFragment;
import com.changarrito.ui.secciones.InicioFragment;
import com.changarrito.ui.secciones.InventarioFragment;
import com.changarrito.ui.secciones.ReportesFragment;
import com.changarrito.ui.secciones.VenderFragment;
import com.changarrito.utils.InsetsUtil;
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
    /** true = ir a Inicio y mostrar el recorrido de bienvenida (Ajustes). */
    public static final String EXTRA_TUTORIAL = "ver_tutorial";

    private static final String ESTADO_SECCION = "seccion_actual";
    private static final int[] SECCIONES = {
            R.id.nav_inicio, R.id.nav_inventario, R.id.nav_vender, R.id.nav_alertas, R.id.nav_reportes};

    private BottomNavigationView nav;
    @IdRes
    private int actual = R.id.nav_inicio;
    private ActivityResultLauncher<String> permisoNotificaciones;

    // Aviso breve de internet
    private ConnectivityManager.NetworkCallback vigilanteRed;
    /** null = todavia no se sabe (al abrir no se avisa si hay internet, solo si falta). */
    private Boolean enLinea;
    private final Handler principal = new Handler(Looper.getMainLooper());
    private final Runnable revisarRed = this::revisarConexion;
    private final Runnable ocultarAviso = this::ocultarAvisoConexion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Igual en todas las versiones de Android: la app se dibuja detras de la barra de
        // notificaciones y la franja fondoBarraEstado ocupa ese espacio.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_principal);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootPrincipal), false, true);
        View fondoBarra = findViewById(R.id.fondoBarraEstado);
        ViewCompat.setOnApplyWindowInsetsListener(fondoBarra, (v, insets) -> {
            int alto = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp.height != alto) {
                lp.height = alto;
                v.setLayoutParams(lp);
            }
            return insets;
        });

        permisoNotificaciones = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), concedido -> { /* la app funciona igual */ });

        nav = findViewById(R.id.navInferior);
        configurarBarra();
        vigilarInventario();

        if (savedInstanceState != null) {
            actual = savedInstanceState.getInt(ESTADO_SECCION, R.id.nav_inicio);
            mostrar(actual, false);
        } else {
            int pedida = seccionPedida(getIntent());
            mostrar(pedida != 0 ? pedida : R.id.nav_inicio, false);
            pedirTutorial(getIntent());
            tareasDeArranque();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int pedida = seccionPedida(intent);
        if (pedida != 0) mostrar(pedida, false);
        pedirTutorial(intent);
    }

    private void pedirTutorial(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(EXTRA_TUTORIAL, false)) return;
        intent.removeExtra(EXTRA_TUTORIAL); // que no se repita al rotar
        mostrar(R.id.nav_inicio, false);
        Fragment f = getSupportFragmentManager().findFragmentByTag(etiqueta(R.id.nav_inicio));
        if (f instanceof InicioFragment) ((InicioFragment) f).verRecorrido();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(ESTADO_SECCION, actual);
    }

    @Override
    protected void onStart() {
        super.onStart();
        vigilarConexion();
    }

    @Override
    protected void onStop() {
        dejarDeVigilarConexion();
        super.onStop();
    }

    // ------------------------------------------------------------------ internet

    private void vigilarConexion() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null || vigilanteRed != null) return;
        vigilanteRed = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                programarRevision();
            }

            @Override
            public void onLost(@NonNull Network network) {
                programarRevision();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capacidades) {
                programarRevision();
            }
        };
        try {
            cm.registerDefaultNetworkCallback(vigilanteRed);
        } catch (RuntimeException e) {
            vigilanteRed = null;
        }
        programarRevision(); // como esta al volver a la app
    }

    private void dejarDeVigilarConexion() {
        principal.removeCallbacks(revisarRed);
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null && vigilanteRed != null) {
            try {
                cm.unregisterNetworkCallback(vigilanteRed);
            } catch (RuntimeException ignorada) {
                // ya no estaba registrado
            }
        }
        vigilanteRed = null;
    }

    /** Al conectarse la red cambia varias veces seguidas: se espera a que se asiente. */
    private void programarRevision() {
        principal.removeCallbacks(revisarRed);
        principal.postDelayed(revisarRed, 1200);
    }

    private void revisarConexion() {
        if (isFinishing()) return;
        boolean ahora = Conexion.hayInternet(this);
        Boolean antes = enLinea;
        enLinea = ahora;
        if (antes != null && antes == ahora) return;
        if (!ahora) {
            mostrarAvisoConexion(getString(R.string.conexion_sin), R.color.color_slate_dark);
            return;
        }
        if (antes == null) return; // al abrir con internet no hace falta avisar
        // Volvio el internet: si hay algo sin respaldar, se respalda ya.
        Application app = getApplication();
        new Thread(() -> {
            boolean pendiente;
            try {
                CambioDeNegocio.Resumen r = CambioDeNegocio.leerSync(app);
                pendiente = r.nombreNegocio != null && r.hayCambiosSinRespaldar();
            } catch (Exception e) {
                pendiente = false;
            }
            boolean respaldando = pendiente;
            if (respaldando) RespaldoWorker.respaldarPronto(app);
            runOnUiThread(() -> {
                if (!isFinishing()) mostrarAvisoConexion(getString(respaldando
                        ? R.string.conexion_volvio_respaldo : R.string.conexion_volvio), R.color.marca_verde);
            });
        }).start();
    }

    /** Franja sobre la barra de abajo; se quita sola en 4 segundos o con un toque. */
    private void mostrarAvisoConexion(CharSequence texto, @ColorRes int color) {
        TextView aviso = findViewById(R.id.avisoConexion);
        principal.removeCallbacks(ocultarAviso);
        aviso.setText(texto);
        aviso.setBackgroundColor(ContextCompat.getColor(this, color));
        aviso.setOnClickListener(v -> ocultarAvisoConexion());
        aviso.animate().cancel();
        if (aviso.getVisibility() != View.VISIBLE) {
            aviso.setAlpha(0f);
            aviso.setVisibility(View.VISIBLE);
        }
        aviso.animate().alpha(1f).setDuration(200).start();
        principal.postDelayed(ocultarAviso, 4000);
    }

    private void ocultarAvisoConexion() {
        principal.removeCallbacks(ocultarAviso);
        TextView aviso = findViewById(R.id.avisoConexion);
        if (aviso == null || aviso.getVisibility() != View.VISIBLE) return;
        aviso.animate().alpha(0f).setDuration(250).withEndAction(() -> aviso.setVisibility(View.GONE)).start();
    }

    /**
     * Mensaje corto (2.5 s, se quita con un toque) arriba de la barra de abajo, o arriba
     * de la vista que pida la seccion (la barra del carrito en Vender).
     */
    public void avisar(CharSequence mensaje, View ancla) {
        View barra = (View) nav.getParent();
        Avisos.mostrar(findViewById(R.id.rootPrincipal), mensaje, ancla != null ? ancla : barra);
    }

    /** "Cerrar caja" de Inicio: Reportes en la pestana Caja (el trabajador ya entra ahi). */
    public void irACaja() {
        irA(R.id.nav_reportes);
        Fragment f = getSupportFragmentManager().findFragmentByTag(etiqueta(R.id.nav_reportes));
        if (f instanceof ReportesFragment) ((ReportesFragment) f).abrirCaja();
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

    /**
     * Revisa las alertas (stock bajo y caducidad) al abrir la app y cada vez que cambia el
     * inventario: al cobrar, editar o surtir un producto, o registrar una merma. Asi la
     * alerta y su notificacion salen en el momento, no hasta la revision de cada 24 h.
     */
    private void vigilarInventario() {
        AlertaRepository alertas = new AlertaRepository(getApplication());
        AppDatabase.getInstance(this).productoDao().getAllProductos()
                .observe(this, productos -> alertas.verificarAlertasAhora());
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
        pintarBarraEstado(destino);
        MenuItem item = nav.getMenu().findItem(destino);
        if (item != null && !item.isChecked()) item.setChecked(true);
    }

    /** Inicio: franja verde (sigue al encabezado) e iconos blancos; lo demas: clara e iconos oscuros. */
    private void pintarBarraEstado(@IdRes int destino) {
        boolean inicio = destino == R.id.nav_inicio;
        int color = inicio ? R.color.marca_verde_oscuro
                : destino == R.id.nav_reportes ? R.color.color_bg : R.color.color_bg_alt;
        findViewById(R.id.fondoBarraEstado).setBackgroundColor(ContextCompat.getColor(this, color));
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(!inicio);
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
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("revision_alertas", ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(AlertaCheckWorker.class, 24, TimeUnit.HOURS).build());
        // Respaldo automatico en la nube una vez al dia, cuando haya internet.
        RespaldoWorker.programarDiario(this);
        // A las 9 de la noche: si falta respaldar algo, se manda y se avisa.
        RecordatorioRespaldoWorker.programar(this);
    }
}
