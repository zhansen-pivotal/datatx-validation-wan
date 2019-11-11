package io.pivotal.datatx.sample.datatxvalidationwan;

import io.pivotal.datatx.sample.datatxvalidationwan.model.ValidationSummary;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.EnablePool;
import org.springframework.data.gemfire.config.annotation.EnablePools;
import org.springframework.geode.config.annotation.EnableClusterAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

  public List<String> setTestRegionNames() {
    testRegionNames.add("test");
    return testRegionNames;
  }

  private List<String> testRegionNames = new ArrayList<>(); //TODO remove after test

  public static void main(String[] args) {
    SpringApplication.run(DatatxValidationWanApplication.class, args);
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

  @Bean
  ApplicationRunner runner(ClientCache clientCache, @Qualifier("site1") Pool site1, @Qualifier(
          "site2") Pool site2,
                           @Qualifier("validationSummary") Region<String, ValidationSummary> validationSummaryRegion) {
    return args -> {
      for (String region : setTestRegionNames()) {
        checkEntrySize(clientCache, site1, site2, validationSummaryRegion, region);
        checkKeyMatchers(clientCache, site1, site2, validationSummaryRegion, region);
      }
      reviewValidationStep(validationSummaryRegion);
    };
  }

  private void reviewValidationStep(@Qualifier("validationSummary") Region<String, ValidationSummary> validationSummaryRegion) {
    Boolean isValid = true;
    Set<String> keys = validationSummaryRegion.keySet();
    for (String key : keys) {
      ValidationSummary validationSummary = validationSummaryRegion.get(key);
      if (!validationSummary.isEntryCount()) {
        isValid = false;
        log.error("reviewValidationStep: EntryCount is not equal for region [{}]",
                validationSummary.getRegionName());
      }
      if (!validationSummary.isKeyMatcher()) {
        isValid = false;
        log.error("reviewValidationStep: KeyMatcher is not equal for region [{}]",
                validationSummary.getRegionName());
      }
      log.info("reviewValidationStep: Validation check result: {}", isValid);
    }
  }

  private void checkEntrySize(ClientCache clientCache, @Qualifier("site1") Pool site1,
                              @Qualifier("site2") Pool site2,
                              @Qualifier("validationSummary") Region<String, ValidationSummary> validationSummaryRegion,
                              String region) {

    ValidationSummary validationSummary = validationSummaryRegion.get(region);

    if (validationSummary == null) {
      validationSummary = new ValidationSummary().builder().regionName(region).build();
    }

    Region site1Region = createRegion(clientCache, site1, region);

    int size = site1Region.keySetOnServer().size();

    destroyRegion(clientCache, site1Region);

    Region site2Region = createRegion(clientCache, site2, region);
    int size2 = site2Region.keySetOnServer().size();
    if (size != size2) {
      log.warn("checkEntrySize: Region Entry Size not Equal");
      validationSummary.setEntryCount(false);
    } else {
      log.info("checkEntrySize; Validated regionEntryCount region[{}]", region);
      validationSummary.setEntryCount(true);
    }
    validationSummaryRegion.put(region, validationSummary);
    destroyRegion(clientCache, site2Region);
  }

  private void checkKeyMatchers(ClientCache clientCache, @Qualifier("site1") Pool site1,
                                @Qualifier("site2") Pool site2,
                                @Qualifier("validationSummary") Region<String, ValidationSummary> validationSummaryRegion, String region) {

    ValidationSummary validationSummary = validationSummaryRegion.get(region);

    if (validationSummary == null) {
      validationSummary = new ValidationSummary().builder().regionName(region).build();
    }
    Region site1Region = createRegion(clientCache, site1, region);


    Set<String> site1Keys = site1Region.keySetOnServer();

    destroyRegion(clientCache, site1Region);

    Region site2Region = createRegion(clientCache, site2, region);

    Set<String> site2Keys = site2Region.keySetOnServer();
    if (!site1Keys.containsAll(site2Keys) | !site2Keys.containsAll(site1Keys)) {
      validationSummary.setKeyMatcher(false);
    } else {
      log.info("checkKeyMatchers: Validated keyMatchers region[{}]", region);
      validationSummary.setKeyMatcher(true);
    }
    validationSummaryRegion.put(region, validationSummary);
    destroyRegion(clientCache, site2Region);
  }

  private Region createRegion(ClientCache cache, Pool pool, String name) {
    log.debug("Creating region test on site[{}]", pool.getName());
    return cache.createClientRegionFactory(ClientRegionShortcut.PROXY).setPoolName(
            pool.getName()).create(name);
  }

  private void destroyRegion(ClientCache cache, Region region) {
    log.debug("Destroying region locally to client [{}]", region.getFullPath());
    cache.getRegion(region.getFullPath()).localDestroyRegion();
  }


}
