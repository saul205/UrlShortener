package urlshortener.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.AggregationStrategy;
import org.springframework.stereotype.Component;

import urlshortener.domain.ShortURL;
import urlshortener.repository.ShortURLRepository;
import urlshortener.service.ShortURLService;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class Route extends RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(Route.class);

    private AggregationStrategy aggregation() {
        return new AggregationStrategy() {
            @Override
            public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
                ShortURL newItem = newExchange.getIn().getBody(ShortURL.class);
                ArrayList<ShortURL> current = null;

                if(oldExchange == null) {
                    current = new ArrayList<ShortURL>();
                    logger.info("Aggregate first item: " + newItem.getTarget());
                } else {
                    current = oldExchange.getIn().getBody(ArrayList.class);
                    logger.info("Aggregate old items: " + current.toString());
                    logger.info("Aggregate new item: " + newItem.getTarget());
                }
                    
                current.add(newItem);
                newExchange.getOut().setBody(current);

                return newExchange;
            }
        };
    }
    
 
    @Override
    public void configure() throws Exception {
        from("direct:checkSafe")
            .aggregate(aggregation())
            .constant(true)
            .completionTimeout(1000)
            .completionSize(500)
            .log("CURRENT: ${body}")
            .bean(ShortURLService.class, "checkSafe");
    }

}