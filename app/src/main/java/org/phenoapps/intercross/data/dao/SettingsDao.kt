package org.phenoapps.intercross.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.phenoapps.intercross.data.models.Settings

@Dao
interface SettingsDao: BaseDao<Settings> {

    @Query("SELECT * FROM settings LIMIT 1")
    fun getSettings(): LiveData<Settings>
}