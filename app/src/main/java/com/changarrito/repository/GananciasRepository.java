package com.changarrito.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.changarrito.database.AppDatabase;
import com.changarrito.database.dao.CierreCajaDAO;
import com.changarrito.database.dao.CierresPorUsuario;
import com.changarrito.database.dao.GananciaDia;
import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.MermaConUsuario;
import com.changarrito.database.dao.MermaDAO;
import com.changarrito.database.dao.MermaPorMotivo;
import com.changarrito.database.dao.ProductoDAO;
import com.changarrito.database.dao.ProductoHueso;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentaDAO;
import com.changarrito.database.dao.VentasPorCategoria;
import com.changarrito.database.dao.VentasPorHora;
import com.changarrito.database.dao.VentasPorUsuario;

import java.util.List;

/** Consultas de la pantalla de Reportes. Solo la usa el dueno. */
public class GananciasRepository {

    private final VentaDAO ventaDAO;
    private final CierreCajaDAO cierreCajaDAO;
    private final MermaDAO mermaDAO;
    private final ProductoDAO productoDAO;

    public GananciasRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        ventaDAO = db.ventaDao();
        cierreCajaDAO = db.cierreCajaDao();
        mermaDAO = db.mermaDao();
        productoDAO = db.productoDao();
    }

    public LiveData<ResumenGanancia> getResumen(long[] r) {
        return ventaDAO.getResumenGanancia(r[0], r[1]);
    }

    public LiveData<List<GananciaProducto>> getPorProducto(long[] r) {
        return ventaDAO.getGananciaPorProducto(r[0], r[1]);
    }

    public LiveData<List<VentasPorUsuario>> getVentasPorUsuario(long[] r) {
        return ventaDAO.getVentasPorUsuario(r[0], r[1]);
    }

    public LiveData<List<CierresPorUsuario>> getCierresPorUsuario(long[] r) {
        return cierreCajaDAO.getCierresPorUsuario(r[0], r[1]);
    }

    public LiveData<List<GananciaDia>> getPorDia(long[] r) {
        return ventaDAO.getGananciaPorDia(r[0], r[1]);
    }

    public LiveData<List<VentasPorHora>> getPorHora(long[] r) {
        return ventaDAO.getVentasPorHora(r[0], r[1]);
    }

    public LiveData<List<VentasPorCategoria>> getPorCategoria(long[] r) {
        return ventaDAO.getVentasPorCategoria(r[0], r[1]);
    }

    public LiveData<List<MermaPorMotivo>> getMermasPorMotivo(long[] r) {
        return mermaDAO.getPorMotivo(r[0], r[1]);
    }

    public LiveData<List<MermaConUsuario>> getMermas(long[] r) {
        return mermaDAO.getEnRango(r[0], r[1]);
    }

    /** Productos con existencia sin venderse desde antes de limiteMs. */
    public LiveData<List<ProductoHueso>> getHuesos(long limiteMs) {
        return productoDAO.getHuesos(limiteMs);
    }
}
