package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.model.DroolsRule;
import it.gov.pagopa.ranker.domain.model.InitiativeConfig;
import it.gov.pagopa.ranker.repository.DroolsRuleRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class InitiativeBeneficiaryRuleServiceTest {

    @Mock private DroolsRuleRepository droolsRuleRepositoryMock;
    private final String initiative1 = "INITIATIVE_1";
    private final String initiative2 = "INITIATIVE_2";
    private final String initiative3 = "INITIATIVE_3";

    private final InitiativeConfig config1 = InitiativeConfig.builder()
            .initiativeId(initiative1).build();
    private final DroolsRule rule1 = DroolsRule.builder()
            .id(initiative1)
            .initiativeConfig(config1).build();

    private final InitiativeConfig config2 = InitiativeConfig.builder()
            .initiativeId(initiative2).build();
    private final DroolsRule rule2 = DroolsRule.builder()
            .id(initiative2)
            .initiativeConfig(config2).build();

    private InitiativeBeneficiaryRuleService initiativeBeneficiaryRuleService;

    @BeforeEach
    void setUp() {
        Mockito.when(droolsRuleRepositoryMock.findByIdIn(List.of(initiative1, initiative2, initiative3)))
                .thenReturn(List.of(rule1, rule2));
        initiativeBeneficiaryRuleService = new InitiativeBeneficiaryRuleServiceImpl(droolsRuleRepositoryMock, List.of(initiative1, initiative2, initiative3));
    }

    @Test
    void getInitiativeConfig_initiativeNotEnable() {
        InitiativeConfig initiativeConfig = initiativeBeneficiaryRuleService.getInitiativeConfig("INITIATIVE_NOT_ENABLE");

        Assertions.assertNull(initiativeConfig);
    }

    @Test
    void getInitiativeConfig_initiativeEnableNotCached() {

        InitiativeConfig config3 = InitiativeConfig.builder()
                .initiativeId(initiative3).build();
        DroolsRule droolsRule3 = DroolsRule.builder().id(initiative3).initiativeConfig(config3).build();
        Mockito.when(droolsRuleRepositoryMock.findById(initiative3)).thenReturn(Optional.of(droolsRule3));

        InitiativeConfig result = initiativeBeneficiaryRuleService.getInitiativeConfig(initiative3);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(config3, result);
    }

    @Test
    void getInitiativeConfig_initiativeEnableNotCachedAndNotPresentInDb() {

        Mockito.when(droolsRuleRepositoryMock.findById(initiative3)).thenReturn(Optional.empty());

        InitiativeConfig result = initiativeBeneficiaryRuleService.getInitiativeConfig(initiative3);

        Assertions.assertNull(result);
    }

    @Test
    void getInitiativeConfig_initiativeEnableNotCachedAndWithoutInitiativeConfig() {

        DroolsRule droolsRule3 = DroolsRule.builder().id(initiative3).build();
        Mockito.when(droolsRuleRepositoryMock.findById(initiative3)).thenReturn(Optional.of(droolsRule3));

        InitiativeConfig result = initiativeBeneficiaryRuleService.getInitiativeConfig(initiative3);

        Assertions.assertNull(result);
    }

    @Test
    void getInitiativeConfig_initiativeEnableCached() {
        InitiativeConfig result = initiativeBeneficiaryRuleService.getInitiativeConfig(initiative1);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(config1, result);
    }

}