package de.sigmalab.junit.rules.h2;

import java.io.File;
import java.io.IOException;

import java.net.ServerSocket;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.h2.store.fs.FileUtils;

import org.h2.tools.Server;

import org.junit.rules.TestRule;

import org.junit.runner.Description;

import org.junit.runners.model.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Make H2 usage in test simpler.
 *
 * @author  jbellmann
 */
public class H2Rule implements TestRule {

    private final Logger logger = LoggerFactory.getLogger(H2Rule.class);

    private final ServerHolder serverHolder;

    protected H2Rule(final ServerHolder serverHolder) {
        this.serverHolder = serverHolder;
    }

    public Statement apply(final Statement base, final Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                logger.debug("START DB");
                serverHolder.beforeStart();
                serverHolder.start();
                logger.debug("DB STARTED");
                try {
                    base.evaluate();
                } finally {
                    logger.debug("SHUTTING DOWN DB");
                    serverHolder.stop();
                    serverHolder.afterStop();
                    logger.debug("DB DOWN");
                }

            }
        };
    }

    public static H2RuleBuilder newBuilder() {
        return new H2RuleBuilder();
    }

    public static class H2RuleBuilder {

        private static final Logger LOG = LoggerFactory.getLogger(H2RuleBuilder.class);

        private String[] arguments = null;

        private int tcpPort = -1;

        private ServerHolder serverHolder = new ServerHolder();

        private String dbDirectoryPath = null;

        public H2RuleBuilder onPort(final int port) {
            this.tcpPort = port;
            return this;
        }

        public H2RuleBuilder withDbDirectoryPath(final String dbDirectoryPath) {
            this.dbDirectoryPath = dbDirectoryPath;
            this.serverHolder.dbDirectoryPath = dbDirectoryPath;
            return this;
        }

        public H2RuleBuilder disableDbDirectoryPathExposing() {
            this.serverHolder.exposeDbDirectoryPath = false;
            return this;
        }

        public H2RuleBuilder disableDbPortExposing() {
            this.serverHolder.exposeDbPort = false;
            return this;
        }

        public H2RuleBuilder useDbDirectoryPathVariableName(final String dbDirectoryPathVariableName) {
            this.serverHolder.dbDirectoryPathVariableName = dbDirectoryPathVariableName;
            return this;
        }

        public H2RuleBuilder useDbPortVariableName(final String dbPortVariableName) {
            this.serverHolder.dbPortVariableName = dbPortVariableName;
            return this;
        }

        public H2RuleBuilder doNotDeleteDbDirectoryOnShutdown() {
            this.serverHolder.deleteDbDirectoryPathOnShutdown = false;
            return this;
        }

        public H2Rule build() {
            try {
                buildArguments();

                this.serverHolder.server = Server.createTcpServer(this.arguments);

                return new H2Rule(this.serverHolder);
            } catch (SQLException e) {
                throw new RuntimeException("Unable to create Server from arguments " + arguments.toString(), e);
            }

        }

        private void buildArguments() {
            String dbDirectoryPath = this.dbDirectoryPath;
            if (dbDirectoryPath == null) {
                dbDirectoryPath = makeRandomH2HomeDirectory();
            }

            if (this.tcpPort < 0) {
                this.tcpPort = findFreePort();
            }

            this.serverHolder.tcpPort = this.tcpPort;
            this.serverHolder.dbDirectoryPath = dbDirectoryPath;

            List<String> args = new ArrayList<String>();

            // baseDir
            args.add("-baseDir");
            args.add(dbDirectoryPath);
            args.add("-tcpAllowOthers");
            args.add("-tcpPort");
            args.add(String.valueOf(this.tcpPort));
            args.add("-tcpDaemon");

            //
            this.arguments = args.toArray(new String[args.size()]);
        }

        protected static String makeRandomH2HomeDirectory() {
            try {
                File tempFile = File.createTempFile("_junit", ".tmp");
                tempFile.deleteOnExit();

                File dbDirectory = new File(tempFile.getParentFile(),
                        "H2_HOME_" + UUID.randomUUID().toString().replace("-", ""));

                boolean wasCreated = dbDirectory.mkdirs();
                if (!wasCreated) {
                    LOG.warn("RandomH2HomeDirectory was not created.");
                }

                LOG.debug("RandomH2HomeDirectory created at {}", dbDirectory.getAbsolutePath());
                return dbDirectory.getAbsolutePath();
            } catch (IOException e) {
                throw new RuntimeException("Not able to create 'RandomH2Home'", e);
            }
        }

        protected static int findFreePort() {
            try {
                ServerSocket server = new ServerSocket(0);
                int port = server.getLocalPort();
                server.close();
                return port;
            } catch (IOException e) {
                throw new RuntimeException("Could not lookup random port", e);
            }
        }
    }

    private static class ServerHolder {

        private final Logger logger = LoggerFactory.getLogger(ServerHolder.class);

        Server server;

        String dbDirectoryPath = null;

        int tcpPort = -1;

        boolean exposeDbDirectoryPath = true;

        boolean exposeDbPort = true;

        String dbDirectoryPathVariableName = "h2.home";

        String dbPortVariableName = "h2.port";

        boolean deleteDbDirectoryPathOnShutdown = true;

        void beforeStart() {
            if (exposeDbDirectoryPath) {
                System.setProperty(dbDirectoryPathVariableName, dbDirectoryPath);
            }

            if (exposeDbPort) {
                System.setProperty(dbPortVariableName, String.valueOf(tcpPort));
            }
        }

        void start() {
            try {
                this.server.start();
            } catch (SQLException e) {
                throw new RuntimeException("Could not start Server-instance", e);
            }
        }

        void stop() {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        try {

                            // give the datasource time to disconnect
                            Thread.sleep(800);
                        } catch (InterruptedException e1) {
                            logger.debug("Thread interrupted: ", e1);
                        }

                        server.stop();
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            logger.debug("Thread interrupted: ", e);
                        }

                        if (deleteDbDirectoryPathOnShutdown) {
                            File file = new File(dbDirectoryPath);
                            if (file.exists()) {

                                FileUtils.deleteRecursive(dbDirectoryPath, true);
                                if (file.exists()) {
                                    logger.warn("'dbDirectory' still existent at {}", file.getAbsolutePath());
                                    file.deleteOnExit();
                                }
                            } else {
                                logger.debug("'dbDirectory' not existent");
                            }
                        } else {
                            logger.info("'dbDirectory' still exists at {}", dbDirectoryPath);
                        }
                    }
                });
        }

        void afterStop() {
            if (exposeDbDirectoryPath) {
                System.clearProperty(dbDirectoryPathVariableName);
            }

            if (exposeDbPort) {
                System.clearProperty(dbPortVariableName);
            }
        }
    }

}
