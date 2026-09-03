package io.github.sms2email.sms2email;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import java.util.Objects;

public class SmsReceiver extends BroadcastReceiver {
  protected MailSender mailSender = new MailSender();

  @Override
  public void onReceive(Context context, Intent intent) {
    if (Objects.equals(intent.getAction(), Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
      SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
      if (messages == null || messages.length == 0) {
        return;
      }

      StringBuilder bodyText = new StringBuilder();
      for (SmsMessage message : messages) {
        bodyText.append(message.getMessageBody());
      }

      SmsMetadata metadata = describe(messages, bodyText.toString());
      SimInfo simInfo =
          SimInfoResolver.resolve(
              context,
              intent,
              PreferencesManager.getConfigBlocking(context).getSimNumberOverrides());
      metadata.setSim(simInfo);

      mailSender.send(
          context,
          ReceivedSmsFormatter.formatSender(metadata),
          ReceivedSmsFormatter.formatSubject(metadata),
          ReceivedSmsFormatter.formatBody(metadata));
    }
  }

  private SmsMetadata describe(SmsMessage[] messages, String body) {
    SmsMessage first = messages[0];
    SmsMetadata metadata =
        new SmsMetadata()
            .setSender(sender(first))
            .setBody(body)
            .setPartCount(messages.length)
            .setSentAtMillis(first.getTimestampMillis())
            .setReceivedAtMillis(System.currentTimeMillis())
            .setServiceCenterAddress(first.getServiceCenterAddress())
            .setPseudoSubject(first.getPseudoSubject());
    if (first.isEmail()) {
      metadata.setEmailGatewaySender(first.getEmailFrom());
    }
    return metadata;
  }

  private String sender(SmsMessage message) {
    String sender = message.getOriginatingAddress();
    if (sender == null || sender.trim().isEmpty()) {
      sender = message.getDisplayOriginatingAddress();
    }
    return sender;
  }
}
