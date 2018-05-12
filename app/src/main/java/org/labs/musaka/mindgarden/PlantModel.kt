package org.labs.musaka.mindgarden

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity (tableName = "tb_almanac")
data class PlantModel (

                       var pType: Int,
                       var pHealth: Int = 3,
                       var pWidth: Int,
                       var pHeight: Int,
                       var xPos: Int,
                       var yPos: Int,
                       @PrimaryKey( autoGenerate = true ) var plantId: Long = 0
        ) {


    companion object {
        final val healthGreat = 3
        final val healthGood = 2
        final val healthBad = 1
        final val pTypeDefault = 0
    }


}