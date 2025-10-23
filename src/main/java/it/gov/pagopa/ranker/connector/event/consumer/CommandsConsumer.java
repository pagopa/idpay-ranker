package it.gov.pagopa.ranker.connector.event.consumer;

import it.gov.pagopa.ranker.domain.dto.QueueCommandOperationDTO;
import it.gov.pagopa.ranker.service.command.ProcessConsumerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;


@Configuration
public class CommandsConsumer {
    @Bean
    public Consumer<QueueCommandOperationDTO> consumerCommands(ProcessConsumerService processConsumerService) {
        return processConsumerService::processCommand;
    }
}
