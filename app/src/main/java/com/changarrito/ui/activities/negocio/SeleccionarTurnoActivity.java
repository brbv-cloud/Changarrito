package com.changarrito.ui.activities.negocio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.changarrito.R;
import com.changarrito.auth.AuthRepository;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.negocio.NegocioRepository;
import com.changarrito.ui.activities.acceso.NavegacionAcceso;
import com.changarrito.utils.InsetsUtil;

/**
 * "¿Quién atiende?": cada persona del negocio elige su perfil y pone su clave de 4
 * dígitos. Solo aparece cuando hay más de un perfil; con uno solo, el turno se abre solo.
 */
public class SeleccionarTurnoActivity extends AppCompatActivity {

    private NegocioRepository repo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccionar_turno);
        InsetsUtil.aplicarPaddingBarrasDelSistema(findViewById(R.id.rootTurno), true, true);
        repo = new NegocioRepository(getApplication());

        LinearLayout contenedor = findViewById(R.id.contenedorPerfiles);
        LayoutInflater inflater = LayoutInflater.from(this);
        repo.getUsuariosActivos().observe(this, usuarios -> {
            contenedor.removeAllViews();
            if (usuarios == null) return;
            for (UsuarioEntity u : usuarios) {
                View renglon = GiroUi.renglonPerfil(inflater, contenedor, u);
                renglon.setClickable(true);
                renglon.setFocusable(true);
                renglon.setOnClickListener(v -> pedirClave(u));
                contenedor.addView(renglon);
            }
        });
    }

    private void pedirClave(UsuarioEntity u) {
        DialogosPerfil.mostrar(this, getString(R.string.turno_clave_titulo, u.nombre), null,
                false, false, getString(R.string.turno_entrar),
                (nombre, clave, respuesta) -> repo.verificarClave(u.id, clave, r -> {
                    NegocioRepository.ResultadoClave resultado = (NegocioRepository.ResultadoClave) r[0];
                    long dato = (Long) r[1];
                    switch (resultado) {
                        case CORRECTA:
                            respuesta.listo();
                            NavegacionAcceso.irAlDestino(this, new AuthRepository());
                            break;
                        case BLOQUEADO:
                            respuesta.error(getString(R.string.turno_bloqueado,
                                    (int) Math.ceil(dato / 60_000.0)));
                            break;
                        default:
                            respuesta.error(getString(R.string.turno_incorrecta, (int) dato));
                    }
                }));
    }

    /** Desde aquí no se regresa a la app sin elegir perfil. */
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
