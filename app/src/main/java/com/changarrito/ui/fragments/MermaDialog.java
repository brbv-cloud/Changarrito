package com.changarrito.ui.fragments;

import android.app.Activity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.changarrito.R;
import com.changarrito.database.entity.MotivoMerma;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UnidadMedida;
import com.changarrito.repository.MermaRepository;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Cuadro para registrar una merma de un producto: cuanto, por que y una nota opcional. */
public final class MermaDialog {

    public interface AlTerminar {
        void onRegistrada();
    }

    private MermaDialog() {
    }

    public static void mostrar(Activity a, ProductoEntity p, AlTerminar alTerminar) {
        View v = LayoutInflater.from(a).inflate(R.layout.dialog_merma, null, false);
        ((TextView) v.findViewById(R.id.tvMermaProducto)).setText(a.getString(R.string.merma_producto,
                p.nombre, MermaRepository.cantidadTexto(p.cantidadDisponible, p.unidadMedida)));
        TextInputLayout tilCantidad = v.findViewById(R.id.tilMermaCantidad);
        TextInputEditText etCantidad = v.findViewById(R.id.etMermaCantidad);
        TextInputEditText etNota = v.findViewById(R.id.etMermaNota);
        ChipGroup chips = v.findViewById(R.id.chipsMotivo);
        TextView tvError = v.findViewById(R.id.tvMermaError);
        boolean porPieza = p.unidadMedida == null || p.unidadMedida == UnidadMedida.PZA;
        etCantidad.setInputType(porPieza ? InputType.TYPE_CLASS_NUMBER
                : InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        tilCantidad.setSuffixText(p.unidadMedida != null ? p.unidadMedida.name() : "");

        AlertDialog dialogo = new MaterialAlertDialogBuilder(a)
                .setTitle(R.string.merma_titulo)
                .setView(v)
                .setPositiveButton(R.string.merma_registrar, null)
                .setNegativeButton(R.string.producto_cancelar, null)
                .create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            tvError.setVisibility(View.GONE);
            double cantidad;
            try {
                String t = etCantidad.getText() == null ? "" : etCantidad.getText().toString().trim().replace(',', '.');
                cantidad = Double.parseDouble(t);
            } catch (NumberFormatException e) {
                tvError.setText(R.string.merma_error_cantidad);
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            MotivoMerma motivo = motivoElegido(chips.getCheckedChipId());
            if (motivo == null) {
                tvError.setText(R.string.merma_error_motivo);
                tvError.setVisibility(View.VISIBLE);
                return;
            }
            String nota = etNota.getText() == null ? null : etNota.getText().toString();
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            new MermaRepository(a.getApplication()).registrar(p.id, cantidad, motivo, nota, new MermaRepository.Callback() {
                @Override
                public void onRegistrada(double quedan) {
                    if (a.isFinishing() || a.isDestroyed()) return;
                    dialogo.dismiss();
                    Toast.makeText(a, a.getString(R.string.merma_listo,
                            MermaRepository.cantidadTexto(quedan, p.unidadMedida)), Toast.LENGTH_LONG).show();
                    if (alTerminar != null) alTerminar.onRegistrada();
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

    private static MotivoMerma motivoElegido(int idChip) {
        if (idChip == R.id.chipDanado) return MotivoMerma.DANADO;
        if (idChip == R.id.chipCaducado) return MotivoMerma.CADUCADO;
        if (idChip == R.id.chipRobo) return MotivoMerma.ROBO;
        if (idChip == R.id.chipOtro) return MotivoMerma.OTRO;
        return null;
    }
}
