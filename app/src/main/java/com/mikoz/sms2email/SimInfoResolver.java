package io.github.sms2email.sms2email;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Figures out which SIM / subscription an incoming SMS arrived on.
 *
 * <p>The receiving subscription is only advertised through extras on the {@code SMS_RECEIVED}
 * broadcast, and the key used depends on the Android version and OEM, so several are probed.
 */
public class SimInfoResolver {
  private static final String TAG = "SimInfoResolver";

  /** Extra keys that have carried the receiving subscription id across Android versions. */
  private static final String[] SUBSCRIPTION_EXTRA_KEYS = {
    "subscription", "android.telephony.extra.SUBSCRIPTION_INDEX", "subscription_id", "simId",
  };

  /** Extra keys that have carried the receiving SIM slot across Android versions. */
  private static final String[] SLOT_EXTRA_KEYS = {
    "slot", "simSlotIndex", "phone", "android.telephony.extra.SLOT_INDEX",
  };

  private SimInfoResolver() {}

  /**
   * Resolves the SIM that received the broadcast.
   *
   * @param simNumberOverrides user configured numbers keyed by slot index, used when the platform
   *     does not report the SIM's own number
   */
  public static SimInfo resolve(
      Context context, Intent intent, Map<Integer, String> simNumberOverrides) {
    int subscriptionId = readIntExtra(intent, SUBSCRIPTION_EXTRA_KEYS);
    int slotIndex = readIntExtra(intent, SLOT_EXTRA_KEYS);
    Log.d(TAG, "SMS intent extras gave subscriptionId=" + subscriptionId + " slot=" + slotIndex);

    SubscriptionInfo subscription = findSubscription(context, subscriptionId, slotIndex);
    SimInfo simInfo = toSimInfo(subscription, subscriptionId, slotIndex);

    if (isBlank(simInfo.getPhoneNumber())) {
      simInfo = simInfo.withPhoneNumber(readNumber(context, simInfo.getSubscriptionId()));
    }
    if (isBlank(simInfo.getPhoneNumber())) {
      simInfo = simInfo.withPhoneNumber(overrideFor(simNumberOverrides, simInfo.getSlotIndex()));
    }

    Log.d(TAG, "Resolved receiving SIM: " + simInfo);
    return simInfo;
  }

  /** Active SIMs, used by the settings screen to let the user fill in missing numbers. */
  public static List<SimInfo> listActiveSims(Context context) {
    if (!hasPhoneStatePermission(context)) {
      return Collections.emptyList();
    }
    SubscriptionManager subscriptionManager = subscriptionManager(context);
    if (subscriptionManager == null) {
      return Collections.emptyList();
    }

    List<SubscriptionInfo> subscriptions;
    try {
      subscriptions = subscriptionManager.getActiveSubscriptionInfoList();
    } catch (RuntimeException e) {
      Log.w(TAG, "Unable to list active subscriptions", e);
      return Collections.emptyList();
    }
    if (subscriptions == null) {
      return Collections.emptyList();
    }

    List<SimInfo> sims = new ArrayList<>();
    for (SubscriptionInfo subscription : subscriptions) {
      if (subscription == null) {
        continue;
      }
      SimInfo simInfo =
          toSimInfo(subscription, subscription.getSubscriptionId(), subscription.getSimSlotIndex());
      if (isBlank(simInfo.getPhoneNumber())) {
        simInfo = simInfo.withPhoneNumber(readNumber(context, simInfo.getSubscriptionId()));
      }
      sims.add(simInfo);
    }
    return sims;
  }

  private static SimInfo toSimInfo(
      SubscriptionInfo subscription, int fallbackSubscriptionId, int fallbackSlotIndex) {
    if (subscription == null) {
      return new SimInfo(fallbackSubscriptionId, fallbackSlotIndex, null, null, null);
    }
    String number = null;
    try {
      number = subscription.getNumber();
    } catch (SecurityException e) {
      Log.w(TAG, "Not allowed to read the subscription number", e);
    }
    return new SimInfo(
        subscription.getSubscriptionId(),
        subscription.getSimSlotIndex(),
        number,
        toStringOrNull(subscription.getCarrierName()),
        toStringOrNull(subscription.getDisplayName()));
  }

  private static SubscriptionInfo findSubscription(Context context, int subscriptionId, int slot) {
    if (!hasPhoneStatePermission(context)) {
      Log.w(TAG, "READ_PHONE_STATE not granted, cannot identify the receiving SIM");
      return null;
    }
    SubscriptionManager subscriptionManager = subscriptionManager(context);
    if (subscriptionManager == null) {
      return null;
    }

    try {
      if (subscriptionId >= 0) {
        SubscriptionInfo subscription =
            subscriptionManager.getActiveSubscriptionInfo(subscriptionId);
        if (subscription != null) {
          return subscription;
        }
      }
      if (slot >= 0) {
        return subscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(slot);
      }
    } catch (RuntimeException e) {
      Log.w(TAG, "Unable to look up the receiving subscription", e);
    }
    return null;
  }

  private static String readNumber(Context context, int subscriptionId) {
    if (subscriptionId < 0 || !hasPhoneNumbersPermission(context)) {
      return null;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      SubscriptionManager subscriptionManager = subscriptionManager(context);
      if (subscriptionManager != null) {
        try {
          String number = subscriptionManager.getPhoneNumber(subscriptionId);
          if (!isBlank(number)) {
            return number;
          }
        } catch (RuntimeException e) {
          Log.w(TAG, "SubscriptionManager.getPhoneNumber failed", e);
        }
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      TelephonyManager telephonyManager =
          (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
      if (telephonyManager != null) {
        try {
          String number = telephonyManager.createForSubscriptionId(subscriptionId).getLine1Number();
          if (!isBlank(number)) {
            return number;
          }
        } catch (RuntimeException e) {
          Log.w(TAG, "TelephonyManager.getLine1Number failed", e);
        }
      }
    }
    return null;
  }

  private static String overrideFor(Map<Integer, String> simNumberOverrides, int slotIndex) {
    if (simNumberOverrides == null || slotIndex < 0) {
      return null;
    }
    String override = simNumberOverrides.get(slotIndex);
    return isBlank(override) ? null : override.trim();
  }

  private static SubscriptionManager subscriptionManager(Context context) {
    return (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
  }

  private static int readIntExtra(Intent intent, String[] keys) {
    if (intent == null) {
      return SimInfo.UNKNOWN_ID;
    }
    for (String key : keys) {
      if (!intent.hasExtra(key)) {
        continue;
      }
      int value = intent.getIntExtra(key, SimInfo.UNKNOWN_ID);
      if (value >= 0) {
        return value;
      }
    }
    return SimInfo.UNKNOWN_ID;
  }

  private static boolean hasPhoneStatePermission(Context context) {
    return isGranted(context, Manifest.permission.READ_PHONE_STATE);
  }

  private static boolean hasPhoneNumbersPermission(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        && isGranted(context, Manifest.permission.READ_PHONE_NUMBERS)) {
      return true;
    }
    return hasPhoneStatePermission(context);
  }

  private static boolean isGranted(Context context, String permission) {
    return ContextCompat.checkSelfPermission(context, permission)
        == PackageManager.PERMISSION_GRANTED;
  }

  private static String toStringOrNull(CharSequence value) {
    return value == null ? null : value.toString();
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
