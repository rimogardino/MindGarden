package org.labs.musaka.mindgarden

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database ( entities = arrayOf(PlantModel::class), version = 2 )
abstract class PlantDatabase : RoomDatabase() {
    abstract fun plantDao() : PlantDAO
}