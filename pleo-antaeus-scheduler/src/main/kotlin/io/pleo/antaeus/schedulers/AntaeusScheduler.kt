
package io.pleo.antaeus.schedulers

import mu.KotlinLogging
import io.pleo.antaeus.schedulers.helper.UtilsScheduler
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private val logger = KotlinLogging.logger {}

class AntaeusScheduler(
    private val firstStartMilliseconds: Long,
    private val schedulingPeriodMilliseconds: Long,
    private val restCall:String,
    private val firstDayMonthExecution:Boolean
) {

    fun schedule() {

        val untilFirstDayNextMonthHours = TimeUnit.MILLISECONDS.toHours(firstStartMilliseconds)

        logger.info("Starting the scheduler to call rest $restCall")
        logger.info("Time remaining for the first execution: $untilFirstDayNextMonthHours hours")

        val runnable = Runnable {
            if (!firstDayMonthExecution || (firstDayMonthExecution && UtilsScheduler.isFirstOfTheMonth())) {
                logger.info { "Calling the Pending Invoices rest" }
                val result = URL(restCall).readText()
                logger.info { "End of API call. This should be invoked async. $result" }
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

