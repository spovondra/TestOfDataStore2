package com.kolecko.testofdatastore2

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainController(private val repository: DataRepository) {

    suspend fun insertOrUpdateData(date: String, value: Double) {
        val day = date.hashCode()
        val formattedDate = SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())

        val existingData = repository.getDataByDate(day)

        if (existingData != null) {
            // Update existing data
            val updatedData = existingData.copy(value = value, formattedDate = formattedDate)
            repository.insertData(updatedData)
        } else {
            // Insert new data
            val newData = DataEntity(day = day, value = value, formattedDate = formattedDate)
            repository.insertData(newData)
        }
    }
    suspend fun getDataByDate(date: String): DataEntity? {
        val day = date.hashCode()
        return repository.getDataByDate(day)
    }
}