package com.effekt3r.mrd.core.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
internal interface MenuDao {
    @Query("SELECT * FROM menus WHERE restaurant_id = :restaurantId")
    suspend fun menu(restaurantId: String): MenuEntity?

    @Upsert
    suspend fun upsert(menu: MenuEntity)

    @Query("DELETE FROM menus WHERE restaurant_id = :restaurantId")
    suspend fun delete(restaurantId: String)
}
