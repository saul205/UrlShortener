package urlshortener.repository;

import java.util.List;
import urlshortener.domain.ShortURL;

public interface ShortURLRepository {

  ShortURL findByKey(String id);

  List<ShortURL> findByTarget(String target);

  ShortURL save(ShortURL su);

  ShortURL mark(ShortURL urlSafe, Integer safeness);

  void update(ShortURL su);

  void delete(String id);

  Long count();

  List<ShortURL> list(Long limit, Long offset);

  List<ShortURL> lastN(Integer n);
}
