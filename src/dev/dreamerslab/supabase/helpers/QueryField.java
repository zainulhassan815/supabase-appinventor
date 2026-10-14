package dev.dreamerslab.supabase.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum QueryField implements OptionList<String> {
  /** The columns to read, comma-separated. Leave it out for all of them. */
  Columns("columns"),
  /** A list of filters made with the Filter block. A row must match every one. */
  Filters("filters"),
  /** A column, or several separated by commas, each optionally followed by asc or desc, such as "done, created_at desc". */
  OrderBy("orderBy"),
  /** How many rows to read at most. Leave it out for no limit. */
  Limit("limit"),
  /** How many rows to skip first, for paging. */
  Offset("offset");

  private static final Map<String, QueryField> lookup = new HashMap<>();

  static {
    for (QueryField option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  QueryField(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static QueryField fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
