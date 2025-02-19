package com.lootfilters.lang;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

@Value
@RequiredArgsConstructor
@ToString
public class Location {
  public static Location UNKNOWN = new Location(0, 0);

  int lineNumber;
  int charNumber;

}
