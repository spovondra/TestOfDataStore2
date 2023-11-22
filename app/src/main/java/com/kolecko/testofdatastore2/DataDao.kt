package com.kolecko.testofdatastore2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertData(dataEntity: DataEntity)

    @Query("SELECT * FROM data_table ORDER BY day ASC")
    suspend fun getAllData(): List<DataEntity>

    @Query("SELECT * FROM data_table WHERE day = :date LIMIT 1")
    suspend fun getDataByDate(date: Int): DataEntity?
}
