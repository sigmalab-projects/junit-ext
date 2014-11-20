package de.sigmalab.junit.rules.hsqldb;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import org.hsqldb.persist.HsqlProperties;

import org.hsqldb.server.Server;
import org.hsqldb.server.ServerAcl.AclFormatException;

import org.junit.rules.TestRule;

import org.junit.runner.Description;

import org.junit.runners.model.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Make HSQLDB usage in test simpler.
 *
 * @author  jbellmann
 */
public class HsqldbRule implements TestRule {

    private final Logger logger = LoggerFactory.getLogger(HsqldbRule.class);

    private final Server server;

    // visible for testing
    protected HsqldbRule(final Server server) {
        if (server == null) {
            throw new IllegalArgumentException("Hsqldb-Server should never be null");
        }

        this.server = server;
    }

    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                logger.debug("START DB");
                server.start();
                logger.debug("DB STARTED");
                try {
                    base.evaluate();
                } finally {
                    logger.debug("SHUTTING DOWN DB");
                    server.stop();
                    logger.debug("DB DOWN");
                }

            }
        };
    }

    public static HsqldbRuleBuilder newBuilder() {
        return new HsqldbRuleBuilder();
    }

    public static class HsqldbRuleBuilder {

        private int port = 9001;
        private Map<String, String> properties = new HashMap<String, String>();

        public HsqldbRuleBuilder onPort(final int port) {
            if (port < 0 || port > 65000) {
                throw new IllegalArgumentException("Invalid port " + port);
            }

            this.port = port;
            return this;
        }

        public HsqldbRuleBuilder withProperties(final Map<String, String> properties) {
            if (properties == null) {
                throw new IllegalArgumentException("Properties should never be null or empty");
            }

            this.properties = properties;
            return this;
        }

        public HsqldbRule build() {
            HsqlProperties p = new HsqlProperties();
            if (properties.isEmpty()) {
                properties = getDefaultProperties();
            }

            for (Map.Entry<String, String> entry : properties.entrySet()) {
                p.setProperty(entry.getKey(), entry.getValue());
            }

            Server server = new Server();
            try {
                server.setProperties(p);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            } catch (AclFormatException e) {
                throw new RuntimeException(e.getMessage(), e);
            }

            server.setPort(port);
            server.setLogWriter(null); // can use custom writer
            server.setErrWriter(null); // can use custom writer

            return new HsqldbRule(server);
        }

        protected Map<String, String> getDefaultProperties() {
            Map<String, String> props = new HashMap<String, String>();
            props.put("server.database.0", getUserDirPath());
            props.put("server.dbname.0", "xdb");
            return props;
        }

        private String getUserDirPath() {
            return "file:" + System.getProperty("user.dir");
        }
    }
}
