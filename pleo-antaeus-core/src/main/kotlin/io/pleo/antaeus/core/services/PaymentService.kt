package io.pleo.antaeus.core.services


import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.PaymentStatus
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

class PaymentService (
    private val paymentProvider: PaymentProvider
){




    /**
     * Logic to make the payment of a single invoice by calling the payment provider
     * It returns a Payment Status based on the result returned by the provider
     */
    fun payInvoice(invoice : Invoice): PaymentStatus {
        return try {
            logger.info { "Invoice: ${invoice.id} to be sent to the Payment Provider"}
            if (paymentProvider.charge(invoice)) PaymentStatus.PAYMENT_SUCCESS else PaymentStatus.PAYMENT_ERROR
        } catch (e: NetworkException) {
            logger.error { "Invoice: ${invoice.id}. Network error"}
            PaymentStatus.NETWORK_ERROR
        }
    }


}