package com.gpms.backend.config;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Translates a platform-style DATABASE_URL into the JDBC properties
 * Spring actually needs.
 *
 * Render, Heroku, Railway and friends all publish the database as
 *
 *     postgresql://user:password@host:5432/dbname
 *
 * which is NOT a JDBC URL. Handing it to Spring straight produces a
 * driver error, and leaving it unset is worse: the application falls
 * back to the localhost default baked into application.yml and dies
 * with "Connection to localhost:5432 refused" - there is no database
 * inside a web service's container.
 *
 * Splitting it here means a Render Blueprint can bind the managed
 * database directly and nothing has to be retyped by hand.
 *
 * An explicit SPRING_DATASOURCE_URL always wins, so local development
 * and docker-compose are unaffected.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "platformDatabaseUrl";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application
    ) {

        /*
         * Read the raw variables rather than resolved properties:
         * application.yml gives spring.datasource.url a default, so a
         * resolved lookup can never tell "explicitly set" apart from
         * "fell back to localhost".
         */
        String explicitJdbcUrl = System.getenv("SPRING_DATASOURCE_URL");

        if (explicitJdbcUrl != null && !explicitJdbcUrl.isBlank()) {
            return;
        }

        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        databaseUrl = databaseUrl.trim();

        /* Already a JDBC URL - nothing to translate. */
        if (databaseUrl.startsWith("jdbc:")) {
            addProperties(environment, databaseUrl, null, null);
            return;
        }

        try {

            URI uri = URI.create(databaseUrl);

            String host = uri.getHost();

            if (host == null) {
                return;
            }

            /*
             * Render's INTERNAL connection string carries no port.
             * Postgres' default is the right answer there.
             */
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();

            String database = uri.getPath() == null ? "" : uri.getPath();

            String query = uri.getQuery() == null ? "" : "?" + uri.getQuery();

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port
                    + database + query;

            String username = null;
            String password = null;

            String userInfo = uri.getUserInfo();

            if (userInfo != null && !userInfo.isBlank()) {

                int separator = userInfo.indexOf(':');

                if (separator >= 0) {
                    username = userInfo.substring(0, separator);
                    password = userInfo.substring(separator + 1);
                } else {
                    username = userInfo;
                }
            }

            addProperties(environment, jdbcUrl, username, password);

        } catch (IllegalArgumentException exception) {
            /*
             * A malformed DATABASE_URL is left alone deliberately, so
             * the failure surfaces as a clear datasource error rather
             * than as a confusing one from in here.
             */
        }
    }

    private void addProperties(
            ConfigurableEnvironment environment,
            String jdbcUrl,
            String username,
            String password
    ) {

        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", jdbcUrl);

        if (username != null) {
            properties.put("spring.datasource.username", username);
        }

        if (password != null) {
            properties.put("spring.datasource.password", password);
        }

        /*
         * addFirst so these beat the defaults in application.yml.
         */
        environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME, properties)
        );
    }
}
