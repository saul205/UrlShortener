package urlshortener.service;

import java.sql.Date;
import java.sql.Timestamp;

import urlshortener.domain.HistoryElement;

public class HistoryElementBuilder {

  private String hash;
  private String target;
  private Timestamp created;

  static HistoryElementBuilder newInstance() {
    return new HistoryElementBuilder();
  }

  HistoryElement build() {
    return new HistoryElement(null, hash, target, created);
  }

  HistoryElementBuilder hash(String hash) {
    this.hash = hash;
    return this;
  }

  HistoryElementBuilder created(Timestamp created) {
    this.created = created;
    return this;
  }

  HistoryElementBuilder target(String target){
      this.target = target;
      return this;
  }

}