package urlshortener.service;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;

import urlshortener.domain.HistoryElement;
import urlshortener.domain.ShortURL;
import urlshortener.repository.HistoryRepository;
import urlshortener.web.UrlShortenerController;

import java.sql.Timestamp;

import java.util.List;

@Service
public class HistoryService {

  private final HistoryRepository historyRepository;
  private String apiKey = "AIzaSyCTp0lW0RBgGuPoPlmgESk9tblrWy9ny08";

  public HistoryService(HistoryRepository historyRepository) {
    this.historyRepository = historyRepository;
  }

  public List<HistoryElement> findByHash(String hash, Integer n) {
    return historyRepository.findByHash(hash, n); 
  }

  public List<HistoryElement> find(Integer n) {
    return historyRepository.find(n);
  }

  public HistoryElement save(String hash, String target, Timestamp created) {
    HistoryElement su = HistoryElementBuilder.newInstance()
        .hash(hash)
        .target(target)
        .created(created)
        .build();
    return historyRepository.save(su);
  }

  public void save(List<ShortURL> l) {
    List<HistoryElement> list = new ArrayList();
    for(ShortURL su : l){
      list.add(HistoryElementBuilder.newInstance()
        .hash(su.getHash())
        .target(su.getTarget())
        .created(su.getCreated())
        .build());
    }
    historyRepository.save(list);
  }

  public Integer count(){
    return historyRepository.count();
  }
}
