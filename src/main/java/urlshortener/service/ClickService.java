package urlshortener.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import urlshortener.domain.Click;
import urlshortener.repository.ClickRepository;
import urlshortener.repository.impl.Tuple;
import java.util.List;

@Service
public class ClickService {

  private static final Logger log = LoggerFactory
      .getLogger(ClickService.class);

  private final ClickRepository clickRepository;

  public ClickService(ClickRepository clickRepository) {
    this.clickRepository = clickRepository;
  }

  public void saveClick(String hash, String ip) {
    Click cl = ClickBuilder.newInstance().hash(hash).createdNow().ip(ip).build();
    cl = clickRepository.save(cl);
    log.info(cl != null ? "[" + hash + "] saved with id [" + cl.getId() + "]" :
        "[" + hash + "] was not saved");
  }

  public List<Tuple> getTopN(int n){
    return clickRepository.topN(n);
  }

  public Long clicksByHash(String hash){
    return clickRepository.clicksByHash(hash);
  }

  public Long count(){
    return clickRepository.count();
  }
}
