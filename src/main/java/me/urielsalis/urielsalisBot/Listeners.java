package me.urielsalis.urielsalisBot;

import me.urielsalis.IRCApi.events.*;

import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings("Since15")
public class Listeners {
    @EventHandler("onRegistered")
    public void onRegistered(Event event) {
        System.out.println("OnRegistered");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(String str: Save.channels) {
            Util.irc.join(str);
            System.out.println("<Join> " + str);
        }
        Util.irc.send("Nickserv", "identify " + Save.nickservUser + " " + Save.nickservPass);
        System.out.println("<NickServ> identify ****** ******");
    }

    @EventHandler("onInvite")
    public void onInvite(Event event) {
        OnInvite e = (OnInvite) event;
        System.out.println("<Join> " + e.chan + " invited by: " + e.u.getNick());
        Util.irc.join(e.chan);
    }

    @Command("quit")
    public void quit(Event event, String[] args) {
        if(args.length > 0) {
            Util.irc.partChannel(args[0], "Nobody likes me :(");
        } else {
            Util.irc.quit("Bai");
        }
    }

    @Command("clearDatabase")
    public void clearDatabase(Event event, String[] args) {
        Bot.setNotes(new HashMap<String, Driver>());
        Bot.setRegex(new HashMap<String, String>());
        Bot.setDrivers(new ArrayList<Driver>());
        Util.downloadIntel();
        Bot.save();
    }

    @Command("listDrivers")
    public void listDrivers(Event event, String[] args) {
        StringBuilder b = new StringBuilder();
        for(Driver d: Bot.getDrivers()) {
            b.append("Driver: " + d.name + " at " + d.url + "\n");
            for(Download dw: d.downloads) {
                b.append(dw.name + " - " + dw.url + " - " + dw.version + " - " + dw.os + "\n");
            }
            b.append("\n");
        }
        System.out.println(b.toString());
    }

    @EventHandler("onNotice")
    public void onNotice(Event event) {
        OnNotice e = (OnNotice) event;
        String message = e.msg;

        System.out.println(e.target + " - " + "<" + e.u.getNick() + "> " + message);
    }

    @Command("getOS")
    public void getOS(Event event, String[] args) {
        String query = String.join(" ", args);
        Util.send(Util.findOS(Util.format(query)), event);
    }

    @Command("addRegex")
    public void addRegex(Event event, String[] args) {
        String query = String.join(" ", args);
        String[] x = query.split("~");
        Bot.addRegex(x[0], x[1]);
        Bot.save();
    }

    @Command("addNote")
    public void addNote(Event event, String[] args) {
        String query = String.join(" ", args);
        String[] x = query.split("~");
        Driver d = Util.getDriver(x[1]);
        if(d != null) {
            Bot.addNote(x[0], Util.getDriver(x[1]));
            Bot.save();
        }
    }

    @EventHandler("onPrivmsg")
    public void onMessage(Event event) {
        OnPrivmsg e = (OnPrivmsg) event;
        String message = e.msg;

        System.out.println(e.chan + " - " + "<" + e.u.getNick() + "> " + message);

        Bot.message(e, message);
    }
}
