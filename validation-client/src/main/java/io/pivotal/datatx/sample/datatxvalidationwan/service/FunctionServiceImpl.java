package io.pivotal.datatx.sample.datatxvalidationwan.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.gemfire.function.execution.GemfireOnRegionFunctionTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class FunctionServiceImpl implements FunctionService {

  @Autowired
  CacheService cacheService;

  @Override
  public void executeFunction(String regionName,
                              @Qualifier("site1") Pool site1,
                              ClientCache clientCache) {

    log.debug("Creating Client Region [{}] for function execution", regionName);
    Region region = cacheService.createRegion(clientCache, site1, regionName);
    Set keys = region.keySetOnServer();
    AtomicInteger counter = new AtomicInteger();
    if (keys.size() > 0) {
      GemfireOnRegionFunctionTemplate template =
              new GemfireOnRegionFunctionTemplate(region);
      //TODO: Should probably execute with a filter of keyset
      Iterable<Map<Object, Object>> result = template.execute("GatherDataFunction", keys);
      //TODO: Handle validation as results come in. Probably need a custom ResultCollector
      result.forEach(q -> {
        if (q != null) {
          log.info("From execute:  FunctionResult: {}", q);
          counter.getAndIncrement(); //TODO: Remove. Only here for testing lastResult logic.
        }
      });
      log.info("Result Set size for region[{}] is [{}]", regionName, counter);
    }
  }
}
