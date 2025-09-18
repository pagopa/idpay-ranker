package it.gov.pagopa.ranker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "it.gov.pagopa")
public class IdPayRankerApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdPayRankerApplication.class, args);
    }
}
