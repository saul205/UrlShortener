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

import urlshortener.domain.Click;
import urlshortener.domain.ShortURL;
import urlshortener.repository.ClickRepository;
import urlshortener.repository.impl.Tuple;
import urlshortener.service.ClickService;
import urlshortener.service.HistoryService;
import urlshortener.service.ShortURLService;
import urlshortener.service.ReachableService;
import urlshortener.service.QRGenerator;
import urlshortener.service.CSVGenerator;
import urlshortener.domain.HistoryElement;

import java.util.ArrayList;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.io.File;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

@RestController
public class UrlShortenerController {

  public enum Estado{
    correcto(1), me_da_q_no(-1), unknown(0);

    public final Integer value;

    private Alcanzable(Integer v){
      this.value = v;
    }
  }

  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  private final ReachableService reachableSVC;

  private final HistoryService historyService;

  private final ThreadPoolExecutor executor;

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService, HistoryService historyService, ReachableService reachableSVC) {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
    this.historyService = historyService;
    this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    this.reachableSVC = reachableSVC; //new ReachableService(null);
    executor.submit(() -> {
      reachableSVC.receiver(shortUrlService, shortUrlService.getSURLSVC());
    });
    executor.submit(() -> {
      try {
        ReachableService.main(null);
      } catch (Exception e) {}
    });
    executor.submit(() -> {
      try {
        ReachableService.main(null);
      } catch (Exception e) {}
    });
  }

  @RequestMapping(value = "/{id:(?!link|index|sh).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id,
                                      HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);

    if(l == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

    if (l.getAlcanzable() == 1 && l.getSafe() == 1) {
      clickService.saveClick(id, extractIP(request));
      return createSuccessfulRedirectToResponse(l);
    } else if(l.getAlcanzable() == 0) {
      return new ResponseEntity<String>("No se sabe si la url es alcanzable o no, intente en un rato", HttpStatus.OK);
    } else if(l.getAlcanzable() == -1) {
      return new ResponseEntity<String>("La url no es alcanzable", HttpStatus.OK);
    } else if(l.getSafe() == -1) {
      return new ResponseEntity<String>("La url no es segura", HttpStatus.OK);
    } else if(l.getSafe() == 0) {
      return new ResponseEntity<String>("No se sabe si la url es segura o no, intente en un rato", HttpStatus.OK);
    }

    if(l.getAlcanzable() == Alcanzable.desconocido.value){
      JSONObject o = new JSONObject();
      o.put("Error", "No se sabe si la url es alcanzable o no, intente en un rato");
      return new ResponseEntity<JSONObject>(o, HttpStatus.CONFLICT);
    }else if(l.getAlcanzable() == Alcanzable.no_alcanzable.value){
      JSONObject o = new JSONObject();
      o.put("Error", "La url no es alcanzable");
      return new ResponseEntity<JSONObject>(o, HttpStatus.BAD_REQUEST);
    }

    if(!l.getSafe()){
      JSONObject o = new JSONObject();
      o.put("Error", "No seguro");
      return new ResponseEntity<JSONObject>(o, HttpStatus.BAD_REQUEST);
    }

    clickService.saveClick(id, extractIP(request));
    return createSuccessfulRedirectToResponse(l);
  }

  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<ShortURL> shortener(@RequestParam("url") String url,
                                            @RequestParam(value = "sponsor", required = false)
                                                String sponsor,
                                             @RequestParam(value = "qr", required = false)
                                                Boolean qr,
                                            HttpServletRequest request) {
    UrlValidator urlValidator = new UrlValidator(new String[] {"http",
        "https"});

    if (urlValidator.isValid(url)) {
      Boolean qrres = (qr == null ? false : qr);
      ShortURL su = shortUrlService.save(url, sponsor, request.getRemoteAddr(), qrres);
      HttpHeaders h = new HttpHeaders();
      h.setLocation(su.getUri());

      reachableSVC.sender(su);

      executor.submit(() -> {
        HistoryElement s = historyService.save(su.getHash(), su.getTarget(), su.getCreated(), su.getIP());
      });

      return new ResponseEntity<>(su, h, HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }


  }

  @RequestMapping(value = "/qr", method = RequestMethod.GET,
                  produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<BufferedImage> generateQR(@RequestParam("hashUS") String hash){
    ShortURL su = shortUrlService.findByKey(hash);
    if (su == null) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    if (su.getAlcanzable() != 1){
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
    if (su.getSafe() != 1){
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
    if (su.getQR() == null) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    String toQR = su.getUri().toString();
    try {
      BufferedImage image = QRGenerator.generateQRImage(toQR);
      return new ResponseEntity<>(image, HttpStatus.OK);
    } catch (Exception e){
      return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @RequestMapping(value = "/stid", method = RequestMethod.GET)
  public ResponseEntity<JSONObject> getStatsURL(@RequestParam("id") String id){
    ShortURL su = shortUrlService.findByKey(id);
    if (su != null){
      JSONObject json = new JSONObject();
      json.put("id", id);
      json.put("alcanzable", su.getAlcanzable());
      json.put("segura", su.getSafe());
      return new ResponseEntity<>(json, HttpStatus.OK);
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

  @RequestMapping(value = "/csv", method = RequestMethod.POST, produces = "text/csv")
  public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file,
                                  HttpServletRequest request) {
    ArrayList<String> lines = CSVGenerator.readCSV(file);
    if(lines == null)
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		ArrayList<Object> csv = CSVGenerator.writeCSV(lines, request.getRemoteAddr(), shortUrlService);
    ArrayList<ShortURL> su = (ArrayList<ShortURL>)csv.get(0);
    int size = su.size();
    if(size == 0)
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

    executor.submit(() -> {

      for(ShortURL s : su) {
        reachableSVC.sender(s);
      }

    });

    File ret = (File)csv.get(1);
    URI location = su.get(0).getUri();
    return ResponseEntity.created(location)
                .header("Content-Disposition", "attachment; filename=ShortURL.csv")
                .contentLength(ret.length())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new FileSystemResource(ret));
	}
}
