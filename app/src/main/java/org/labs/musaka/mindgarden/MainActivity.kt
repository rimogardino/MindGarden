package org.labs.musaka.mindgarden

import android.annotation.SuppressLint
import android.arch.persistence.room.Room
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat

import java.util.*



class MainActivity(private var simpleHourFormater: SimpleDateFormat = SimpleDateFormat("yy-DD-HH")) : AppCompatActivity() {

    private val startOfDayTime = 6001
    private var isTimerRunning = false

    private val rand = Random()
    private lateinit var prepTimer: CountDownTimer
    private lateinit var countDownTimer: CountDownTimer
    private var timeLeftInMilliSeconds: Long = 0
    private lateinit var plantDAO: PlantDAO
    private var plantdatabase = Room.databaseBuilder(
            this,
            PlantDatabase::class.java,
            "plantDatabase")

    private lateinit var audioManager: AudioManager


    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        np_time_setter!!.minValue = 0
        np_time_setter!!.maxValue = 20

        button_start!!.setOnClickListener { toggleTimer() }


        plantDAO = plantdatabase.allowMainThreadQueries().fallbackToDestructiveMigration().build().plantDao()
        audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager

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
        return currentDateFormated[1].toInt() - lastDateFormated[1].toInt()
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
        val daysFromLastSession = currentDateFormated[1].toInt() - lastDateFormated[1].toInt()

        val yearGE = currentDateFormated[0].toInt() > lastDateFormated[0].toInt()
        val nextDay = currentDateFormated[1].toInt() > lastDateFormated[1].toInt()


        val newSession = (yearGE or nextDay) or (lastDate == 99.toLong())


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


    }

    private fun degradePlants(n: Int) {
        val plantList = plantDAO.getAllPlants()
        val indexiesSet = emptySet<Int>().toMutableSet()
        val degradatoinCount = if (n > plantList.size) (plantList.size - 1)  else n


        (0 until degradatoinCount).forEach {
            val plantIndex = rand.nextInt(plantList.size)
            indexiesSet.add(plantIndex)
        }

        Log.d(TAG, "generated indexies to degrade: ${indexiesSet.joinToString()}")
        indexiesSet.forEach {
            val plantHealth = plantList[it].pHealth
            if (plantHealth > 1) {
                plantList[it].pHealth  = plantList[it].pHealth - 1
                plantDAO.updatePlant(plantList[it])
            } else {
                val plantDeltedReturnID = plantDAO.deletePlant(plantList[it])
                Log.d(TAG, "removing plantID: ${plantList[it].plantId} return value: $plantDeltedReturnID")
            }
        }
        showPlants()

    }



    private fun showPlants() {
        cl_garden.removeAllViews()
        val plantList = plantDAO.getAllPlants()
        Log.d(TAG, "showing plants: ${plantList.joinToString()}")
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

        plantDAO.insertPlant(newPlant)

        Log.d(TAG,"Plant coordinates chosen: Xi: ${newPlant.xPos} Topi: ${newPlant.yPos}")

        return newPlant
    }


    private fun showPlant(plantModel: PlantModel) {
        val testImageView = ImageView(this)

        val params = android.support.constraint.ConstraintLayout.LayoutParams(plantModel.pWidth, plantModel.pHeight)

        testImageView.id = plantModel.plantId.toInt()

        //Todo the images are square and look bad on my phone, this has to change
        val flowerImages = listOf(R.mipmap.flower_h1,R.mipmap.flower_h2,R.mipmap.flower_h3)

        testImageView.setImageResource(flowerImages[plantModel.pHealth - 1])
        testImageView.layoutParams = params
        testImageView.x = plantModel.xPos.toFloat()
        testImageView.y = plantModel.yPos.toFloat()


        cl_garden.addView(testImageView,params)

    }



    private fun toggleTimer() {
        if (np_time_setter.value != 0) {
            if (isTimerRunning) {
                pauseTimer()
            } else {
//                if (audioManager.ringerMode != AudioManager.RINGER_MODE_VIBRATE) {
//                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
//                }
                //Todo make the ringer mode toggle function, we need a permission for it or something
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                startTimer()
            }
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
//                if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
//                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
//                }
                //Todo make the ringer mode toggle function, we need a permission for it or something
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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
                countDownTimer.start()
                mediaPlayerStart.start()
            }

        }.start()

    }


    private fun pauseTimer() {
        button_start!!.text = getString(R.string.str_begin)
        isTimerRunning = false
        prepTimer.cancel()
        countDownTimer.cancel()
    }

    companion object {

        private const val TAG = "MainActivity"
    }


}
