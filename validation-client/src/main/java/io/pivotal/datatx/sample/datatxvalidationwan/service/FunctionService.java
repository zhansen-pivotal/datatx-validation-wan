package io.pivotal.datatx.sample.datatxvalidationwan.service;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.springframework.beans.factory.annotation.Qualifier;

public interface FunctionService {

  void executeFunction(String regionName,
                       @Qualifier("site1") Pool site1,
                       ClientCache clientCache);
}
