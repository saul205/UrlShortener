package urlshortener.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import urlshortener.service.ShortURLService;
import urlshortener.service.ReachableService;
import urlshortener.service.HistoryService;
import urlshortener.service.CSVGenerator;
import urlshortener.domain.ShortURL;
import urlshortener.ApplicationContextProvider;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class CSVService {

  @Autowired
  Environment environment;

  public String generateLine(String line) {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    ShortURLService sus = ApplicationContextProvider.getContext().getBean(ShortURLService.class);
    ReachableService rs = ApplicationContextProvider.getContext().getBean(ReachableService.class);
    HistoryService h = ApplicationContextProvider.getContext().getBean(HistoryService.class);

    String sol = "";
    String lines[] = line.split("\n");
    int len = lines.length - 1;
    if(len <= 0) return "";
    ArrayList<ShortURL> su = new ArrayList<>();
    String port = environment.getProperty("local.server.port");
    int tamSu = 0;
    for(int i = 0; i < len; ++i) {
      String res = CSVGenerator.readLine(lines[i]);
      if(!res.contains(",,")) {
        su.add(sus.save(res, "", lines[len], false));
        res += ",http://localhost:" + port + "/" + su.get(tamSu).getHash() + ",";
        ++tamSu;
      } 
      sol += res + "\n";
    }

    if(tamSu == 0) return "INVALID";

    executor.submit(() -> {

      for(ShortURL s : su) {
        rs.sender(s);
      }

    });

    return sol;
  }

}
