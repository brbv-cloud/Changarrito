package com.changarrito.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.changarrito.database.entity.ConfiguracionNegocioEntity;

@Dao
public interface ConfiguracionNegocioDAO {

    /** Inserta o reemplaza la única fila de configuración. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(ConfiguracionNegocioEntity configuracion);

    @Query("SELECT * FROM configuracion_negocio WHERE id = 1")
    LiveData<ConfiguracionNegocioEntity> getConfiguracion();

    /** Null si el negocio todavía no eligió su giro (primer arranque). */
    @Query("SELECT * FROM configuracion_negocio WHERE id = 1")
    ConfiguracionNegocioEntity getConfiguracionSync();
}
