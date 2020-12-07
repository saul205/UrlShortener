package urlshortener.service;

import org.springframework.stereotype.Service;

import urlshortener.service.ShortURLService;
import urlshortener.service.ReachableService;
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

  public String generateLine(String line) {
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    ShortURLService sus = ApplicationContextProvider.getContext().getBean(ShortURLService.class);
    ReachableService rs = ApplicationContextProvider.getContext().getBean(ReachableService.class);

    String sol = "";
    String lines[] = line.split("\n");
    int len = lines.length - 1;
    int lenSu = 0;
    ArrayList<ShortURL> su = new ArrayList<>();
    for(int i = 0; i < len && i < 500; ++i) {
      String res = CSVGenerator.readLine(lines[i]);
      if(!res.contains(",,")) {
        su.add(sus.save(res, "", lines[len], false));
        res += ",http://localhost:8080/" + su.get(i).getHash();
      } else {
        ++lenSu;
      }
      sol += res + "\n";
    }

    executor.submit(() -> {

      for(ShortURL s : su) {
        rs.sender(s);
      }

    });

    return sol;
  }

}
