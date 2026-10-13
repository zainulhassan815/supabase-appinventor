package dev.dreamerslab.supabase.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum ChangeEvent implements OptionList<String> {
  /** Any change. */
  All("*"),
  /** A row was added. */
  Insert("INSERT"),
  /** A row was changed. */
  Update("UPDATE"),
  /** A row was deleted. */
  Delete("DELETE");

  private static final Map<String, ChangeEvent> lookup = new HashMap<>();

  static {
    for (ChangeEvent option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  ChangeEvent(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static ChangeEvent fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
