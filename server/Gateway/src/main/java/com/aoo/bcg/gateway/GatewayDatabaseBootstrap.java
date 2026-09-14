package com.aoo.bcg.gateway;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Fail-fast database readiness probe kept outside Netty handler code. */
final class GatewayDatabaseBootstrap {
    private GatewayDatabaseBootstrap() {}

    static void verify(DataSource source) {
        try (Connection connection = source.getConnection();
             PreparedStatement query = connection.prepareStatement("SELECT 1 FROM gateway_ws_ticket LIMIT 1")) {
            query.executeQuery();
        } catch (SQLException failure) {
            throw new IllegalStateException("gateway database readiness failed", failure);
        }
    }
}
