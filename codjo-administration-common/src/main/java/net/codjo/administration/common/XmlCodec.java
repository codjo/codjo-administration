package net.codjo.administration.common;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import java.util.ArrayList;
import java.util.List;

public class XmlCodec {
    private XmlCodec() {
    }


    public static List<String> listFromXml(String xml) {
        //noinspection unchecked
        return (List<String>)createXstream().fromXML(xml);
    }


    public static String listToXml(List<String> plugins) {
        return XmlCodec.createXstream().toXML(new ArrayList<String>(plugins));
    }


    public static String logFromXml(String xml) {
        return (String)createXstream().fromXML(xml);
    }


    public static String logToXml(String text) {
        return createXstream().toXML(text);
    }


    private static XStream createXstream() {
        return new XStream(new DomDriver());
    }
}
