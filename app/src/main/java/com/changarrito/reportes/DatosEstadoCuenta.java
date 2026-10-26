package com.changarrito.reportes;

import com.changarrito.database.dao.CierreConUsuario;
import com.changarrito.database.dao.GananciaProducto;
import com.changarrito.database.dao.MermaConUsuario;
import com.changarrito.database.dao.MermaPorMotivo;
import com.changarrito.database.dao.ProductoHueso;
import com.changarrito.database.dao.ResumenGanancia;
import com.changarrito.database.dao.VentasPorCategoria;
import com.changarrito.database.dao.VentasPorHora;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;

import java.util.ArrayList;
import java.util.List;

/** Todo lo que lleva el PDF "Estado de cuenta" de un periodo. */
public class DatosEstadoCuenta {
    public ConfiguracionNegocioEntity configuracion;
    public RangoReporte rango;
    public long inicioMs;
    public long finMs;
    public long generadoMs;
    public ResumenGanancia resumen = new ResumenGanancia();
    public List<DiaReporte> dias = new ArrayList<>();
    public List<GananciaProducto> productos = new ArrayList<>();
    public List<CalculoGanancias.FilaPersona> personas = new ArrayList<>();
    public List<CierreConUsuario> cierres = new ArrayList<>();
    public List<VentasPorHora> porHora = new ArrayList<>();
    public List<VentasPorCategoria> porCategoria = new ArrayList<>();
    /** Productos con existencia sin venderse en mas de DIAS_HUESO dias (al momento de generar). */
    public List<ProductoHueso> huesos = new ArrayList<>();
    public List<MermaPorMotivo> mermasPorMotivo = new ArrayList<>();
    public List<MermaConUsuario> mermas = new ArrayList<>();

    public static final int DIAS_HUESO = 30;
}
