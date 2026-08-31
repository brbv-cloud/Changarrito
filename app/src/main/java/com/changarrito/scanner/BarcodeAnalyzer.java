package com.changarrito.scanner;

import android.annotation.SuppressLint;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

public class BarcodeAnalyzer implements ImageAnalysis.Analyzer {

    public interface OnBarcodeDetected {
        void onDetected(String codigo);
    }

    private final BarcodeScanner scanner;
    private final OnBarcodeDetected callback;
    private boolean yaDetectado = false;

    public BarcodeAnalyzer(OnBarcodeDetected callback) {
        this.callback = callback;

        // Formatos típicos de abarrotes: EAN-13, EAN-8, UPC, y QR por si acaso
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_QR_CODE
                )
                .build();
        this.scanner = BarcodeScanning.getClient(options);
    }

    @SuppressLint("UnsafeOptInUsageError")
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (yaDetectado) {
            imageProxy.close();
            return;
        }

        Image mediaImage = imageProxy.getImage();
        if (mediaImage == null) {
            imageProxy.close();
            return;
        }

        InputImage image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.getImageInfo().getRotationDegrees()
        );

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String valor = barcode.getRawValue();
                        if (valor != null && !valor.isEmpty() && !yaDetectado) {
                            yaDetectado = true;   // evita disparar 20 veces por segundo
                            callback.onDetected(valor);
                            break;
                        }
                    }
                })
                .addOnCompleteListener(task -> imageProxy.close());
    }
}