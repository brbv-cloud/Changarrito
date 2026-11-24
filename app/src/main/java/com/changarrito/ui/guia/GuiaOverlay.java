package com.changarrito.ui.guia;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.changarrito.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Guia visual que se pone ENCIMA de la pantalla (sin cambiar sus layouts).
 *
 * Dos formas:
 *  - PUNTOS (boton "?"): se oscurece todo y aparecen puntos naranjas que laten sobre cada
 *    parte. Al tocar uno, esa parte queda iluminada y sale una tarjeta con encabezado
 *    amarillo que la explica; "Siguiente" recorre las demas en orden.
 *  - RECORRIDO (primera vez, desde la bienvenida): "reflector" paso por paso
 *    ("PASO 1 DE 6"), sin puntos: todo oscuro menos la parte que se explica.
 *
 * El oscurecido se dibuja con un hueco redondeado (Path EVEN_ODD) alrededor de la vista
 * que se explica. Las posiciones se leen en el momento de la vista real, asi que funciona
 * en cualquier tamano de pantalla.
 */
public class GuiaOverlay extends FrameLayout {

    public enum Modo { PUNTOS, RECORRIDO }

    /** Una parte de la pantalla a explicar. */
    public static final class Paso {
        final View objetivo;
        @DrawableRes
        final int icono;
        final CharSequence titulo;
        final CharSequence texto;

        public Paso(View objetivo, @DrawableRes int icono, CharSequence titulo, CharSequence texto) {
            this.objetivo = objetivo;
            this.icono = icono;
            this.titulo = titulo;
            this.texto = texto;
        }
    }

    private static final String ETIQUETA = "guia_overlay";

    private final List<Paso> pasos = new ArrayList<>();
    private final Modo modo;
    @Nullable
    private final Runnable alTerminar;
    private final float dp;

    private final Paint pintaOscuro = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaBorde = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaPunto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaAro = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaCentro = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path camino = new Path();
    private final RectF hueco = new RectF();
    private final int[] posicion = new int[2];
    private final int[] posicionMia = new int[2];

    private boolean hayHueco;
    private int actual = -1;
    private float pulso;
    private boolean terminado;
    private ValueAnimator latido;
    private OnBackPressedCallback atras;

    private final View tarjeta;
    private final LinearLayout pista;
    private final TextView btnSalir;

    /** Cierra la guia abierta, si hay (por ejemplo, al cambiar de seccion). */
    public static void cerrar(@Nullable Activity actividad) {
        if (actividad == null) return;
        ViewGroup raiz = actividad.findViewById(android.R.id.content);
        View abierta = raiz != null ? raiz.findViewWithTag(ETIQUETA) : null;
        if (abierta instanceof GuiaOverlay) ((GuiaOverlay) abierta).terminar();
    }

