package urlshortener.service;

import static com.google.common.hash.Hashing.murmur3_32;


import java.net.URI;
import java.nio.charset.StandardCharsets;
import urlshortener.domain.Counts;

public class CountsBuilder {

  private String hash;
  private String target;
  private Long count;

  static CountsBuilder newInstance() {
    return new CountsBuilder();
  }

  Counts build() {
    return new Counts(hash, target, count);
  }

  CountsBuilder hash(String hash){
    this.hash = hash;
    return this;
  }

  CountsBuilder target(String target) {
    if(hash == null){
        hash = murmur3_32().hashString(target, StandardCharsets.UTF_8).toString();
    }
    this.target = target;
    return this;
  }

  CountsBuilder count(Long count) {
    this.count = count;
    return this;
  }

}
