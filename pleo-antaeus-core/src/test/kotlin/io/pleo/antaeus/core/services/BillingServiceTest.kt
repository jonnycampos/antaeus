package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
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
    fun `change status of a Pending invoice after payment OK`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentProvider = paymentOK(),
            customerService = existingCustomer()
        )
        val invoiceAfterPay = billingService.payInvoice(invoicePending1)
        Assertions.assertEquals(InvoiceStatus.PAID, invoiceAfterPay.status)
    }

    @Test
    fun `payment of an invoice with non existing consumer`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentProvider = paymentOK(),
            customerService = nonExistingCustomer()
        )
        val invoiceAfterPay = billingService.payInvoice(invoicePending1)
        Assertions.assertEquals(InvoiceStatus.FAIL, invoiceAfterPay.status)
    }


    @Test
    fun `payment of an invoice with currency mismatch`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentProvider = paymentOK(),
            customerService = existingCustomerWrongCurrency()
        )
        val invoiceAfterPay = billingService.payInvoice(invoicePending1)
        Assertions.assertEquals(InvoiceStatus.FAIL, invoiceAfterPay.status)
    }


    @Test
    fun `change of status of an invoice after the payment fails`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentProvider = paymentKO(),
            customerService = existingCustomer()
        )
        val invoiceAfterPay = billingService.payInvoice(invoicePending1)
        Assertions.assertEquals(InvoiceStatus.RETRY, invoiceAfterPay.status)
    }


    @Test
    fun `payment of an invoice with network issue`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentProvider = paymentNetworkIssue(),
            customerService = existingCustomer()
        )
        val invoiceAfterPay = billingService.payInvoice(invoicePending1)
        Assertions.assertEquals(InvoiceStatus.RETRY, invoiceAfterPay.status)
    }


    @Test
    fun `payment of an invoice with PAID status`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentProvider = paymentKO(),
            customerService = existingCustomer()
        )
        val invoiceAfterPay = billingService.payInvoice(invoicePaid)
        Assertions.assertEquals(InvoiceStatus.PAID, invoiceAfterPay.status)
    }



    @Test
    fun `payment of PENDING invoices`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentProvider = paymentOK(),
            customerService = existingCustomer()
        )
        val counter = billingService.processPendingInvoices()
        Assertions.assertEquals(counter, 2)
    }

    @Test
    fun `payment of RETRY invoices`() {
        val billingService = BillingService(
            invoiceService = updateStatusOK(),
            paymentProvider = paymentOK(),
            customerService = existingCustomer()
        )
        val counter = billingService.processRetryInvoices()
        Assertions.assertEquals(counter, 1)
    }



    /////////PRIVATE HELPERS TO MOCK CUSTOMER SERVICE
    private fun nonExistingCustomer(): CustomerService {
        return mockk {
            every<Customer> { fetch(any()) }.throws(CustomerNotFoundException(1))
        }
    }

    private fun existingCustomer(): CustomerService {
        return mockk {
            every<Customer> { fetch(any()) } returns customer
        }
    }


    private fun existingCustomerWrongCurrency(): CustomerService {
        return mockk {
            every<Customer> { fetch(any()) } returns customerWrongCurrency
        }
    }

    /////////PRIVATE HELPERS TO MOCK PAYMENT PROVIDER
    private fun paymentOK(): PaymentProvider {
        return mockk {
            every<Boolean> { charge(any()) } returns true
        }
    }

    private fun paymentKO(): PaymentProvider {
        return mockk {
            every<Boolean> { charge(any()) } returns false
        }
    }

    private fun paymentNetworkIssue(): PaymentProvider {
        return mockk {
            every<Boolean> { charge(any()) }.throws(NetworkException())
        }
    }


    /////////PRIVATE HELPERS TO MOCK INVOICE SERVICE
    private fun updateStatusOK(): InvoiceService {
        return mockk {
            every<Unit> { updateInvoiceStatus(any()) } returns Unit
            every {fetchAll()} returns listOf(invoicePending1,invoicePending2,invoiceFail,invoiceRetry,invoicePaid)
        }
    }

}