    /** Muestra la guia sobre la Activity (si ya hay una abierta, no hace nada). */
    public static void mostrar(Activity actividad, List<Paso> pasos, Modo modo, @Nullable Runnable alTerminar) {
        if (actividad == null || pasos == null || pasos.isEmpty()) return;
        ViewGroup raiz = actividad.findViewById(android.R.id.content);
        if (raiz == null || raiz.findViewWithTag(ETIQUETA) != null) return;
        GuiaOverlay guia = new GuiaOverlay(actividad, pasos, modo, alTerminar);
        guia.setTag(ETIQUETA);
        raiz.addView(guia, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        guia.setAlpha(0f);
        guia.animate().alpha(1f).setDuration(180).start();
        guia.post(guia::arrancar);
    }

    @SuppressLint("ClickableViewAccessibility")
    private GuiaOverlay(Activity actividad, List<Paso> lista, Modo modo, @Nullable Runnable alTerminar) {
        super(actividad);
        this.modo = modo;
        this.alTerminar = alTerminar;
        this.pasos.addAll(lista);
        dp = getResources().getDisplayMetrics().density;
        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
        setElevation(64 * dp);
        setOutlineProvider(null);

        Context c = actividad;
        pintaOscuro.setColor(ContextCompat.getColor(c, R.color.guia_oscuro));
        pintaBorde.setStyle(Paint.Style.STROKE);
        pintaBorde.setStrokeWidth(3 * dp);
        pintaBorde.setColor(ContextCompat.getColor(c, R.color.guia_amarillo));
        int ambar = ContextCompat.getColor(c, R.color.marca_ambar);
        pintaPunto.setColor(ambar);
        pintaAro.setColor(ambar);
        pintaCentro.setColor(ContextCompat.getColor(c, R.color.white));

        tarjeta = LayoutInflater.from(c).inflate(R.layout.guia_tarjeta, this, false);
        tarjeta.setVisibility(GONE);
        addView(tarjeta);

        // Pista de abajo (modo puntos): "Toca los puntos…" y "o haz el recorrido completo ›".
        pista = new LinearLayout(c);
        pista.setOrientation(LinearLayout.VERTICAL);
        pista.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView tvPista = textoBlanco(c, R.string.guia_pista, 15);
        TextView tvRecorrido = textoBlanco(c, R.string.guia_recorrido, 15);
        tvRecorrido.setPaintFlags(tvRecorrido.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        tvRecorrido.setMinHeight((int) (44 * dp));
        tvRecorrido.setGravity(Gravity.CENTER);
        tvRecorrido.setOnClickListener(v -> abrir(0));
        pista.addView(tvPista);
        pista.addView(tvRecorrido);
        addView(pista, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        btnSalir = textoBlanco(c, R.string.guia_salir, 14);
        btnSalir.setBackgroundResource(R.drawable.bg_salir_guia);
        int px = (int) (14 * dp);
        btnSalir.setPadding(px, (int) (8 * dp), px, (int) (8 * dp));
        btnSalir.setOnClickListener(v -> terminar());
        addView(btnSalir, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END));
        boolean conPuntos = modo == Modo.PUNTOS;
        pista.setVisibility(conPuntos ? VISIBLE : GONE);
        btnSalir.setVisibility(conPuntos ? VISIBLE : GONE);

        if (actividad instanceof ComponentActivity) {
            atras = new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (GuiaOverlay.this.modo == Modo.PUNTOS && actual >= 0) mostrarPuntos();
                    else terminar();
                }
            };
            ((ComponentActivity) actividad).getOnBackPressedDispatcher().addCallback(atras);
        }
    }

