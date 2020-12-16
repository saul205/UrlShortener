package urlshortener.domain;

import java.sql.Timestamp;

public class HistoryElement {

  private final Long id;
  private final String hash;
  private final String target;
  private final Timestamp created;

  public HistoryElement(Long id, String hash, String target, Timestamp created) {
    this.id = id;
    this.hash = hash;
    this.target = target;
    this.created = created;
  }

  public Long getId() {
    return id;
  }

  public String getHash() {
    return hash;
  }

  public String getTarget() {
    return target;
  }

  public Timestamp getCreated() {
    return created;
  }
}