package io.pivotal.datatx.sample.datatxvalidationwan.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CacheServiceImpl implements CacheService {

  @Override
  public Region createRegion(ClientCache cache, Pool pool, String name) {
    log.info("Creating region test on site[{}]", pool.getName());
    return cache.createClientRegionFactory(ClientRegionShortcut.PROXY).setPoolName(
            pool.getName()).create(name);
  }

  @Override
  public void destroyRegion(ClientCache cache, Region region) {
    log.debug("Destroying region locally to client [{}]", region.getFullPath());
    cache.getRegion(region.getFullPath()).localDestroyRegion();
  }
}
