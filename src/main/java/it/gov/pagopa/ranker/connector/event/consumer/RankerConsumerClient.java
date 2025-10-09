package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RankerConsumerClient {
    private final RankerService rankerService;
    private final InitiativeCountersService initiativeCountersService;

    private final String connectionString;
    private final String queueName;

    private ServiceBusProcessorClient processorClient;

    public RankerConsumerClient(RankerService rankerService,
                          InitiativeCountersService initiativeCountersService,
                          @Value("${azure.servicebus.onboarding-request.connection-string}") String connectionString,
                          @Value("${azure.servicebus.onboarding-request.queue-name}") String queueName) {
        this.rankerService = rankerService;
        this.initiativeCountersService = initiativeCountersService;
        this.connectionString = connectionString;
        this.queueName = queueName;
    }

    @PostConstruct
    public void init() {
        processorClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .queueName(queueName)
                .processMessage(this::handleMessage)
                .processError(context -> log.error("[RANKER_CONTEXT] Error in processor: {}", context.getException().getMessage()))
                .buildProcessorClient();

        checkResidualBudgetAndStartConsumer();
    }

    private void handleMessage(ServiceBusReceivedMessageContext context) {
        try {
            rankerService.execute(context.getMessage());
        } catch (BudgetExhaustedException e) {
            log.error("[BUDGET_CONTEXT] Budget exhausted.");
            stopConsumer();
        } catch (Exception e) {
            log.error("[RANKER_CONTEXT] Error processing message", e);
        }
    }

    public void stopConsumer() {
        if (processorClient != null && processorClient.isRunning()) {
            processorClient.close();
            log.info("[RANKER_CONTEXT] Consumer stopped");
        }
    }

    public void startConsumer() {
        if (processorClient != null && !processorClient.isRunning() && initiativeCountersService.hasAvailableBudget()) {
            processorClient.start();
            log.info("[RANKER_CONTEXT] Consumer started");
        }
    }


    @Scheduled(cron = "${app.initiative.schedule-check-budget}")
    public void checkResidualBudgetAndStartConsumer() {
        log.info("[BUDGET_CONTEXT_START] Starting initiative budget check...");
        boolean hasAvailableBudget = initiativeCountersService.hasAvailableBudget();
        if (hasAvailableBudget && !processorClient.isRunning()){
            startConsumer();
            log.info("[BUDGET_CONTEXT_START] Consumer started");
        } else {
            log.info("[BUDGET_CONTEXT_START] Consumer running {}, initiative has budget {}", processorClient.isRunning(), hasAvailableBudget);
        }
    }

}
