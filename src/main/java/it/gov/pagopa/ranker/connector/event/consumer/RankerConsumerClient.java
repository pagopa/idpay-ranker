package it.gov.pagopa.ranker.connector.event.consumer;

import com.azure.messaging.servicebus.*;
import com.azure.messaging.servicebus.models.DeferOptions;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import it.gov.pagopa.ranker.connector.event.producer.CommandsProducer;
import it.gov.pagopa.ranker.domain.dto.QueueCommandOperationDTO;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.exception.UnableToAddSeqException;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static it.gov.pagopa.ranker.constants.CommonConstants.DELETE_SEQUENCE_NUMBER;
import static it.gov.pagopa.ranker.constants.CommonConstants.SEQUENCE_NUMBER_PROPERTY;

@Slf4j
@Service
public class RankerConsumerClient {
    private final RankerService rankerService;
    private final InitiativeCountersService initiativeCountersService;
    private final CommandsProducer commandsProducer;

    private final String connectionString;
    private final String queueName;

    private ServiceBusProcessorClient processorClient;


    public RankerConsumerClient(RankerService rankerService,
                                InitiativeCountersService initiativeCountersService, CommandsProducer commandsProducer,
                                @Value("${azure.servicebus.onboarding-request.connection-string}") String connectionString,
                                @Value("${azure.servicebus.onboarding-request.queue-name}") String queueName) {
        this.rankerService = rankerService;
        this.initiativeCountersService = initiativeCountersService;
        this.commandsProducer = commandsProducer;
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
                .disableAutoComplete()
                .processError(context -> log.error("[RANKER_CONTEXT] Error in processor: {}", context.getException().getMessage()))
                .buildProcessorClient();

        checkResidualBudgetAndStartConsumer();
    }

    private void handleMessage(ServiceBusReceivedMessageContext context) {
        try {
            rankerService.execute(context.getMessage());
            context.complete();
        } catch (BudgetExhaustedException e) {
            log.error("[BUDGET_CONTEXT] Budget exhausted.");
            try {
                context.defer();
                rankerService.addSequenceIdToInitiative(context.getMessage());
            } catch (UnableToAddSeqException addSeqException) {
                log.error("[RANKER_CONTEXT] Unable to add deferred message with sequence {}",
                        context.getMessage().getSequenceNumber());
            } catch (Exception deferError) {
                log.error("[RANKER_CONTEXT] Unable to defer");
            } finally {
                stopConsumer();
            }
        } catch (Exception e) {
            log.error("[RANKER_CONTEXT] Error processing message", e);
            context.abandon();
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
            Set<Pair<String,Long>> sequenceIdToProcess = initiativeCountersService.getMessageToProcess();
            if (!sequenceIdToProcess.isEmpty()) {
                try {
                    processDeferredMessage(sequenceIdToProcess);
                } catch (Exception e) {
                    log.error("[BUDGET_CONTEXT_START] Unable to process sequences numbers, stopping", e);
                    return;
                }
            }
            startConsumer();
            log.info("[BUDGET_CONTEXT_START] Consumer started");
        } else {
            log.info("[BUDGET_CONTEXT_START] Consumer running {}, initiative has budget {}", processorClient.isRunning(), hasAvailableBudget);
        }
    }


    public void processDeferredMessage(Set<Pair<String,Long>> sequenceIdToProcess) {
        try (ServiceBusReceiverClient deferReceiverClient = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .receiver()
                .queueName(queueName)
                .disableAutoComplete()
                .buildClient()) {
            log.info("[BUDGET_CONTEXT_START] Received started to process deferred messages");
            for (Pair<String,Long> sequenceToProcess : sequenceIdToProcess) {
                ServiceBusReceivedMessage message =
                        deferReceiverClient.receiveDeferredMessage(sequenceToProcess.getRight());
                rankerService.execute(message);
                try {
                    initiativeCountersService.removeMessageToProcessOnInitative(
                            sequenceToProcess.getLeft(), sequenceToProcess.getRight());
                } catch (Exception e) {
                    log.error("[BUDGET_CONTEXT_START] Error encountered during sequence number removal", e);
                    QueueCommandOperationDTO deleteSeqNumberCommand = QueueCommandOperationDTO.builder()
                            .entityId(sequenceToProcess.getLeft())
                            .operationType(DELETE_SEQUENCE_NUMBER)
                            .operationTime(LocalDateTime.now())
                            .properties(Map.of(SEQUENCE_NUMBER_PROPERTY, String.valueOf(sequenceToProcess.getRight())))
                            .build();
                    if(!commandsProducer.sendCommand(deleteSeqNumberCommand)){
                        log.error("[BUDGET_CONTEXT_START] - Initiative: {}. Something went wrong while " +
                                "sending the message on Commands Queue", sequenceToProcess.getLeft());
                    }
                }
            }
        }
        log.info("[BUDGET_CONTEXT_START] Defer receiver stopped due to completion");
    }


}
