package io.pivotal.datatx.sample.datatxvalidationwan.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pivotal.datatx.sample.datatxvalidationwan.model.Customer;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.Pool;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Profile({"loadSite1", "loadSite2"})
public class LoadServiceImpl implements LoadService {
  @Autowired
  CacheService cacheService;

  @Autowired
  ResourceLoader resourceLoader;

  @Override
  public void loadCustomerData(ClientCache cache, Pool site) throws IOException,
          ParseException {
    Region<String, Customer> customerRegion = cacheService.createRegion(cache, site,
            "customer");
    loadData(customerRegion);
    cacheService.destroyRegion(cache, customerRegion);
  }

  private void loadData(Region<String, Customer> region) throws IOException, ParseException {
    Map<String, Customer> customers = new HashMap<>();
    ObjectMapper mapper = new ObjectMapper();
    JSONParser jsonParser = new JSONParser();
    File customerJSON = resourceLoader.getResource("classpath:MOCK_DATA.json").getFile();
    JSONArray customerArray = (JSONArray) jsonParser.parse(new FileReader(customerJSON));
    customerArray.forEach(j -> {
      try {
        Customer customer = mapper.readValue(j.toString(), Customer.class);
        customers.put(customer.getAccountNumber(), customer);
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      }
    });
    region.putAll(customers);
  }
}
