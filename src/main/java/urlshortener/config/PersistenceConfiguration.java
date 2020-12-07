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
import urlshortener.service.ReachableService;

import org.springframework.http.converter.BufferedImageHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import java.awt.image.BufferedImage;

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

  /*@Bean
  ReachableService reachableSVC() {
    return new ReachableService(null);
  }*/

  @Bean
	public HttpMessageConverter<BufferedImage> createImageHttpMessageConverter() {
	    return new BufferedImageHttpMessageConverter();
	}

}
