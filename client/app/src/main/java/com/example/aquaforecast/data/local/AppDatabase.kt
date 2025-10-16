package com.example.aquaforecast.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.aquaforecast.data.local.dao.FarmDataDao
import com.example.aquaforecast.data.local.dao.PondDao
import com.example.aquaforecast.data.local.dao.PredictionDao
import com.example.aquaforecast.data.local.entity.FarmDataEntity
import com.example.aquaforecast.data.local.entity.PondEntity
import com.example.aquaforecast.data.local.entity.PredictionEntity

@Database(
    entities = [
    FarmDataEntity::class,
    PondEntity::class,
    PredictionEntity::class
               ],
    version = 1,
    exportSchema = false
)
//@TypeConverters(Converters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun farmDataDao(): FarmDataDao
    abstract fun pondDao(): PondDao
    abstract fun predictionDao(): PredictionDao
}
