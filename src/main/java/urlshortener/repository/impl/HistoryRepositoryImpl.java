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

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import urlshortener.domain.HistoryElement;
import urlshortener.domain.ShortURL;
import urlshortener.repository.HistoryRepository;

import java.net.URI;

@Repository
public class HistoryRepositoryImpl implements HistoryRepository{

  private static final Logger log = LoggerFactory
      .getLogger(HistoryRepositoryImpl.class);

  private static final RowMapper<HistoryElement> rowMapper =
      (rs, rowNum) -> new HistoryElement(rs.getLong("id"), 
      rs.getString("hash"), rs.getString("target"), rs.getTimestamp("created"));

  private final JdbcTemplate jdbc;

  public HistoryRepositoryImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<HistoryElement> findByHash(String hash, Integer n) {
    try {
      return jdbc.query("SELECT * FROM HISTORIAL WHERE hash = ? limit " + n,
          rowMapper, hash);
    } catch (Exception e) {
      log.debug("When select for key {}", hash, e);
      return null;
    }
  }

  @Override
  public List<HistoryElement> find(Integer n) {
    try {
      return jdbc.query("SELECT * FROM HISTORIAL ORDER BY created DESC limit " + n,
          rowMapper);
    } catch (Exception e) {
      log.debug("When select for key", e);
      return null;
    }
  }

  @Override
  public HistoryElement save(final HistoryElement he) {
    try {
      List<HistoryElement> exists = findByHash(he.getHash(), 1);
      if(exists.size() > 0){
        update(he);
        return he;
      }

      Integer c = count();
      if(c == 10){
        exists = findOlder();
        delete(exists.get(0).getId());
      }

      KeyHolder holder = new GeneratedKeyHolder();
      jdbc.update(conn -> {
        PreparedStatement ps = conn
            .prepareStatement(
                "INSERT INTO HISTORIAL VALUES (?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        ps.setNull(1, Types.BIGINT);
        ps.setString(2, he.getHash());
        ps.setString(3, he.getTarget());
        ps.setTimestamp(4, he.getCreated());
        return ps;
      }, holder);
      if (holder.getKey() != null) {
        new DirectFieldAccessor(he).setPropertyValue("id", holder.getKey()
            .longValue());
      } else {
        log.info("Key from database is null");
      }
    } catch (DuplicateKeyException e) {
      log.info("When insert for historyElement with id " + he.getId(), e);
      return he;
    } catch (Exception e) {
      log.info("When insert a historyElement", e);
      return null;
    }
    return he;
  }

  @Override
  public void save(final List<HistoryElement> l){

    jdbc.update("delete from historial where true");

    for(int i = 0; i < 10 && i < l.size(); i++){
      final HistoryElement h = l.get(i);
      try{
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update((conn) -> {
          PreparedStatement ps = conn
              .prepareStatement(
                  "INSERT INTO HISTORIAL VALUES (?, ?, ?, ?)",
                  Statement.RETURN_GENERATED_KEYS);
          ps.setNull(1, Types.BIGINT);
          ps.setString(2, h.getHash());
          ps.setString(3, h.getTarget());
          ps.setTimestamp(4, h.getCreated());
          return ps;
        }, holder);
        if (holder.getKey() != null) {
          new DirectFieldAccessor(l.get(i)).setPropertyValue("id", holder.getKey()
              .longValue());
        } else {
          log.info("Key from database is null");
        }
      }catch (DuplicateKeyException e) {
        log.info("When insert for historyElement", e);
      }catch(Exception e){
        log.info("When insert for historyElement", e);
      }
    }
  }

  @Override
  public void update(HistoryElement he) {
    try {
      jdbc.update(
          "update historial set created=? where hash=?",
          he.getCreated(), he.getHash());
    } catch (Exception e) {
      log.debug("When update for hash {}", he.getHash(), e);
    }
  }

  @Override
  public void delete(Long id) {
    try {
      jdbc.update("delete from historial where ID=?", id);
    } catch (Exception e) {
      log.debug("When delete for hash {}", id, e);
    }
  }

  @Override
  public Integer count() {
    try {
      return jdbc.queryForObject("select count(*) from historial",
      Integer.class);
    } catch (Exception e) {
      log.debug("When counting", e);
    }
    return -1;
  }

  public List<HistoryElement> findOlder(){
    try {
      return jdbc.query("SELECT * FROM historial order by created ASC Limit 1",
          rowMapper);
    } catch (Exception e) {
      log.debug("When select older", e);
      return null;
    }
  }
}
