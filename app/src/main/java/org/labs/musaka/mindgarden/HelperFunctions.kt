package org.labs.musaka.mindgarden


import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

fun freshLogIn(sharedPref : SharedPreferences, simpleHourFormater: SimpleDateFormat, context: Context) {

    val repeatInterval = sharedPref.getInt("repeat_interval_key",R.integer.repeat_interval_default)
    Toast.makeText(context, repeatInterval.toString(), Toast.LENGTH_LONG).show()


    val daysFromLastSession = daysFromLastSession(sharedPref, simpleHourFormater, context)
    val lastDate : Long = sharedPref.getLong(context.resources.getString(R.string.last_log_in_key),context.resources.getInteger(R.integer.last_log_in_default).toLong())
    val currentDate =  Calendar.getInstance().timeInMillis- (6001 * 60 * 60)//Takes in account of the starting time of the user's day, maybe
    val lastDateFormated= simpleHourFormater.format(lastDate).split("-")
    val currentDateFormated = simpleHourFormater.format(currentDate).split("-")
    val yearGE = currentDateFormated[0].toInt() > lastDateFormated[0].toInt()
    val nextDay = currentDateFormated[1].toInt() > lastDateFormated[1].toInt()


    val newDayLogIn = (yearGE || nextDay) || (lastDate == 99.toLong())


    val sharedPrefEditor = sharedPref.edit()
    Log.d(TAGs.TAG,"login currentDate $currentDate lastDate: $lastDate - (startOfDayTime * 60 * 60) ${- (6001 * 60 * 60)}")
    if (newDayLogIn) {
        Log.d(TAGs.TAG,"New login recorded")
        //val currentDate =  Calendar.getInstance().timeInMillis

        sharedPrefEditor.putLong(context.resources.getString(R.string.last_log_in_key),currentDate)
        if (daysFromLastSession >= 2) {
            sharedPrefEditor.putInt(context.resources.getString(R.string.current_streak_key),1)
            //degradePlants(daysFromLastSession)
        }
    }

    sharedPrefEditor.apply()


    Log.d(TAGs.TAG,"Fresh login check currentDateFormated $currentDateFormated lastDateFormated $lastDateFormated newDayLogIn $newDayLogIn")
}


private fun daysFromLastSession(sharedPref : SharedPreferences, simpleHourFormater: SimpleDateFormat, context: Context) : Int {

    val lastDate : Long = sharedPref.getLong(context.resources.getString(R.string.last_day_meditated_key),context.resources.getInteger(R.integer.last_day_meditated_default).toLong())
    val currentDate =  Calendar.getInstance().timeInMillis
    val lastDateFormated= simpleHourFormater.format(lastDate - (6001 * 60 * 60)).split("-")
    val currentDateFormated = simpleHourFormater.format(currentDate).split("-")
    return currentDateFormated[1].toInt() - lastDateFormated[1].toInt()
}



object TAGs {

    const val TAG = "MainActivity"
}
