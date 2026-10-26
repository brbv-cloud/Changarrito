package com.changarrito.ui.activities.negocio;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.changarrito.R;
import com.changarrito.auth.AuthRepository;
import com.changarrito.database.entity.GiroNegocio;
import com.changarrito.negocio.CatalogoGiro;
import com.changarrito.negocio.NegocioRepository;
import com.changarrito.respaldo.RespaldoNube;
import com.changarrito.respaldo.RespaldoRepository;
import com.changarrito.ui.activities.acceso.NavegacionAcceso;
import com.changarrito.utils.InsetsUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;

import java.util.EnumMap;
import java.util.Map;

/**
 * Primera pantalla después del primer inicio de sesión: el dueño elige el tipo de
 * negocio. Eso define unidades, caducidad y el catálogo sugerido (ConfiguracionNegocio).
 */
public class ElegirGiroActivity extends AppCompatActivity {

    private final Map<GiroNegocio, View> tarjetas = new EnumMap<>(GiroNegocio.class);
    private GiroNegocio elegido;

    private TextInputEditText etNombreNegocio;
    private TextView tvError;
    private MaterialButton btnContinuar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_elegir_giro);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootElegirGiro), true, true);

        etNombreNegocio = findViewById(R.id.etNombreNegocio);
        tvError = findViewById(R.id.tvError);
        btnContinuar = findViewById(R.id.btnContinuar);

        LinearLayout contenedor = findViewById(R.id.contenedorGiros);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (GiroNegocio giro : GiroNegocio.values()) {
            View tarjeta = inflater.inflate(R.layout.item_giro, contenedor, false);
            ((ImageView) tarjeta.findViewById(R.id.ivIcono)).setImageResource(GiroUi.icono(giro));
            ((TextView) tarjeta.findViewById(R.id.tvNombre)).setText(giro.nombreVisible);
            ((TextView) tarjeta.findViewById(R.id.tvDescripcion)).setText(GiroUi.descripcion(giro));
            tarjeta.setOnClickListener(v -> elegir(giro));
            tarjetas.put(giro, tarjeta);
            contenedor.addView(tarjeta);
        }

        btnContinuar.setOnClickListener(v -> continuar());
        if (savedInstanceState == null) buscarRespaldoEnLaNube();
    }

    /**
     * Celular nuevo o app reinstalada: si esta cuenta ya tiene un negocio respaldado,
     * se ofrece recuperarlo en vez de configurar todo de cero. Sin internet o sin
     * respaldo no se muestra nada y se sigue normal.
     */
    private void buscarRespaldoEnLaNube() {
        new RespaldoRepository(getApplication()).consultar(new RespaldoRepository.Callback<RespaldoNube.InfoRespaldo>() {
            @Override
            public void onExito(RespaldoNube.InfoRespaldo info) {
                // Si ya le dio "Continuar", no lo interrumpimos.
                if (info == null || !btnContinuar.isEnabled()) return;
                DialogosRespaldo.ofrecerRecuperar(ElegirGiroActivity.this, info);
            }

            @Override
            public void onError(String mensaje) {
                // Sin internet o sin sesion: se configura normal.
            }
        });
    }

    private void elegir(GiroNegocio giro) {
        elegido = giro;
        tvError.setVisibility(View.GONE);
        for (Map.Entry<GiroNegocio, View> e : tarjetas.entrySet()) {
            boolean sel = e.getKey() == giro;
            e.getValue().setSelected(sel);
            e.getValue().findViewById(R.id.ivCheck).setVisibility(sel ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void continuar() {
        if (elegido == null) {
            tvError.setText(R.string.giro_elige_uno);
            tvError.setVisibility(View.VISIBLE);
            return;
        }
        AuthRepository auth = new AuthRepository();
        FirebaseUser usuario = auth.getUsuarioActual();
        if (usuario == null) {
            NavegacionAcceso.irAlDestino(this, auth);
            return;
        }
        String nombreNegocio = etNombreNegocio.getText() == null ? "" : etNombreNegocio.getText().toString().trim();
        if (nombreNegocio.isEmpty()) nombreNegocio = elegido.nombreVisible;

        btnContinuar.setEnabled(false);
        btnContinuar.setText(R.string.giro_guardando);
        double margen = CatalogoGiro.cargar(this, elegido).margenSugerido;
        new NegocioRepository(getApplication()).guardarGiro(elegido, nombreNegocio, usuario.getUid(),
                usuario.getDisplayName(), margen, tieneCatalogo -> {
                    if (tieneCatalogo) {
                        startActivity(new Intent(this, VistaPreviaCatalogoActivity.class));
                        finish();
                    } else {
                        NavegacionAcceso.irAlDestino(this, auth);
                    }
                });
    }
}
