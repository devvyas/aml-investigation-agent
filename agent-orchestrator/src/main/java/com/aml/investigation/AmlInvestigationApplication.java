package com.aml.investigation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Deliberately placed at com.aml.investigation, not com.aml.investigation.orchestrator —
 * Spring Boot's default component/entity/repository scanning covers only this package and its
 * sub-packages. Sitting here is what lets it find beans in .kyc, .sanctions, .network, .store,
 * and .orchestrator all at once, without an explicit @ComponentScan(basePackages = ...).
 */
@SpringBootApplication
public class AmlInvestigationApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmlInvestigationApplication.class, args);
    }
}
