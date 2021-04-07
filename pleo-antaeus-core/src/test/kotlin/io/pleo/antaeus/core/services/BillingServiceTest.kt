package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class BillingServiceTest {

    private val invoicePending1 = Invoice(1, 1, Money(10.toBigDecimal(), Currency.EUR), InvoiceStatus.PENDING)
    private val invoicePending2 = Invoice(2, 1, Money(30.toBigDecimal(), Currency.EUR), InvoiceStatus.PENDING)

    private val invoicePaid = Invoice(3, 1, Money(10.toBigDecimal(), Currency.EUR), InvoiceStatus.PAID)
    private val invoiceFail = Invoice(4, 1, Money(10.toBigDecimal(), Currency.EUR), InvoiceStatus.FAIL)
    private val invoiceRetry = Invoice(5, 1, Money(10.toBigDecimal(), Currency.EUR), InvoiceStatus.RETRY)

    private val customer = Customer(id = 1, currency = Currency.EUR)
    private val customerWrongCurrency = Customer(id = 2, currency = Currency.DKK)




    @Test
    fun `change status of a Pending invoice to Paid after payment OK`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentService = paymentOK(),
            customerService = existingCustomer()
        )
        val invoiceAfterPay = billingService.processInvoice(invoicePending1)
        Assertions.assertEquals(InvoiceStatus.PAID, invoiceAfterPay.status)
    }

    @Test
    fun `change status of a Pending invoice to Fail if customer does not exist`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentService = paymentOK(),
            customerService = nonExistingCustomer()
        )
        val invoiceAfterPay = billingService.processInvoice(invoicePending1)
        Assertions.assertEquals(InvoiceStatus.FAIL, invoiceAfterPay.status)
    }


    @Test
    fun `change status of a Pending invoice to Fail if currency does not match`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentService = paymentOK(),
            customerService = existingCustomerWrongCurrency()
        )
        val invoiceAfterPay = billingService.processInvoice(invoicePending1)
        Assertions.assertEquals(InvoiceStatus.FAIL, invoiceAfterPay.status)
    }


    @Test
    fun `change status of a Pending invoice to Retry if payment is KO`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentService = paymentKO(),
            customerService = existingCustomer()
        )
        val invoiceAfterPay = billingService.processInvoice(invoicePending1)
        Assertions.assertEquals(InvoiceStatus.RETRY, invoiceAfterPay.status)
    }


    @Test
    fun `change status of a Pending invoice to Retry if network error during payment`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentService = paymentNetworkIssue(),
            customerService = existingCustomer()
        )
        val invoiceAfterPay = billingService.processInvoice(invoicePending1)
        Assertions.assertEquals(InvoiceStatus.RETRY, invoiceAfterPay.status)
    }


    @Test
    fun `paid invoice does not change status after being processed`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentService = paymentKO(),
            customerService = existingCustomer()
        )
        val invoiceAfterPay = billingService.processInvoice(invoicePaid)
        Assertions.assertEquals(InvoiceStatus.PAID, invoiceAfterPay.status)
    }



    @Test
    fun `change status of all Pending invoices to Paid after processing`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentService = paymentOK(),
            customerService = existingCustomer()
        )
        val counter = billingService.processPendingInvoices()
        Assertions.assertEquals(counter, 2)
    }

    @Test
    fun `change status of all Retry invoices to Paid after processing`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentService = paymentOK(),
            customerService = existingCustomer()
        )
        val counter = billingService.processRetryInvoices()
        Assertions.assertEquals(counter, 1)
    }



    /////////PRIVATE HELPERS TO MOCK CUSTOMER SERVICE
    private fun nonExistingCustomer(): CustomerService {
        return mockk {
            every { fetch(any()) }.throws(CustomerNotFoundException(1))
        }
    }

    private fun existingCustomer(): CustomerService {
        return mockk {
            every { fetch(any()) } returns customer
        }
    }


    private fun existingCustomerWrongCurrency(): CustomerService {
        return mockk {
            every { fetch(any()) } returns customerWrongCurrency
        }
    }

    /////////PRIVATE HELPERS TO MOCK PAYMENT PROVIDER
    private fun paymentOK(): PaymentService {
        return mockk {
            every { payInvoice(any()) } returns PaymentStatus.PAYMENT_SUCCESS
        }
    }

    private fun paymentKO(): PaymentService {
        return mockk {
            every{ payInvoice(any()) } returns PaymentStatus.PAYMENT_ERROR
        }
    }

    private fun paymentNetworkIssue(): PaymentService {
        return mockk {
            every{ payInvoice(any()) } returns PaymentStatus.PAYMENT_ERROR
        }
    }


    /////////PRIVATE HELPERS TO MOCK INVOICE SERVICE
    private fun updateStatusOK(): InvoiceService {
        return mockk {
            every { updateInvoiceStatus(any()) } returns Unit
            every {fetchAll()} returns listOf(invoicePending1,invoicePending2,invoiceFail,invoiceRetry,invoicePaid)
        }
    }

}
