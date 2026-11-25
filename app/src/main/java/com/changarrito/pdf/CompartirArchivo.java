package com.changarrito.pdf;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/** Ver, compartir (WhatsApp, correo...) o guardar en Descargas un PDF generado. */
public final class CompartirArchivo {

    private CompartirArchivo() {
    }

    private static Uri uri(Activity a, File f) {
        return FileProvider.getUriForFile(a, a.getPackageName() + ".archivos", f);
    }

    public static void compartir(Activity a, File f, String asunto) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_STREAM, uri(a, f));
        i.putExtra(Intent.EXTRA_SUBJECT, asunto);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        a.startActivity(Intent.createChooser(i, asunto));
    }

    /** @return false si no hay ninguna app para abrir PDFs. */
    public static boolean ver(Activity a, File f) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri(a, f), "application/pdf");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            a.startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            return false;
        }
    }

    /**
     * Copia el PDF a Descargas/Changarrito (Android 10 o mas nuevo, sin pedir permisos).
     * @return false en Android 9 o anterior, o si fallo la copia.
     */
    public static boolean guardarEnDescargas(Activity a, File f) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false;
        ContentResolver cr = a.getContentResolver();
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, f.getName());
        v.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Changarrito");
        Uri destino = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (destino == null) return false;
        try (InputStream in = new FileInputStream(f); OutputStream out = cr.openOutputStream(destino)) {
            if (out == null) return false;
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return true;
        } catch (Exception e) {
            cr.delete(destino, null, null);
            return false;
        }
    }
}
