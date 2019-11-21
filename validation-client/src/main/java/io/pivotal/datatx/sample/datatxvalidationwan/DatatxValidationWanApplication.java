package io.pivotal.datatx.sample.datatxvalidationwan;

import io.pivotal.datatx.sample.datatxvalidationwan.model.ValidationSummary;
import io.pivotal.datatx.sample.datatxvalidationwan.service.FunctionService;
import io.pivotal.datatx.sample.datatxvalidationwan.service.LoadService;
import io.pivotal.datatx.sample.datatxvalidationwan.service.ValidationService;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.EnablePool;
import org.springframework.data.gemfire.config.annotation.EnablePools;
import org.springframework.geode.config.annotation.EnableClusterAware;

import java.util.ArrayList;
import java.util.List;

@EnablePools(
        pools = {
                @EnablePool(name = "site1"),
                @EnablePool(name = "site2")
        })
@EnablePdx(readSerialized = true)
@EnableClusterAware
@SpringBootApplication
public class DatatxValidationWanApplication {

  public static final String WAN_REGION_PROP = "gemfire.wan.regions";

  private List<String> regionNames = new ArrayList<>(); //TODO remove after test

  public static void main(String[] args) {
    SpringApplication.run(DatatxValidationWanApplication.class, args);
  }

  @Bean
  @Profile({"local", "function"})
  ApplicationRunner validationRunner(@Qualifier("regions") List<String> regions,
                                     ClientCache clientCache,
                                     @Qualifier("site1") Pool site1,
                                     @Qualifier(
                                             "site2") Pool site2,
                                     @Qualifier("validationSummary") Region<String, ValidationSummary> validationSummaryRegion, ValidationService validationService) {
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


  @Bean
  @Profile("loadSite1")
  ApplicationRunner runnerLoad1(ClientCache clientCache, @Qualifier("site1") Pool site1,
                                LoadService loadService) {
    return args -> loadService.loadCustomerData(clientCache, site1);
  }

  @Bean
  @Profile("loadSite2")
  ApplicationRunner runnerLoad2(ClientCache clientCache, @Qualifier("site2") Pool site2,
                                LoadService loadService) {
    return args -> loadService.loadCustomerData(clientCache, site2);
  }

  @Bean("regions")
  public List<String> setRegionNames(Environment environment) {
    String regions = environment.getProperty(WAN_REGION_PROP);
    String[] r = regions.split(",");
    for (String region : r) {
      regionNames.add(region);
    }
    return regionNames;
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

