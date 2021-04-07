package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.mockk
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class PaymentServiceTest {


    private val invoicePending = Invoice(1, 1, Money(10.toBigDecimal(), Currency.EUR), InvoiceStatus.PENDING)



    @Test
    fun `returns a payment success state if the payment was made`() {
        val paymentService = PaymentService(
            paymentProvider = paymentOK()
        )
        val paymentStatus = paymentService.payInvoice(invoicePending)
        Assertions.assertEquals(paymentStatus, PaymentStatus.PAYMENT_SUCCESS)
    }

    @Test
    fun `returns a payment error state if the payment failed`() {
        val paymentService = PaymentService(
            paymentProvider = paymentKO()
        )
        val paymentStatus = paymentService.payInvoice(invoicePending)
        Assertions.assertEquals(paymentStatus, PaymentStatus.PAYMENT_ERROR)
    }


    @Test
    fun `returns a network error state if the provider throws a network exception`() {
        val paymentService = PaymentService(
            paymentProvider = paymentNetworkIssue()
        )
        val paymentStatus = paymentService.payInvoice(invoicePending)
        Assertions.assertEquals(paymentStatus, PaymentStatus.NETWORK_ERROR)
    }


    /////////PRIVATE HELPERS TO MOCK PAYMENT PROVIDER
    private fun paymentOK(): PaymentProvider {
        return mockk {
            every { charge(any()) } returns true
        }
    }

    private fun paymentKO(): PaymentProvider {
        return mockk {
            every { charge(any()) } returns false
        }
    }

    private fun paymentNetworkIssue(): PaymentProvider {
        return mockk {
            every { charge(any()) }.throws(NetworkException())
        }
    }


}
