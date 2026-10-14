package dev.dreamerslab.supabase.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum Region implements OptionList<String> {
  /** The region closest to the user. */
  Any("any"),
  /** Tokyo. */
  ApNortheast1("ap-northeast-1"),
  /** Seoul. */
  ApNortheast2("ap-northeast-2"),
  /** Mumbai. */
  ApSouth1("ap-south-1"),
  /** Singapore. */
  ApSoutheast1("ap-southeast-1"),
  /** Sydney. */
  ApSoutheast2("ap-southeast-2"),
  /** Canada. */
  CaCentral1("ca-central-1"),
  /** Frankfurt. */
  EuCentral1("eu-central-1"),
  /** Ireland. */
  EuWest1("eu-west-1"),
  /** London. */
  EuWest2("eu-west-2"),
  /** Paris. */
  EuWest3("eu-west-3"),
  /** São Paulo. */
  SaEast1("sa-east-1"),
  /** North Virginia. */
  UsEast1("us-east-1"),
  /** North California. */
  UsWest1("us-west-1"),
  /** Oregon. */
  UsWest2("us-west-2");

  private static final Map<String, Region> lookup = new HashMap<>();

  static {
    for (Region option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  Region(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static Region fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
