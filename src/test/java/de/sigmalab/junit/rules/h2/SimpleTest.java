package de.sigmalab.junit.rules.h2;

import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

/**
 * @author  jbellmann
 */
public class SimpleTest {

    @Rule
    public H2Rule h2Rule = H2Rule.newBuilder().build();

    @Test
    public void simpleTest() throws InterruptedException {
        TimeUnit.SECONDS.sleep(5);
        System.out.println("H2-HOME : " + System.getProperty("h2.home"));
        System.out.println("H2-PORT : " + System.getProperty("h2.port"));
        TimeUnit.SECONDS.sleep(2);
    }

}
