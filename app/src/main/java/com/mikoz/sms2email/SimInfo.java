package io.github.sms2email.sms2email;

/** Details about the SIM / subscription that received an SMS. */
public class SimInfo {
  public static final int UNKNOWN_ID = -1;

  private final int subscriptionId;
  private final int slotIndex;
  private final String phoneNumber;
  private final String carrierName;
  private final String displayName;

  public SimInfo(
      int subscriptionId,
      int slotIndex,
      String phoneNumber,
      String carrierName,
      String displayName) {
    this.subscriptionId = subscriptionId;
    this.slotIndex = slotIndex;
    this.phoneNumber = phoneNumber;
    this.carrierName = carrierName;
    this.displayName = displayName;
  }

  public static SimInfo unknown() {
    return new SimInfo(UNKNOWN_ID, UNKNOWN_ID, null, null, null);
  }

  public int getSubscriptionId() {
    return subscriptionId;
  }

  public int getSlotIndex() {
    return slotIndex;
  }

  /** SIM's own phone number, or {@code null} when the platform did not report one. */
  public String getPhoneNumber() {
    return phoneNumber;
  }

  public String getCarrierName() {
    return carrierName;
  }

  /** User visible SIM label, e.g. "Personal". */
  public String getDisplayName() {
    return displayName;
  }

  /** Human readable SIM slot number, counting from 1 as shown in the system UI. */
  public int getSlotNumber() {
    return slotIndex >= 0 ? slotIndex + 1 : UNKNOWN_ID;
  }

  public SimInfo withPhoneNumber(String number) {
    return new SimInfo(subscriptionId, slotIndex, number, carrierName, displayName);
  }

  @Override
  public String toString() {
    return "SimInfo{subscriptionId="
        + subscriptionId
        + ", slotIndex="
        + slotIndex
        + ", phoneNumber="
        + phoneNumber
        + ", carrierName="
        + carrierName
        + ", displayName="
        + displayName
        + '}';
  }
}
