package org.labs.musaka.mindgarden

import android.annotation.SuppressLint
import android.app.ActionBar
import android.arch.persistence.room.Room
import android.graphics.Color
import android.media.Image
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.constraint.ConstraintSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import kotlinx.android.synthetic.main.activity_main.*

import java.util.*


class MainActivity : AppCompatActivity() {


    private var isTimerRunning = false
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMilliSeconds: Long = 0
    private var plantDAO: PlantDAO? = null
    private var plantdatabase = Room.databaseBuilder(
            this,
            PlantDatabase::class.java,
            "plantDatabase")

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        np_time_setter!!.minValue = 0
        np_time_setter!!.maxValue = 20

        button_start!!.setOnClickListener { toggleTimer() }


        plantDAO = plantdatabase.allowMainThreadQueries().fallbackToDestructiveMigration().build().plantDao()





    }

    private fun createFlower() {
        //This fun shound't do all of these things, should be split so that you can call a single funstion to add the plants from the database
        var rand = Random()
        var xi = rand.nextInt(cl_garden.width - 150)
        var yi = rand.nextInt(cl_garden.height - 150)


        var newPlant = PlantModel(PlantModel.pTypeDefault,PlantModel.healthGreat,150,150,xi,yi)
        plantDAO?.insertPlant(newPlant)

        var testImageView = ImageView(this)

        var params = android.support.constraint.ConstraintLayout.LayoutParams(newPlant.pWidth, newPlant.pHeight)

        testImageView.id = View.generateViewId()
        testImageView.setImageResource(R.mipmap.flower)
        testImageView.layoutParams = params
        testImageView.x = newPlant.xPos.toFloat()
        testImageView.y = newPlant.yPos.toFloat()


        cl_garden.addView(testImageView,params)

        Log.d(TAG,"Coordinates chosen: Xi: ${newPlant.xPos} Topi: ${newPlant.yPos}")


        var plnatList = plantDAO?.getAllPlants()

        Log.d(TAG,plnatList.toString())

    }



    private fun toggleTimer() {
        if (isTimerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }


    private fun startTimer() {
        createFlower()

        var mediaPlayer = MediaPlayer.create(this,R.raw.common_quail_excited_chirping)

        mediaPlayer.start()

        /*
        val userTimeInMilliseconds = (np_time_setter!!.value * 60 * 1000).toLong()
        button_start!!.text = "Pause"
        isTimerRunning = true

        countDownTimer = object : CountDownTimer(userTimeInMilliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val numberPickerPosition = (millisUntilFinished / 1000 / 60).toInt()

                Log.d(TAG, "onTick: $numberPickerPosition")

                timeLeftInMilliSeconds = millisUntilFinished

                np_time_setter!!.value = numberPickerPosition

            }

            override fun onFinish() {
                button_start!!.text = "Start"
                isTimerRunning = false
            }
        }.start()
        */

    }


    private fun pauseTimer() {
        button_start!!.text = "Start"
        isTimerRunning = false
        countDownTimer!!.cancel()
    }

    companion object {

        private val TAG = "MainActivity"
    }


}
