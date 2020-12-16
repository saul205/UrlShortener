package urlshortener.repository;

import java.util.List;
import urlshortener.domain.Counts;

public interface CountsRepository {

  Counts save(Counts c);

  void update(Counts c);

  void delete(String hash);

  Counts findByHash(String hash);

  List<Counts> findByTarget(String target);

  List<Counts> list();
}