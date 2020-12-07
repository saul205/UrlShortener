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
import urlshortener.repository.HistoryRepository;

import java.net.URI;

@Repository
public class HistoryRepositoryImpl implements HistoryRepository{

  private static final Logger log = LoggerFactory
      .getLogger(HistoryRepositoryImpl.class);

  private static final RowMapper<HistoryElement> rowMapper =
      (rs, rowNum) -> new HistoryElement(rs.getLong("id"), 
      rs.getString("hash"), rs.getString("target"), rs.getTimestamp("created"), rs.getString("ip"));

  private final JdbcTemplate jdbc;

  public HistoryRepositoryImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<HistoryElement> findByHashIp(String hash, String ip, Integer n) {
    try {
      return jdbc.query("SELECT * FROM HISTORIAL WHERE hash = ? and ip = ? limit " + n,
          rowMapper, hash, ip);
    } catch (Exception e) {
      log.debug("When select for key {}", hash, e);
      return null;
    }
  }

  @Override
  public List<HistoryElement> findByIp(String ip, Integer n) {
    try {
      return jdbc.query("SELECT * FROM HISTORIAL WHERE ip = ? ORDER BY created DESC limit " + n,
          rowMapper, ip);
    } catch (Exception e) {
      log.debug("When select for key {}", ip, e);
      return null;
    }
  }

  @Override
  public HistoryElement save(final HistoryElement he) {
    try {
      String ip = he.getIp();
      List<HistoryElement> exists = findByHashIp(he.getHash(), ip, 1);
      if(exists.size() > 0){
        update(he);
        return he;
      }

      Integer c = countByIp(ip);
      if(c == 10){
        exists = findOlderIp(ip);
        delete(exists.get(0).getId());
      }

      KeyHolder holder = new GeneratedKeyHolder();
      jdbc.update(conn -> {
        PreparedStatement ps = conn
            .prepareStatement(
                "INSERT INTO HISTORIAL VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS);
        ps.setNull(1, Types.BIGINT);
        ps.setString(2, he.getHash());
        ps.setString(3, he.getTarget());
        ps.setTimestamp(4, he.getCreated());
        ps.setString(5, he.getIp());
        return ps;
      }, holder);
      if (holder.getKey() != null) {
        new DirectFieldAccessor(he).setPropertyValue("id", holder.getKey()
            .longValue());
      } else {
        log.debug("Key from database is null");
      }
    } catch (DuplicateKeyException e) {
      log.debug("When insert for historyElement with id " + he.getId(), e);
      return he;
    } catch (Exception e) {
      log.debug("When insert a historyElement", e);
      return null;
    }
    return he;
  }

  @Override
  public void update(HistoryElement he) {
    try {
      jdbc.update(
          "update historial set created=? where hash=? and ip=?",
          he.getCreated(), he.getHash(), he.getIp());
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
  public Long count() {
    try {
      return jdbc.queryForObject("select count(*) from historial",
          Long.class);
    } catch (Exception e) {
      log.debug("When counting", e);
    }
    return -1L;
  }

  @Override
  public Integer countByIp(String ip) {
    try {
      return jdbc.queryForObject("select count(*) from historial where ip = ?", new Object[] {ip},
        Integer.class);
    } catch (Exception e) {
      log.debug("When counting", e);
    }
    return -1;
  }

  public List<HistoryElement> findOlderIp(String ip){
    try {
      return jdbc.query("SELECT * FROM historial WHERE ip=? order by created ASC Limit 1",
          rowMapper, ip);
    } catch (Exception e) {
      log.debug("When select for key {}", ip, e);
      return null;
    }
  }
}
