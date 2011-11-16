package net.codjo.administration.server.operation.systemProperties;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class DefaultSystemProperties implements SystemProperties {

    public String getSystemProperties() {
        Properties properties = System.getProperties();

        StringBuilder sb = new StringBuilder();
        List<Object> keyList = new ArrayList<Object>();
        keyList.addAll(properties.keySet());
        Collections.sort(keyList, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                return String.valueOf(o1).compareTo(String.valueOf(o2));
            }
        });
        for (Object key : keyList) {
            Object value = properties.get(key);
            sb.append(key).append("=").append(value).append("\n");
        }
        return sb.toString();
    }


    public String getSystemEnvironment() {
        Map<String, String> map = System.getenv();

        StringBuilder sb = new StringBuilder();
        List<String> keyList = new ArrayList<String>();
        keyList.addAll(map.keySet());
        Collections.sort(keyList);
        for (String key : keyList) {
            String value = map.get(key);
            sb.append(key).append("=").append(value).append("\n");
        }
        return sb.toString();
    }
}
