package org.labs.musaka.mindgarden

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity (tableName = "tb_almanac")
data class PlantModel (

                       var pType: Int,
                       var pLifeStage: Int = pLifeStageDefault,
                       var pHealth: Int = pHealthDefault,
                       var pWidth: Int,
                       var pHeight: Int,
                       var xPos: Int,
                       var yPos: Int,
                       @PrimaryKey( autoGenerate = true ) var plantId: Long = 0
        ) {


    companion object {
        final val pHealthDefault = 0
        final val pHealthStates = arrayOf(0,1,2)
        final val pLifeStageDefault = 0
        final val pLifeStages = arrayOf(0,1,2)

        final val pTypeDefault = 0
        final val pTypes = arrayOf(0,1,2,3)
    }


}