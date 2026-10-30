package business.global.strategy;

import cenum.pack.HttpPackEnum;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("packSpringContext")
public class PackSpringContext {

  private final Map<String, AbstractPackStrategy> strategyMap;

  public PackSpringContext(Map<String, AbstractPackStrategy> strategyMap) {
    this.strategyMap = strategyMap;
  }


  public AbstractPackStrategy getService(int value) {
    HttpPackEnum parkEnum = HttpPackEnum.valueOf(value);
    return strategyMap.get(parkEnum.getIclassName());
  }

}
