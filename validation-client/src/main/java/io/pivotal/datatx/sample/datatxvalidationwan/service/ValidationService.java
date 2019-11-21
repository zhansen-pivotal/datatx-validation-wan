package io.pivotal.datatx.sample.datatxvalidationwan.service;

import io.pivotal.datatx.sample.datatxvalidationwan.model.ValidationSummary;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.query.QueryException;
import org.springframework.beans.factory.annotation.Qualifier;

public interface ValidationService {

  void reviewValidationStep(@Qualifier("validationSummary") Region<String,
          ValidationSummary> validationSummaryRegion);

  void checkEntrySize(ClientCache clientCache, @Qualifier("site1") Pool site1,
                      @Qualifier("site2") Pool site2,
                      @Qualifier("validationSummary") Region<String, ValidationSummary> validationSummaryRegion,
                      String region) throws QueryException;

  void checkKeyMatchers(ClientCache clientCache, @Qualifier("site1") Pool site1,
                        @Qualifier("site2") Pool site2,
                        @Qualifier("validationSummary") Region<String, ValidationSummary> validationSummaryRegion, String region);

  void checkDataMatchers(ClientCache clientCache, @Qualifier("site1") Pool site1,
                         @Qualifier("site2") Pool site2,
                         @Qualifier("validationSummary") Region<String, ValidationSummary> validationSummaryRegion, String region) throws QueryException;
}
