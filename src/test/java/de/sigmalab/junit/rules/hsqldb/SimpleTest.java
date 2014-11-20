package de.sigmalab.junit.rules.hsqldb;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;

import org.junit.Rule;
import org.junit.Test;

public class SimpleTest {

    @Rule
    public HsqldbRule hsqldbRule = HsqldbRule.newBuilder().onPort(10002).withProperties(serverProperties()).build();

    @Test
    public void simpleConnectionWithStatementExecution() throws InterruptedException, SQLException {
        Connection c = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:10002/xdb", "SA", "SA");
        CallableStatement cs = c.prepareCall("SELECT 1");
        boolean result = cs.execute();
        Assertions.assertThat(result).isTrue();
    }

    static Map<String, String> serverProperties() {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("server.database.0", "mem:xdb;sql.syntax_pgs=true;user=SA;password=SA");
        properties.put("server.dbname.0", "xdb");
        properties.put("server.remote_open", "true");
        return properties;
    }

}
