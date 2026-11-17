package com.changarrito.ui.secciones;

import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.changarrito.ui.activities.PrincipalActivity;

/**
 * Base de las cinco secciones de la barra de abajo (Inicio, Inventario, Vender,
 * Alertas y Reportes/Caja). Todas viven dentro de {@link PrincipalActivity}: al cambiar
 * de pestana solo se esconde una y se muestra otra, sin abrir pantallas nuevas, por eso
 * el cambio es instantaneo y no se pierde nada (el carrito, lo escrito en un buscador...).
 *
 * Como una seccion escondida no pasa por onResume al volver a mostrarse, lo que deba
 * refrescarse cada vez que el usuario la ve va en {@link #alMostrar()}.
 */
public abstract class SeccionFragment extends Fragment {

    protected SeccionFragment(@LayoutRes int layout) {
        super(layout);
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
        if (!isHidden()) alMostrar();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isResumed()) alMostrar();
    }
}
