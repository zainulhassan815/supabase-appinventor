package dev.dreamerslab.supabase.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum SessionStatus implements OptionList<String> {
  /** A user is signed in. */
  SignedIn("SignedIn"),
  /** No user is signed in. */
  SignedOut("SignedOut"),
  /** The saved session couldn't be refreshed, usually because the device is offline. */
  RefreshFailed("RefreshFailed");

  private static final Map<String, SessionStatus> lookup = new HashMap<>();

  static {
    for (SessionStatus option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  SessionStatus(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static SessionStatus fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
