package io.pivotal.datatx.sample.datatxvalidationwan.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class Customer {
  String accountNumber;
  String firstName;
  String lastName;
  String email;
  String phone;
}
