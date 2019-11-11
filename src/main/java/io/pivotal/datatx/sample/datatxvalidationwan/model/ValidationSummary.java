package io.pivotal.datatx.sample.datatxvalidationwan.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class ValidationSummary {
  String regionName;
  boolean entryCount;
  boolean keyMatcher;
}
