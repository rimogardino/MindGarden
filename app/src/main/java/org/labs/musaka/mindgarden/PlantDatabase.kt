package org.labs.musaka.mindgarden

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database ( entities = [(PlantModel::class)], version = 3 )
abstract class PlantDatabase : RoomDatabase() {
    abstract fun plantDao() : PlantDAO
}