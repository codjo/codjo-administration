package net.codjo.administration.server.operation.configuration;
import java.util.ArrayList;
import java.util.List;
/**
 *
 */
public class Errors {
    // lines that has been found but expected to be absent
    private List<Object> failures = new ArrayList<Object>();


    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }


    public StringBuilder appendTo(StringBuilder buffer) {
        appendTo(buffer, 1);
        return buffer;
    }


    public StringBuilder appendTo(StringBuilder buffer, int failureNum) {
        for (Object object : failures) {
            if (object instanceof Failure) {
                Failure failure = (Failure)object;
                failure.appendTo(buffer, failureNum);
                buffer.append('\n');

                failureNum++;
            }
            else {
                Object[] errorsGroup = (Object[])object;
                buffer.append(errorsGroup[0]);
                Errors errors = (Errors)errorsGroup[1];
                errors.appendTo(buffer, failureNum);

                failureNum += errors.failures.size();
            }
        }
        return buffer;
    }


    public void appendAssertLineError(List<String> lines,
                                      String expectedLine,
                                      boolean pattern,
                                      boolean expectPresent) {
        failures.add(new Failure(lines, expectedLine, pattern, expectPresent));
    }


    public boolean isEmpty() {
        return failures.isEmpty();
    }


    public void append(String errorsHeader, Errors errors) {
        Object[] errorsGroup = new Object[]{errorsHeader, errors};
        failures.add(errorsGroup);
    }


    private static class Failure {
        private final List<String> lines;
        private final String expectedLine;
        private final boolean pattern;
        private final boolean expectPresent;


        public Failure(List<String> lines, String expectedLine, boolean pattern, boolean expectPresent) {
            this.lines = lines;
            this.expectedLine = expectedLine;
            this.pattern = pattern;
            this.expectPresent = expectPresent;
        }


        @Override
        public String toString() {
            return appendTo(new StringBuilder(), 0).toString();
        }


        public StringBuilder appendTo(StringBuilder buffer, int failureNum) {
            buffer.append("Failure #").append(failureNum).append(":\n");

            // expectation
            buffer.append('\t');
            if (pattern) {
                buffer.append(expectPresent ? "A" : "NO");
                buffer.append(" line with this pattern was expected : '").append(expectedLine).append("'");
            }
            else {
                buffer.append("This line was expected ").append(expectPresent ? "" : "to be ABSENT").append(" :\n");
                buffer.append("\t\t").append(expectedLine);
            }
            buffer.append('\n');

            // actual lines
            buffer.append("\tActual lines:\n");
            for (int i = 0; i < lines.size(); i++) {
                buffer.append("\t\tLine L+").append(i).append(": ").append(lines.get(i)).append('\n');
            }

            return buffer;
        }
    }
}
