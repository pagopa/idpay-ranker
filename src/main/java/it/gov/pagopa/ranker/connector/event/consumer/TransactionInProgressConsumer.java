package it.gov.pagopa.ranker.connector.event.consumer;

import it.gov.pagopa.ranker.service.transactionInProgress.TransactionInProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class TransactionInProgressConsumer {
  @Bean
  public Consumer<Message<String>> trxProcessor(TransactionInProgressService transactionInProgressService){
    return transactionInProgressService::execute;
  }
}