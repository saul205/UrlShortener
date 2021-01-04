package urlshortener.web;

import java.util.List;

public class JsonSearch{

    Long count = 0L;
    String hash = null;
    String target = null;

    JsonSearch(Long count, String target){
        this.count = count;
        this.target = target;
    }

    JsonSearch(Long count, String target, String hash){
        this.count = count;
        this.target = target;
        this.hash = hash;
    }

    public Long getCount(){
        return count;
    }

    public String getHash(){
        return hash;
    }

    public String getTarget(){
        return target;
    }
    
}