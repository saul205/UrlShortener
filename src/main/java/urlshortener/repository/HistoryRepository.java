package urlshortener.repository;

import java.util.List;
import urlshortener.domain.HistoryElement;

public interface HistoryRepository {

  List<HistoryElement> findByHashIp(String hash, String ip, Integer n);

  List<HistoryElement> findByIp(String ip, Integer n);

  HistoryElement save(HistoryElement su);

  void update(HistoryElement su);

  void delete(Long id);

  Long count();

  Integer countByIp(String ip);
}
