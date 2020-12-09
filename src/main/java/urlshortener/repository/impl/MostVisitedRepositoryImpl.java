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
import urlshortener.repository.MostVisitedRepository;

import java.net.URI;

@Repository
public class MostVisitedRepositoryImpl implements MostVisitedRepository{

  private static final Logger log = LoggerFactory
      .getLogger(MostVisitedRepositoryImpl.class);

  private static final RowMapper<Counts> rowMapper =
      (rs, rowNum) -> new Counts(rs.getString("hash"), rs.getString("target"), rs.getLong("counts"));

  private final JdbcTemplate jdbc;

  public MostVisitedRepositoryImpl(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }
  

  @Override
  public void save(final List<Counts> c) {
    try {
      
      jdbc.update("DELETE from MOSTVISITED where true");

      for(Counts cs : c){
        try{
          jdbc.update("INSERT INTO MOSTVISITED VALUES (?, ?, ?)",
            cs.getHash(), cs.getTarget(), cs.getCount());
        }catch(DuplicateKeyException d){
          try{
            jdbc.update("UPDATE MOSTVISITED SET counts = ? where hash=?", new Object[]{cs.getCount(), cs.getHash()});
          }catch(Exception e2){
            log.debug("When updating on DuplicateKey");
          }
        }
      }
    }catch(Exception e){
      log.debug("Error al insertar: " + e);
    }
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
  public List<Counts> find(){
    try{
      return jdbc.query("SELECT * from MOSTVISITED ORDER BY counts DESC", rowMapper);
    }catch(Exception e){
      return Collections.emptyList();
    }
  }
}