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
import urlshortener.repository.MostVisitedRepository;
import urlshortener.repository.impl.MostVisitedRepositoryImpl;
import urlshortener.repository.impl.Tuple;
import urlshortener.web.UrlShortenerController;

import java.sql.Timestamp;

import java.util.List;
import java.util.ArrayList;

@Service
public class MostVisitedService {

  private final MostVisitedRepository mostVisitedRepository;

  public MostVisitedService(MostVisitedRepository mostVisitedRepository) {
    this.mostVisitedRepository = mostVisitedRepository;
  }

  public void save(List<Tuple> t) {
    List<Counts> c = new ArrayList();
    for(Tuple tu : t){
        c.add(CountsBuilder.newInstance().target(tu.getKey()).count(tu.getValue()).build());
    }

    mostVisitedRepository.save(c);
  }

  public List<Counts> find(){
      return mostVisitedRepository.find();
  }
}