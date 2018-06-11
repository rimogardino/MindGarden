package org.labs.musaka.mindgarden

import android.annotation.SuppressLint
import android.arch.persistence.room.Room
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.CountDownTimer
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.LayoutRes

import android.support.constraint.ConstraintSet

import android.transition.AutoTransition
import android.transition.ChangeBounds

import android.transition.TransitionManager
import android.util.Log
import android.view.ViewGroup

import android.view.WindowManager
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*


import java.text.SimpleDateFormat

import java.util.*



class MainActivity(private var simpleHourFormater: SimpleDateFormat = SimpleDateFormat("yy-DD-HH")) : AppCompatActivity() {

    private val startOfDayTime = 6001
    private var isTimerRunning = false
    private var repeatInterval: Int = 0
    private var currentLayout = R.layout.activity_main
    private val rand = Random()
    private lateinit var prepTimer: CountDownTimer
    private lateinit var countDownTimer: CountDownTimer
    private var timeLeftInMilliSeconds: Long = 0
    private lateinit var plantDAO: PlantDAO
    private var plantdatabase = Room.databaseBuilder(
            this,
            PlantDatabase::class.java,
            "plantDatabase")
    private lateinit var sharedPref : SharedPreferences
    private lateinit var audioManager: AudioManager


    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpNumberPickers()
        setUpClickListeners()


        plantDAO = plantdatabase.allowMainThreadQueries().fallbackToDestructiveMigration().build().plantDao()
        audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        sharedPref =  this.getPreferences(Context.MODE_PRIVATE) //Initiate the shared preferences

        val sharedPrefEditor = sharedPref.edit()

        repeatInterval = sharedPref.getInt(getString(R.string.repeat_interval_key),R.integer.repeat_interval_default)
        val daysFromLastSession = daysFromLastSession()

        if (freshLogIn()) {
            Log.d(TAG,"New login recorded")
            val currentDate =  Calendar.getInstance().timeInMillis
            Log.d(TAG,"login currentDate $currentDate")
            sharedPrefEditor.putLong(getString(R.string.last_log_in_key),currentDate)
            if (daysFromLastSession >= 2) {
                sharedPrefEditor.putInt(getString(R.string.current_streak_key),1)
                degradePlants(daysFromLastSession)


            }
        }

