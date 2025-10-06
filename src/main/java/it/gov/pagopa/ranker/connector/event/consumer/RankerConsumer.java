package it.gov.pagopa.ranker.connector.event.consumer;

import com.mongodb.DuplicateKeyException;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.function.Consumer;

@Slf4j
@Configuration
public class RankerConsumer{

  public static final String RANKER_PROCESSOR_BINDING_NAME = "rankerProcessor-in-0";

  private final RankerService rankerService;
  private final InitiativeCountersService initiativeCountersService;
  private Binding<?> rankerConsumerBinding;
  private final BindingsLifecycleController bindingsLifecycleController;

  public RankerConsumer(RankerService rankerService, InitiativeCountersService initiativeCountersService, BindingsLifecycleController bindingsLifecycleController) {
    this.rankerService = rankerService;
    this.initiativeCountersService = initiativeCountersService;
    this.bindingsLifecycleController = bindingsLifecycleController;
  }

  @Bean
  public Consumer<Message<OnboardingDTO>> rankerProcessor() {
      return this::processesMessage;
  }

  public void processesMessage(Message<OnboardingDTO> dto){
    try {
      rankerService.execute(dto);
    } catch (DuplicateKeyException e) {
      log.error("[BUDGET_CONTEXT] Budget exhausted.");
      stopConsumer();
      log.info("[BUDGET_CONTEXT_STOP] Consumer stopped");
    }
  }

//  @Override
//  public void onApplicationEvent(ApplicationReadyEvent event) {
//    log.info("[BUDGET_CONTEXT_START] Application ready, performing initial budget check.");
//    checkResidualBudgetAndStartConsumer();
//  }

  @Scheduled(cron = "${app.initiative.schedule-check-budget}")
  public void checkResidualBudgetAndStartConsumer() {
    log.info("[BUDGET_CONTEXT_START] Starting initiative budget check...");
    boolean hasAvailableBudget = initiativeCountersService.hasAvailableBudget();
    if (hasAvailableBudget){
      startConsumer();
      log.info("[BUDGET_CONTEXT_START] Consumer started");
    }
  }

  private synchronized void startConsumer() {
    if (rankerConsumerBinding != null) {
      makeServiceBusBindingRestartable(rankerConsumerBinding);
      bindingsLifecycleController.changeState(
              RANKER_PROCESSOR_BINDING_NAME,
              BindingsLifecycleController.State.STARTED
      );
      log.info("Consumer [{}] started", RANKER_PROCESSOR_BINDING_NAME);
    } else {
      log.warn("Cannot start consumer: binding not yet created");
    }
  }

  private synchronized void stopConsumer() {
    if (rankerConsumerBinding != null) {
      bindingsLifecycleController.changeState(
              RANKER_PROCESSOR_BINDING_NAME,
              BindingsLifecycleController.State.STOPPED
      );
      log.info("Consumer [{}] stopped", RANKER_PROCESSOR_BINDING_NAME);
    } else {
      log.warn("Cannot stop consumer: binding not yet created");
    }
  }

  @SuppressWarnings("squid:S3011")
  private static void makeServiceBusBindingRestartable(Binding<?> binding) {
    try {
      Field restartableField = ReflectionUtils.findField(binding.getClass(), "restartable");
      if (restartableField != null) {
        restartableField.setAccessible(true);
        restartableField.setBoolean(binding, true);
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Cannot make servicebus binding restartable", e);
    }
  }

//  @Override
//  public boolean supportsAsyncExecution() {
//    return ApplicationListener.super.supportsAsyncExecution();
//  }
}
