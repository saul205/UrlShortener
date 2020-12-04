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
import urlshortener.service.ShortURLService;
import urlshortener.service.ReachableService;
import urlshortener.service.QRGenerator;
import urlshortener.service.CSVGenerator;

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
  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  private final ReachableService reachableService;

  private final ThreadPoolExecutor executor;

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService, ReachableService reachableService) {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
    this.reachableService = reachableService;
    this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
  }

  @RequestMapping(value = "/{id:(?!link|index|sh).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id,
                                      HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);
    if (l != null && l.getAlcanzable() == 1) {
      clickService.saveClick(id, extractIP(request));
      return createSuccessfulRedirectToResponse(l);
    } else if(l != null && l.getAlcanzable() == 0){
      return new ResponseEntity<String>("No se sabe si la url es alcanzable o no, intente en un rato", HttpStatus.OK);
    }else if(l != null && l.getAlcanzable() == -1){
      return new ResponseEntity<String>("La url no es alcanzable", HttpStatus.OK);
    }
    else{
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
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

      executor.submit(() -> {
        reachableService.isReachable(su.getHash());
        ShortURL aux = shortUrlService.findByKey(su.getHash());
        if(aux.getAlcanzable() == 1) 
          shortUrlService.checkSafe(new ShortURL[] {su});
      });
      /*new Thread(() -> {
        reachableService.isReachable(su.getHash());
      }).start();
      new Thread(() -> {
        shortUrlService.checkSafe(new ShortURL[] {su});
      }).start();*/

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
    if (!su.getSafe()){
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

    executor.submit(() -> {
      ArrayList<ShortURL> su = (ArrayList<ShortURL>)csv.get(0);
      CountDownLatch latch = new CountDownLatch(su.size());
      ArrayList<ShortURL> check = new ArrayList<ShortURL>();
      for(ShortURL s : su) {
        executor.submit(() -> {
          reachableService.isReachable(s.getHash());
          ShortURL aux = shortUrlService.findByKey(s.getHash());
          if(aux.getAlcanzable() == 1) check.add(aux);
          latch.countDown();
        });
      }

      try {
        latch.await();
      } catch(InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      
      if(check.size() > 0) shortUrlService.checkSafe(check);
    });

    /*new Thread(() -> {
      for(ShortURL su : (ArrayList<ShortURL>)csv.get(0)) {
        reachableService.isReachable(su.getHash());
      }
    }).start();
    new Thread(() -> {
      shortUrlService.checkSafe((ArrayList<ShortURL>)csv.get(0));
    }).start();*/
    File ret = (File)csv.get(1);
    return ResponseEntity.created(URI.create("/csv"))
                .header("Content-Disposition", "attachment; filename=ShortURL.csv")
                .contentLength(ret.length())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new FileSystemResource(ret));
	}
}
