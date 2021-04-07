@file:JvmName("InitSchedulers")

package io.pleo.antaeus.schedulers

import io.pleo.antaeus.schedulers.helper.UtilsScheduler

/*
    Defines the main() entry point for the schedulers
 */

fun main() {

    /**
     * Launch the scheduler for pending invoices
     */
    AntaeusScheduler(
        firstStartMilliseconds = UtilsScheduler.untilFirstDayNextMonthMilliseconds(),
        schedulingPeriodMilliseconds = UtilsScheduler.millisecondsOneDay(),
        restCall = "http://localhost:7000/rest/v1/billing/pending",
        firstDayMonthExecution = true
    ).schedule()

    /**
     * Launch the scheduler to retry failed invoices
     */
    AntaeusScheduler(
        firstStartMilliseconds = UtilsScheduler.untilFirstDayNextMonthMilliseconds(),
        schedulingPeriodMilliseconds = UtilsScheduler.millisecondsOneDay(),
        restCall = "http://localhost:7000/rest/v1/billing/retry",
        firstDayMonthExecution = false
    ).schedule()




}