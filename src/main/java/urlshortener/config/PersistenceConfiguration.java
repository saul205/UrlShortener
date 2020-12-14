package urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import urlshortener.repository.ClickRepository;
import urlshortener.repository.ShortURLRepository;
import urlshortener.repository.HistoryRepository;
import urlshortener.repository.impl.ClickRepositoryImpl;
import urlshortener.repository.impl.ShortURLRepositoryImpl;
import urlshortener.repository.impl.HistoryRepositoryImpl;
import urlshortener.service.ClickService;
import urlshortener.service.CountsService;
import urlshortener.service.MostVisitedService;
import urlshortener.service.ReachableService;
import urlshortener.service.ShortURLService;
import urlshortener.repository.CountsRepository;
import urlshortener.repository.impl.CountsRepositoryImpl;
import urlshortener.repository.MostVisitedRepository;
import urlshortener.repository.impl.MostVisitedRepositoryImpl;

import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import java.awt.image.BufferedImage;
import java.beans.BeanProperty;

@Configuration
public class PersistenceConfiguration {

  private final JdbcTemplate jdbc;

  public PersistenceConfiguration(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Bean
  ShortURLRepository shortURLRepository() {
    return new ShortURLRepositoryImpl(jdbc);
  }

  @Bean
  ClickRepository clickRepository() {
    return new ClickRepositoryImpl(jdbc);
  }

  @Bean
  HistoryRepository historyRepository() {
    return new HistoryRepositoryImpl(jdbc);
  }

  @Bean
  CountsRepository countsRepository(){
    return new CountsRepositoryImpl(jdbc);
  }

  @Bean
  MostVisitedRepository mostVisitedRepository(){
    return new MostVisitedRepositoryImpl(jdbc);
  }
  /*@Bean
  ReachableService reachableSVC() {
    return new ReachableService(null);
  }*/

  @Bean
	public HttpMessageConverter<BufferedImage> createImageHttpMessageConverter() {
	    return new BufferedImageHttpMessageConverter();
	}

}
