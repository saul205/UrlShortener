package urlshortener.web;

import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.FileSystemResource;

import jdk.internal.net.http.common.Pair;
import urlshortener.domain.Click;
import urlshortener.domain.ShortURL;
import urlshortener.repository.ClickRepository;
import urlshortener.repository.impl.Tuple;
import urlshortener.service.ClickService;
import urlshortener.service.ShortURLService;
import urlshortener.service.ReachableService;
import urlshortener.service.QRGenerator;
import urlshortener.service.CSVGenerator;

import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.util.List;
import java.io.File;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

@RestController
public class UrlShortenerController {
  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  private final ReachableService reachableService;

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService, ReachableService reachableService) {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
    this.reachableService = reachableService;
  }

  @RequestMapping(value = "/{id:(?!link|index).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id,
                                      HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null) {
      clickService.saveClick(id, extractIP(request));
      return createSuccessfulRedirectToResponse(l);
    } else {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url,
                                            @RequestParam(value = "sponsor", required = false)
                                                String sponsor,
                                            HttpServletRequest request) {
    UrlValidator urlValidator = new UrlValidator(new String[] {"http",
        "https"});

    if (urlValidator.isValid(url)) {
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr());
      HttpHeaders h = new HttpHeaders();
      h.setLocation(su.getUri());

      // Reachable
      new Thread(() -> {
        reachableService.isReachable(su.getHash());
      }).start();
      new Thread(() -> {
        shortUrlService.checkSafe(new ShortURL[] {su});
      }).start();
      
      return new ResponseEntity<>(su, h, HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }


  }

  @RequestMapping(value = "/qr", method = RequestMethod.GET,
                  produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<BufferedImage> generateQR(@RequestParam("hashUS") String hash){
    ShortURL su = shortUrlService.findByKey(hash);
    if (su != null){
      String toQR = su.getUri().toString();
      try {
        BufferedImage image = QRGenerator.generateQRImage(toQR);
        return new ResponseEntity<>(image, HttpStatus.OK);
      } catch (Exception e){
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
      }
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
  }

  private String extractIP(HttpServletRequest request) {
    return request.getRemoteAddr();
  }

  private ResponseEntity<?> createSuccessfulRedirectToResponse(ShortURL l) {
    HttpHeaders h = new HttpHeaders();
    h.setLocation(URI.create(l.getTarget()));
    return new ResponseEntity<>(h, HttpStatus.valueOf(l.getMode()));
  }

  @RequestMapping(value = "/db", method = RequestMethod.GET)
  public ResponseEntity<JSONObject> getData(){
    List<Tuple> urls = clickService.getTopN(10);

    JSONObject obj = new JSONObject();
    JSONObject mainObj = new JSONObject();

    for(Tuple u : urls){
     obj.put(u.getKey(), u.getValue());
    }

    mainObj.put("clicks", clickService.count());
    mainObj.put("urls", shortUrlService.count());
    mainObj.put("top", obj);

    return new ResponseEntity<>(mainObj, HttpStatus.OK);
  }

  @RequestMapping(value = "/db/search", method = RequestMethod.POST)
  public ResponseEntity<JSONObject> getTargetCount(@RequestParam("url") String target){
    List<ShortURL> urls = shortUrlService.findByTarget(target);

    JSONObject json = new JSONObject();

    String clkText = "";
    for(ShortURL u : urls){
      Long count = clickService.clicksByHash(u.getHash());
      json.put("target", target);
      json.put("hash", u.getHash());
      json.put("count", count);

      //TODO Quitar
      json.put("alcanzable", u.getAlcanzable());
    }
    return new ResponseEntity<>(json, HttpStatus.OK);
  }

  @RequestMapping(value = "/csv", method = RequestMethod.POST, produces = "text/csv")
  public ResponseEntity handleFileUpload(@RequestParam("file") MultipartFile file,
                                  HttpServletRequest request) {
    ArrayList<String> lines = CSVGenerator.readCSV(file);
		File csv = CSVGenerator.writeCSV(lines, request.getRemoteAddr(), shortUrlService); 
		return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=ShortURL.csv")
                .contentLength(csv.length())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new FileSystemResource(csv));
	}
}
