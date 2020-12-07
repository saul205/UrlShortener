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

  public List<HistoryElement> findByHashIp(String hash, String ip, Integer n) {
    return historyRepository.findByHashIp(hash, ip, n);
  }

  public List<HistoryElement> findByIp(String ip, Integer n) {
    return historyRepository.findByIp(ip, n);
  }

  public HistoryElement save(String hash, String target, Timestamp created, String ip) {
    HistoryElement su = HistoryElementBuilder.newInstance()
        .hash(hash)
        .target(target)
        .created(created)
        .ip(ip)
        .build();
    return historyRepository.save(su);
  }

  public Long count(){
    return historyRepository.count();
  }

  public Integer countByIp(String ip){
    return historyRepository.countByIp(ip);
  }
}
