package urlshortener.domain;

import java.sql.Timestamp;

public class HistoryElement {

  private final Long id;
  private final String hash;
  private final String target;
  private final Timestamp created;
  private final String ip;

  public HistoryElement(Long id, String hash, String target, Timestamp created, String ip) {
    this.id = id;
    this.hash = hash;
    this.target = target;
    this.created = created;
    this.ip = ip;
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

  public String getIp() {
    return ip;
  }
}