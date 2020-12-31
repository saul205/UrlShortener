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

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import urlshortener.domain.Click;
import urlshortener.domain.ShortURL;
import urlshortener.domain.ErrorCode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class UrlShortenerController {

  public enum State {
    correct(1), incorrect(-1), unknown(0);

    public final Integer value;

    private State(Integer v){
      this.value = v;
    }
  }

  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  private final ReachableService reachableSVC;

  private final HistoryService historyService;

  private final ThreadPoolExecutor executor;
  private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);

  public UrlShortenerController(ShortURLService shortUrlService, ClickService clickService, HistoryService historyService, ReachableService reachableSVC) {
    this.shortUrlService = shortUrlService;
    this.clickService = clickService;
    this.historyService = historyService;
    this.executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    this.reachableSVC = reachableSVC; //new ReachableService(null);
    executor.submit(() -> {
      reachableSVC.receiver(shortUrlService);
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

  @ApiResponses(value = { 
    @ApiResponse(responseCode = "307", description = "Redirect to the corresponding page"),
    @ApiResponse(responseCode = "400", description = "The URL isn't safe or reachable", 
    content = { @Content(mediaType = "application/json", 
    schema = @Schema(implementation = ErrorCode.class)) }), 
    @ApiResponse(responseCode = "409", description = "The app doesn't know if the URL is safe or reachable", 
    content = { @Content(mediaType = "application/json", 
    schema = @Schema(implementation = ErrorCode.class)) }), 
    @ApiResponse(responseCode = "404", description = "ID not found", content = @Content) })
  @RequestMapping(value = "/{id:(?!link|index|sh).*}", method = RequestMethod.GET)
  public ResponseEntity<?> redirectTo(@PathVariable String id,
                                      HttpServletRequest request) {
    ShortURL l = shortUrlService.findByKey(id);

    if(l == null) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

    if(l.getAlcanzable() == State.unknown.value) {
      return new ResponseEntity<ErrorCode>(new ErrorCode("No se sabe si la url es alcanzable o no, intente en un rato"), HttpStatus.CONFLICT);
    } else if(l.getAlcanzable() == State.incorrect.value) {
      return new ResponseEntity<ErrorCode>(new ErrorCode("La url no es alcanzable"), HttpStatus.BAD_REQUEST);
    }

    if(l.getSafe() == State.unknown.value) {
      return new ResponseEntity<ErrorCode>(new ErrorCode("No se sabe si la url es segura o no, intente en un rato"), HttpStatus.CONFLICT);
    } else if(l.getSafe() == State.incorrect.value) {
      return new ResponseEntity<ErrorCode>(new ErrorCode("La url no es segura"), HttpStatus.BAD_REQUEST);
    }

    clickService.saveClick(id, extractIP(request));
    return createSuccessfulRedirectToResponse(l);
  }

  @Operation(summary = "Shortens the introduced URL")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "201", description = "URL shortened",
      content = { @Content(mediaType = "application/json",
        schema = @Schema(implementation = ShortURL.class)) }),
    @ApiResponse(responseCode = "400", description = "Introduced URL not valid",
      content = @Content)})
  @RequestMapping(value = "/link", method = RequestMethod.POST)
  public ResponseEntity<?> shortener(
        @Parameter(allowEmptyValue = false,
          schema = @Schema(example = "https://www.google.com/"),
          description = "URL to shorten") @RequestParam("url") String url,
        @Parameter(allowEmptyValue = false,
          schema = @Schema(example = "Coca-Cola"),
          description = "Ads") @RequestParam(value = "sponsor", required = false)
                                        String sponsor,
        @Parameter(allowEmptyValue = false,
          schema = @Schema(example = "true"),
          description = "Generate QR") @RequestParam(value = "qr", required = false)
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

      return new ResponseEntity<>(su, h, HttpStatus.CREATED);
    } else {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }


  }

  @Operation(summary = "Generate a QR code by its id")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "404", description = "Invalid ID supplied", content = @Content),
    @ApiResponse(responseCode = "409", description = "ID found. Not reachable or not safe", content = @Content),
    @ApiResponse(responseCode = "400", description = "This ID has no QR associated", content = @Content),
    @ApiResponse(responseCode = "500", description = "Internal server error", content = { @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorCode.class))}),
    @ApiResponse(responseCode = "200", description = "QR code generated", content = { @Content(mediaType = "image/png") })
  })
  @RequestMapping(value = "/qr", method = RequestMethod.GET,
                  produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<?> generateQR( @Parameter( allowEmptyValue = false,
      schema = @Schema(example = "qwerty123"), description = "ID to get his QR code")
    @RequestParam("hashUS") String hash){
    ShortURL su = shortUrlService.findByKey(hash);
    if (su == null) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    if (su.getAlcanzable() != State.correct.value){
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
    if (su.getSafe() != State.correct.value){
      return new ResponseEntity<>(HttpStatus.CONFLICT);
    }
    if (su.getQR() == null) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    String toQR = su.getUri().toString();
    try {
      BufferedImage image = QRGenerator.generateQRImage(toQR);
      return new ResponseEntity<BufferedImage>(image, HttpStatus.OK);
    } catch (Exception e){
      return new ResponseEntity<ErrorCode>(new ErrorCode("No se ha podido obtener el QR"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "Get stats from one url by its id")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "404", description = "Invalid ID supplied", content = @Content),
    @ApiResponse(responseCode = "200", description = "Stats recovered", content = { @Content(mediaType = "aplication/json") })
  })
  @RequestMapping(value = "/stid", method = RequestMethod.GET)
  public ResponseEntity<JSONObject> getStatsURL( @Parameter( allowEmptyValue = false,
      schema = @Schema(example = "qwerty123"), description = "URL ID used to get their stats") @RequestParam("id") String id){
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

  @Operation(summary = "Shortens all the URLs contained in a CSV file")
  @ApiResponses(value = { 
    @ApiResponse(responseCode = "201", description = "Created the CSV file with all the URLs shortened", 
      content = { @Content(mediaType = "text/csv")}),
    @ApiResponse(responseCode = "400", description = "Empty CSV file or all URLs invalid", 
      content = { @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorCode.class))}),
    @ApiResponse(responseCode = "415", description = "File with extension different than CSV or null", 
      content = { @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorCode.class))})})
  @RequestBody(description = "CSV file with the URLs to shorten",
    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
  @RequestMapping(value = "/csv", method = RequestMethod.POST, 
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "text/csv")
  public ResponseEntity<?> handleFileUpload(@RequestParam("file") MultipartFile file,
                                            HttpServletRequest request) {
    if(file == null || !file.getContentType().equals("text/csv")) {
      return new ResponseEntity<>(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
    
    ArrayList<String> lines = CSVGenerator.readCSV(file);

    if(lines.size() == 0) {
      ErrorCode er = new ErrorCode("Fichero vacío");
      HttpHeaders h = new HttpHeaders();
      h.setContentType(MediaType.parseMediaType("application/json"));
      return new ResponseEntity<ErrorCode>(er, h, HttpStatus.BAD_REQUEST);
    }

		ArrayList<Object> csv = CSVGenerator.writeCSV(lines, request.getRemoteAddr(), shortUrlService);
    ArrayList<ShortURL> su = (ArrayList<ShortURL>)csv.get(0);
    int size = su.size();
    if(size == 0) { 
      ErrorCode er = new ErrorCode("Todas las URL son inválidas");
      HttpHeaders h = new HttpHeaders();
      h.setContentType(MediaType.parseMediaType("application/json"));
      return new ResponseEntity<ErrorCode>(er, h, HttpStatus.BAD_REQUEST);
    }

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
