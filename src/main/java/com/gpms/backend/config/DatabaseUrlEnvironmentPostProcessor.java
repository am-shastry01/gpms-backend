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

            /*
             * Nothing configured, so application.yml's localhost
             * default is about to be used. On a hosted platform there
             * is no database in the web container, and the failure
             * that follows - "Connection to localhost:5432 refused" -
             * says nothing about the actual cause.
             *
             * Printed rather than logged because this runs before the
             * logging system is initialised. It still reaches the
             * platform's log stream, and its presence also confirms
             * this processor is in the running build.
             */
            System.out.println(
                    "\n============================================================\n"
                            + " NO DATABASE CONFIGURED\n"
                            + "------------------------------------------------------------\n"
                            + " Neither DATABASE_URL nor SPRING_DATASOURCE_URL is set, so\n"
                            + " the localhost default from application.yml will be used.\n"
                            + "\n"
                            + " That is correct for local development and docker-compose.\n"
                            + " On a hosted platform it will fail, because there is no\n"
                            + " database inside this container.\n"
                            + "\n"
                            + " Fix: add DATABASE_URL to this service's environment,\n"
                            + " set to your managed database's INTERNAL connection string.\n"
                            + "============================================================\n"
            );

            return;
        }

        databaseUrl = databaseUrl.trim();

        /* Already a JDBC URL - nothing to translate. */
        if (databaseUrl.startsWith("jdbc:")) {
            System.out.println(
                    "[datasource] DATABASE_URL is already a JDBC URL; using it as is.");
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

            String query = uri.getQuery() == null ? "" : uri.getQuery();

            /*
             * Managed Postgres reached over the public internet -
             * Neon, Supabase, Aiven - refuses non-TLS connections. A
             * connection string pasted without sslmode fails with an
             * error that says nothing about SSL, so it is added here
             * when it is missing.
             *
             * A host with no dot in it is a platform-internal name
             * (Render's "dpg-abc123-a", a docker-compose service
             * name), which is already on a private network and where
             * requiring TLS would break the connection instead.
             */
            /*
             * Strip parameters that belong to libpq but not to the
             * JDBC driver.
             *
             * Neon hands out connection strings ending
             * "?sslmode=require&channel_binding=require". sslmode is
             * understood by both; channel_binding is libpq's spelling
             * (pgjdbc calls it channelBinding), so the driver forwards
             * it to the server as a configuration parameter and the
             * connection dies with "unrecognized configuration
             * parameter". Dropping it costs nothing - the connection
             * is still encrypted by sslmode.
             *
             * Done here so a string pasted straight from the provider
             * simply works.
             */
            query = stripLibpqOnlyParameters(query);

            boolean isExternalHost = host.contains(".");
            boolean sslAlreadySpecified = query.contains("sslmode");

            if (isExternalHost && !sslAlreadySpecified) {
                query = query.isEmpty() ? "sslmode=require" : query + "&sslmode=require";
            }

            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port
                    + database + (query.isEmpty() ? "" : "?" + query);

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

            /*
             * Host and port only - never the credentials.
             */
            System.out.println(
                    "[datasource] DATABASE_URL translated to jdbc:postgresql://"
                            + host + ":" + port + database);

            addProperties(environment, jdbcUrl, username, password);

        } catch (IllegalArgumentException exception) {
            /*
             * A malformed DATABASE_URL is left alone deliberately, so
             * the failure surfaces as a clear datasource error rather
             * than as a confusing one from in here.
             */
        }
    }

    /**
     * Removes query parameters the PostgreSQL JDBC driver does not
     * accept, keeping everything else in its original order.
     */
    private String stripLibpqOnlyParameters(String query) {

        if (query == null || query.isEmpty()) {
            return "";
        }

        StringBuilder kept = new StringBuilder();

        for (String parameter : query.split("&")) {

            if (parameter.isEmpty()) {
                continue;
            }

            String name = parameter.contains("=")
                    ? parameter.substring(0, parameter.indexOf('='))
                    : parameter;

            if ("channel_binding".equalsIgnoreCase(name)) {
                System.out.println(
                        "[datasource] dropped libpq-only parameter '"
                                + name + "' from DATABASE_URL");
                continue;
            }

            if (kept.length() > 0) {
                kept.append('&');
            }

            kept.append(parameter);
        }

        return kept.toString();
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
