package urlshortener.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import urlshortener.service.ShortURLService;
import urlshortener.service.ClickService;
import urlshortener.service.CountsService;
import urlshortener.service.DataPrecalculator;
import urlshortener.service.HistoryService;
import urlshortener.service.MostVisitedService;
import urlshortener.domain.Click;
import urlshortener.domain.Counts;
import urlshortener.domain.ShortURL;
import urlshortener.domain.HistoryElement;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import org.springframework.stereotype.Component;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@Component
@RestControllerEndpoint(id = "data")
public class DBActuator{
	
  private static final Logger logger = LoggerFactory.getLogger(UrlShortenerController.class);

  private final CountsService countsService;

  private final MostVisitedService mostVisitedService;
  
  private final ScheduledExecutorService executor;

  private final HistoryService historyService;

  private final DataPrecalculator dataPrecalculator;

  public DBActuator(CountsService countsService, 
					  MostVisitedService mostVisitedService,
					  HistoryService historyService,
					  DataPrecalculator dataPrecalculator) {

		this.countsService = countsService;
		this.mostVisitedService = mostVisitedService;
		this.historyService = historyService;
		this.dataPrecalculator = dataPrecalculator;

		executor = Executors.newScheduledThreadPool(2);

		executor.submit(() -> {
			try {
				dataPrecalculator.receiver();
			} catch (Exception e) {}
		});

		executor.submit(() -> {
			try {
				DataPrecalculator.main(null);
			} catch (Exception e) {}
		});

		executor.submit(() -> {
			try {
				DataPrecalculator.main(null);
			} catch (Exception e) {}
		});

		scheduledUpdate();
  }

  public void scheduledUpdate(){
	  TimerTask task = new TimerTask() {
		  public void run() {
			  updateProcess();
		  }
		};
	  executor.scheduleAtFixedRate(task, 0L, 10000L, TimeUnit.MILLISECONDS);
  }
	  
  @Operation(summary = "Returns the systems data to the user")
  @ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Url shortened, Url redirected, Top urls redirected and last 10 url shortened",
      content = { @Content(mediaType = "application/json",
        schema = @Schema(implementation = JsonData.class)) }) })
  @GetMapping(value = "")
  public ResponseEntity<JsonData> getData(){

		List<Counts> counts = mostVisitedService.find();
		Long clicks = 0L, urls = 0L;

		Counts c = countsService.findByHash("cl");
		if(c != null){
			clicks = c.getCount();
		}

		c = countsService.findByHash("shu");
		if(c != null){
			urls = c.getCount();
		}

		JsonData data = new JsonData(clicks, urls, counts, historyService.find(10));
		updateProcess();
		
		return new ResponseEntity<>(data, HttpStatus.OK);
  }

  private void updateProcess(){
	  dataPrecalculator.sender();
  }

	@Operation(summary = "Returns specific data for a given target url")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Number of times the url got redirected", 
			content = { @Content(mediaType = "application/json",
			schema = @Schema(implementation=JsonSearch.class)) }),
		@ApiResponse(responseCode = "400", description = "Introduced URL not valid", content = { @Content }) })
    @PostMapping(value = "")
 	public ResponseEntity<JsonSearch> getTargetCount(@org.springframework.web.bind.annotation.RequestBody JsonTarget jtarget){

		String target = jtarget.getTarget();
		logger.info("TARGET: " + target);

		if(target == "") return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		List<Counts> urls = countsService.findByTarget(target);

		if(urls.size() == 0){
			JsonSearch search = new JsonSearch(0L, target);

			updateProcess();
			return new ResponseEntity<>(search, HttpStatus.OK);
		}

		JsonSearch search = new JsonSearch(0L, null);

		for(Counts u : urls){

			search = new JsonSearch(u.getCount(), u.getHash(), u.getTarget());
		}

		updateProcess();
		return new ResponseEntity<>(search, HttpStatus.OK);
	}
}