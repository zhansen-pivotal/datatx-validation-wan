package io.pivotal.datatx.sample.datatxvalidationwan.service;

import io.pivotal.datatx.sample.datatxvalidationwan.model.ValidationSummary;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.apache.geode.cache.query.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ValidationServiceImpl implements ValidationService {

  @Autowired
  CacheService cacheService;

  @Override //TODO should probably replace business logic with function for scalable solution
  public void checkDataMatchers(ClientCache clientCache, Pool site1, Pool site2, Region<String, ValidationSummary> validationSummaryRegion, String region) throws NameResolutionException, TypeMismatchException, QueryInvocationTargetException, FunctionDomainException {
    ValidationSummary validationSummary = validationSummaryRegion.get(region);
    if (validationSummary == null) {
      validationSummary = new ValidationSummary().builder().regionName(region).build();
    }

    List<Boolean> checks = new ArrayList<>();
    Region site2Region = cacheService.createRegion(clientCache, site2, region);
    SelectResults<Struct> site1Entries = (SelectResults<Struct>) clientCache
            .getQueryService(site1.getName())
            .newQuery("Select e.key, e.value from /" + region + ".entrySet e")
            .execute();

    site1Entries.forEach(e -> {
      Object key = e.get("key");
      Object site1result = e.get("value");
      Object site2result = site2Region.get(key);
      log.debug("From site 1 [{},{}]", key, site1result);
      log.debug("From site 2 [{},{}]", key, site2result);
      if (!site1result.equals(site2result)) {
        checks.add(false);
      } else {
        checks.add(true);
      }
    });

    if (checks.contains(false)) {
      log.error("checkDataMatchers: dataMatcher not equal region[{}]", region);
      validationSummary.setDataMatcher(false);
    } else {
      log.info("checkDataMatchers: Validated dataMatcher region[{}]", region);
      validationSummary.setDataMatcher(true);
    }

    validationSummaryRegion.put(region, validationSummary);
    cacheService.destroyRegion(clientCache, site2Region);
  }

  @Override
  public void reviewValidationStep(@Qualifier("validationSummary") Region<String,
          ValidationSummary> validationSummaryRegion) {
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
      if (!validationSummary.isDataMatcher()) {
        isValid = false;
        log.error("reviewValidationStep: DataMatcher is not equal for region [{}]",
                validationSummary.getRegionName());
      }
    }
    log.info("reviewValidationStep: Validation check result: {}", isValid);
  }

  @Override
  public void checkEntrySize(ClientCache clientCache, @Qualifier("site1") Pool site1,
                             @Qualifier("site2") Pool site2,
                             @Qualifier("validationSummary") Region<String, ValidationSummary> validationSummaryRegion,
                             String region) {

    ValidationSummary validationSummary = validationSummaryRegion.get(region);

    if (validationSummary == null) {
      validationSummary = new ValidationSummary().builder().regionName(region).build();
    }

    Region site1Region = cacheService.createRegion(clientCache, site1, region);

    int size = site1Region.keySetOnServer().size();

    cacheService.destroyRegion(clientCache, site1Region);

    Region site2Region = cacheService.createRegion(clientCache, site2, region);
    int size2 = site2Region.keySetOnServer().size();
    if (size != size2) {
      log.error("checkEntrySize: Region Entry Size not Equal");
      validationSummary.setEntryCount(false);
    } else {
      log.info("checkEntrySize; Validated regionEntryCount region[{}]", region);
      validationSummary.setEntryCount(true);
    }
    validationSummaryRegion.put(region, validationSummary);
    cacheService.destroyRegion(clientCache, site2Region);
  }

  @Override
  public void checkKeyMatchers(ClientCache clientCache, @Qualifier("site1") Pool site1,
                               @Qualifier("site2") Pool site2,
                               @Qualifier("validationSummary") Region<String, ValidationSummary> validationSummaryRegion, String region) {

    ValidationSummary validationSummary = validationSummaryRegion.get(region);

    if (validationSummary == null) {
      validationSummary = new ValidationSummary().builder().regionName(region).build();
    }
    Region site1Region = cacheService.createRegion(clientCache, site1, region);


    Set<String> site1Keys = site1Region.keySetOnServer();

    cacheService.destroyRegion(clientCache, site1Region);

    Region site2Region = cacheService.createRegion(clientCache, site2, region);

    Set<String> site2Keys = site2Region.keySetOnServer();
    if (!site1Keys.containsAll(site2Keys) | !site2Keys.containsAll(site1Keys)) {
      validationSummary.setKeyMatcher(false);
    } else {
      log.info("checkKeyMatchers: Validated keyMatchers region[{}]", region);
      validationSummary.setKeyMatcher(true);
    }
    validationSummaryRegion.put(region, validationSummary);
    cacheService.destroyRegion(clientCache, site2Region);
  }
}
