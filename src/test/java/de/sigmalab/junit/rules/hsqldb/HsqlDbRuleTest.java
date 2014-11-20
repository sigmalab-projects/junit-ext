package de.sigmalab.junit.rules.hsqldb;

import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.Test;

public class HsqlDbRuleTest {

    @ClassRule
    public static HsqldbRule rule = HsqldbRule.newBuilder().onPort(9003).build();

    @Test
    public void createHsqldbRule() throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
    }

    @Test
    public void createHsqldbRuleSecond() throws InterruptedException {
        TimeUnit.SECONDS.sleep(3);
    }

}
