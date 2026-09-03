package io.github.sms2email.sms2email;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** Renders a received SMS and its metadata into the forwarded email subject and body. */
public class ReceivedSmsFormatter {
  private static final String SEPARATOR = "----------------------------------------";
  private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss z";
  private static final String UNKNOWN_SIM = "unknown SIM";
  private static final String UNKNOWN_SENDER = "unknown sender";
  private static final String NUMBER_UNKNOWN_HINT = "number unknown, set it in the app";

  private ReceivedSmsFormatter() {}

  /** Sender to show as the email's display name. */
  public static String formatSender(SmsMetadata metadata) {
    return valueOrDefault(metadata.getSender(), UNKNOWN_SENDER);
  }

  public static String formatSubject(SmsMetadata metadata) {
    String sender = formatSender(metadata);
    String receiver = shortReceiverLabel(metadata);
    if (receiver == null) {
      return "SMS from " + sender;
    }
    return "SMS from " + sender + " to " + receiver;
  }

  public static String formatBody(SmsMetadata metadata) {
    return formatBody(metadata, TimeZone.getDefault());
  }

  public static String formatBody(SmsMetadata metadata, TimeZone timeZone) {
    List<String> lines = new ArrayList<>();
    lines.add("Received on: " + receivedOnLine(metadata));
    lines.add("From: " + formatSender(metadata));
    addIfPresent(lines, "Sent", formatTimestamp(metadata.getSentAtMillis(), timeZone));
    addIfPresent(lines, "Received", formatTimestamp(metadata.getReceivedAtMillis(), timeZone));
    addIfPresent(lines, "SIM", simLine(metadata));
    addIfPresent(lines, "Service center", metadata.getServiceCenterAddress());
    if (metadata.getPartCount() > 0) {
      lines.add("Parts: " + metadata.getPartCount());
    }
    addIfPresent(lines, "Subject", metadata.getPseudoSubject());
    addIfPresent(lines, "Email gateway sender", metadata.getEmailGatewaySender());

    StringBuilder builder = new StringBuilder();
    for (String line : lines) {
      builder.append(line).append('\n');
    }
    builder.append('\n').append(SEPARATOR).append('\n');
    builder.append(metadata.getBody() == null ? "" : metadata.getBody());
    return builder.toString();
  }

  /** The "Received on" value, preferring the SIM's number and degrading to whatever is known. */
  private static String receivedOnLine(SmsMetadata metadata) {
    String simDescription = simDescription(metadata);
    if (isPresent(metadata.getReceivingNumber())) {
      if (simDescription == null) {
        return metadata.getReceivingNumber().trim();
      }
      return metadata.getReceivingNumber().trim() + " (" + simDescription + ")";
    }
    if (simDescription != null) {
      return simDescription + " (" + NUMBER_UNKNOWN_HINT + ")";
    }
    return UNKNOWN_SIM + " (" + NUMBER_UNKNOWN_HINT + ")";
  }

  /** Compact SIM description such as "SIM 1 - T-Mobile". */
  private static String simDescription(SmsMetadata metadata) {
    List<String> parts = new ArrayList<>();
    if (metadata.getSimSlotNumber() > 0) {
      parts.add("SIM " + metadata.getSimSlotNumber());
    }
    String carrier = firstPresent(metadata.getCarrierName(), metadata.getSimLabel());
    if (carrier != null) {
      parts.add(carrier);
    }
    return parts.isEmpty() ? null : join(parts, " - ");
  }

  private static String simLine(SmsMetadata metadata) {
    List<String> parts = new ArrayList<>();
    if (metadata.getSimSlotNumber() > 0) {
      parts.add("slot " + metadata.getSimSlotNumber());
    }
    if (metadata.getSubscriptionId() >= 0) {
      parts.add("subscription id " + metadata.getSubscriptionId());
    }
    if (isPresent(metadata.getSimLabel())) {
      parts.add("label \"" + metadata.getSimLabel().trim() + "\"");
    }
    if (isPresent(metadata.getCarrierName())) {
      parts.add("carrier " + metadata.getCarrierName().trim());
    }
    return parts.isEmpty() ? null : join(parts, ", ");
  }

  /** Value used in the subject line to say where the SMS landed. */
  private static String shortReceiverLabel(SmsMetadata metadata) {
    if (isPresent(metadata.getReceivingNumber())) {
      return metadata.getReceivingNumber().trim();
    }
    return simDescription(metadata);
  }

  private static String formatTimestamp(long millis, TimeZone timeZone) {
    if (millis <= 0) {
      return null;
    }
    SimpleDateFormat format = new SimpleDateFormat(TIMESTAMP_PATTERN, Locale.US);
    format.setTimeZone(timeZone);
    return format.format(new Date(millis));
  }

  private static void addIfPresent(List<String> lines, String label, String value) {
    if (isPresent(value)) {
      lines.add(label + ": " + value.trim());
    }
  }

  private static String firstPresent(String first, String second) {
    if (isPresent(first)) {
      return first.trim();
    }
    return isPresent(second) ? second.trim() : null;
  }

  private static String join(List<String> parts, String delimiter) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < parts.size(); i++) {
      if (i > 0) {
        builder.append(delimiter);
      }
      builder.append(parts.get(i));
    }
    return builder.toString();
  }

  private static String valueOrDefault(String value, String fallback) {
    return isPresent(value) ? value.trim() : fallback;
  }

  private static boolean isPresent(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
