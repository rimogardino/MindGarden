package org.labs.musaka.mindgarden

import android.arch.persistence.room.*


@Dao
interface PlantDAO {

    @Insert
    fun insertPlant(plantModel: PlantModel) : Long

    @Delete
    fun deletePlant(plantModel: PlantModel)

    @Query ("SELECT * FROM tb_almanac")
    fun getAllPlants(): List<PlantModel>

    @Query ("SELECT * FROM tb_almanac WHERE plantId = :plantID")
    fun getPlant(plantID: Int): PlantModel

    @Query ("SELECT * FROM tb_almanac WHERE pHealth IS NOT 3 ")
    fun getDegradedPlants(): List<PlantModel>

    @Update ()
    fun updatePlant(plantModel: PlantModel) : Int

}