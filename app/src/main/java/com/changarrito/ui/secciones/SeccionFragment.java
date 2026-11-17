package com.changarrito.ui.secciones;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.changarrito.R;
import com.changarrito.ui.Ayuda;
import com.changarrito.ui.activities.PrincipalActivity;
import com.changarrito.ui.guia.GuiaOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Base de las cinco secciones de la barra de abajo (Inicio, Inventario, Vender,
 * Alertas y Reportes/Caja). Todas viven dentro de {@link PrincipalActivity}: al cambiar
 * de pestana solo se esconde una y se muestra otra, sin abrir pantallas nuevas, por eso
 * el cambio es instantaneo y no se pierde nada (el carrito, lo escrito en un buscador...).
 *
 * Como una seccion escondida no pasa por onResume al volver a mostrarse, lo que deba
 * refrescarse cada vez que el usuario la ve va en {@link #alMostrar()}.
 *
 * Guia: cada seccion dice que partes explicar en {@link #pasosGuia(Pasos)}. El boton
 * "?" (id btnGuia) la abre con puntos, y sale sola la primera vez que se abre la seccion
 * si esta define {@link #claveGuia()}.
 */
public abstract class SeccionFragment extends Fragment {

    protected SeccionFragment(@LayoutRes int layout) {
        super(layout);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View boton = view.findViewById(R.id.btnGuia);
        if (boton != null) boton.setOnClickListener(v -> mostrarGuia());
    }

    /** Atajo para no escribir requireView().findViewById(...) en cada linea. */
    protected <T extends View> T vista(@IdRes int id) {
        return requireView().findViewById(id);
    }

    /** Mensaje corto que se quita con un toque (en lugar de Toast). */
    protected void avisar(CharSequence mensaje) {
        if (getActivity() instanceof PrincipalActivity) {
            ((PrincipalActivity) getActivity()).avisar(mensaje, anclaAvisos());
        } else if (getView() != null) {
            com.changarrito.ui.Avisos.mostrar(getView(), mensaje, null);
        }
    }

    protected void avisar(@StringRes int mensaje) {
        if (isAdded()) avisar(getString(mensaje));
    }

    /** Vista arriba de la cual salen los avisos; null = arriba de la barra de abajo. */
    @Nullable
    protected View anclaAvisos() {
        return null;
    }

    /** Cambia a otra pestana (por ejemplo, los accesos grandes de Inicio). */
    protected void irA(@IdRes int itemNavegacion) {
        if (getActivity() instanceof PrincipalActivity) {
            ((PrincipalActivity) getActivity()).irA(itemNavegacion);
        }
    }

    /**
     * Se llama cada vez que la seccion queda a la vista: la primera vez, al volver a su
     * pestana y al regresar de otra pantalla (Ajustes, escaner...).
     */
    protected void alMostrar() {
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isHidden()) {
            alMostrar();
            guiaPrimeraVez();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isResumed()) {
            alMostrar();
            guiaPrimeraVez();
        }
    }

    // ------------------------------------------------------------------ guia

    /** Lista de partes a explicar; ignora las vistas que no existen. */
    protected final class Pasos {
        final List<GuiaOverlay.Paso> lista = new ArrayList<>();

        public Pasos agregar(@Nullable View objetivo, @DrawableRes int icono, @StringRes int titulo, @StringRes int texto) {
            if (objetivo != null) lista.add(new GuiaOverlay.Paso(objetivo, icono, getText(titulo), getText(texto)));
            return this;
        }
    }

    /** Las partes de esta seccion que explica la guia con puntos. */
    protected void pasosGuia(Pasos p) {
    }

    /** Nombre para recordar que ya se mostro la guia la primera vez; null = no sale sola. */
    @Nullable
    protected String claveGuia() {
        return null;
    }

    /** Boton "?": guia con puntos sobre esta seccion. */
    public void mostrarGuia() {
        if (!isAdded() || getView() == null) return;
        Pasos p = new Pasos();
        pasosGuia(p);
        GuiaOverlay.mostrar(requireActivity(), p.lista, GuiaOverlay.Modo.PUNTOS, null);
    }

    private void guiaPrimeraVez() {
        String clave = claveGuia();
        if (clave == null || getView() == null) return;
        String llave = "guia_" + clave;
        if (Ayuda.yaVista(requireContext(), llave)) return;
        Ayuda.marcarVista(requireContext(), llave);
        // Un momento despues, cuando la seccion ya se dibujo (y la lista ya cargo).
        getView().postDelayed(() -> {
            if (isAdded() && !isHidden()) mostrarGuia();
        }, 600);
    }
}
