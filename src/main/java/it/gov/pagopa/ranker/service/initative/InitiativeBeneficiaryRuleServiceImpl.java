package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.model.DroolsRule;
import it.gov.pagopa.ranker.domain.model.InitiativeConfig;
import it.gov.pagopa.ranker.repository.DroolsRuleRepository;
import it.gov.pagopa.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InitiativeBeneficiaryRuleServiceImpl implements InitiativeBeneficiaryRuleService{
    private final DroolsRuleRepository droolsRuleRepository;
    private final List<String> initiativesEnable;
    private final Map<String, InitiativeConfig> initiativesConfigCached = new ConcurrentHashMap<>();

    public InitiativeBeneficiaryRuleServiceImpl(DroolsRuleRepository droolsRuleRepository,
                                                @Value("${app.initiative.identified}") List<String> initiativesEnable) {
        this.droolsRuleRepository = droolsRuleRepository;
        this.initiativesEnable = initiativesEnable;
        initializeInitiativeConfigCache();
    }

    private void initializeInitiativeConfigCache() {
        if (initiativesEnable.isEmpty()) {
            return;
        }

        Map<String, InitiativeConfig> retrieved = droolsRuleRepository.findByIdIn(initiativesEnable)
                .stream()
                .collect(Collectors.toMap(
                        DroolsRule::getId,
                        DroolsRule::getInitiativeConfig
                ));

        initiativesConfigCached.clear();
        initiativesConfigCached.putAll(retrieved);
    }

    @Override
    public InitiativeConfig getInitiativeConfig(String initiativeId) {
        if(!initiativesEnable.contains(initiativeId)){
            log.info("[RETRIEVE_INITIATIVE_CONFIG] Initiative {} is not enable for processing.", CommonUtils.sanitizeString(initiativeId));
            return null;
        }
        InitiativeConfig initiativeConfig = initiativesConfigCached.get(initiativeId);
        if(initiativeConfig == null){
            DroolsRule droolsRule = droolsRuleRepository.findById(initiativeId).orElse(null);
            if(droolsRule != null && droolsRule.getInitiativeConfig() != null){
                initiativesConfigCached.put(droolsRule.getId(),droolsRule.getInitiativeConfig());
                initiativeConfig = droolsRule.getInitiativeConfig();
            }
        }
        return initiativeConfig;
    }
}
