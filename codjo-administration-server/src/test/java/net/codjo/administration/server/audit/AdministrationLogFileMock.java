package net.codjo.administration.server.audit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import net.codjo.test.common.LogString;
import org.apache.log4j.Logger;

import static org.junit.Assert.fail;
/**
 *
 */
public class AdministrationLogFileMock extends AdministrationLogFile {
    public static final int[] ALL_COLUMNS = new int[0];
    private static final int[] DEFAULT_COLUMNS_TO_KEEP = {0, 1};

    private final LogString log;
    private final int[] columnsToKeep;
    private final List<Object[]> lines = new ArrayList<Object[]>();
    private int currentLine = 0;


    public AdministrationLogFileMock(LogString log) {
        this(log, DEFAULT_COLUMNS_TO_KEEP);
    }


    public AdministrationLogFileMock(LogString log, int... columnsToKeep) {
        this.log = log;
        this.columnsToKeep = columnsToKeep;
        Arrays.sort(this.columnsToKeep);
    }


    @Override
    protected void write(String tag1, String tag2, Long when, Object... values) {
        boolean withTime = (when != null);
        int index = (withTime ? 3 : 2);
        Object[] parameters = new Object[index + values.length];
        parameters[0] = tag1;
        parameters[1] = tag2;
        if (withTime) {
            parameters[2] = formatDate(when);
        }
        System.arraycopy(values, 0, parameters, index, values.length);

        if (columnsToKeep != ALL_COLUMNS) { // yes, we compare by reference
            List<Object> parameterList = new ArrayList<Object>();
            for (int i = 0; i < parameters.length; i++) {
                if (Arrays.binarySearch(columnsToKeep, i) >= 0) {
                    parameterList.add(parameters[i]);
                }
            }
            parameters = parameterList.toArray();
        }

        lines.add(parameters);
        traceCall(log, parameters);
    }


    public void assertLine(String expectedContent) {
        LogString logLine = logLine(currentLine);
        try {
            logLine.assertContent(expectedContent);
        }
        catch (AssertionError cf) {
            Logger.getLogger(getClass()).error(linesToString());
            throw cf;
        }
        currentLine++;
    }


    public void assertLinePattern(String expectedContent) {
        try {
            logLine(currentLine).assertContent(Pattern.compile(expectedContent));
        }
        catch (AssertionError cf) {
            Logger.getLogger(getClass()).error(linesToString());
            throw cf;
        }
        currentLine++;
    }


    public void assertNoMoreLines() {
        if (currentLine < lines.size()) {
            StringBuilder message = new StringBuilder("There are more lines:\n");
            for (int i = currentLine; i < lines.size(); i++) {
                message.append(logLine(i).getContent()).append('\n');
            }
            fail(message.toString());
        }
    }


    public String[] extractLines(int nbLines) {
        String[] actualLines = new String[nbLines];
        for (int i = 0; i < nbLines; i++) {
            actualLines[i] = logLine(currentLine).getContent();
            currentLine++;
        }
        return actualLines;
    }


    private LogString logLine(int lineNum) {
        LogString logLine = new LogString();
        if (lineNum < lines.size()) {
            traceCall(logLine, lines.get(lineNum));
        }
        return logLine;
    }


    private static void traceCall(LogString log, Object[] parameters) {
        log.call("write", parameters);
    }


    private String linesToString() {
        return appendLinesTo(new StringBuilder()).toString();
    }


    public StringBuilder appendLinesTo(StringBuilder buffer) {
        buffer.append("\nLog content (currentLine=").append(currentLine).append(") :\n");
        int lineNum = 0;
        for (Object[] line : lines) {
            if (lineNum == currentLine) {
                buffer.append("-->");
            }
            else {
                buffer.append("   ");
            }
            buffer.append(lineNum).append(": ");

            boolean first = true;
            for (Object column : line) {
                if (!first) {
                    buffer.append(", ");
                }
                first = false;

                buffer.append(column);
            }
            buffer.append('\n');
            lineNum++;
        }
        buffer.append("--- end of log content ---");

        return buffer;
    }
}
