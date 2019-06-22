package org.labs.musaka.mindgarden

import androidx.room.Database
import androidx.room.RoomDatabase


@Database( entities = [(PlantModel::class)], version = 3 )
abstract class PlantDatabase : RoomDatabase() {
    abstract fun plantDao() : PlantDAO
}