package dev.dreamerslab.supabase.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum ChannelStatus implements OptionList<String> {
  /** The channel is connecting. */
  Subscribing("Subscribing"),
  /** The channel is connected and receiving. */
  Subscribed("Subscribed"),
  /** The channel is disconnected. */
  Unsubscribed("Unsubscribed");

  private static final Map<String, ChannelStatus> lookup = new HashMap<>();

  static {
    for (ChannelStatus option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  ChannelStatus(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static ChannelStatus fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
