package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Customer
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging


private val logger = KotlinLogging.logger {}

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val customerService: CustomerService,
    private val invoiceService: InvoiceService
) {


    /**
     * Process all PENDING invoices by calling the payment provide
     *
     * Return the number of invoices successfully paid
     */
    fun processPendingInvoices() : Int {
        logger.debug{"Processing payment of PENDING invoices"}
        var counter = 0
        invoiceService.fetchAll().
            filter{ invoice ->  invoice.status == InvoiceStatus.PENDING }.
            forEach{
                    invoice ->
                if (payInvoice(invoice).status == InvoiceStatus.PAID) {
                    counter++
                }
            }
        logger.debug{"Processed successfully $counter  invoices"}
        return counter
    }


    /**
     * Process all RETRY invoices by calling the payment provide
     *
     * Return the number of invoices successfully paid
     */
    fun processRetryInvoices() : Int {
        logger.debug{"Processing payment of RETRY invoices"}
        var counter = 0
        invoiceService.fetchAll().
            filter{ invoice ->  invoice.status == InvoiceStatus.RETRY }.
            forEach { invoice ->
                if (payInvoice(invoice).status == InvoiceStatus.PAID) {
                    counter++
                }
            }
        logger.debug{"Processed successfully $counter  invoices"}
        return counter
    }

    /**
     * Invoice payment given the invoice ID
     */
    fun payInvoice(id : Int): Invoice? {
        try {
            val invoice: Invoice = invoiceService.fetch(id)
            return payInvoice(invoice)
        } catch(e: InvoiceNotFoundException) {
            logger.error { "Invoice: $id  does not exist"}
        }
        return null
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
     * - If the payment fails, the status of the invoice will be set to FAIL
     *
     * In case there are technical issues the status will be RETRY so a different scheduler can try later
     *
     * The method will return the invoice with the new status
     */
    fun payInvoice(invoice : Invoice): Invoice {
        logger.debug{"Processing payment of invoice:  ${invoice.id}"}

        var invoiceStatus : InvoiceStatus = InvoiceStatus.PAID
        val updatedInvoice: Invoice = invoice
        var customer : Customer? = null

        //NOTE: We will be dealing with consumer and currency validation from this service
        //We should call to external payment provider when the data is validated internally

        //Invoice Validation: We will only manage PENDING invoices. Early Return
        if (invoice.status != InvoiceStatus.PENDING && invoice.status != InvoiceStatus.RETRY) {
            logger.error { "Invoice: ${invoice.id} won't be processed since the status is ${invoice.status}"}
            return invoice
        }

        //Invoice Validation: Consumer exists
        try {
            customer = customerService.fetch(invoice.customerId)
        } catch(e : CustomerNotFoundException) {
            logger.error { "Invoice: ${invoice.id} customer ${invoice.customerId} does not exist"}
            invoiceStatus = InvoiceStatus.FAIL
        }


        //Invoice Validation: Currency. No need to charge
        if (customer != null) {
            if (customer.currency != invoice.amount.currency) {
                logger.error { "Invoice: ${invoice.id} customer ${invoice.customerId} different currencies"}
                invoiceStatus = InvoiceStatus.FAIL
            }
        }

        //If no internal validation failed we can call the external payment provider
        if (invoiceStatus != InvoiceStatus.FAIL) {
            //Otherwise we can try to charge the invoice through the Payment Service
            invoiceStatus = try {
                if (paymentProvider.charge(invoice)) {
                    logger.info { "Invoice: ${invoice.id} successfully processed and paid"}
                    InvoiceStatus.PAID
                } else {
                    logger.error { "Invoice: ${invoice.id} payment failed"}
                    InvoiceStatus.FAIL
                }

            } catch (e: NetworkException) {
                //Retry only in case of network issues
                logger.error { "Invoice: ${invoice.id}. Network error"}
                InvoiceStatus.RETRY
            }
        }

        updatedInvoice.status = invoiceStatus

        //Update the status of the invoice in the database
        invoiceService.updateInvoiceStatus(invoice)

        return updatedInvoice

    }

}
