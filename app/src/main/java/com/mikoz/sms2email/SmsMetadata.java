package io.github.sms2email.sms2email;

/**
 * Everything worth reporting about a received SMS.
 *
 * <p>Deliberately free of Android types so that {@link ReceivedSmsFormatter} stays unit testable.
 */
public class SmsMetadata {
  private String sender;
  private String body;
  private String receivingNumber;
  private int simSlotNumber = SimInfo.UNKNOWN_ID;
  private int subscriptionId = SimInfo.UNKNOWN_ID;
  private String simLabel;
  private String carrierName;
  private long sentAtMillis;
  private long receivedAtMillis;
  private String serviceCenterAddress;
  private int partCount;
  private String pseudoSubject;
  private String emailGatewaySender;

  public String getSender() {
    return sender;
  }

  public SmsMetadata setSender(String sender) {
    this.sender = sender;
    return this;
  }

  public String getBody() {
    return body;
  }

  public SmsMetadata setBody(String body) {
    this.body = body;
    return this;
  }

  public String getReceivingNumber() {
    return receivingNumber;
  }

  public SmsMetadata setReceivingNumber(String receivingNumber) {
    this.receivingNumber = receivingNumber;
    return this;
  }

  public int getSimSlotNumber() {
    return simSlotNumber;
  }

  public SmsMetadata setSimSlotNumber(int simSlotNumber) {
    this.simSlotNumber = simSlotNumber;
    return this;
  }

  public int getSubscriptionId() {
    return subscriptionId;
  }

  public SmsMetadata setSubscriptionId(int subscriptionId) {
    this.subscriptionId = subscriptionId;
    return this;
  }

  public String getSimLabel() {
    return simLabel;
  }

  public SmsMetadata setSimLabel(String simLabel) {
    this.simLabel = simLabel;
    return this;
  }

  public String getCarrierName() {
    return carrierName;
  }

  public SmsMetadata setCarrierName(String carrierName) {
    this.carrierName = carrierName;
    return this;
  }

  /** Timestamp written by the service center, i.e. when the sender's network accepted the SMS. */
  public long getSentAtMillis() {
    return sentAtMillis;
  }

  public SmsMetadata setSentAtMillis(long sentAtMillis) {
    this.sentAtMillis = sentAtMillis;
    return this;
  }

  public long getReceivedAtMillis() {
    return receivedAtMillis;
  }

  public SmsMetadata setReceivedAtMillis(long receivedAtMillis) {
    this.receivedAtMillis = receivedAtMillis;
    return this;
  }

  public String getServiceCenterAddress() {
    return serviceCenterAddress;
  }

  public SmsMetadata setServiceCenterAddress(String serviceCenterAddress) {
    this.serviceCenterAddress = serviceCenterAddress;
    return this;
  }

  public int getPartCount() {
    return partCount;
  }

  public SmsMetadata setPartCount(int partCount) {
    this.partCount = partCount;
    return this;
  }

  public String getPseudoSubject() {
    return pseudoSubject;
  }

  public SmsMetadata setPseudoSubject(String pseudoSubject) {
    this.pseudoSubject = pseudoSubject;
    return this;
  }

  public String getEmailGatewaySender() {
    return emailGatewaySender;
  }

  public SmsMetadata setEmailGatewaySender(String emailGatewaySender) {
    this.emailGatewaySender = emailGatewaySender;
    return this;
  }

  public SmsMetadata setSim(SimInfo simInfo) {
    if (simInfo == null) {
      return this;
    }
    this.receivingNumber = simInfo.getPhoneNumber();
    this.simSlotNumber = simInfo.getSlotNumber();
    this.subscriptionId = simInfo.getSubscriptionId();
    this.simLabel = simInfo.getDisplayName();
    this.carrierName = simInfo.getCarrierName();
    return this;
  }
}
