package com.changarrito.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.changarrito.database.entity.UsuarioEntity;

import java.util.List;

@Dao
public interface UsuarioDAO {

    @Insert
    long insert(UsuarioEntity usuario);

    @Update
    int update(UsuarioEntity usuario);

    /** Perfiles para el selector de "¿Quién eres?" al empezar turno (solo activos). */
    @Query("SELECT * FROM usuario WHERE activo = 1 ORDER BY rol, nombre")
    LiveData<List<UsuarioEntity>> getUsuariosActivos();

    @Query("SELECT * FROM usuario WHERE activo = 1 ORDER BY rol, nombre")
    List<UsuarioEntity> getUsuariosActivosSync();

    @Query("SELECT * FROM usuario WHERE id = :id")
    UsuarioEntity getUsuarioByIdSync(int id);

    /** El perfil del dueño (hay uno por negocio). Null si todavía no se crea. */
    @Query("SELECT * FROM usuario WHERE rol = 'DUENO' AND activo = 1 LIMIT 1")
    UsuarioEntity getDuenoSync();

    @Query("SELECT COUNT(*) FROM usuario WHERE activo = 1")
    int contarActivosSync();
}
