package urlshortener.web;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import urlshortener.service.ShortURLService;
import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.CountsService;
import urlshortener.service.MostVisitedService;
import urlshortener.domain.Click;
import urlshortener.domain.Counts;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import org.springframework.stereotype.Component;
import org.springframework.boot.actuate.endpoint.annotation.*;

import java.util.List;
import urlshortener.repository.impl.Tuple;
import org.springframework.web.bind.annotation.RestController;

@Component
@Endpoint(id = "data")
public class DBActuator{

  private final ShortURLService shortUrlService;

  private final ClickService clickService;

  private final CountsService countsService;

  private final MostVisitedService mostVisitedService;

  public DBActuator(ShortURLService shortUrlService, ClickService clickService, CountsService countsService, MostVisitedService mostVisitedService) {
		this.shortUrlService = shortUrlService;
		this.clickService = clickService;
		this.countsService = countsService;
		this.mostVisitedService = mostVisitedService;
  }
	  
  @ReadOperation
  public ResponseEntity<JSONObject> getData(){

		List<Counts> counts = mostVisitedService.find();

		JSONObject obj = new JSONObject();
		JSONObject mainObj = new JSONObject();

		int i = 0;
		for(Counts u : counts){
			JSONArray j = new JSONArray();
			j.add(u.getHash());
			j.add(u.getTarget());
			j.add(u.getCount());
			obj.put(i, j);
			i++;
		}

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

		
		mainObj.put("top", obj);
		updateProcess();
		
		return new ResponseEntity<>(mainObj, HttpStatus.OK);
  }

  private void updateProcess(){
	  Long c = shortUrlService.count();
	  countsService.save("shu", c, true);

	  c = clickService.count();
	  countsService.save("cl", c, true);

	  List<Tuple> urls = clickService.getTopN(10);
	  mostVisitedService.save(urls);
  }

    @WriteOperation
 	public ResponseEntity<JSONObject> getTargetCount(String target){

		if(target == "") return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		List<Counts> urls = countsService.findByTarget(target);
		JSONObject json = new JSONObject();

		if(urls.size() == 0){
			json.put("target", target);	
			json.put("count", 0);

			updateCount(target);
			return new ResponseEntity<>(json, HttpStatus.OK);
		}

		String clkText = "";
		for(Counts u : urls){
			json.put("target", u.getTarget());
			json.put("hash", u.getHash());
			json.put("count", u.getCount());

			updateCount(u.getTarget());
		}
		return new ResponseEntity<>(json, HttpStatus.OK);
	}

	private void updateCount(String target){

		List<ShortURL> s = shortUrlService.findByTarget(target);

		if(s.size() > 0){
			Long c = clickService.clicksByHash(s.get(0).getHash());
			countsService.save(target, c, false);
			return;
		}

		countsService.save(target, 0L, false);
	}
}