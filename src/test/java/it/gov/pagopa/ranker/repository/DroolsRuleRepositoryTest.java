package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.common.mongo.MongoTest;
import it.gov.pagopa.ranker.domain.model.DroolsRule;
import it.gov.pagopa.ranker.domain.model.InitiativeConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Disabled
@MongoTest
class DroolsRuleRepositoryTest {
    @Autowired
    private DroolsRuleRepository droolsRuleRepository;

    @Test
    void findByIdIn() {
        DroolsRule droolsRule = creteDroolRule(1);
        DroolsRule droolsRule1 = creteDroolRule(2);

        droolsRuleRepository.saveAll(List.of(droolsRule, droolsRule1));

        List<DroolsRule> result = droolsRuleRepository.findByIdIn(List.of("INITIATIVEID_1", "ANOTHER_INITIATIVE"));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(droolsRule, result.getFirst());

        droolsRuleRepository.deleteAllById(List.of(droolsRule.getId(), droolsRule1.getId()));
    }

    private DroolsRule creteDroolRule(Integer bias){
        DroolsRule droolsRule = new DroolsRule();
        droolsRule.setId("INITIATIVEID_%d".formatted(bias));

        InitiativeConfig config = new InitiativeConfig();
        config.setInitiativeId("INITIATIVEID_%d".formatted(bias));
        droolsRule.setInitiativeConfig(config);

        return droolsRule;
    }
}