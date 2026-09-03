package io.github.sms2email.sms2email

import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceivedSmsFormatterTest {
  private val utc = TimeZone.getTimeZone("UTC")

  private fun metadata() =
      SmsMetadata()
          .setSender("+15553334444")
          .setBody("Hello there")
          .setReceivingNumber("+15551112222")
          .setSimSlotNumber(2)
          .setSubscriptionId(4)
          .setSimLabel("Personal")
          .setCarrierName("T-Mobile")
          .setSentAtMillis(1_772_000_000_000L)
          .setReceivedAtMillis(1_772_000_002_000L)
          .setServiceCenterAddress("+12063130004")
          .setPartCount(2)

  @Test
  fun body_startsWithReceivingNumberAndListsMetadata() {
    val body = ReceivedSmsFormatter.formatBody(metadata(), utc)

    assertEquals(
        listOf(
            "Received on: +15551112222 (SIM 2 - T-Mobile)",
            "From: +15553334444",
            "Sent: 2026-02-25 06:13:20 UTC",
            "Received: 2026-02-25 06:13:22 UTC",
            "SIM: slot 2, subscription id 4, label \"Personal\", carrier T-Mobile",
            "Service center: +12063130004",
            "Parts: 2",
            "",
            "----------------------------------------",
            "Hello there",
        ),
        body.lines(),
    )
  }

  @Test
  fun body_omitsUnknownFields() {
    val body =
        ReceivedSmsFormatter.formatBody(
            SmsMetadata().setSender("+15553334444").setBody("Hi"),
            utc,
        )

    assertEquals(
        listOf(
            "Received on: unknown SIM (number unknown, set it in the app)",
            "From: +15553334444",
            "",
            "----------------------------------------",
            "Hi",
        ),
        body.lines(),
    )
  }

  @Test
  fun body_fallsBackToSimDescriptionWhenNumberUnknown() {
    val body =
        ReceivedSmsFormatter.formatBody(
            metadata().setReceivingNumber(null),
            utc,
        )

    assertTrue(
        body.startsWith("Received on: SIM 2 - T-Mobile (number unknown, set it in the app)\n"),
    )
  }

  @Test
  fun body_includesEmailGatewayDetailsWhenPresent() {
    val body =
        ReceivedSmsFormatter.formatBody(
            metadata().setPseudoSubject("Alert").setEmailGatewaySender("alerts@example.com"),
            utc,
        )

    assertTrue(body.contains("\nSubject: Alert\n"))
    assertTrue(body.contains("\nEmail gateway sender: alerts@example.com\n"))
  }

  @Test
  fun subject_namesSenderAndReceivingNumber() {
    assertEquals(
        "SMS from +15553334444 to +15551112222",
        ReceivedSmsFormatter.formatSubject(metadata()),
    )
  }

  @Test
  fun subject_fallsBackToSimDescription() {
    assertEquals(
        "SMS from +15553334444 to SIM 2 - T-Mobile",
        ReceivedSmsFormatter.formatSubject(metadata().setReceivingNumber(null)),
    )
  }

  @Test
  fun subject_omitsReceiverWhenNothingIsKnown() {
    assertEquals(
        "SMS from +15553334444",
        ReceivedSmsFormatter.formatSubject(SmsMetadata().setSender("+15553334444")),
    )
  }
}
