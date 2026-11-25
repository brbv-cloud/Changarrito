package com.changarrito.ui.guia;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.changarrito.R;

/**
 * Bienvenida de la primera vez: "¡Bienvenido a tu changarrito!" con la opcion de ver el
 * recorrido de 1 minuto o dejarlo para despues (siempre esta el boton "?").
 */
public final class Bienvenida {

    private Bienvenida() {
    }

    public static void mostrar(Activity actividad, Runnable alEnsenar) {
        if (actividad == null || actividad.isFinishing()) return;
        View vista = actividad.getLayoutInflater().inflate(R.layout.dialogo_bienvenida, null);
        AlertDialog dialogo = new AlertDialog.Builder(actividad)
                .setView(vista)
                .setCancelable(true)
                .create();
        if (dialogo.getWindow() != null) {
            dialogo.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        vista.findViewById(R.id.btnBienvenidaSi).setOnClickListener(v -> {
            dialogo.dismiss();
            if (alEnsenar != null) alEnsenar.run();
        });
        vista.findViewById(R.id.btnBienvenidaNo).setOnClickListener(v -> dialogo.dismiss());
        dialogo.show();
    }
}
