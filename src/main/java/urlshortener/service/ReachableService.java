package urlshortener.service;

import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.springframework.stereotype.Service;

//Implementacion de RbbMQ con spring es secuencial
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.nio.charset.StandardCharsets;

import urlshortener.domain.ShortURL;
import urlshortener.repository.ShortURLRepository;
import urlshortener.service.ShortURLService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReachableService {

  private static final Logger logger = LoggerFactory.getLogger(ReachableService.class);

  public static final String JOBS_TO_DO = "JOBS_TO_DO";
  public static final String FINISHED_JOBS = "FINISHED_JOBS";

  // Worker's method (Worker's Work)
  private static Integer Work(String str_url){
    Boolean rble = false;
    URL url; HttpURLConnection huc;
    try {
      url = new URL(str_url);
      // setFollowRedirects -> SecurityException
      HttpURLConnection.setFollowRedirects(false);
      huc = (HttpURLConnection) url.openConnection();
      huc.setConnectTimeout(10000);
      huc.getResponseCode();
      rble = true;
    } catch(UnknownHostException u) {
      rble = false;
    } catch(SocketTimeoutException s) {
      rble = false;
    } catch(Exception e1) { // SecurityException || IOException
      try{
        url = new URL(str_url);
        HttpURLConnection.setFollowRedirects(true); // By default
        huc = (HttpURLConnection) url.openConnection();
        huc.setConnectTimeout(10000);
        huc.getResponseCode();
        rble = true;
      } catch (Exception e2){ //SocketTimeoutException, IOException or unknown exception
        rble = false;
      }
    } 
    Integer r;
    if (rble) r = 1;
    else r = -1;
    return r;
  }

  // Suscriber (Worker)
  public static void main(String[] args) throws Exception {
    // ASSUME ip = localhost, port = 5672 and rabbitmq default account
    /*if (args.length != 2){
      System.out.println(" [ Err args ]: must be invoked with ./<exec> <host> <port>");
      throw new Exception(" [ Err args ]: must be invoked with ./<exec> <host> <port>");
    }*/
    ConnectionFactory factory = new ConnectionFactory();
    //factory.setHost(args[0]);
    //factory.setPort(Integer.valueOf(args[1]).intValue());
    //  "    .setuser + .setpassword

    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.queueDeclare(JOBS_TO_DO, false, false, true, null);
    channel.queueDeclare(FINISHED_JOBS, false, false, true, null);

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), "UTF-8");
      //logger.info("---> [WORKER|r]: " + message);
      String[] splitted = message.split(",");
      if (splitted.length == 2){

        Integer aux = Work(splitted[1]);
        message = splitted[0] + "," + aux.toString();
        //logger.info("---> [WORKER|s]: " + message);
        channel.basicPublish("", FINISHED_JOBS, null, message.getBytes(StandardCharsets.UTF_8));
      }
    };

    channel.basicConsume(JOBS_TO_DO, false, deliverCallback, consumerTag -> { });
  }

  private ConnectionFactory fact;
  private Connection conn;
  private Channel ch;

  public ReachableService(String[] args) {
    try {
      // ASSUME ip = localhost, port = 5672 and rabbitmq default account
      /*if (args.length != 2){
        System.out.println(" [ Err args ]: must be invoked with ./<exec> <host> <port>");
        throw new Exception(" [ Err args ]: must be invoked with ./<exec> <host> <port>");
      }*/
      fact = new ConnectionFactory();
      //fact.setHost(args[0]);
      //fact.setPort(Integer.valueOf(args[1]).intValue());

      conn = fact.newConnection();
      ch = conn.createChannel();

      ch.queueDeclare(FINISHED_JOBS, false, false, true, null);
      ch.queueDeclare(JOBS_TO_DO, false, false, true, null);
          
    } catch (Exception e) {
      logger.info(e.toString());
    }
    
  }

  public void receiver(ShortURLService surlsvc) {
    DeliverCallback deliverCB = (consumerTag, delivery) -> {

      new Thread(() -> {
        try {
          String message = new String(delivery.getBody(), "UTF-8");
          //logger.info("---> [RECEIVER|r]: " + message);
          String[] splitted = message.split(",");
          if (splitted.length == 2){
            ShortURL surl = surlsvc.findByKey(splitted[0]);
            if (surl != null) {
              surl.setAlcanzable(Integer.valueOf(splitted[1]));
              surlsvc.update(surl);

              if (surl.getAlcanzable() == 1) surlsvc.checkSafe(new ShortURL[] {surl});
            }
          }
        } catch (Exception e) {
          logger.info(e.toString());
        }
      }).start();

    };

    try {
      ch.basicConsume(FINISHED_JOBS, false, deliverCB, consumerTag -> { });
    } catch (Exception e) {
      logger.info(e.toString());
    }
  }

  public void sender(ShortURL surl) {
    if (surl == null) return;
    logger.info("---> [SURL|s]: ");
    String msg = surl.getHash() + "," + surl.getTarget();
    logger.info("---> [SENDER|s]: " + msg);

    try {
      ch.basicPublish("", JOBS_TO_DO, null, msg.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      logger.info(e.toString());
    }
  }

}
