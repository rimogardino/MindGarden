package org.labs.musaka.mindgarden

import android.annotation.SuppressLint
import android.arch.persistence.room.Room
import android.content.Context
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat

import java.util.*



class MainActivity(private var simpleHourFormater: SimpleDateFormat = SimpleDateFormat("yy-DD-HH")) : AppCompatActivity() {

    private val startOfDayTime = 6001
    private var isTimerRunning = false

    private val rand = Random()
    private var prepTimer: CountDownTimer? = null
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


        val daysFromLastSession = daysFromLastSession()

        val sharedPref =  this.getPreferences(Context.MODE_PRIVATE)
        val sharedPrefEditor = sharedPref.edit()

        if (daysFromLastSession >= 2) {
            sharedPrefEditor.putInt(getString(R.string.current_streak_key),1)
            degradePlants(daysFromLastSession - 1)


        }

        sharedPrefEditor.apply()



        showPlants()



    }


    private fun daysFromLastSession() : Int {
        val sharedPref =  this.getPreferences(Context.MODE_PRIVATE)


        val lastDate : Long = sharedPref.getLong(getString(R.string.last_day_meditated_key),resources.getInteger(R.integer.last_day_meditated_default).toLong())
        val currentDate =  Calendar.getInstance().timeInMillis
        val lastDateFormated= simpleHourFormater.format(lastDate - (startOfDayTime * 60 * 60)).split("-")
        val currentDateFormated = simpleHourFormater.format(currentDate).split("-")
        val daysFromLastSession = currentDateFormated[1].toInt() - lastDateFormated!![1].toInt()

        return daysFromLastSession
    }


    private fun addStats(timeMeditatedInMilliseconds: Long) {
        val sharedPref =  this.getPreferences(Context.MODE_PRIVATE)
        val currentStreak = sharedPref.getInt(getString(R.string.current_streak_key),resources.getInteger(R.integer.current_streak_default))



        val sharedPrefEditor = sharedPref.edit()

        val tMeditatedUntilNow = sharedPref.getLong(getString(R.string.time_meditated_key),resources.getInteger(R.integer.time_meditated_default).toLong())
        sharedPrefEditor.putLong(getString(R.string.time_meditated_key),(tMeditatedUntilNow+timeMeditatedInMilliseconds))

        val lastDate : Long = sharedPref.getLong(getString(R.string.last_day_meditated_key),resources.getInteger(R.integer.last_day_meditated_default).toLong())
        val currentDate =  Calendar.getInstance().timeInMillis
        val lastDateFormated= simpleHourFormater.format(lastDate - (startOfDayTime * 60 * 60)).split("-")
        val currentDateFormated = simpleHourFormater.format(currentDate).split("-")
        val daysFromLastSession = currentDateFormated[1].toInt() - lastDateFormated!![1].toInt()

        val yearGE = currentDateFormated[0].toInt() > lastDateFormated[0].toInt()
        val nextDay = currentDateFormated[1].toInt() > lastDateFormated[1].toInt()


        val newSession = (yearGE or nextDay) and !lastDate.equals(-999.toLong())


        //Log.d(TAG,"New session: this date: $currentDateFormated lastdate: $lastDate streak: $currentStreak time meditated: $tMeditatedUntilNow")

        if (newSession) {
            Log.d(TAG,"New session recorded: date: $currentDateFormated streak: $currentStreak time meditated: $tMeditatedUntilNow")


            if (daysFromLastSession < 2) {
                sharedPrefEditor.putInt(getString(R.string.current_streak_key),currentStreak + 1)
            } else {
                sharedPrefEditor.putInt(getString(R.string.current_streak_key),1)
            }


            sharedPrefEditor.putLong(getString(R.string.last_day_meditated_key), currentDate)
            showPlant(createPlant())
        }

        sharedPrefEditor.apply()

        //showPlant(createPlant()) // add plants for testing

        degradePlants(2)
        showPlants()
    }

    private fun degradePlants(n: Int) {
        val plantList = plantDAO!!.getAllPlants()
        val indexiesList = arrayOfNulls<Int>(n)

        if (n > plantList.size) return


        (0 until n).forEach {
            var newIndex =false
            while (!newIndex) {
                val plantIndex = rand.nextInt(plantList.size)
                if (plantIndex !in indexiesList) {
                    newIndex = true
                    indexiesList[it] = plantIndex
                }
            }
        }
        Log.d(TAG, "generated indexies to degrade: ${indexiesList.joinToString()}")
        indexiesList.forEach {
            val plantHealth = plantList[it!!].pHealth
            if (plantHealth > 1) {
                plantList[it!!].pHealth  = plantList[it!!].pHealth - 1
                plantDAO!!.updatePlant(plantList[it!!])
            } else {
                Log.d(TAG, "removing plantID: ${plantList[it!!].plantId}")
                plantDAO!!.deletePlant(plantList[it!!])
            }
        }
        showPlants()

    }



    private fun showPlants() {
        val plantList = plantDAO!!.getAllPlants()

        for (plant in plantList) {
            showPlant(plant)

        }

    }

    private fun createPlant(): PlantModel {
        //This fun shound't do all of these things, should be split so that you can call a single function to add the plants from the database


        val pType = rand.nextInt(PlantModel.pTypes.size)

        val pWidth = rand.nextInt(201) + 100
        val pHeight = rand.nextInt(201) + 100

        val xi = rand.nextInt(cl_garden.width - 150)
        val yi = rand.nextInt(cl_garden.height - 150)


        val newPlant = PlantModel(pType = pType,pWidth = pWidth,pHeight = pHeight,xPos = xi,yPos = yi)

        plantDAO?.insertPlant(newPlant)

        Log.d(TAG,"Coordinates chosen: Xi: ${newPlant.xPos} Topi: ${newPlant.yPos}")

        return newPlant
    }


    private fun showPlant(plantModel: PlantModel) {
        val testImageView = ImageView(this)

        val params = android.support.constraint.ConstraintLayout.LayoutParams(plantModel.pWidth, plantModel.pHeight)

        testImageView.id = View.generateViewId()
        testImageView.setImageResource(R.mipmap.flower)
        testImageView.layoutParams = params
        testImageView.x = plantModel.xPos.toFloat()
        testImageView.y = plantModel.yPos.toFloat()


        cl_garden.addView(testImageView,params)

    }



    private fun toggleTimer() {
        if (isTimerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }


    private fun startTimer() {
        val mediaPlayerStart = MediaPlayer.create(this,R.raw.short_quail_chirps)
        val mediaPlayerEnd = MediaPlayer.create(this,R.raw.short_quail_chirps_type_2)

        val userTimeInMilliseconds = (np_time_setter!!.value * 60 * 1000).toLong()

        isTimerRunning = true

        countDownTimer = object : CountDownTimer(userTimeInMilliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val numberPickerPosition = (millisUntilFinished / 1000 / 60).toInt()

                timeLeftInMilliSeconds = millisUntilFinished

                np_time_setter!!.value = numberPickerPosition + 1

            }

            override fun onFinish() {
                addStats(userTimeInMilliseconds)
                np_time_setter!!.value = 0
                button_start!!.text = getString(R.string.str_begin)
                isTimerRunning = false
                mediaPlayerEnd.start()
            }
        }


        prepTimer = object  : CountDownTimer(5000,1000) {
            override fun onTick(millisUntilFinished: Long) {
                button_start!!.text = ((millisUntilFinished/1000) + 1).toString()
            }

            override fun onFinish() {
                button_start!!.text = getString(R.string.str_pause)
                (countDownTimer as CountDownTimer).start()
                mediaPlayerStart.start()
            }

        }.start()

    }


    private fun pauseTimer() {
        button_start!!.text = getString(R.string.str_begin)
        isTimerRunning = false
        prepTimer!!.cancel()
        countDownTimer!!.cancel()
    }

    companion object {

        private const val TAG = "MainActivity"
    }


}
