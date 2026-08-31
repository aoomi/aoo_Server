package com.aoo.bcg.common.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnvironmentDifferenceReportTest {
    @Test void reportsAllowedAndUnexpectedEnvironmentDifferences() {
        var baseline = Map.of(RuntimeConfigKey.ENVIRONMENT, "staging", RuntimeConfigKey.ADMIN_DB_URL, "jdbc:mysql://stage/app",
            RuntimeConfigKey.ADMIN_API_TOKEN, "secret://vault/stage/admin#v1", RuntimeConfigKey.MQ_PRODUCER_GROUP, "stage-group");
        var production = Map.of(RuntimeConfigKey.ENVIRONMENT, "production", RuntimeConfigKey.ADMIN_DB_URL, "jdbc:mysql://prod/app",
            RuntimeConfigKey.ADMIN_API_TOKEN, "secret://vault/prod/admin#v7", RuntimeConfigKey.MQ_PRODUCER_GROUP, "prod-group");
        var report = EnvironmentDifferenceReport.compare(DeploymentEnvironment.STAGING, baseline,
            DeploymentEnvironment.PRODUCTION, production);
        assertFalse(report.accepted());
        assertTrue(report.differences().stream().anyMatch(value -> value.key() == RuntimeConfigKey.MQ_PRODUCER_GROUP && !value.allowed()));
        assertFalse(report.toString().contains("vault/prod"));
        assertThrows(IllegalStateException.class, report::requireAccepted);
    }
}
