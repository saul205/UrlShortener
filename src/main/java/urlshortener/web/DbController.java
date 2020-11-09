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
import java.io.File;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

@RestController
@RequestMapping("/db")
public class DbController {

	private final ShortURLService shortUrlService;

  private final ClickService clickService;

  public DbController(ShortURLService shortUrlService, ClickService clickService, ReachableService reachableService) {
		this.shortUrlService = shortUrlService;
		this.clickService = clickService;
	}
	  
  @RequestMapping(value = "", method = RequestMethod.GET)
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

  @RequestMapping(value = "/search", method = RequestMethod.POST)
 	public ResponseEntity<JSONObject> getTargetCount(@RequestParam("url") String target){
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
	  
	@RequestMapping(value = "/history", method = RequestMethod.GET)
	public ResponseEntity<JSONObject> getHistory(HttpServletRequest request){
		String ip = request.getRemoteAddr();
		List<ShortURL> top = shortUrlService.getLastNByIp(ip, 10);

		JSONObject json = new JSONObject();
		int i = 0;
		for(ShortURL u : top){
			JSONObject j = new JSONObject();
			j.put(u.getTarget(), u.getCreated().toString());
			json.put(i, j);
			i++;
		}
		return new ResponseEntity<>(json, HttpStatus.OK);
	}
}