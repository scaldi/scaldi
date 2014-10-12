package scaldi.jsr330;

import junit.framework.Test;
import org.atinject.tck.Tck;
import org.atinject.tck.auto.Car;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

/**
 * Starts JSR 330 TCK Test Suite.
 *
 * This is a java class, because JUnit requires usage of static methods :(
 */
@RunWith(AllTests.class)
public class Jsr330CompatibilityTest {
    public static Test suite() {
        Car car = new Jsr330Modules().injectCar();

        return Tck.testsFor(car,
            false /* supportsStatic */,
            true /* supportsPrivate */);
    }
}
