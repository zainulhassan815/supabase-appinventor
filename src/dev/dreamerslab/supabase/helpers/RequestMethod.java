package dev.dreamerslab.supabase.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum RequestMethod implements OptionList<String> {
  /** Sends the body to the function. */
  Post("POST"),
  /** Asks the function for data, without a body. */
  Get("GET"),
  /** Replaces data, with a body. */
  Put("PUT"),
  /** Changes part of the data, with a body. */
  Patch("PATCH"),
  /** Deletes data. */
  Delete("DELETE");

  private static final Map<String, RequestMethod> lookup = new HashMap<>();

  static {
    for (RequestMethod option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  RequestMethod(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static RequestMethod fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
