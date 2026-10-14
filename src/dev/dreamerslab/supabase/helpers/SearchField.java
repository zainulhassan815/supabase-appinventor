package dev.dreamerslab.supabase.helpers;

import com.google.appinventor.components.common.OptionList;
import java.util.HashMap;
import java.util.Map;

public enum SearchField implements OptionList<String> {
  /** Only files whose names contain this text. */
  Search("search"),
  /** name, created_at, updated_at or last_accessed_at, optionally followed by asc or desc, such as "created_at desc". Sorts by name when left out. */
  SortBy("sortBy"),
  /** How many files to return at most. 100 when left out. */
  Limit("limit"),
  /** How many files to skip first, for paging. */
  Offset("offset");

  private static final Map<String, SearchField> lookup = new HashMap<>();

  static {
    for (SearchField option : values()) {
      lookup.put(option.value, option);
    }
  }

  private final String value;

  SearchField(String value) {
    this.value = value;
  }

  public String toUnderlyingValue() {
    return value;
  }

  public static SearchField fromUnderlyingValue(String value) {
    return lookup.get(value);
  }
}