    private TextView textoBlanco(Context c, int texto, int sp) {
        TextView tv = new TextView(c);
        tv.setText(texto);
        tv.setTextColor(ContextCompat.getColor(c, R.color.white));
        tv.setTextSize(sp);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    // ------------------------------------------------------------------ arranque

    private void arrancar() {
        acomodarMargenes();
        List<Paso> visibles = new ArrayList<>();
        for (Paso p : pasos) {
            if (esVisible(p.objetivo, modo == Modo.PUNTOS)) visibles.add(p);
        }
        pasos.clear();
        pasos.addAll(visibles);
        if (pasos.isEmpty()) {
            terminar();
            return;
        }
        if (modo == Modo.RECORRIDO) abrir(0);
        else mostrarPuntos();
    }

    /** Respeta la barra de estado y la de navegacion (edge-to-edge). */
    private void acomodarMargenes() {
        WindowInsetsCompat ins = ViewCompat.getRootWindowInsets(this);
        Insets barras = ins != null ? ins.getInsets(WindowInsetsCompat.Type.systemBars()) : Insets.NONE;
        FrameLayout.LayoutParams lpSalir = (FrameLayout.LayoutParams) btnSalir.getLayoutParams();
        lpSalir.topMargin = barras.top + (int) (10 * dp);
        lpSalir.rightMargin = (int) (12 * dp);
        btnSalir.setLayoutParams(lpSalir);
        FrameLayout.LayoutParams lpPista = (FrameLayout.LayoutParams) pista.getLayoutParams();
        lpPista.bottomMargin = barras.bottom + (int) (78 * dp);
        lpPista.leftMargin = (int) (24 * dp);
        lpPista.rightMargin = (int) (24 * dp);
        pista.setLayoutParams(lpPista);
    }

    private boolean esVisible(@Nullable View v, boolean exigirEnPantalla) {
        if (v == null || !v.isShown() || v.getWidth() == 0 || v.getHeight() == 0) return false;
        if (!exigirEnPantalla) return true;
        RectF r = rectDe(v);
        return r.bottom > 0 && r.top < getHeight() && r.right > 0 && r.left < getWidth()
                && r.centerY() > 0 && r.centerY() < getHeight();
    }

    private RectF rectDe(View v) {
        v.getLocationInWindow(posicion);
        getLocationInWindow(posicionMia);
        float x = posicion[0] - posicionMia[0];
        float y = posicion[1] - posicionMia[1];
        return new RectF(x, y, x + v.getWidth(), y + v.getHeight());
    }

    // ------------------------------------------------------------------ modo puntos

    private void mostrarPuntos() {
        actual = -1;
        hayHueco = false;
        tarjeta.setVisibility(GONE);
        pista.setVisibility(VISIBLE);
        if (latido == null) {
            latido = ValueAnimator.ofFloat(0f, 1f);
            latido.setDuration(1300);
            latido.setRepeatCount(ValueAnimator.INFINITE);
            latido.setInterpolator(new DecelerateInterpolator());
            latido.addUpdateListener(a -> {
                pulso = (float) a.getAnimatedValue();
                invalidate();
            });
        }
        if (!latido.isStarted()) latido.start();
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            performClick();
            if (modo == Modo.PUNTOS && actual < 0) {
                int i = puntoTocado(e.getX(), e.getY());
                if (i >= 0) abrir(i);
            }
        }
        return true; // la pantalla de abajo no recibe toques mientras esta la guia
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int puntoTocado(float x, float y) {
        float radio = 40 * dp;
        int mejor = -1;
        float mejorDist = Float.MAX_VALUE;
        for (int i = 0; i < pasos.size(); i++) {
            RectF r = rectDe(pasos.get(i).objetivo);
            float d = (float) Math.hypot(r.centerX() - x, r.centerY() - y);
            if (d < radio && d < mejorDist) {
                mejor = i;
                mejorDist = d;
            }
        }
        if (mejor >= 0) return mejor;
        // Tocar en cualquier parte de la zona tambien la abre.
        for (int i = 0; i < pasos.size(); i++) {
            if (rectDe(pasos.get(i).objetivo).contains(x, y)) return i;
        }
        return -1;
    }

    // ------------------------------------------------------------------ explicar un paso

    private void abrir(int i) {
        if (i < 0 || i >= pasos.size()) return;
        actual = i;
        pista.setVisibility(GONE);
        View objetivo = pasos.get(i).objetivo;
        RectF r = rectDe(objetivo);
        boolean fuera = r.top < 0 || r.bottom > getHeight();
        if (fuera) {
            // Esta abajo (o arriba) en la pantalla con scroll: se trae a la vista primero.
            objetivo.requestRectangleOnScreen(new Rect(0, 0, objetivo.getWidth(), objetivo.getHeight()), true);
            postDelayed(() -> colocar(i), 260);
        } else {
            colocar(i);
        }
    }

    private void colocar(int i) {
        if (terminado || i != actual) return;
        Paso p = pasos.get(i);
        if (!esVisible(p.objetivo, false)) {
            // Lo que se iba a explicar ya no esta en pantalla (se cambio de seccion).
            terminar();
            return;
        }
        hueco.set(rectDe(p.objetivo));
        float margen = 6 * dp;
        hueco.inset(-margen, -margen);
        hayHueco = true;
        llenarTarjeta(i);
        posicionarTarjeta();
        invalidate();
    }

    private void llenarTarjeta(int i) {
        Paso p = pasos.get(i);
        int total = pasos.size();
        boolean ultimo = i == total - 1;
        boolean conPuntos = modo == Modo.PUNTOS;

        tarjeta.findViewById(R.id.guiaEncabezado).setVisibility(conPuntos ? VISIBLE : GONE);
        ((ImageView) tarjeta.findViewById(R.id.guiaIcono)).setImageResource(p.icono);
        TextView tvPaso = tarjeta.findViewById(R.id.guiaPaso);
        tvPaso.setVisibility(conPuntos ? GONE : VISIBLE);
        tvPaso.setText(getContext().getString(R.string.guia_paso, i + 1, total));
        ((TextView) tarjeta.findViewById(R.id.guiaTitulo)).setText(p.titulo);
        ((TextView) tarjeta.findViewById(R.id.guiaTexto)).setText(p.texto);
        TextView contador = tarjeta.findViewById(R.id.guiaContador);
        contador.setVisibility(conPuntos ? VISIBLE : GONE);
        contador.setText(getContext().getString(R.string.guia_contador, i + 1, total));

        MaterialButton izq = tarjeta.findViewById(R.id.guiaIzquierda);
        MaterialButton der = tarjeta.findViewById(R.id.guiaDerecha);
        izq.setText(conPuntos ? R.string.guia_cerrar : R.string.guia_saltar);
        izq.setOnClickListener(v -> {
            if (conPuntos) mostrarPuntos();
            else terminar();
        });
        der.setText(!ultimo ? R.string.guia_siguiente : conPuntos ? R.string.guia_entendido : R.string.guia_listo);
        der.setOnClickListener(v -> {
            if (!ultimo) abrir(i + 1);
            else if (conPuntos) mostrarPuntos();
            else terminar();
        });
    }

    /** Debajo de la parte iluminada si cabe; si no, arriba; si tampoco, abajo de la pantalla. */
    private void posicionarTarjeta() {
        WindowInsetsCompat ins = ViewCompat.getRootWindowInsets(this);
        Insets barras = ins != null ? ins.getInsets(WindowInsetsCompat.Type.systemBars()) : Insets.NONE;
        int lado = (int) (18 * dp);
        int separacion = (int) (12 * dp);
        int arriba = barras.top + (int) (8 * dp);
        int abajo = getHeight() - barras.bottom - (int) (8 * dp);
        int ancho = getWidth() - 2 * lado;

        tarjeta.measure(MeasureSpec.makeMeasureSpec(ancho, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));
        int alto = tarjeta.getMeasuredHeight();
        float y;
        if (hueco.bottom + separacion + alto <= abajo) y = hueco.bottom + separacion;
        else if (hueco.top - separacion - alto >= arriba) y = hueco.top - separacion - alto;
        else y = Math.max(arriba, abajo - alto);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tarjeta.getLayoutParams();
        lp.width = ancho;
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = lado;
        lp.topMargin = (int) y;
        tarjeta.setLayoutParams(lp);
        tarjeta.setVisibility(VISIBLE);
        tarjeta.setAlpha(0f);
        tarjeta.setScaleX(0.96f);
        tarjeta.setScaleY(0.96f);
        tarjeta.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(170).start();
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        float radio = 18 * dp;
        camino.reset();
        camino.setFillType(Path.FillType.EVEN_ODD);
        camino.addRect(0, 0, w, h, Path.Direction.CW);
        if (hayHueco) camino.addRoundRect(hueco, radio, radio, Path.Direction.CW);
        canvas.drawPath(camino, pintaOscuro);
        if (hayHueco) canvas.drawRoundRect(hueco, radio, radio, pintaBorde);

        if (modo == Modo.PUNTOS && actual < 0) {
            float r0 = 14 * dp;
            for (Paso p : pasos) {
                RectF r = rectDe(p.objetivo);
                float cx = r.centerX();
                float cy = r.centerY();
                pintaAro.setAlpha((int) (120 * (1f - pulso)));
                canvas.drawCircle(cx, cy, r0 + r0 * 1.2f * pulso, pintaAro);
                canvas.drawCircle(cx, cy, r0, pintaPunto);
                canvas.drawCircle(cx, cy, r0 * 0.42f, pintaCentro);
            }
        }
    }

    // ------------------------------------------------------------------ cierre

    private void terminar() {
        if (terminado) return;
        terminado = true;
        if (latido != null) latido.cancel();
        if (atras != null) atras.remove();
        animate().alpha(0f).setDuration(160).withEndAction(() -> {
            ViewGroup padre = (ViewGroup) getParent();
            if (padre != null) padre.removeView(this);
        }).start();
        if (alTerminar != null) alTerminar.run();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (latido != null) latido.cancel();
        if (atras != null) atras.remove();
    }
}
