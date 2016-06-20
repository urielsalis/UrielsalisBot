package me.urielsalis.urielsalisBot;

import java.util.ArrayList;

/**
 * @author Uriel Salischiker
 */
public class Driver {
    public String name;
    public String url;
    public ArrayList<Download> downloads = new ArrayList<Download>();

    public Driver(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public void add(String os, String url, String name, String version) {
        downloads.add(new Download(os, url, name, version));
    }

    public Driver() {}
}
