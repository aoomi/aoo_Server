package com.aoo.bcg.account;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountHttpRoutesContractTest {
    @Test void exposesOnlyCanonicalV2AccountPrefix() {
        assertEquals("/api/v2/account", AccountHttpRoutes.PREFIX);
        assertFalse(AccountHttpRoutes.PREFIX.contains("/v1/"));
    }
}
