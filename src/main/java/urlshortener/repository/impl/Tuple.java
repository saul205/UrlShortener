package urlshortener.repository.impl;

public class Tuple{
    String key;
    Long count;

    public Tuple(String k, Long c){
        key = k;
        count = c;
    }

    public String getKey(){
        return key;
    }

    public Long getValue(){
        return count;
    }
}