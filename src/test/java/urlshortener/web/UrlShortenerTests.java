package urlshortener.web;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static urlshortener.fixtures.ShortURLFixture.someUrl;
import static urlshortener.fixtures.ShortURLFixture.someUrlNotAvailable;
import static urlshortener.fixtures.ShortURLFixture.someUrlNotYetAvailable;
import static urlshortener.fixtures.ShortURLFixture.urlNotSafe;


import java.net.URI;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import urlshortener.domain.ShortURL;
import urlshortener.service.ClickService;
import urlshortener.service.ReachableService;
import urlshortener.service.ShortURLService;

public class UrlShortenerTests {

  private MockMvc mockMvc;

  @Mock
  private ClickService clickService;

  @Mock
  private ShortURLService shortUrlService;

  @Mock
  private ReachableService reachableSVC;

  @InjectMocks
  private UrlShortenerController urlShortener;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.mockMvc = MockMvcBuilders.standaloneSetup(urlShortener).build();
  }

  @Test
  public void thatRedirectToReturnsTemporaryRedirectIfKeyExistsAndAvailable()
      throws Exception {
    when(shortUrlService.findByKey("someKey")).thenReturn(someUrl());

    mockMvc.perform(get("/{id}", "someKey")).andDo(print())
        .andExpect(status().isTemporaryRedirect())
        .andExpect(redirectedUrl("http://example.com/"));
  }

  @Test
  public void thatRedirectToReturnsConflictIfKeyExistsAndNotYetAvailable()
      throws Exception {
    when(shortUrlService.findByKey("someKey")).thenReturn(someUrlNotYetAvailable());

    mockMvc.perform(get("/{id}", "someKey")).andDo(print())
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.Error", is("No se sabe si la url es alcanzable o no, intente en un rato")));
  }

  @Test
  public void thatRedirectToReturnsTemporaryRedirectIfKeyExistsAndNotAvailable()
      throws Exception {
    when(shortUrlService.findByKey("someKey")).thenReturn(someUrlNotAvailable());

    mockMvc.perform(get("/{id}", "someKey")).andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.Error", is("La url no es alcanzable")));
  }

  @Test
  public void thatRedirectToReturnsTemporaryRedirectIfKeyExistsAndNotSafe()
      throws Exception {
    when(shortUrlService.findByKey("someKey")).thenReturn(urlNotSafe());

    mockMvc.perform(get("/{id}", "someKey")).andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.Error", is("La url no es segura")));
  }

  @Test
  public void thatRedirecToReturnsNotFoundIdIfKeyDoesNotExist()
      throws Exception {
    when(shortUrlService.findByKey("someKey")).thenReturn(null);

    mockMvc.perform(get("/{id}", "someKey")).andDo(print())
        .andExpect(status().isNotFound());
  }

  @Test
  public void thatShortenerCreatesARedirectIfTheURLisOK() throws Exception {
    configureSave(null);

    mockMvc.perform(post("/link").param("url", "http://example.com/"))
        .andDo(print())
        .andExpect(redirectedUrl("http://localhost/f684a3c4"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.hash", is("f684a3c4")))
        .andExpect(jsonPath("$.uri", is("http://localhost/f684a3c4")))
        .andExpect(jsonPath("$.target", is("http://example.com/")))
        .andExpect(jsonPath("$.sponsor", is(nullValue())));
  }

  @Test
  public void thatShortenerCreatesARedirectWithSponsor() throws Exception {
    configureSave("http://sponsor.com/");

    mockMvc.perform(
        post("/link").param("url", "http://example.com/").param(
            "sponsor", "http://sponsor.com/")).andDo(print())
        .andExpect(redirectedUrl("http://localhost/f684a3c4"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.hash", is("f684a3c4")))
        .andExpect(jsonPath("$.uri", is("http://localhost/f684a3c4")))
        .andExpect(jsonPath("$.target", is("http://example.com/")))
        .andExpect(jsonPath("$.sponsor", is("http://sponsor.com/")));
  }

  @Test
  public void thatShortenerFailsIfTheURLisWrong() throws Exception {
    configureSave(null);

    mockMvc.perform(post("/link").param("url", "someKey")).andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void thatShortenerFailsIfTheRepositoryReturnsNull() throws Exception {
    when(shortUrlService.save(any(String.class), any(String.class), any(String.class), any(Boolean.class)))
        .thenReturn(null);

    mockMvc.perform(post("/link").param("url", "someKey")).andDo(print())
        .andExpect(status().isBadRequest());
  }

  @Test
  public void thatShortenerCreatesARedirectWithQR() throws Exception {
    configureSave(null);
    
    mockMvc.perform(
        post("/link").param("url", "http://example.com/").param("qr", "true")).andDo(print())
        .andExpect(redirectedUrl("http://localhost/f684a3c4"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.hash", is("f684a3c4")))
        .andExpect(jsonPath("$.uri", is("http://localhost/f684a3c4")))
        .andExpect(jsonPath("$.target", is("http://example.com/")))
        .andExpect(jsonPath("$.qr", is("http://localhost/qr?hashUS=f684a3c4")));
  }

  private void configureSave(String sponsor) {
    when(shortUrlService.save(any(), any(), any(), any()))
        .then((Answer<ShortURL>) invocation -> new ShortURL(
            "f684a3c4",
            "http://example.com/",
            URI.create("http://localhost/f684a3c4"),
            sponsor,
            null,
            null,
            0,
            -1,
            "http://localhost/qr?hashUS=f684a3c4",
            null,
            null));
  }
}
