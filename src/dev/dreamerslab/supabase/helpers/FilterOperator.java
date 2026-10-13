package dev.dreamerslab.supabase.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum FilterOperator implements OptionList<String> {
  /** The column equals the value. */
  Equals("eq"),
  /** The column doesn't equal the value. */
  NotEquals("neq"),
  /** The column is greater than the value. */
  GreaterThan("gt"),
  /** The column is greater than or equal to the value. */
  GreaterOrEqual("gte"),
  /** The column is less than the value. */
  LessThan("lt"),
  /** The column is less than or equal to the value. */
  LessOrEqual("lte"),
  /** The column matches a pattern, where % matches any text. Case-sensitive. */
  Like("like"),
  /** The column matches a pattern, where % matches any text. Ignores case. */
  LikeIgnoreCase("ilike"),
  /** The column is null, true or false. Use the text null, true or false as the value. */
  Is("is"),
  /** The column equals one of the values in a list. */
  In("in"),
  /** The array or JSON column contains every value in a list. */
  Contains("cs"),
  /** Every value of the array or JSON column is in a list. */
  ContainedBy("cd"),
  /** The array column shares at least one value with a list. */
  Overlaps("ov"),
  /** The text column matches a full-text search query. */
  TextSearch("fts");

  private static final Map<String, FilterOperator> lookup = new HashMap<>();

  static {
    for (FilterOperator option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  FilterOperator(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static FilterOperator fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
