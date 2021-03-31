package io.pleo.antaeus.schedulers.helper


import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.Duration



class UtilsScheduler{


    companion object {

        /**
         * Return true if it is the first day of the current month
         */
        fun isFirstOfTheMonth():Boolean{
            val now = now()
            return now.dayOfMonth == 1
        }


        /**
         * How many milliseconds between now and the first day of next month at 00:00 (Using the local time zone of the
         * server running in this context)
         */
        fun untilFirstDayNextMonthMilliseconds(): Long {
            val now = now()
            val firstDayNextMonth =
                if (now.monthValue == 12) {
                    LocalDateTime.of(now.year + 1 , 1, 1, 0, 0)
                } else {
                    LocalDateTime.of(now.year, now.month + 1, 1, 0, 0)
                }
            return Duration.between(now(), firstDayNextMonth).toMillis()
        }

        /**
         * Milliseconds in 24 hour
         */
        fun millisecondsOneDay(): Long {
            return 24*60*60*1000
        }


    }


}