package com.changarrito.ui.activities.negocio;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.changarrito.R;
import com.changarrito.database.entity.GiroNegocio;
import com.changarrito.database.entity.Rol;
import com.changarrito.database.entity.UsuarioEntity;

/** Íconos, descripciones y renglones compartidos por las pantallas del negocio. */
final class GiroUi {

    private GiroUi() {
    }

    static int icono(GiroNegocio giro) {
        switch (giro) {
            case ABARROTES: return R.drawable.ic_giro_abarrotes;
            case PAPELERIA: return R.drawable.ic_giro_papeleria;
            case FERRETERIA: return R.drawable.ic_giro_ferreteria;
            case FARMACIA: return R.drawable.ic_giro_farmacia;
            default: return R.drawable.ic_giro_independiente;
        }
    }

    static int descripcion(GiroNegocio giro) {
        switch (giro) {
            case ABARROTES: return R.string.giro_desc_abarrotes;
            case PAPELERIA: return R.string.giro_desc_papeleria;
            case FERRETERIA: return R.string.giro_desc_ferreteria;
            case FARMACIA: return R.string.giro_desc_farmacia;
            default: return R.string.giro_desc_independiente;
        }
    }

    static String inicial(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return "?";
        return nombre.trim().substring(0, 1).toUpperCase();
    }

    /** Infla un renglón item_perfil con la inicial, el nombre y el rol del usuario. */
    static View renglonPerfil(LayoutInflater inflater, ViewGroup padre, UsuarioEntity u) {
        View v = inflater.inflate(R.layout.item_perfil, padre, false);
        ((TextView) v.findViewById(R.id.tvInicial)).setText(inicial(u.nombre));
        ((TextView) v.findViewById(R.id.tvNombre)).setText(u.nombre);
        ((TextView) v.findViewById(R.id.tvRol)).setText(
                u.rol == Rol.DUENO ? R.string.rol_dueno : R.string.rol_trabajador);
        return v;
    }
}
