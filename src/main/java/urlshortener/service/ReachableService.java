package urlshortener.service;

import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.stereotype.Service;
import urlshortener.domain.ShortURL;
import urlshortener.repository.ShortURLRepository;
import urlshortener.web.UrlShortenerController;

@Service
public class ReachableService {

  private final ShortURLRepository shortURLRepository;

  public ReachableService(ShortURLRepository shortURLRepository) {
    this.shortURLRepository = shortURLRepository;
  }

  public Boolean isReachable(String id){
    ShortURL surl = shortURLRepository.findByKey(id);
    Boolean rble = false;
    URL url; HttpURLConnection huc;
    if (surl == null) return rble;
    try {
      url = new URL(surl.getTarget());
      // setFollowRedirects -> SecurityException
      HttpURLConnection.setFollowRedirects(false);
      huc = (HttpURLConnection) url.openConnection();
      huc.setConnectTimeout(10000);
      huc.getResponseCode();
      rble = true;
    } catch(UnknownHostException u) {
      rble = false;
    } catch(SocketTimeoutException s) {
      rble = false;
    } catch(Exception e1) { // SecurityException || IOException
      try{
        url = new URL(surl.getTarget());
        HttpURLConnection.setFollowRedirects(true); // By default
        huc = (HttpURLConnection) url.openConnection();
        huc.setConnectTimeout(10000);
        huc.getResponseCode();
        rble = true;
      } catch (Exception e2){ //SocketTimeoutException, IOException or unknown exception
        rble = false;
      }
    } 
    Integer r;
    if (rble) r = 1;
    else r = -1;
    shortURLRepository.setAlcanzableByHash(surl.getHash(), r);
    return rble;
  }
}
