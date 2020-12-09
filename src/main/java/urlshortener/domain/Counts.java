package urlshortener.domain;

import java.sql.Timestamp;

public class Counts {

  private final String hash;
  private final String target;
  private final Long count;

  public Counts(String hash, String target, Long count) {
    this.hash = hash;
    this.target = target;
    this.count = count;
  }

  public String getHash() {
    return hash;
  }

  public String getTarget() {
    return target;
  }

  public Long getCount() {
    return count;
  }
}