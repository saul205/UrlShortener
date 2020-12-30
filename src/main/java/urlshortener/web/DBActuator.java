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
import org.springframework.boot.actuate.endpoint.annotation.*;

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
@Endpoint(id = "data")
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
	  
  @ReadOperation
  public ResponseEntity<JSONObject> getData(){

		List<Counts> counts = mostVisitedService.find();
		JSONObject mainObj = new JSONObject();

		Counts c = countsService.findByHash("cl");
		if(c == null){
			mainObj.put("clicks", 0);
		}else{
			mainObj.put("clicks", c.getCount());
		}

		c = countsService.findByHash("shu");
		if(c == null){
			mainObj.put("urls", 0);
		}else{
			mainObj.put("urls", c.getCount());
		}

		mainObj.put("historial", historyService.find(10));
		mainObj.put("top", counts);
		updateProcess();
		
		return new ResponseEntity<>(mainObj, HttpStatus.OK);
  }

  private void updateProcess(){
	  dataPrecalculator.sender();
  }

	@WriteOperation
 	public ResponseEntity<JSONObject> getTargetCount(String target){

		logger.info("TARGET: " + target);

		if(target == "") return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		List<Counts> urls = countsService.findByTarget(target);
		JSONObject json = new JSONObject();

		if(urls.size() == 0){
			json.put("target", target);	
			json.put("count", 0);

			updateProcess();
			return new ResponseEntity<>(json, HttpStatus.OK);
		}

		String clkText = "";
		for(Counts u : urls){
			json.put("target", u.getTarget());
			json.put("hash", u.getHash());
			json.put("count", u.getCount());
		}

		updateProcess();
		return new ResponseEntity<>(json, HttpStatus.OK);
	}
}