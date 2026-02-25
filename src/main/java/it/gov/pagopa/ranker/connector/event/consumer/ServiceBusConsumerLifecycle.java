  package it.gov.pagopa.ranker.connector.event.consumer;

  import lombok.extern.slf4j.Slf4j;
  import org.springframework.context.SmartLifecycle;
  import org.springframework.stereotype.Component;

  @Slf4j
  @Component
  public class ServiceBusConsumerLifecycle implements SmartLifecycle {

    private final RankerConsumerClient rankerConsumerClient;
    private volatile boolean running = false;

    public ServiceBusConsumerLifecycle(RankerConsumerClient rankerConsumerClient) {
      this.rankerConsumerClient = rankerConsumerClient;
    }

    @Override
    public void start() {
      rankerConsumerClient.checkResidualBudgetAndStartConsumer();
      running = true;
      log.info("[RANKER_CONTEXT] ServiceBusConsumerLifecycle started");
    }

    @Override
    public void stop() {
      rankerConsumerClient.stopConsumer();
      running = false;
      log.info("[RANKER_CONTEXT] ServiceBusConsumerLifecycle stopped");
    }

    @Override
    public boolean isRunning() {
      return running;
    }

    @Override
    public int getPhase() {
      return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
      return true;
    }
  }