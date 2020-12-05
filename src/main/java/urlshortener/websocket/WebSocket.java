package urlshortener.websocket;

import org.springframework.stereotype.Controller;

import javax.websocket.*;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerEndpoint;

import java.io.IOException;

@ServerEndpoint(value = "/csvws")
@Controller
public class WebSocket {

  @OnMessage
  public void handleTextMessage(Session session, String message) throws IOException {
    if (message.contains("bye")) {
      session.getAsyncRemote().sendText("---");
    } else {
      session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Alright then, goodbye!"));
    }
  }

  @OnOpen
  public void afterConnectionEstablished(Session session) throws IOException {
    session.getAsyncRemote().sendText("Conectado");
  }

  @OnClose
  public void afterConnectionClosed(Session session, CloseReason closeReason) throws IOException {
    //LOGGER.info(String.format("Session %s closed because of %s", session.getId(), closeReason));  
  }
  @OnError
  public void handleTransportError(Session session, Throwable errorReason) throws IOException {
    //LOGGER.log(Level.SEVERE,
    //        String.format("Session %s closed because of %s", session.getId(), errorReason.getClass().getName()),
     //       errorReason);
  }
}
