package net.codjo.administration.server.operation.log;
import java.io.IOException;
import java.util.List;

public interface LogReader {
    List<String> getLogFiles();


    String readLog(String logFile) throws IOException;
}