        sharedPrefEditor.apply()
        showPlants()
    }



    private fun testFun() {
        degradePlants(28)


    }

    private fun testFun2() {
        showPlant(createPlant())
    }


    private fun setUpNumberPickers() {
        np_time_setter.minValue = 0
        np_time_setter.maxValue = 40

        np_set_interval.minValue = 0
        np_set_interval.maxValue = 20
    }

    private fun setUpClickListeners() {
        button_start.setOnClickListener { toggleTimer() }
        button_set_interval.setOnClickListener { changeLayout(if (currentLayout != R.layout.activity_set_interval) R.layout.activity_set_interval else R.layout.activity_main) }
        textView_congratulations.setOnClickListener { changeLayout(if (currentLayout != R.layout.activity_congratulations) R.layout.activity_congratulations else R.layout.activity_main) }


        //Todo these will be removed when shit works better
        button_test.setOnClickListener { testFun() }
        button_test2.setOnClickListener { testFun2() }

    }


    private fun changeLayout(@LayoutRes id: Int) {
        Log.d(TAG,"change layout")
        if (id == R.layout.activity_set_interval) {
            //Show the set interval layout
            currentLayout = R.layout.activity_set_interval
            setLayout(id)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                constraintLayout_main.setBackgroundColor(getColor(R.color.colorSetIntervalBackground))
            }
            button_set_interval.text = getString(R.string.str_set_interval)

            //Trying to update the nuberpicker to the correct position, but it is not displaying the value
            np_set_interval.value = repeatInterval
        } else if (id == R.layout.activity_congratulations) {
            currentLayout = R.layout.activity_congratulations
            setLayout(id)
            val tMeditated = sharedPref.getLong(getString(R.string.time_meditated_key),resources.getInteger(R.integer.time_meditated_default).toLong()) / (60 * 1000)
            var plantsAmount = plantDAO.getAllPlants().size
            //todo : make the string a resource
            textView_congratulations.text = "Well done!! You have now spent $tMeditated minutes in this garden and have grown $plantsAmount plants!! \n\n Tap here to continue."
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                textView_congratulations.setBackgroundColor(getColor(R.color.colorPrimary))
//                textView_congratulations
//            }

        } else {
            //Show the main layout and record the picked interval in the shared preferences
            currentLayout = R.layout.activity_main
            setLayout(R.layout.activity_main)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                constraintLayout_main.setBackgroundColor(getColor(R.color.colorMainBackground))
            }

            button_set_interval.text = getString(R.string.str_interval)

            val sharedPrefEditor = sharedPref.edit()

            repeatInterval = np_set_interval.value
            sharedPrefEditor.putInt(getString(R.string.repeat_interval_key),repeatInterval)
            sharedPrefEditor.apply()
        }


    }

    private fun setLayout(@LayoutRes id: Int) {
        val constraint = ConstraintSet()
        constraint.load(this,id)

        val transition = ChangeBounds()
        transition.duration = 700


        TransitionManager.beginDelayedTransition(constraintLayout_main,transition)
        constraint.applyTo(constraintLayout_main)
    }




    private fun freshLogIn() : Boolean {

        val lastDate : Long = sharedPref.getLong(getString(R.string.last_log_in_key),resources.getInteger(R.integer.last_log_in_default).toLong())
        val currentDate =  Calendar.getInstance().timeInMillis
        val lastDateFormated= simpleHourFormater.format(lastDate - (startOfDayTime * 60 * 60)).split("-")
        val currentDateFormated = simpleHourFormater.format(currentDate).split("-")
        val yearGE = currentDateFormated[0].toInt() > lastDateFormated[0].toInt()
        val nextDay = currentDateFormated[1].toInt() > lastDateFormated[1].toInt()


        val newDayLogIn = (yearGE || nextDay) || (lastDate == 99.toLong())

        Log.d(TAG,"Fresh login check currentDateFormated $currentDateFormated lastDateFormated $lastDateFormated newDayLogIn $newDayLogIn")
        return newDayLogIn
    }


    private fun daysFromLastSession() : Int {

        val lastDate : Long = sharedPref.getLong(getString(R.string.last_day_meditated_key),resources.getInteger(R.integer.last_day_meditated_default).toLong())
        val currentDate =  Calendar.getInstance().timeInMillis
        val lastDateFormated= simpleHourFormater.format(lastDate - (startOfDayTime * 60 * 60)).split("-")
        val currentDateFormated = simpleHourFormater.format(currentDate).split("-")
        return currentDateFormated[1].toInt() - lastDateFormated[1].toInt()
    }


    private fun addStats(timeMeditatedInMilliseconds: Long) {

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


        val newSession = (yearGE || nextDay) || (lastDate == 99.toLong())


        Log.d(TAG,"New session: $newSession this date: $currentDateFormated lastdate: $lastDate streak: $currentStreak time meditated: $tMeditatedUntilNow")

        if (newSession) {



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
        var plantList = plantDAO.getAllPlants()
        val indexiesSet = emptyList<Int>().toMutableList()


        (0 until n).forEach {
            if (plantList.isEmpty()) return@forEach
            val plantIndex = rand.nextInt(plantList.size)
            val plantHealth = plantList[plantIndex].pHealth
            if (plantHealth > 1) {
                plantList[plantIndex].pHealth  = plantList[plantIndex].pHealth - 1
                plantDAO.updatePlant(plantList[plantIndex])
            } else {
                plantDAO.deletePlant(plantList[plantIndex])
                Log.d(TAG, "removing plantID: ${plantList[plantIndex].plantId} current plantList.size is ${plantList.size}")
                plantList = plantList.minus(plantList[plantIndex])
            }


            indexiesSet.add(plantIndex)
        }

        Log.d(TAG, "generated indexies to degrade: ${indexiesSet.joinToString()}")

        showPlants()

    }



    private fun showPlants() {
        val imageViewGrass = cl_garden.getViewById(iv_grass_background.id)
        cl_garden.removeAllViews()
        cl_garden.addView(imageViewGrass)

        val plantList = plantDAO.getAllPlants()
        Log.d(TAG, "showing plants: ${plantList.joinToString()}")
        for (plant in plantList) {
            showPlant(plant)

        }

    }





    private fun createPlant(): PlantModel {
        //This fun shound't do all of these things, should be split so that you can call a single function to add the plants from the database


        val pType = rand.nextInt(PlantModel.pTypes.size)


        val pHeight = rand.nextInt(2010) / 10 + 100
        val pWeight = (pHeight-100)/200.0

        //This chooses the Y position based on how big the plant is, puts the bigger plants lower
        val yi = if (pWeight > 0.5) {
                (rand.nextInt((cl_garden.height * (1-pWeight) * 0.5).toInt()) + (cl_garden.height * (pWeight))).toInt() - 200
            } else {
                rand.nextInt((cl_garden.height * (pWeight) * 0.5).toInt())
            }


        val xi = rand.nextInt(cl_garden.width - 150)



        val newPlant = PlantModel(pType = pType,pWidth = pHeight,pHeight = pHeight,xPos = xi,yPos = yi)

        plantDAO.insertPlant(newPlant)

        Log.d(TAG,"Plant coordinates chosen: Xi: ${newPlant.xPos} Topi: ${newPlant.yPos} pHeight $pHeight pWeight ${pWeight}")

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

        val userTimeInMilliseconds = (np_time_setter.value * 60 * 1000).toLong()

        isTimerRunning = true

        countDownTimer = object : CountDownTimer(userTimeInMilliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val numberPickerPosition = (millisUntilFinished / 1000 / 60).toInt()

                timeLeftInMilliSeconds = millisUntilFinished

                np_time_setter.value = numberPickerPosition + 1

                //Repeat interval magic is happning here
                val repeatIntervalInLong = (repeatInterval*60*1000)

                if (millisUntilFinished.toInt() % repeatIntervalInLong < 1000) {
                    Log.d(TAG,"repeatInterval on millisUntilFinished $millisUntilFinished")
                    mediaPlayerEnd.start()
                }

            }

            override fun onFinish() {
//                if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
//                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
//                }
                //Todo make the ringer mode toggle function, we need a permission for it or something
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                addStats(userTimeInMilliseconds)
                np_time_setter.value = 0
                button_start.text = getString(R.string.str_begin)
                isTimerRunning = false
                mediaPlayerEnd.start()
                changeLayout(R.layout.activity_congratulations)
            }
        }


        prepTimer = object  : CountDownTimer(5000,1000) {
            override fun onTick(millisUntilFinished: Long) {
                button_start.text = ((millisUntilFinished/1000) + 1).toString()
            }

            override fun onFinish() {
                button_start.text = getString(R.string.str_pause)
                countDownTimer.start()
                mediaPlayerStart.start()
            }

        }.start()

    }


    private fun pauseTimer() {
        button_start.text = getString(R.string.str_begin)
        isTimerRunning = false
        prepTimer.cancel()
        countDownTimer.cancel()
    }

    companion object {

        private const val TAG = "MainActivity"
    }


}
