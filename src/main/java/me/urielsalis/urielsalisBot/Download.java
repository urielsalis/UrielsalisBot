package me.urielsalis.urielsalisBot;

/**
 * @author Uriel Salischiker
 */
public class Download {
    public String os;
    public String url;
    public String name;
    public String version;

    public Download(String os, String url, String name, String version) {
        this.os = os;
        this.url = url;
        this.name = name;
        this.version = version;
    }

    public Download() {}
}
