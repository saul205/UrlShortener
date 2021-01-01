package urlshortener.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.Ignore;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import java.util.HashMap;
import java.util.List;
import java.net.URI;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import com.google.zxing.Result;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@DirtiesContext
public class SystemTests {

  private static final Logger logger = LoggerFactory.getLogger(SystemTests.class);

  @Autowired
  private TestRestTemplate restTemplate;

  @LocalServerPort
  private int port;

  @Test
  public void testHome() {
    ResponseEntity<String> entity = restTemplate.getForEntity("/", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.OK));
    assertNotNull(entity.getHeaders().getContentType());
    assertTrue(
        entity.getHeaders().getContentType().isCompatibleWith(new MediaType("text", "html")));
    assertThat(entity.getBody(), containsString("<title>URL"));
  }

  @Test
  public void testCss() {
    ResponseEntity<String> entity =
        restTemplate.getForEntity("/webjars/bootstrap/3.3.5/css/bootstrap.min.css", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.OK));
    assertThat(entity.getHeaders().getContentType(), is(MediaType.valueOf("text/css")));
    assertThat(entity.getBody(), containsString("body"));
  }

  @Test
  public void testCreateLink() throws Exception {
    ResponseEntity<String> entity = postLink("http://example.com/");

    assertThat(entity.getStatusCode(), is(HttpStatus.CREATED));
    assertThat(entity.getHeaders().getLocation(),
        is(new URI("http://localhost:" + this.port + "/f684a3c4")));
    assertThat(entity.getHeaders().getContentType(), is(new MediaType("application", "json")));
    ReadContext rc = JsonPath.parse(entity.getBody());
    assertThat(rc.read("$.hash"), is("f684a3c4"));
    assertThat(rc.read("$.uri"), is("http://localhost:" + this.port + "/f684a3c4"));
    assertThat(rc.read("$.target"), is("http://example.com/"));
    assertThat(rc.read("$.sponsor"), is(nullValue()));
  }

  @Test
  public void testRedirection() throws Exception {
    postLink("http://example.com/");

    Thread.sleep(2000);

    ResponseEntity<String> entity = restTemplate.getForEntity("/f684a3c4", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.TEMPORARY_REDIRECT));
    assertThat(entity.getHeaders().getLocation(), is(new URI("http://example.com/")));
  }

  @Test
  public void testRedirectionNotYet() throws Exception {
    postLink("http://example.com/");

    ResponseEntity<String> entity = restTemplate.getForEntity("/f684a3c4", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.CONFLICT));

    ReadContext rc = JsonPath.parse(entity.getBody());
    assertThat(rc.read("$.Error"), is("No se sabe si la url es alcanzable o no, intente en un rato"));
  }

  @Test
  public void testRedirectionNotAvailable() throws Exception {
    postLink("http://noexiste.es/");

    Thread.sleep(2000);

    ResponseEntity<String> entity = restTemplate.getForEntity("/295d6175", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    ReadContext rc = JsonPath.parse(entity.getBody());
    assertThat(rc.read("$.Error"), is("La url no es alcanzable"));
  }

  @Test
  public void testRedirectionNotSafe() throws Exception {
    postLink("https://testsafebrowsing.appspot.com/s/malware.html");

    Thread.sleep(6000);

    ResponseEntity<String> entity = restTemplate.getForEntity("/b61e4f44", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    ReadContext rc = JsonPath.parse(entity.getBody());
    assertThat(rc.read("$.Error"), is("La url no es segura"));
  }

  @Test
  public void testQrOK() throws Exception {
    ResponseEntity<String> entity = postLinkBool("https://www.google.com/", new Boolean(true));

    assertThat(entity.getStatusCode(), is(HttpStatus.CREATED));
    ReadContext rc = JsonPath.parse(entity.getBody());
    assertThat(rc.read("$.qr"), is("http://localhost:" + this.port + "/qr?hashUS=" + rc.read("$.hash")));

    Thread.sleep(6000);

    ResponseEntity<byte[]> entity2 = getQR(rc.read("$.hash"));
    assertThat(entity2.getStatusCode(), is(HttpStatus.OK));

    BinaryBitmap binaryBitmap = new BinaryBitmap( new HybridBinarizer(
        new BufferedImageLuminanceSource(
            ImageIO.read(
              new ByteArrayInputStream(entity2.getBody())
            ))));
    Result result = new MultiFormatReader().decode(binaryBitmap);
    
    assertThat(rc.read("$.uri"), is(result.getText()));
  }

  @Ignore
  @Test
  public void testDB() throws Exception {
    postLink("http://example.com/");

    ResponseEntity<String> entity = restTemplate.getForEntity("/actuator/data", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.OK));
    assertThat(entity.getHeaders().getContentType(), is(new MediaType("application", "json")));
    ReadContext rc = JsonPath.parse(entity.getBody());
    assertThat(rc.read("$.clicks"), is(0));
    assertThat(rc.read("$.urls"), is(2));
    assertThat(rc.read("$.top"), is(new HashMap()));
  }

  @Ignore
  @Test
  public void testDBWithRedirects() throws Exception {

    postLink("http://example.com/");
    postLink("http://google.com/");

    Thread.sleep(2000);

    ResponseEntity<String> entity1 = restTemplate.getForEntity("/f684a3c4", String.class);
    assertThat(entity1.getStatusCode(), is(HttpStatus.TEMPORARY_REDIRECT));
    assertThat(entity1.getHeaders().getLocation(), is(new URI("http://example.com/")));

    entity1 = restTemplate.getForEntity("/5e399431", String.class);
    assertThat(entity1.getStatusCode(), is(HttpStatus.TEMPORARY_REDIRECT));
    assertThat(entity1.getHeaders().getLocation(), is(new URI("http://google.com/")));

    entity1 = restTemplate.getForEntity("/5e399431", String.class);
    assertThat(entity1.getStatusCode(), is(HttpStatus.TEMPORARY_REDIRECT));
    assertThat(entity1.getHeaders().getLocation(), is(new URI("http://google.com/")));

    ResponseEntity<String> entity = restTemplate.getForEntity("/actuator/data", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.OK));
    assertThat(entity.getHeaders().getContentType(), is(new MediaType("application", "json")));
    ReadContext rc = JsonPath.parse(entity.getBody());
    assertThat(rc.read("$.clicks"), is(4));
    assertThat(rc.read("$.urls"), is(4));
    HashMap top = new HashMap();
    top.put("http://example.com/", 1);
    HashMap top2 = new HashMap();
    top2.put("http://google.com/", 3);
    assertThat(rc.read("$.top.0"), is(top2));
    assertThat(rc.read("$.top.1"), is(top));
  }

  @Ignore
  @Test
  public void testDBSearch() throws Exception {

    postLink("http://google.com/");

    Thread.sleep(2000);

    ResponseEntity<String> entity1 = restTemplate.getForEntity("/5e399431", String.class);
    assertThat(entity1.getStatusCode(), is(HttpStatus.TEMPORARY_REDIRECT));
    assertThat(entity1.getHeaders().getLocation(), is(new URI("http://google.com/")));

    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("target", "http://google.com/");
    ResponseEntity<String> entity = restTemplate.postForEntity("/actuator/data", parts, String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.OK));
    assertThat(entity.getHeaders().getContentType(), is(new MediaType("application", "json")));
    ReadContext rc = JsonPath.parse(entity.getBody());
    assertThat(rc.read("$.hash"), is("5e399431"));
    assertThat(rc.read("$.target"), is("http://google.com/"));
    assertThat(rc.read("$.count"), is(1));
  }

  @Ignore
  @Test
  public void testDBHistory() throws Exception {
    
    postLink("http://google.com/");

    ResponseEntity<String> entity = restTemplate.getForEntity("/history", String.class);
    assertThat(entity.getStatusCode(), is(HttpStatus.OK));
    assertThat(entity.getHeaders().getContentType(), is(new MediaType("application", "json")));
    ReadContext rc = JsonPath.parse(entity.getBody());
    List h = rc.read("$.0", List.class);
    logger.info(h.toString());
    assert(h.contains("http://google.com/"));

    h = rc.read("$.1", List.class);
    assert(h.contains("http://example.com/"));
  }

  private ResponseEntity<String> postLink(String url) {
    return postLinkBool(url, new Boolean(false));
  }

  private ResponseEntity<String> postLinkBool(String url, Boolean bool) {
    MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
    parts.add("url", url);
    parts.add("qr", bool.toString());
    return restTemplate.postForEntity("/link", parts, String.class);
  }

  private ResponseEntity<byte[]> getQR(String hash) {
    return restTemplate.getForEntity("/qr?hashUS=" + hash, byte[].class);
  }

}