package urlshortener.service;

import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.*;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

//Implementacion de RbbMQ con spring es secuencial
//import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import urlshortener.service.ShortURLService;
import urlshortener.service.ClickService;
import urlshortener.service.CountsService;
import urlshortener.service.HistoryService;
import urlshortener.service.MostVisitedService;
import urlshortener.ApplicationContextProvider;
import urlshortener.domain.Click;
import urlshortener.domain.Counts;
import urlshortener.domain.ShortURL;
import urlshortener.domain.HistoryElement;
import urlshortener.repository.impl.Tuple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import urlshortener.ApplicationContextProvider;

@Service
public class DataPrecalculator {

  private static final Logger logger = LoggerFactory.getLogger(DataPrecalculator.class);

  private final ClickService clickService;

  private final ShortURLService shortURLService;

  private final CountsService countsService;

  private final HistoryService historyService;

  private final MostVisitedService mostVisitedService;

  public static final String JOBS_TO_DO = "PRECALCULUS_TO_DO";
  public static final String FINISHED_JOBS = "FINISHED_PRECALCULUS";

  // Worker's method (Worker's Work)
  private void Work(String str){

    if(str.equals("precalculate-1")){
      updateProcess1();
    }else if(str.equals("precalculate-2")){
      updateProcess2();
    }
    
  }

  private void updateProcess2(){
    
    List<ShortURL> s = shortURLService.list();

		for(ShortURL shu : s){
			Long c = clickService.clicksByHash(shu.getHash());
			countsService.save(shu.getTarget(), c, false);
		}
  }

  private void updateProcess1(){

    Long c = shortURLService.count();
    countsService.save("shu", c, true);

    c = clickService.count();
    countsService.save("cl", c, true);

    List<Tuple> urls = clickService.getTopN(10);
    mostVisitedService.save(urls);

    List<ShortURL> s = shortURLService.getLastN(10);
    historyService.save(s);
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

      new DataPrecalculator(new String[] {}).Work(message);

      channel.basicPublish("", FINISHED_JOBS, null, "finished".getBytes(StandardCharsets.UTF_8));

      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    };

    channel.basicConsume(JOBS_TO_DO, false, deliverCallback, consumerTag -> { });
  }

  private ConnectionFactory fact;
  private Connection conn;
  private Channel ch;

  public DataPrecalculator(String[] args) {

    shortURLService = ApplicationContextProvider.getContext().getBean(ShortURLService.class);
    clickService = ApplicationContextProvider.getContext().getBean(ClickService.class);
    mostVisitedService = ApplicationContextProvider.getContext().getBean(MostVisitedService.class);
    countsService = ApplicationContextProvider.getContext().getBean(CountsService.class);
    historyService = ApplicationContextProvider.getContext().getBean(HistoryService.class);

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

  public void receiver() {
    DeliverCallback deliverCB = (consumerTag, delivery) -> {
      ch.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
    };

    try {
      ch.basicConsume(FINISHED_JOBS, false, deliverCB, consumerTag -> { });
    } catch (Exception e) {
      logger.info(e.toString());
    }
  }

  public void sender() {

    try {
      ch.basicPublish("", JOBS_TO_DO, null, "precalculate-1".getBytes(StandardCharsets.UTF_8));
      ch.basicPublish("", JOBS_TO_DO, null, "precalculate-2".getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      logger.info(e.toString());
    }
  }

}
