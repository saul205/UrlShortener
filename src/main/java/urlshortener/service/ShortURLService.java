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

import urlshortener.domain.ShortURL;
import urlshortener.repository.ShortURLRepository;
import urlshortener.web.UrlShortenerController;
import urlshortener.web.UrlShortenerController.State;

import java.util.List;

@Service
public class ShortURLService {

  private final ShortURLRepository shortURLRepository;
  private String apiKey = "AIzaSyCTp0lW0RBgGuPoPlmgESk9tblrWy9ny08";

  public ShortURLService(ShortURLRepository shortURLRepository) {
    this.shortURLRepository = shortURLRepository;
  }

  public ShortURLRepository getSURLSVC(){
    return shortURLRepository;
  }

  public ShortURL findByKey(String id) {
    return shortURLRepository.findByKey(id);
  }

  public ShortURL save(String url, String sponsor, String ip, Boolean qrres) {
    ShortURL su = ShortURLBuilder.newInstance()
        .target(url)
        .uri((String hash) -> linkTo(methodOn(UrlShortenerController.class).redirectTo(hash, null))
            .toUri())
        .sponsor(sponsor)
        .createdNow()
        .randomOwner()
        .temporaryRedirect()
        .treatAsUnknown()
        .qrResource(qrres, (String hash) -> linkTo(methodOn(UrlShortenerController.class)
                                            .generateQR(hash)).toUri())
        .ip(ip)
        .unknownCountry()
        .build();
    return shortURLRepository.save(su);
  }

  public List<ShortURL> findByTarget(String target) {
    return shortURLRepository.findByTarget(target);
  }

  public Long count(){
    return shortURLRepository.count();
  }

  public void checkSafe(ShortURL urlShort[]) {
    ArrayList<ShortURL> l = new ArrayList<>(Arrays.asList(urlShort));
    checkSafe(l);
  }

  public void checkSafe(ArrayList<ShortURL> l) {
    String safeUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + apiKey;

    HttpHeaders head = new HttpHeaders();
    head.setContentType(MediaType.APPLICATION_JSON);

    JSONObject req = new JSONObject();

    JSONObject client = new JSONObject();
    client.put("clientId", "unizar");
    client.put("clientVersion", "1.0");
    req.put("client", client);

    JSONObject threat = new JSONObject();
    JSONArray platform = new JSONArray();
    platform.put("ANY_PLATFORM");
    JSONArray entry = new JSONArray();
    entry.put("URL");
    JSONArray types = new JSONArray();
    types.put("MALWARE");
    types.put("SOCIAL_ENGINEERING");
    JSONArray entries = new JSONArray();
    for(int i = 0; i < 500 && i < l.size(); ++i) {
      JSONObject urls = new JSONObject();
      urls.put("url", l.get(i).getTarget());
      entries.put(urls);
    }

    try {
      threat.put("threatTypes", types);
      threat.put("platformTypes", platform);
      threat.put("threatEntryTypes", entry);
      threat.put("threatEntries", entries);
    } catch(JSONException e) {
      e.printStackTrace();
    }

    req.put("threatInfo", threat);

    RestTemplate restTemplate = new RestTemplate();
    HttpEntity<String> request = new HttpEntity<String>(req.toString(), head);
    String response = restTemplate.postForObject(safeUrl, request, String.class);

    JSONObject resp = new JSONObject(response);

    if(resp.has("matches")) {
      JSONArray iter = resp.getJSONArray("matches");
      for(int i = 0; i < iter.length(); ++i) {
        String js = iter.getJSONObject(i).getJSONObject("threat").getString("url");
        for(int j = 0; j < l.size(); ++j) {
          ShortURL aux = l.get(j);
          if(aux.getTarget().equals(js)) {
            shortURLRepository.mark(aux, State.incorrect.value);
            l.remove(j);
            break;
          }
        }
      }
    }

    if(l.size() > 0) {
      for(int i = 0; i < l.size(); ++i) {
        ShortURL aux = l.get(i);
        shortURLRepository.mark(aux, State.correct.value);
      }
    }
  }

  public List<ShortURL> getLastNByIp(String Ip, Integer n){
    return shortURLRepository.lastNByIp(Ip, n);
  }
}
