package urlshortener.repository;


import urlshortener.domain.Counts;
import java.util.List;

public interface MostVisitedRepository{

    public void save(final List<Counts> c);
    public void update(Counts c);
    public void delete(String hash);
    public List<Counts> find();

}