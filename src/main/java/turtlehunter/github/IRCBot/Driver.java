package turtlehunter.github.IRCBot;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * turtlehunter.github.IRCBot - uriel IRCBot 21/2/2016
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
