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
        su.add(sus.save(res, "", lines[len]));
        res += ",http://localhost:8080/" + su.get(i).getHash();
      } else {
        ++lenSu;
      }
      sol += res + "\n";
    }

    final int flen = len - lenSu;
    executor.submit(() -> {
      CountDownLatch latch = new CountDownLatch(flen);
      ArrayList<ShortURL> check = new ArrayList<ShortURL>();
      for(ShortURL s : su) {
        executor.submit(() -> {
          if(rs.isReachable(s.getHash())) check.add(s);
          latch.countDown();
        });
      }

      try {
        latch.await();
      } catch(InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      if(check.size() > 0) sus.checkSafe(check);
    });

    return sol;
  }

}
