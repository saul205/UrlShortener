package urlshortener.repository;

import java.util.List;
import java.util.HashMap;
import urlshortener.domain.Click;
import urlshortener.repository.impl.Tuple;

public interface ClickRepository {

  List<Click> findByHash(String hash);

  Long clicksByHash(String hash);

  Click save(Click cl);

  void update(Click cl);

  void delete(Long id);

  void deleteAll();

  Long count();

  List<Click> list(Long limit, Long offset);

  List<Tuple> topN(int n);
}
