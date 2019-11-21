package io.pivotal.datatx.sample.datatxvalidationwan.function;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.apache.geode.cache.execute.RegionFunctionContext;
import org.apache.geode.cache.execute.ResultSender;
import org.apache.geode.cache.partition.PartitionRegionHelper;
import org.apache.geode.internal.cache.PartitionedRegion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class GatherDataFunction implements Function, Declarable {

  @Override
  public String getId() {
    return GatherDataFunction.class.getSimpleName();
  }

  @Override
  public boolean hasResult() {
    return true;
  }

  @Override
  public boolean optimizeForWrite() {
    return true;
  }

  @Override
  public boolean isHA() {
    return false;
  }

  @Override
  public void execute(FunctionContext context) {
    final ResultSender<Map<Object, Object>> sender = context.getResultSender();
    try {
      RegionFunctionContext rfc = (RegionFunctionContext) context;
      Region executedRegion;
      Set<Object> keys;
      if (rfc.getDataSet() instanceof PartitionedRegion) {
        executedRegion = PartitionRegionHelper.getLocalDataForContext(rfc);
        CacheFactory.getAnyInstance().getLogger().info("Using Partition Region");
      } else {
        executedRegion = ((RegionFunctionContext) context).getDataSet();
        CacheFactory.getAnyInstance().getLogger().info("Using Replicate Region");
      }

      //TODO : Should probably get keyset from filter on function execution.
      keys = executedRegion.keySet();

      if (keys.size() == 0 || keys == null) {
        sender.lastResult(new HashMap<>());
      }

      Iterator<Object> itr = keys.iterator();
      while (itr.hasNext()) {
        Object k = itr.next();
        Object v = executedRegion.get(k);
        if (itr.hasNext()) {
          CacheFactory.getAnyInstance().getLogger().info("Streaming results in 1 object chunks");
          //TODO: Should probably send a configurable batch size starting with 1000 records
          sender.sendResult(getData(k, v));
        } else {
          CacheFactory.getAnyInstance().getLogger().info("Last result in 1 object chunk");
          sender.lastResult(getData(k, v));
        }
      }
    } catch (Exception e) {
      CacheFactory.getAnyInstance().getLogger().error("FunctionException in GatherDataFunction" +
              ".execute [" + e + "] ");
      e.printStackTrace();
      sender.sendException(e);
    }
  }

  private Map<Object, Object> getData(Object k, Object v) {
    Map<Object, Object> result = new HashMap<>();
    result.put(k, v);
    CacheFactory.getAnyInstance().getLogger().info("Found a record {" + result.toString() +
            "}");
    return result;
  }
}
