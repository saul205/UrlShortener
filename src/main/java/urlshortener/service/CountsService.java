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

import urlshortener.domain.Counts;
import urlshortener.repository.CountsRepository;
import urlshortener.web.UrlShortenerController;

import java.sql.Timestamp;

import java.util.List;

@Service
public class CountsService {

  private final CountsRepository countsRepository;

  public CountsService(CountsRepository countsRepository) {
    this.countsRepository = countsRepository;
  }

  public Counts save(String target, Long count, Boolean hash) {

    if(hash){
        Counts c = CountsBuilder.newInstance()
        .hash(target)
        .target(target)
        .count(count)
        .build();
        return countsRepository.save(c);
    }

    Counts c = CountsBuilder.newInstance()
        .target(target)
        .count(count)
        .build();
    return countsRepository.save(c);
  }

  public Counts findByHash(String hash){
    return countsRepository.findByHash(hash);
  }

  public List<Counts> findByTarget(String target){
    return countsRepository.findByTarget(target);
  }
}