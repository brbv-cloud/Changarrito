package com.changarrito.ui.fragments;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.changarrito.R;
import com.changarrito.database.entity.TipoGasto;
import com.changarrito.negocio.SesionTurno;
import com.changarrito.reportes.CalculoGanancias;
import com.changarrito.repository.GastoRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * "Registrar gasto": de que es, cuanto, una nota y si salio de la caja.
 * Dueno: todos los tipos, puede elegir la fecha (por ejemplo, la luz que pago el lunes).
 * Trabajador: solo lo que saca de la caja en ese momento, sin renta ni sueldos.
 */
public final class GastoDialog {

    private GastoDialog() {
    }

    public static void mostrar(Activity a, @Nullable Runnable alTerminar) {
        if (a == null || a.isFinishing()) return;
        boolean esDueno = SesionTurno.esDueno(a);
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_gasto, null, false);
        ChipGroup chips = v.findViewById(R.id.chipsTipoGasto);
        TextInputEditText etMonto = v.findViewById(R.id.etGastoMonto);
        TextInputEditText etNota = v.findViewById(R.id.etGastoNota);
        MaterialCheckBox cbCaja = v.findViewById(R.id.cbGastoCaja);
        TextView tvCajaAyuda = v.findViewById(R.id.tvGastoCajaAyuda);
        MaterialButton btnFecha = v.findViewById(R.id.btnGastoFecha);
        TextView tvError = v.findViewById(R.id.tvGastoError);

        LayoutInflater inflater = LayoutInflater.from(a);
        for (TipoGasto t : TipoGasto.disponibles(esDueno)) {
            Chip chip = (Chip) inflater.inflate(R.layout.chip_tipo_gasto, chips, false);
            chip.setId(View.generateViewId());
            chip.setText(t.nombreVisible);
            chip.setTag(t);
            chips.addView(chip);
        }

        // Fecha: 0 = hoy, ahora mismo.
        long[] fecha = {0};
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("es-MX"));
        Runnable mostrarFecha = () -> btnFecha.setText(fecha[0] == 0
                ? a.getString(R.string.gasto_fecha_hoy)
                : a.getString(R.string.gasto_fecha, fmt.format(new Date(fecha[0]))));
        mostrarFecha.run();
        btnFecha.setOnClickListener(x -> {
            Calendar c = Calendar.getInstance();
            if (fecha[0] != 0) c.setTimeInMillis(fecha[0]);
            DatePickerDialog dp = new DatePickerDialog(a, (picker, anio, mes, dia) -> {
                Calendar elegido = Calendar.getInstance();
                elegido.set(anio, mes, dia, 12, 0, 0);
                elegido.set(Calendar.MILLISECOND, 0);
                Calendar hoy = Calendar.getInstance();
                boolean esHoy = elegido.get(Calendar.YEAR) == hoy.get(Calendar.YEAR)
                        && elegido.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR);
                fecha[0] = esHoy ? 0 : elegido.getTimeInMillis();
                mostrarFecha.run();
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            dp.getDatePicker().setMaxDate(System.currentTimeMillis());
            dp.show();
        });

        if (esDueno) {
            // Lo que sale del cajon es de este momento: no se le cambia la fecha.
            cbCaja.setOnCheckedChangeListener((b, marcado) -> {
                tvCajaAyuda.setVisibility(marcado ? View.VISIBLE : View.GONE);
                btnFecha.setVisibility(marcado ? View.GONE : View.VISIBLE);
                if (marcado) {
                    fecha[0] = 0;
                    mostrarFecha.run();
                }
            });
            tvCajaAyuda.setVisibility(View.GONE);
        } else {
            cbCaja.setVisibility(View.GONE);
            btnFecha.setVisibility(View.GONE);
            tvCajaAyuda.setText(R.string.gasto_caja_trabajador);
            tvCajaAyuda.setVisibility(View.VISIBLE);
        }

        AlertDialog dialogo = new MaterialAlertDialogBuilder(a)
                .setTitle(esDueno ? R.string.gasto_titulo : R.string.gasto_titulo_trabajador)
                .setView(v)
                .setPositiveButton(R.string.gasto_registrar, null)
                .setNegativeButton(R.string.producto_cancelar, null)
                .create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            tvError.setVisibility(View.GONE);
            View marcado = chips.findViewById(chips.getCheckedChipId());
            TipoGasto tipo = marcado != null ? (TipoGasto) marcado.getTag() : null;
            if (tipo == null) {
                error(tvError, R.string.gasto_error_tipo);
                return;
            }
            double monto;
            try {
                String t = etMonto.getText() == null ? "" : etMonto.getText().toString()
                        .replace("$", "").replace(" ", "").replace(',', '.').trim();
                monto = Double.parseDouble(t);
            } catch (NumberFormatException e) {
                error(tvError, R.string.gasto_error_monto);
                return;
            }
            if (!(monto > 0)) {
                error(tvError, R.string.gasto_error_monto);
                return;
            }
            String nota = etNota.getText() == null ? null : etNota.getText().toString();
            boolean conCaja = !esDueno || cbCaja.isChecked();
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            double m = monto;
            new GastoRepository(a.getApplication()).registrar(tipo, monto, nota, conCaja, fecha[0],
                    new GastoRepository.Callback() {
                        @Override
                        public void onListo() {
                            if (a.isFinishing() || a.isDestroyed()) return;
                            dialogo.dismiss();
                            Toast.makeText(a, a.getString(R.string.gasto_listo, tipo.nombreVisible,
                                    CalculoGanancias.dinero(m)), Toast.LENGTH_SHORT).show();
                            if (alTerminar != null) alTerminar.run();
                        }

                        @Override
                        public void onError(String mensaje) {
                            if (a.isFinishing() || a.isDestroyed()) return;
                            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            tvError.setText(mensaje);
                            tvError.setVisibility(View.VISIBLE);
                        }
                    });
        }));
        dialogo.show();
    }

    private static void error(TextView tv, int texto) {
        tv.setText(texto);
        tv.setVisibility(View.VISIBLE);
    }
}
