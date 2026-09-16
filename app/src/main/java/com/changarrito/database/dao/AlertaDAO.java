package com.changarrito.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.changarrito.database.entity.AlertaEntity;
import com.changarrito.database.entity.EstadoAlerta;
import com.changarrito.database.entity.TipoAlerta;

import java.util.List;

@Dao
public interface AlertaDAO {

    @Insert
    long insert(AlertaEntity alerta);

    @Delete
    int delete(AlertaEntity alerta);

    @Query("SELECT * FROM alerta ORDER BY timestampCreacionMs DESC")
    LiveData<List<AlertaEntity>> getAllAlertas();

    @Query("SELECT * FROM alerta WHERE estado = :estado ORDER BY timestampCreacionMs DESC")
    LiveData<List<AlertaEntity>> getAlertasPorEstado(EstadoAlerta estado);

    @Query("SELECT COUNT(*) FROM alerta WHERE estado = :estado")
    LiveData<Integer> contarPorEstado(EstadoAlerta estado);

    /**
     * Sincrono, para usarse dentro de AlertaRepository.verificarAlertasSync() en un
     * hilo de fondo (WorkManager o el ExecutorService del repository). Evita crear
     * una alerta duplicada si ya existe una activa del mismo tipo para el mismo
     * producto. Nunca llamar desde el hilo principal.
     */
    @Query("SELECT * FROM alerta WHERE idProducto = :idProducto AND tipoAlerta = :tipo AND estado = :estado LIMIT 1")
    AlertaEntity getAlertaExistenteSync(int idProducto, TipoAlerta tipo, EstadoAlerta estado);

    @Query("UPDATE alerta SET estado = :nuevoEstado, timestampResolucionMs = :ahora WHERE id = :idAlerta")
    int actualizarEstado(int idAlerta, EstadoAlerta nuevoEstado, long ahora);

    @Query("SELECT alerta.id AS id, alerta.idProducto AS idProducto, alerta.tipoAlerta AS tipoAlerta, "
            + "alerta.estado AS estado, alerta.timestampCreacionMs AS timestampCreacionMs, "
            + "producto.nombre AS nombreProducto, producto.cantidadDisponible AS cantidadDisponible, "
            + "producto.unidadMedida AS unidadMedida, producto.fechaCaducidadMs AS fechaCaducidadMs, "
            + "producto.telefonoProveedor AS telefonoProveedor "
            + "FROM alerta INNER JOIN producto ON alerta.idProducto = producto.id "
            + "WHERE alerta.estado = :estado ORDER BY alerta.timestampCreacionMs DESC")
    LiveData<List<AlertaConProducto>> getAlertasConProducto(EstadoAlerta estado);
}
