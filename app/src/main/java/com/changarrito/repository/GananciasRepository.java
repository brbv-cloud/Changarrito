package com.changarrito.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.CierreCajaDAO;
import com.changarrito.database.dao.CierresPorUsuario;
import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentaDAO;
import com.changarrito.database.dao.VentasPorUsuario;

import java.util.List;

/** Consultas del reporte de Ganancias. Solo lo usa el dueno. */
public class GananciasRepository {

    private final VentaDAO ventaDAO;
    private final CierreCajaDAO cierreCajaDAO;

    public GananciasRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        ventaDAO = db.ventaDao();
        cierreCajaDAO = db.cierreCajaDao();
    }

    public LiveData<ResumenGanancia> getResumen(long[] rango) {
        return ventaDAO.getResumenGanancia(rango[0], rango[1]);
    }

    public LiveData<List<GananciaProducto>> getPorProducto(long[] rango) {
        return ventaDAO.getGananciaPorProducto(rango[0], rango[1]);
    }

    public LiveData<List<VentasPorUsuario>> getVentasPorUsuario(long[] rango) {
        return ventaDAO.getVentasPorUsuario(rango[0], rango[1]);
    }

    public LiveData<List<CierresPorUsuario>> getCierresPorUsuario(long[] rango) {
        return cierreCajaDAO.getCierresPorUsuario(rango[0], rango[1]);
    }
}
