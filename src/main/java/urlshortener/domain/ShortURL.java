package urlshortener.domain;

import java.net.URI;
import java.sql.Timestamp;

public class ShortURL {

  private String hash;
  private String target;
  private URI uri;
  private String sponsor;
  private Timestamp created;
  private String owner;
  private Integer mode;
  private Integer safe;
  private String ip;
  private String country;
  private Integer alcanzable;

  public ShortURL(String hash, String target, URI uri, String sponsor,
                  Timestamp created, String owner, Integer mode, Integer safe, String ip,
                  String country) {
    this.hash = hash;
    this.target = target;
    this.uri = uri;
    this.sponsor = sponsor;
    this.created = created;
    this.owner = owner;
    this.mode = mode;
    this.safe = safe;
    this.ip = ip;
    this.country = country;
    this.alcanzable = 0;
  }

  public ShortURL(String hash, String target, String uri, String sponsor,
                  Timestamp created, String owner, Integer mode, Integer safe, String ip,
                  String country, Integer alcanzable){
    this.hash = hash;
    this.target = target;
    try{
      this.uri = new URI(uri);
    }catch(Exception e){
      this.uri = null;
    }
    this.sponsor = sponsor;
    this.created = created;
    this.owner = owner;
    this.mode = mode;
    this.safe = safe;
    this.ip = ip;
    this.country = country;
    this.alcanzable = alcanzable;
  }

  public ShortURL(String hash, String target, URI uri, String sponsor,
                  Timestamp created, String owner, Integer mode, Integer safe, String ip,
                  String country, Integer alcanzable) {
    this.hash = hash;
    this.target = target;
    this.uri = uri;
    this.sponsor = sponsor;
    this.created = created;
    this.owner = owner;
    this.mode = mode;
    this.safe = safe;
    this.ip = ip;
    this.country = country;
    this.alcanzable = alcanzable;
  }

  public ShortURL() {
  }

  public void setAlcanzable(Integer a){
    this.alcanzable = a;
  }

  public String getHash() {
    return hash;
  }

  public String getTarget() {
    return target;
  }

  public URI getUri() {
    return uri;
  }

  public Timestamp getCreated() {
    return created;
  }

  public String getOwner() {
    return owner;
  }

  public Integer getMode() {
    return mode;
  }

  public String getSponsor() {
    return sponsor;
  }

  public Integer getSafe() {
    return safe;
  }

  public String getIP() {
    return ip;
  }

  public String getCountry() {
    return country;
  }

  public Integer getAlcanzable(){
    return alcanzable;
  }

}
