package org.labs.musaka.mindgarden



import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.AnimationDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.room.Room
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private val startOfDayTime = 6001
    private var isTimerRunning = false
    private var repeatInterval: Int = 4
    private var currentLayout = R.layout.activity_main
    private val rand = Random()
    private lateinit var prepTimer: CountDownTimer
    private lateinit var countDownTimer: CountDownTimer
    private var timeLeftInMilliSeconds: Long = 0
    private var simpleHourFormater: SimpleDateFormat = SimpleDateFormat("yy-DD-HH")
    private lateinit var plantDAO: PlantDAO
    private var plantdatabase = Room.databaseBuilder(
            this,
            PlantDatabase::class.java,
            "plantDatabase")
    private lateinit var sharedPref : SharedPreferences
    private lateinit var audioManager: AudioManager


    //todo apperantly I'm dividing by 0 or something, need to fix

    @SuppressLint("PrivateResource")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        plantDAO = plantdatabase.allowMainThreadQueries().fallbackToDestructiveMigration().build().plantDao()
        audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        sharedPref =  this.getPreferences(Context.MODE_PRIVATE) //Initiate the shared preferences



        setUpNumberPickers()
        freshLogIn(sharedPref, simpleHourFormater, applicationContext)
        showPlants()
        setUpClickListeners()

        scheduleWorkManagerNotifications()
    }






    private fun setUpNumberPickers() {
        repeatInterval = sharedPref.getInt(getString(R.string.repeat_interval_key),R.integer.repeat_interval_default)

        np_set_interval.value = repeatInterval
        np_time_setter.value = sharedPref.getInt(getString(R.string.length_time_meditation_key),R.integer.length_time_meditation_default)

        np_time_setter.minValue = 0
        np_time_setter.maxValue = 40

        np_set_interval.minValue = 0
        np_set_interval.maxValue = 20
        Log.d(TAG,"repeatInterval in setUpNumberPickers $repeatInterval")
    }

    private fun setUpClickListeners() {
        button_start.setOnClickListener { toggleTimer() }
        button_set_interval.setOnClickListener { changeLayout(if (currentLayout != R.layout.activity_set_interval) R.layout.activity_set_interval else R.layout.activity_main) }
        textView_congratulations.setOnClickListener { changeLayout(if (currentLayout != R.layout.activity_congratulations) R.layout.activity_congratulations else R.layout.activity_main) }


        setClickAnimations()//Show an animation when one of the plants is touched
    }


    private fun setClickAnimations() {
        //Show an animation when one of the plants is touched
        val shownPlants = plantDAO.getAllPlants()

        for (plant in shownPlants) {
            cl_garden.getViewById(plant.plantId).setOnClickListener {
                Log.d(TAG, "Plant clicked")
                val touchAnimations = listOf(R.drawable.animation_plant_touch_health_1, R.drawable.animation_plant_touch_health_2, R.drawable.animation_plant_touch_health_3)

                it.setBackgroundResource(touchAnimations[plantDAO.getPlant(it.id).pHealth - 1])

                val animationTouch: AnimationDrawable = it.background as AnimationDrawable

                if (animationTouch.isRunning && animationTouch.current == animationTouch.getFrame(animationTouch.numberOfFrames-1)) {
                    animationTouch.stop()
                    animationTouch.start()
                } else {
                    animationTouch.start()
                }
            }
        }

    }


    private fun getCongratulationText(amountDegradedPlants : Int = 0,amountRemovedPlants : Int = 0) : String {
        //todo : make the string a resource

        val tMeditated = sharedPref.getLong(getString(R.string.time_meditated_key),resources.getInteger(R.integer.time_meditated_default).toLong()) / (60 * 1000)
        val plantsAmount = plantDAO.getAllPlants().size
        var congrats = "Welcome! Stay as much as you want"

        if (amountDegradedPlants == 0 && amountRemovedPlants == 0 && tMeditated > 0 && plantsAmount > 0) {
            congrats = "Well done!! You have now spent $tMeditated minutes in this garden and have grown $plantsAmount ${if (plantsAmount>1) " plants!! " else " plant!! "} " +
                    "\n\n Tap here to continue."
        }  else if (amountDegradedPlants > 0 || amountRemovedPlants > 0) {
            congrats = if (amountRemovedPlants > 0 && amountDegradedPlants > 0) {
                "Unfortunately $amountDegradedPlants of your plants have faded and $amountRemovedPlants have died out.  \n\n Tap here to continue."
            } else if (amountRemovedPlants == 0 && amountDegradedPlants > 0) {
                "Unfortunately $amountDegradedPlants of your plants have faded.\n\n Tap here to continue."
            } else {
                "Unfortunately $amountRemovedPlants of your plants have died out\n\n Tap here to continue."
            }
        }

        return congrats
    }


    private fun changeLayout(@LayoutRes id: Int, amountDegradedPlants : Int = 0, amountRemovedPlants : Int = 0) {
        Log.d(TAG,"change layout")
        if (id == R.layout.activity_set_interval) {
            //Show the set interval layout
            currentLayout = R.layout.activity_set_interval
            setLayout(id)

             button_set_interval.text = getString(R.string.str_set_interval)

            //Trying to update the nuberpicker to the correct position, but it is not displaying the value
            np_set_interval.value = repeatInterval
            Log.d(TAG,"repeatInterval in activity_set_interval $repeatInterval")
        } else if (id == R.layout.activity_congratulations) {
            currentLayout = R.layout.activity_congratulations
            setLayout(id)

            textView_congratulations.text = getCongratulationText(amountDegradedPlants,amountRemovedPlants)
        } else {
            //Show the main layout and record the picked interval in the shared preferences
            currentLayout = R.layout.activity_main
            setLayout(R.layout.activity_main)

            button_set_interval.text = getString(R.string.str_interval)

            val sharedPrefEditor = sharedPref.edit()

            repeatInterval = if (np_set_interval.value > 0) np_set_interval.value else repeatInterval
            Log.d(TAG,"repeatInterval when recording the user value $repeatInterval")
            sharedPrefEditor.putInt(getString(R.string.repeat_interval_key),repeatInterval)
            sharedPrefEditor.apply()
        }

        setClickAnimations()
    }

    private fun setLayout(@LayoutRes id: Int) {
        val constraint = ConstraintSet()
        constraint.load(this,id)

        val transition = ChangeBounds()
        transition.duration = 700


        TransitionManager.beginDelayedTransition(constraintLayout_main,transition)
        constraint.applyTo(constraintLayout_main)
    }





    private fun addStats(timeMeditatedInMilliseconds: Long) {
        //For testing, may add for real something easier to achieve
        createPlant()


        val sharedPrefEditor = sharedPref.edit()

        val currentStreak = sharedPref.getInt(getString(R.string.current_streak_key),resources.getInteger(R.integer.current_streak_default))
        val tMeditatedUntilNow = sharedPref.getLong(getString(R.string.time_meditated_key),resources.getInteger(R.integer.time_meditated_default).toLong())

        sharedPrefEditor.putLong(getString(R.string.time_meditated_key),(tMeditatedUntilNow+timeMeditatedInMilliseconds))
        sharedPrefEditor.putInt(getString(R.string.length_time_meditation_key),(timeMeditatedInMilliseconds/(1000*60)).toInt())

        val lastDate : Long = sharedPref.getLong(getString(R.string.last_day_meditated_key),resources.getInteger(R.integer.last_day_meditated_default).toLong())
        val currentDate =  Calendar.getInstance().timeInMillis - (startOfDayTime * 60 * 60)//Takes in account of the starting time of the user's day, maybe
        val lastDateFormated= simpleHourFormater.format(lastDate).split("-")
        val currentDateFormated = simpleHourFormater.format(currentDate).split("-")
        val daysFromLastSession = currentDateFormated[1].toInt() - lastDateFormated[1].toInt()

        val yearGE = currentDateFormated[0].toInt() > lastDateFormated[0].toInt()
        val nextDay = currentDateFormated[1].toInt() > lastDateFormated[1].toInt()


        val newSession = (yearGE || nextDay) || (lastDate == 99.toLong())


        Log.d(TAG,"New session: $newSession this date: $currentDateFormated lastdate: $lastDate streak: $currentStreak time meditated: $tMeditatedUntilNow")

        if (newSession) {
            showPlant(createPlant())


            if (daysFromLastSession < 2) {
                sharedPrefEditor.putInt(getString(R.string.current_streak_key),currentStreak + 1)
            } else {
                sharedPrefEditor.putInt(getString(R.string.current_streak_key),1)
            }


            sharedPrefEditor.putLong(getString(R.string.last_day_meditated_key), currentDate)

            val degradedPlants = plantDAO.getDegradedPlants()
            Log.d(TAG, "degraded plants: ${degradedPlants.size}")

            if (degradedPlants.isEmpty()) {

            } else {
                degradedPlants[0].pHealth = PlantModel.pHealthDefault
                plantDAO.updatePlant(degradedPlants[0])
            }
        }
        showPlants()// makes the new plants be clickable, but also shows the animation of growing every time
        sharedPrefEditor.apply()


    }

    private fun degradePlants(n: Int) {
        var plantList = plantDAO.getAllPlants()
        val degradedPlants = emptyList<PlantModel>().toMutableList()
        val indexiesList = emptyList<Int>().toMutableList()
        var removedPlants = 0


        (0 until n).forEach {
            if (plantList.isEmpty()) return@forEach

            val plantIndex = rand.nextInt(plantList.size)
            val plantHealth = plantList[plantIndex].pHealth

            if (plantList[plantIndex] !in degradedPlants) degradedPlants.add(plantList[plantIndex])

            if (plantHealth > 1) {


                plantList[plantIndex].pHealth  = plantList[plantIndex].pHealth - 1
                plantDAO.updatePlant(plantList[plantIndex])

            } else {
                plantDAO.deletePlant(plantList[plantIndex])
                Log.d(TAG, "removing plantID: ${plantList[plantIndex].plantId} current plantList.size is ${plantList.size}")
                plantList = plantList.minus(plantList[plantIndex])
                removedPlants++
            }


            indexiesList.add(plantIndex)
        }

        val amountOfDegradedPlants = degradedPlants.size
        changeLayout(R.layout.activity_congratulations,amountOfDegradedPlants,removedPlants)

        Log.d(TAG, "degradedPlants: $degradedPlants amountOfDegradedPlants $amountOfDegradedPlants removedPlants $removedPlants")

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

        setClickAnimations()

    }





    private fun createPlant(): PlantModel {
        //This fun shound't do all of these things, should be split so that you can call a single function to add the plants from the database
        //todo The coordinates generator is shit, have to fix it.

        val pType = rand.nextInt(PlantModel.pTypes.size)


        val pHeight = rand.nextInt(cl_garden.height) / 10 + 100
        val pWeight = (pHeight-100)/200.0



        //This chooses the Y position based on how big the plant is, puts the bigger plants lower
//        val yi = if (pWeight > 0.5) {
//                (rand.nextInt((cl_garden.height * (1-pWeight) * 0.5).toInt()) + (cl_garden.height * (pWeight))).toInt() - 200
//            } else {
//            Log.d(TAG,"bound must be positive: ${cl_garden.height * (pWeight) * 0.5}  args: (${cl_garden.height} * ($pWeight) * 0.5")
//                val nextInt = if ((cl_garden.height * (pWeight) * 0.5) > 0) (cl_garden.height * (pWeight) * 0.5)  else 0.3
//                rand.nextInt(nextInt.toInt())
//            }

        val yi = rand.nextInt(cl_garden.height/2) + cl_garden.height/2 - 150
        val xi = rand.nextInt(cl_garden.width - 150)



        val newPlant = PlantModel(pType = pType,pWidth = pHeight,pHeight = pHeight,xPos = xi,yPos = yi)

        plantDAO.insertPlant(newPlant)

        Log.d(TAG,"Plant coordinates chosen: Xi: ${newPlant.xPos} Topi: ${newPlant.yPos} pHeight $pHeight pWeight $pWeight")

        return newPlant
    }


    private fun showPlant(plantModel: PlantModel) {
        val testImageView = ImageView(this)

        val params = ConstraintLayout.LayoutParams(plantModel.pWidth, plantModel.pHeight)

        testImageView.tag = "clickbleImage"
        testImageView.id = plantModel.plantId

        val animations = listOf(R.drawable.animation_plant_growing_health_1,R.drawable.animation_plant_growing_health_2,R.drawable.animation_plant_growing)

        testImageView.setBackgroundResource(animations[plantModel.pHealth - 1])

        val animationGrowing : AnimationDrawable = testImageView.background as AnimationDrawable


        testImageView.layoutParams = params
        testImageView.x = plantModel.xPos.toFloat()
        testImageView.y = plantModel.yPos.toFloat()


        cl_garden.addView(testImageView,params)

        animationGrowing.start()

    }

    private fun scheduleWorkManagerNotifications() {
        val periodicNotificationRequest = PeriodicWorkRequestBuilder<NotifyWorker>(3,TimeUnit.MINUTES)
                .build()

        WorkManager.getInstance().enqueue(periodicNotificationRequest)
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
                val numberPickerPosition = (millisUntilFinished / (1000 * 60)).toInt()

                timeLeftInMilliSeconds = millisUntilFinished

                np_time_setter.value = numberPickerPosition + 1

                //Repeat interval magic is happning here
                val repeatIntervalInLong = (repeatInterval*60*1000)
                Log.d(TAG,"repeatInterval in long $repeatIntervalInLong")
                if (millisUntilFinished.toInt() % repeatIntervalInLong < 1000) {
                    Log.d(TAG,"repeatInterval on millisUntilFinished $millisUntilFinished")
                    mediaPlayerStart.start()
                }

            }

            override fun onFinish() {
//                if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
//                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
//                }
                //Todo make the ringer mode toggle function, we need a permission for it or something
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                addStats(userTimeInMilliseconds)
                np_time_setter.value = (userTimeInMilliseconds /(60 * 1000)).toInt()
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



/*moved in a new file
    private fun freshLogIn() {

        val daysFromLastSession = daysFromLastSession()
        val lastDate : Long = sharedPref.getLong(getString(R.string.last_log_in_key),resources.getInteger(R.integer.last_log_in_default).toLong())
        val currentDate =  Calendar.getInstance().timeInMillis- (startOfDayTime * 60 * 60)//Takes in account of the starting time of the user's day, maybe
        val lastDateFormated= simpleHourFormater.format(lastDate).split("-")
        val currentDateFormated = simpleHourFormater.format(currentDate).split("-")
        val yearGE = currentDateFormated[0].toInt() > lastDateFormated[0].toInt()
        val nextDay = currentDateFormated[1].toInt() > lastDateFormated[1].toInt()


        val newDayLogIn = (yearGE || nextDay) || (lastDate == 99.toLong())


        val sharedPrefEditor = sharedPref.edit()
        Log.d(TAG,"login currentDate $currentDate lastDate: $lastDate - (startOfDayTime * 60 * 60) ${- (startOfDayTime * 60 * 60)}")
        if (newDayLogIn) {
            Log.d(TAG,"New login recorded")
            val currentDate =  Calendar.getInstance().timeInMillis

            sharedPrefEditor.putLong(getString(R.string.last_log_in_key),currentDate)
            if (daysFromLastSession >= 2) {
                sharedPrefEditor.putInt(getString(R.string.current_streak_key),1)
                degradePlants(daysFromLastSession)
            }
        }

        sharedPrefEditor.apply()


        Log.d(TAG,"Fresh login check currentDateFormated $currentDateFormated lastDateFormated $lastDateFormated newDayLogIn $newDayLogIn")
    }


    private fun daysFromLastSession() : Int {

        val lastDate : Long = sharedPref.getLong(getString(R.string.last_day_meditated_key),resources.getInteger(R.integer.last_day_meditated_default).toLong())
        val currentDate =  Calendar.getInstance().timeInMillis
        val lastDateFormated= simpleHourFormater.format(lastDate - (startOfDayTime * 60 * 60)).split("-")
        val currentDateFormated = simpleHourFormater.format(currentDate).split("-")
        return currentDateFormated[1].toInt() - lastDateFormated[1].toInt()
    }*/