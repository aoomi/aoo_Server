package server.aoo.dao.config.jpa;

import org.hibernate.dialect.MySQLDialect;

/** Compatibility name retained for existing configuration; Hibernate 7 handles utf8mb4 via JDBC metadata. */
public class MySQL5DialectUTF8 extends MySQLDialect {
}
