package com.changarrito.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.IngresoDia;
import com.changarrito.database.dao.ProductoConTotal;
import com.changarrito.database.dao.VentaDAO;
import com.changarrito.utils.FechaUtil;

import java.util.List;

/**
 * Capa de acceso a datos para la pantalla de Reportes. Es casi un pasa-manos
 * directo a VentaDAO: la unica logica real es calcular los rangos de fecha
 * (hoy, ultimos N dias) con FechaUtil antes de consultar.
 */
public class ReportesRepository {

    private final VentaDAO ventaDAO;

    public ReportesRepository(Application application) {
        ventaDAO = AppDatabase.getInstance(application).ventaDao();
    }

    public LiveData<Double> getIngresosHoy() {
        long[] rango = FechaUtil.rangoDelDia(System.currentTimeMillis());
        return ventaDAO.getTotalVentasPorDia(rango[0], rango[1]);
    }

    public LiveData<Integer> getTransaccionesHoy() {
        long[] rango = FechaUtil.rangoDelDia(System.currentTimeMillis());
        return ventaDAO.getCountVentasEnRango(rango[0], rango[1]);
    }

    public LiveData<ProductoConTotal> getProductoTopHoy() {
        long[] rango = FechaUtil.rangoDelDia(System.currentTimeMillis());
        return ventaDAO.getProductoMasVendidoEnRango(rango[0], rango[1]);
    }

    public LiveData<List<IngresoDia>> getIngresosUltimosDias(int dias) {
        long finMs = System.currentTimeMillis();
        long inicioMs = finMs - ((long) dias * 24 * 60 * 60 * 1000);
        return ventaDAO.getIngresosPorDiaEnRango(inicioMs, finMs);
    }

    public LiveData<List<ProductoConTotal>> getProductosMasVendidos(int top) {
        return ventaDAO.getProductosMasVendidos(top);
    }
}
