package io.pivotal.datatx.sample.datatxvalidationwan.service;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;

public interface CacheService {
  Region createRegion(ClientCache cache, Pool pool, String name);

  void destroyRegion(ClientCache cache, Region region);
}
