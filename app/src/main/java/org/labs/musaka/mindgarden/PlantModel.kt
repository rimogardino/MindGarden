package org.labs.musaka.mindgarden

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity (tableName = "tb_almanac")
data class PlantModel (

                       var pType: Int = pTypeDefault,
                       var pLifeStage: Int = pLifeStageDefault,
                       var pHealth: Int = pHealthDefault,
                       var pWidth: Int,
                       var pHeight: Int,
                       var xPos: Int,
                       var yPos: Int,
                       @PrimaryKey( autoGenerate = true ) var plantId: Int = 0
        ) {


    companion object {
        const val pHealthDefault = 3
        val pHealthStates = arrayOf(0,1,2,3)
        const val pLifeStageDefault = 0
        val pLifeStages = arrayOf(0,1,2)

        const val pTypeDefault = 0
        val pTypes = arrayOf(0,1,2,3)
    }


}