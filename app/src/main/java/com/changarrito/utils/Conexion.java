package com.changarrito.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/** ¿Hay internet de verdad ahora? (no solo WiFi conectado: que ya se haya comprobado la salida). */
public final class Conexion {

    private Conexion() {
    }

    public static boolean hayInternet(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return true; // no se puede saber: que lo intente
        Network red = cm.getActiveNetwork();
        if (red == null) return false;
        NetworkCapabilities cap = cm.getNetworkCapabilities(red);
        return cap != null
                && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
