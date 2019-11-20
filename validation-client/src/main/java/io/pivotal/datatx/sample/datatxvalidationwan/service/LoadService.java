package io.pivotal.datatx.sample.datatxvalidationwan.service;

import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public interface LoadService {
  void loadCustomerData(ClientCache cache, Pool site) throws IOException,
          ParseException;
}
