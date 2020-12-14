package urlshortener.repository.impl;

import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import urlshortener.domain.ShortURL;
import urlshortener.repository.ShortURLRepository;

import java.net.URI;

@Repository
public class ShortURLRepositoryImpl implements ShortURLRepository{

  private static final Logger log = LoggerFactory
      .getLogger(ShortURLRepositoryImpl.class);

  private static final RowMapper<ShortURL> rowMapper =
      (rs, rowNum) -> new ShortURL(rs.getString("hash"), rs.getString("target"),
          rs.getString("uri"), rs.getString("sponsor"), rs.getTimestamp("created"),
          rs.getString("owner"), rs.getInt("mode"), rs.getInt("safe"), 
          rs.getString("qr"), rs.getString("ip"),
          rs.getString("country"), rs.getInt("alcanzable"));

  private final JdbcTemplate jdbc;

  public ShortURLRepositoryImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ShortURL findByKey(String id) {
    try {
      return jdbc.queryForObject("SELECT * FROM shorturl WHERE hash=?",
          rowMapper, id);
    } catch (Exception e) {
      log.debug("When select for key {}", id, e);
      return null;
    }
  }

  @Override
  public ShortURL save(ShortURL su) {
    try {
      String uri;
      if(su.getUri() == null){
        uri = null;
      }else {
        uri = su.getUri().toString();
      }
      jdbc.update("INSERT INTO shorturl VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
          su.getHash(), su.getTarget(), su.getSponsor(),
          su.getCreated(), su.getOwner(), su.getMode(), su.getSafe(), su.getQR(),
          su.getAlcanzable(), su.getIP(), su.getCountry(), uri);
    } catch (DuplicateKeyException e) {
      log.debug("When insert for key {}", su.getHash(), e);
      try{
        jdbc.update("UPDATE shorturl SET created=? WHERE hash=?", new Object[]{su.getCreated(), su.getHash()});
      }catch(Exception e2){
        log.debug("When updating on DuplicateKey");
      }
      return su;
    } catch (Exception e) {
      log.info("When insert", e);
      return null;
    }

    log.debug("When insert 2", su.getTarget());
    return su;
  }

  @Override
  public ShortURL mark(ShortURL su, Integer safeness) {
    try {
      jdbc.update("UPDATE shorturl SET safe=? WHERE hash=?", safeness,
          su.getHash());
      return new ShortURL(
        su.getHash(), su.getTarget(), su.getUri(), su.getSponsor(),
        su.getCreated(), su.getOwner(), su.getMode(), safeness, su.getQR(),
        su.getIP(), su.getCountry()
      );
    } catch (Exception e) {
      log.debug("When update", e);
      return null;
    }
  }

  @Override
  public void update(ShortURL su) {
    String uri;
    if(su.getUri() == null) uri = null;
    else uri = su.getUri().toString();
    try {
      jdbc.update(
          "update shorturl set target=?, sponsor=?, created=?, owner=?, "+
          "mode=?, safe=?, qr=?, ip=?, country=?, uri=?, alcanzable=?  where hash=?",
          su.getTarget(), su.getSponsor(), su.getCreated(),
          su.getOwner(), su.getMode(), su.getSafe(), su.getQR(), su.getIP(),
          su.getCountry(), uri, su.getAlcanzable(), su.getHash());
    } catch (Exception e) {
      log.debug("When update for hash {}", su.getHash(), e);
    }
  }

  @Override
  public void delete(String hash) {
    try {
      jdbc.update("delete from shorturl where hash=?", hash);
    } catch (Exception e) {
      log.debug("When delete for hash {}", hash, e);
    }
  }

  @Override
  public Long count() {
    try {
      return jdbc.queryForObject("select count(*) from shorturl",
          Long.class);
    } catch (Exception e) {
      log.debug("When counting", e);
    }
    return -1L;
  }

  @Override
  public List<ShortURL> list(Long limit, Long offset) {
    try {
      if(limit == -1L){
        return jdbc.query("SELECT * FROM shorturl OFFSET ?",
          new Object[] {offset}, rowMapper);
      }
      return jdbc.query("SELECT * FROM shorturl LIMIT ? OFFSET ?",
          new Object[] {limit, offset}, rowMapper);
    } catch (Exception e) {
      log.debug("When select for limit {} and offset {}", limit, offset, e);
      return Collections.emptyList();
    }
  }

  @Override
  public List<ShortURL> findByTarget(String target) {
    try {
      return jdbc.query("SELECT * FROM shorturl WHERE target = ?",
          new Object[] {target}, rowMapper);
    } catch (Exception e) {
      log.debug("When select for target " + target, e);
      return Collections.emptyList();
    }
  }

  @Override
  public List<ShortURL> lastN(Integer n){
    try {
      return jdbc.query("SELECT * FROM shorturl ORDER BY created DESC LIMIT " + n, rowMapper);
    }catch(Exception e){
      log.debug("When selecting last " + n + " shorted urls");
      return Collections.emptyList();
    }
  }
}
