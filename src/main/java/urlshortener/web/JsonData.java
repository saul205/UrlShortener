package urlshortener.web;

import java.util.List;

public class JsonData{

    Long clicks;
    Long urls;

    List top;
    List historial;

    JsonData(Long clicks, Long urls, List top, List historial){
        this.clicks = clicks;
        this.urls = urls;
        this.top = top;
        this.historial = historial;
    }

    public Long getClicks(){
        return clicks;
    }

    public Long getUrls(){
        return clicks;
    }

    public List getTop(){
        return top;
    }

    public List getHistorial(){
        return historial;
    }
    
}