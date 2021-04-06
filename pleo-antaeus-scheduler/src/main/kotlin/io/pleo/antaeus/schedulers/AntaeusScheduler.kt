
package io.pleo.antaeus.schedulers

import mu.KotlinLogging
import io.pleo.antaeus.schedulers.helper.UtilsScheduler
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*



private val logger = KotlinLogging.logger {}

class AntaeusScheduler(
    private val firstStartMilliseconds: Long,
    private val schedulingPeriodMilliseconds: Long,
    private val restCall:String,
    private val firstDayMonthExecution:Boolean
) {


    /**
     * Call the rest service async
     */
    private fun callRest() {
        logger.info { "Calling the scheduler rest: $restCall"}
        runBlocking {
            val resultRest: Deferred<String> =
            withContext(Dispatchers.IO) {
                async { URL(restCall).readText() }
            }
        }
        //logger.info { "End of API call. This should be invoked async. ${resultRest.await()}" }
    }



    /**
     * Scheduler function: Creates the scheduler with the rest call to be launched
     */
    fun schedule() {

        val untilFirstDayNextMonthHours = TimeUnit.MILLISECONDS.toHours(firstStartMilliseconds)

        logger.info("Starting the scheduler to call rest $restCall")
        logger.info("Time remaining for the first execution: $untilFirstDayNextMonthHours hours")

        //When the scheduler is launching, it calls async this one
        val runnable = Runnable {
            if (!firstDayMonthExecution || (firstDayMonthExecution && UtilsScheduler.isFirstOfTheMonth())) {
                callRest()
            }
        }

        val scheduler = Executors.newScheduledThreadPool(1)

        scheduler.scheduleAtFixedRate(
                runnable,
                firstStartMilliseconds,
                schedulingPeriodMilliseconds,
                TimeUnit.MILLISECONDS)

        logger.info("The scheduler is launched")
    }




}


