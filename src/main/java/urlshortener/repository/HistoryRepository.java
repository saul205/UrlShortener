package urlshortener.repository;

import java.util.List;
import urlshortener.domain.HistoryElement;
import urlshortener.domain.ShortURL;

public interface HistoryRepository {

  List<HistoryElement> findByHash(String hash, Integer n);

  List<HistoryElement> find(Integer n);

  HistoryElement save(HistoryElement su);

  void save(List<HistoryElement> l);

  void update(HistoryElement su);

  void delete(Long id);

  Integer count();
}
