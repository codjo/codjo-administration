package net.codjo.administration.server.operation.log;
import net.codjo.util.file.FileUtil;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultLogReader implements LogReader {
    private String path;


    public DefaultLogReader(String path) {
        this.path = path;
    }


    public void setPath(String path) {
        this.path = path;
    }


    public List<String> getLogFiles() {
        if (path == null || !new File(path).exists()) {
            return Collections.emptyList();
        }

        List<String> logFiles = Arrays.asList(new File(path).list(new FilenameFilter() {
            public boolean accept(File file, String name) {
                return new File(file, name).isFile();
            }
        }));
        Collections.sort(logFiles);
        return logFiles;
    }


    public String readLog(String logFile) throws IOException {
        return FileUtil.loadContent(new File(path, logFile));
    }
}
