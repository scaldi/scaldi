package scaldi.jsr330;

import java.io.Serializable;
import java.lang.annotation.Annotation;

public class TestQualifierImpl implements TestQualifier, Serializable {
    private static final long serialVersionUID = 0;

    private final String value;

    public TestQualifierImpl(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public int hashCode() {
        return (127 * "value".hashCode()) ^ value.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof TestQualifier)) {
            return false;
        }

        TestQualifier other = (TestQualifier) o;
        return value.equals(other.value());
    }

    public String toString() {
        return "@" + TestQualifier.class.getName() + "(value=" + value + ")";
    }

    public Class<? extends Annotation> annotationType() {
        return TestQualifier.class;
    }

    public static TestQualifier of(String value) {
        return new TestQualifierImpl(value);
    }
}
