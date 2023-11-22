package com.kolecko.testofdatastore2

class MainController(private val repository: DataRepository) {

    suspend fun insertOrUpdateData(date: String, value: Double) {
        // Převod stringu na int (mělo by být nějakým vhodným způsobem implementováno)
        val day = date.hashCode()

        val existingData = repository.getDataByDate(day)

        if (existingData != null) {
            // Update existing data
            val updatedData = existingData.copy(value = value)
            repository.insertData(updatedData)
        } else {
            // Insert new data
            val newData = DataEntity(day = day, value = value)
            repository.insertData(newData)
        }


    }
    suspend fun getDataByDate(date: String): DataEntity? {
        val day = date.hashCode()
        return repository.getDataByDate(day)
    }
}