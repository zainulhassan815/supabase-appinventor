package dev.dreamerslab.supabase.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum CodeType implements OptionList<String> {
  /** A code from SendEmailCode, to sign in. */
  SignIn("email"),
  /** A code from ResetPassword, to sign in and then set a new password with UpdateUser. */
  PasswordReset("recovery");

  private static final Map<String, CodeType> lookup = new HashMap<>();

  static {
    for (CodeType option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  CodeType(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static CodeType fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
