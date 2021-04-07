package io.pleo.antaeus.core.services


import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.PaymentStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging



private val logger = KotlinLogging.logger {}

class BillingService(
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService,
    private val paymentService: PaymentService
) {


    /**
     * Process all PENDING invoices by calling the payment provide
     *
     * Return the number of invoices successfully paid
     */
    fun processPendingInvoices() : Int {
        logger.info{"Processing payment of PENDING invoices"}
        var counter = 0
        invoiceService.fetchAll().
            filter{ invoice ->  invoice.status == InvoiceStatus.PENDING }.
            forEach{
                    invoice ->
                if (processInvoice(invoice).status == InvoiceStatus.PAID) {
                    counter++
                }
            }
        logger.info{"Processed successfully $counter  invoices"}
        return counter
    }


    /**
     * Process all RETRY invoices by calling the payment provide
     *
     * Return the number of invoices successfully paid
     */
    fun processRetryInvoices() : Int {
        logger.info{"Processing payment of RETRY invoices"}
        var counter = 0
        invoiceService.fetchAll().
            filter{ invoice ->  invoice.status == InvoiceStatus.RETRY }.
            forEach { invoice ->
                if (processInvoice(invoice).status == InvoiceStatus.PAID) {
                    counter++
                }
            }
        logger.info{"Processed successfully $counter  invoices"}
        return counter
    }

    /**
     * Invoice payment given the invoice ID
     */
    fun processInvoice(id : Int): Invoice? {
        try {
            val invoice: Invoice = invoiceService.fetch(id)
            return processInvoice(invoice)
        } catch(e: InvoiceNotFoundException) {
            logger.error { "Invoice: $id  does not exist"}
        }
        return null
    }

    /**
     * Check if the invoice is in a final state
     */
    private fun invoiceIsInFinalState(invoice : Invoice) : Boolean {
        return (invoice.status != InvoiceStatus.PENDING && invoice.status != InvoiceStatus.RETRY)
    }


    /**
     * Check if the consumer of an invoice is valid:
     * - Exists
     * - The currency in the customer is the same than the one in the invoice
     */
    private fun customerInInvoiceIsValid(invoice : Invoice) : Boolean {

        return try {
            val customer : Customer? = customerService.fetch(invoice.customerId)
            if (customer != null) {
                (customer.currency == invoice.amount.currency)
            } else {
                false
            }
        } catch(e : CustomerNotFoundException) {
            false
        }
    }




    /**
     * This is the method to process an invoice
     *
     * Performs invoice validation:
     * - The status of the invoice must be PENDING or RETRY -> No changes in the invoice
     * - The customer must exist in the database -> The invoice status will be set to FAIL
     * - The customer currency should also uses the same currency than the one in the invoice -> The invoice status will
     * be set to fail
     *
     * For all valid invoices, we will call the payment provider:
     * - If the payment is done, the status of the invoice will be set to PAID
     * - If the payment fails, the status of the invoice will be set to RETRY
     *
     * In case there are technical issues the status will be RETRY so a different scheduler can try later
     *
     * The method will return the invoice with the new status
     */
    fun processInvoice(invoice : Invoice): Invoice {
        logger.debug { "Processing payment of invoice:  ${invoice.id}" }

        var invoiceStatus: InvoiceStatus = InvoiceStatus.PAID
        val updatedInvoice: Invoice = invoice


        //NOTE: We will be dealing with consumer and currency validation from this service
        //We should call to external payment provider when the data is validated internally

        //Invoice Validation: We will only manage PENDING invoices. Early Return
        if (invoiceIsInFinalState(invoice)) {
            logger.error { "Invoice: ${invoice.id} won't be processed since the status is ${invoice.status}" }
            return invoice
        }


        //Invoice Validation: Customer is valid. Early return
        if (!customerInInvoiceIsValid(invoice)) {
            logger.error { "Invoice: ${invoice.id} customer ${invoice.customerId} is invalid" }
            invoiceStatus = InvoiceStatus.FAIL
        }


        //If no internal validation failed we can call the external payment provider
        if (invoiceStatus != InvoiceStatus.FAIL) {
            runBlocking {
                val paymentResult =
                    async { paymentService.payInvoice(invoice) }
                invoiceStatus = when (paymentResult.await()) {
                    PaymentStatus.NETWORK_ERROR -> {
                        logger.error { "Invoice: ${invoice.id}. Network error" }
                        InvoiceStatus.RETRY
                    }
                    PaymentStatus.PAYMENT_ERROR -> {
                        logger.error { "Invoice: ${invoice.id} payment failed" }
                        InvoiceStatus.RETRY
                    }
                    PaymentStatus.PAYMENT_SUCCESS -> {
                        logger.info { "Invoice: ${invoice.id} successfully processed and paid" }
                        InvoiceStatus.PAID
                    }
                }

            }
        }

        //Update the status of the invoice in the database
        updatedInvoice.status = invoiceStatus
        invoiceService.updateInvoiceStatus(updatedInvoice)
        return updatedInvoice
    }









}
