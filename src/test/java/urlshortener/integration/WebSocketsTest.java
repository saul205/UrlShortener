package urlshortener.web;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Before;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class WebSocketsTest {

  @LocalServerPort
  private int port;

  @ClientEndpoint
  public static class WebSocketClient {

    private Session userSession = null;
    public String answer;

    public void connect(String url) {
      try {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(this, new URI(url));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void sendMessage(String message) throws IOException {
      if (userSession != null && userSession.isOpen())
        this.userSession.getAsyncRemote().sendText(message);
      else 
        System.out.println("Sesion cerrada");
    }

    public boolean isClosed() {
      return userSession == null || !userSession.isOpen();
    }

    @OnOpen
    public void onOpen(Session userSession) {
      this.userSession = userSession;
    }

    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
      this.userSession = null;
    }

    @OnMessage
    public void onMessage(String message) {
      this.answer = message;
    }
  }

  private WebSocketClient client;

  @Before
  public void setup() throws Exception {
    client = new WebSocketClient();
  }

  @Test
  public void testCSVWebSocketEmpty() throws Exception {
    assertThat(client.isClosed(), is(true));
    client.connect("ws://localhost:" + this.port + "/csvws");
    Thread.sleep(500);
    assertThat(client.isClosed(), is(false));
    assertThat(client.answer, is("Connected"));

    client.sendMessage("");
    Thread.sleep(500);
    assertThat(client.answer, is(""));
    assertThat(client.isClosed(), is(true));

    client.connect("ws://localhost:" + this.port + "/csvws");
    Thread.sleep(500);
    assertThat(client.isClosed(), is(false));
    assertThat(client.answer, is("Connected"));

    client.sendMessage("\n");
    Thread.sleep(500);
    assertThat(client.answer, is(""));
    assertThat(client.isClosed(), is(true));

    client.connect("ws://localhost:" + this.port + "/csvws");
    Thread.sleep(500);
    assertThat(client.isClosed(), is(false));
    assertThat(client.answer, is("Connected"));

    client.sendMessage("127.0.0.1");
    Thread.sleep(500);
    assertThat(client.answer, is(""));
    assertThat(client.isClosed(), is(true));

    client.connect("ws://localhost:" + this.port + "/csvws");
    Thread.sleep(500);
    assertThat(client.isClosed(), is(false));
    assertThat(client.answer, is("Connected"));

    client.sendMessage("127.0.0.1\n");
    Thread.sleep(500);
    assertThat(client.answer, is(""));
    assertThat(client.isClosed(), is(true));
  }

  @Test
  public void testCSVWebSocketCorrect() throws Exception {
    assertThat(client.isClosed(), is(true));
    client.connect("ws://localhost:" + this.port + "/csvws");
    Thread.sleep(500);
    assertThat(client.isClosed(), is(false));
    assertThat(client.answer, is("Connected"));

    client.sendMessage("http://google.com/\nhttp://example.com/\n127.0.0.1");
    Thread.sleep(500);
    assertThat(client.answer, is("http://google.com/,http://localhost:" + this.port + "/5e399431,\n" +
              "http://example.com/,http://localhost:" + this.port + "/f684a3c4,\n"));
    assertThat(client.isClosed(), is(true));
  }

  @Test
  public void testCSVWebSocketInvalid() throws Exception {
    assertThat(client.isClosed(), is(true));
    client.connect("ws://localhost:" + this.port + "/csvws");
    Thread.sleep(500);
    assertThat(client.isClosed(), is(false));
    assertThat(client.answer, is("Connected"));

    client.sendMessage("notvalid\nhttp://example.com/\n127.0.0.1");
    Thread.sleep(500);
    assertThat(client.answer, is("notvalid,,Debe ser una URI http o https valida\n" +
              "http://example.com/,http://localhost:" + this.port + "/f684a3c4,\n"));
    assertThat(client.isClosed(), is(true));
  }

  @Test
  public void testCSVWebSocketAllInvalid() throws Exception {
    assertThat(client.isClosed(), is(true));
    client.connect("ws://localhost:" + this.port + "/csvws");
    Thread.sleep(500);
    assertThat(client.isClosed(), is(false));
    assertThat(client.answer, is("Connected"));

    client.sendMessage("notvalid\nnotvalid2\n127.0.0.1");
    Thread.sleep(500);
    assertThat(client.answer, is("INVALID"));
    assertThat(client.isClosed(), is(true));
  }
}
