package com.changarrito.respaldo;

import com.changarrito.database.entity.BitacoraEntity;
import com.changarrito.database.entity.CierreCajaEntity;
import com.changarrito.database.entity.ConfiguracionNegocioEntity;
import com.changarrito.database.entity.MermaEntity;
import com.changarrito.database.entity.ProductoEntity;
import com.changarrito.database.entity.UsuarioEntity;
import com.changarrito.database.entity.VentaEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * "Foto" completa del negocio: todo lo que se sube a la nube y se recupera.
 * Las alertas no van: se recalculan solas al abrir la app (AlertaRepository).
 */
public class RespaldoCompleto {
    public ConfiguracionNegocioEntity configuracion;
    public List<UsuarioEntity> usuarios = new ArrayList<>();
    public List<ProductoEntity> productos = new ArrayList<>();
    public List<VentaEntity> ventas = new ArrayList<>();
    public List<CierreCajaEntity> cierres = new ArrayList<>();
    public List<BitacoraEntity> bitacora = new ArrayList<>();
    public List<MermaEntity> mermas = new ArrayList<>();

    /** Sin configuracion no hay negocio que restaurar. */
    public boolean estaVacio() {
        return configuracion == null;
    }
}
