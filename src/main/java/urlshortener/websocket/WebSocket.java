package urlshortener.websocket;

import org.springframework.stereotype.Controller;

import javax.websocket.*;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerEndpoint;

import urlshortener.ApplicationContextProvider;
import urlshortener.service.CSVService;

import java.util.concurrent.ExecutionException;
import java.io.IOException;
import java.nio.channels.InterruptedByTimeoutException;

@ServerEndpoint(value = "/csvws")
@Controller
public class WebSocket {

  @OnMessage
  public void handleTextMessage(Session session, String message) throws IOException, InterruptedException, ExecutionException {
    if(message.equals("END")) {
      session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "create CSV"));
    } else {
      CSVService csv = ApplicationContextProvider.getContext().getBean(CSVService.class);
      session.getAsyncRemote().sendText(csv.generateLine(message));
      session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE, "create CSV"));
    }
  }

  @OnOpen
  public void afterConnectionEstablished(Session session) throws IOException {
    session.getAsyncRemote().sendText("Connected");
  }

}
