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

import jdk.nashorn.internal.ir.CatchNode;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import urlshortener.domain.Counts;
import urlshortener.repository.CountsRepository;

import java.net.URI;

@Repository
public class CountsRepositoryImpl implements CountsRepository{

  private static final Logger log = LoggerFactory
      .getLogger(CountsRepositoryImpl.class);

  private static final RowMapper<Counts> rowMapper =
      (rs, rowNum) -> new Counts(rs.getString("hash"), rs.getString("target"), rs.getLong("counts"));

  private final JdbcTemplate jdbc;

  public CountsRepositoryImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  

  @Override
  public Counts save(final Counts c) {
    try {
        jdbc.update("INSERT INTO CONTADOR VALUES (?, ?, ?)",
          c.getHash(), c.getTarget(), c.getCount());
    }catch(DuplicateKeyException d){
        try{
            jdbc.update("UPDATE CONTADOR SET counts = ? where hash=?", new Object[]{c.getCount(), c.getHash()});
          }catch(Exception e2){
            log.debug("When updating on DuplicateKey");
          }
    }catch(Exception e){
        return null;
    }

    return c;
  }

  @Override
  public void update(Counts c) {
    try {
      jdbc.update(
          "update CONTADOR set counts=? where hash = ?", c.getCount(), c.getHash());
    } catch (Exception e) {
      log.debug("When update for hash {}", c.getHash(), e);
    }
  }

  @Override
  public void delete(String hash) {
    try {
      jdbc.update("delete from CONTADOR where hash=?", hash);
    } catch (Exception e) {
      log.debug("When delete for hash {}", hash, e);
    }
  }

  @Override
  public Counts findByHash(String hash){
    try{
        return jdbc.queryForObject("SELECT * FROM CONTADOR WHERE HASH=?", rowMapper, hash);
    }catch(Exception e){
        return null;
    }
  }

  @Override
  public List<Counts> findByTarget(String target){
    try{
        return jdbc.query("SELECT * FROM CONTADOR WHERE target=?", rowMapper, target);
    }catch(Exception e){
        return null;
    }
  }

  @Override
  public List<Counts> list(){
    try{
      return jdbc.query("SELECT * FROM CONTADOR where hash != \"shu\" and hash != \"clk\"", rowMapper);
    }catch(Exception e){
        return null;
    }
  }
}