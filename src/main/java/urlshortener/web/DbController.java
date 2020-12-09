package urlshortener.web;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

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
import urlshortener.domain.HistoryElement;
import urlshortener.repository.impl.Tuple;
import urlshortener.service.ClickService;
import urlshortener.service.ShortURLService;
import urlshortener.service.HistoryService;
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
@RequestMapping("/history")
public class DbController {

  private final HistoryService historyService;

  public DbController(HistoryService historyService) {
		this.historyService = historyService;
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	public ResponseEntity<JSONObject> getHistory(HttpServletRequest request){
		JSONObject json = new JSONObject();
		String ip = request.getRemoteAddr();

		List<HistoryElement> top = historyService.findByIp(ip, 10);

		int i = 0;
		for(HistoryElement he : top){
			JSONArray j = new JSONArray();
			j.add(linkTo(methodOn(UrlShortenerController.class).redirectTo(he.getHash(), null)).toUri().toString());
			j.add(he.getCreated().toString());
			j.add(he.getTarget());
			json.put(i, j);
			i++;
		}
		return new ResponseEntity<JSONObject>(json, HttpStatus.OK);
	}
}
