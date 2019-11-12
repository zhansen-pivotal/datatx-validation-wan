package io.pivotal.datatx.sample.datatxvalidationwan;

import io.pivotal.datatx.sample.datatxvalidationwan.model.ValidationSummary;
import io.pivotal.datatx.sample.datatxvalidationwan.service.CacheService;
import io.pivotal.datatx.sample.datatxvalidationwan.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.EnablePool;
import org.springframework.data.gemfire.config.annotation.EnablePools;
import org.springframework.geode.config.annotation.EnableClusterAware;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
@Slf4j
@EnablePdx
@EnablePools(
        pools = {
                @EnablePool(name = "site1"),
                @EnablePool(name = "site2")
        })
@EnableClusterAware
public class DatatxValidationWanApplication {

  public static final String WAN_REGION_PROP = "gemfire.wan.regions";

  @Bean("regions")
  public List<String> setRegionNames(Environment environment) {
    String regions = environment.getProperty(WAN_REGION_PROP);
    String[] r = regions.split(",");
    for (String region : r) {
      testRegionNames.add(region);
    }
    return testRegionNames;
  }

  @Autowired
  ValidationService validationService;

  @Autowired
  CacheService cacheService;

  private List<String> testRegionNames = new ArrayList<>(); //TODO remove after test

  public static void main(String[] args) {
    SpringApplication.run(DatatxValidationWanApplication.class, args);
  }


  @Bean
  ApplicationRunner runner(@Qualifier("regions") List<String> regions, ClientCache clientCache,
                           @Qualifier("site1") Pool site1,
                           @Qualifier(
                                   "site2") Pool site2,
                           @Qualifier("validationSummary") Region<String, ValidationSummary> validationSummaryRegion) {
    return args -> {
      for (String region : regions) {
        validationService.checkEntrySize(clientCache, site1, site2, validationSummaryRegion,
                region);
        validationService.checkKeyMatchers(clientCache, site1, site2, validationSummaryRegion, region);
        validationService.checkDataMatchers(clientCache, site1, site2, validationSummaryRegion, region);
      }
      validationService.reviewValidationStep(validationSummaryRegion);
    };
  }

  @Bean("validationSummary")
  ClientRegionFactoryBean<String, ValidationSummary> validationSummaryRegion(GemFireCache cache) {
    ClientRegionFactoryBean<String, ValidationSummary> validationSummaryRegion =
            new ClientRegionFactoryBean();
    validationSummaryRegion.setShortcut(ClientRegionShortcut.LOCAL);
    validationSummaryRegion.setName("validation_summary");
    validationSummaryRegion.setCache(cache);
    validationSummaryRegion.setClose(false);
    return validationSummaryRegion;
  }
}
