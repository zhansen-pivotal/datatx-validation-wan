package io.pivotal.datatx.sample.datatxvalidationwan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pivotal.datatx.sample.datatxvalidationwan.model.Customer;
import io.pivotal.datatx.sample.datatxvalidationwan.model.ValidationSummary;
import io.pivotal.datatx.sample.datatxvalidationwan.service.CacheService;
import io.pivotal.datatx.sample.datatxvalidationwan.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.apache.geode.cache.client.Pool;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.EnablePdx;
import org.springframework.data.gemfire.config.annotation.EnablePool;
import org.springframework.data.gemfire.config.annotation.EnablePools;
import org.springframework.geode.config.annotation.EnableClusterAware;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

  private List<String> testRegionNames = new ArrayList<>(); //TODO remove after test

  public static void main(String[] args) {
    SpringApplication.run(DatatxValidationWanApplication.class, args);
  }

  @Autowired
  ValidationService validationService;

  @Autowired
  CacheService cacheService;

  @Autowired
  ResourceLoader resourceLoader;


  @Bean
  @Profile("test")
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

  @Bean
  @Profile("loadSite1")
  ApplicationRunner runnerLoad1(ClientCache clientCache, @Qualifier("site1") Pool site1) {
    return args -> {
      loadCustomerData(clientCache, site1);
    };
  }

  @Bean
  @Profile("loadSite2")
  ApplicationRunner runnerLoad2(ClientCache clientCache, @Qualifier("site2") Pool site2) {
    return args -> {
      loadCustomerData(clientCache, site2);
    };
  }


  @Bean("regions")
  public List<String> setRegionNames(Environment environment) {
    String regions = environment.getProperty(WAN_REGION_PROP);
    String[] r = regions.split(",");
    for (String region : r) {
      testRegionNames.add(region);
    }
    return testRegionNames;
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
