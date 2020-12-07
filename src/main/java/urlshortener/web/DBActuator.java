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
import urlshortener.domain.Click;
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

  public DBActuator(ShortURLService shortUrlService, ClickService clickService) {
		this.shortUrlService = shortUrlService;
		this.clickService = clickService;
  }
	  
  @ReadOperation
  public ResponseEntity<JSONObject> getData(){
		List<Tuple> urls = clickService.getTopN(10);

		JSONObject obj = new JSONObject();
		JSONObject mainObj = new JSONObject();

		int i = 0;
		for(Tuple u : urls){
			JSONObject j = new JSONObject();
			j.put(u.getKey(), u.getValue());
			obj.put(i, j);
			i++;
		}

		mainObj.put("clicks", clickService.count());
		mainObj.put("urls", shortUrlService.count());
		mainObj.put("top", obj);
		
		return new ResponseEntity<>(mainObj, HttpStatus.OK);
  }

    @WriteOperation
 	public ResponseEntity<JSONObject> getTargetCount(@RequestParam("url") String target){

		if(target == "") return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

		List<ShortURL> urls = shortUrlService.findByTarget(target);
		JSONObject json = new JSONObject();

		if(urls.size() == 0){
			json.put("target", target);	
			json.put("count", 0);	
			return new ResponseEntity<>(json, HttpStatus.OK);
		}

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
}