package me.urielsalis.IrcBot;

import me.urielsalis.IRCApi.events.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Uriel Salischiker
 */
public class Listeners {
    /**
     * Join channels when we connected to the irc network
     *
     * @param event OnRegistered event. Ignored
     */
    @EventHandler("onRegistered")
    public void onRegistered(Event event) {
        System.out.println("OnRegistered");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for(String str: Save.channels) {
            Main.irc.join(str);
            System.out.println("<Join> " + str);
        }
        Main.irc.send("Nickserv", "identify " + Save.nickservUser + " " + Save.nickservPass);
        System.out.println("<NickServ> identify ****** ******");
    }

    @EventHandler("onNotice")
    public void onNotice(Event event) {
        OnNotice e = (OnNotice) event;
        String message = e.msg;

        System.out.println(e.target + " - " + "<" + e.u.getNick() + "> " + message);
    }

    @EventHandler("onInvite")
    public void onInvite(Event event) {
        OnInvite e = (OnInvite) event;
        System.out.println("<Join> " + e.chan + " invited by: " + e.u.getNick());
        Main.irc.join(e.chan);
        Main.irc.send(e.chan, "Urielsalads reporting for duty o7");
    }

    @EventHandler("onPrivmsg")
    public void onMessage(Event event) {
        OnPrivmsg e = (OnPrivmsg) event;
        String message = e.msg;

        System.out.println(e.chan + " - " + "<" + e.u.getNick() + "> " + message);

        if(message.contains("Enum\\PCI\\VEN_8086")) {
            if (message.contains("Graphics card")) {
                String str[] = message.split(", ");
                for (String stR : str) System.out.print(stR + "-");
                String graphics = null;
                if (str[1].contains("HD") || str[1].contains("Graphics")) graphics = str[1];
                if (str[2].contains("HD") || str[2].contains("Graphics")) graphics = str[2];
                if (str[3].contains("HD") || str[3].contains("Graphics")) graphics = str[3];
                if (graphics != null) {
                    String str2 = Main.main.findDriver(Main.main.format(graphics), Main.main.tempOS);
                    if (!str2.equals("Not found")) Main.send(str2, event);
                }
            } else {
                String data[] = message.split("\\|");
                String os2 = Main.main.removeEdition(data[2].replace("-bit", "")).replace(" 64", "").replace(" 32", "");
                if (data[2].contains("32")) os2 += " 32";
                else if (data[2].contains("64")) os2 += " 64";
                Main.main.tempOS = os2;
                if (data[3].contains("not find card")) return;
                String tmp = data[3].replace("(R)", "");
                String graphics = Main.main.removeHTML(Main.main.format(tmp.substring(tmp.indexOf("(") + 1, tmp.indexOf(")")).replace("(R)", "").replace("Family", "").replace("-Chipsatzfamilie", "").replace("Familia", "")));
                graphics = Main.main.format(graphics);
                System.out.println(os2 + "-" + graphics);
                String str2 = Main.main.findDriver(graphics, os2);
                System.out.println(str2);
                if (!str2.equals("Not found")) Main.send(str2, event);
            }
        } else if(message.startsWith(".dx")) {
            //get CPU and save it for the future
            Main.main.getCPU(message);
        }
    }

    @Command("quit")
    public void quit(Event event, String[] args) {
        if(args.length > 0) {
            Main.irc.partChannel(args[0], "Nobody likes me :(");
        } else {
            Main.irc.quit("Bai");
        }
    }
    /**
     * Get Driver command
     *
     * @param event OnPrivMsg event
     * @param args Arguments of command
     */
    @Command("getDriver")
    public void getDriver(Event event, String[] args) {
        String query = String.join(" ", args).toLowerCase();
        String driver = query.substring(0, query.indexOf("windows")).trim();
        String os = Main.main.removeEdition(query.substring(driver.length()).trim());
        System.out.println(driver);
        System.out.println(os);
        Main.send(Main.main.findDriver(Main.main.format(driver), os), event);

    }

    @Command("getOS")
    public void getOS(Event event, String[] args) {
        String query = String.join(" ", args);
        Main.send(Main.main.findOS(Main.main.format(query)), event);
    }


    @Command("addDriver")
    public void addDriver(Event event, String[] args) {
        String query = String.join(" ", args);
        String[] x = query.split("|");
        Main.main.drivers.add(new Driver(x[0], x[1]));
        Main.main.save();

    }

    @Command("addDownload")
    public void addDownload(Event event, String[] args) {
        String query = String.join(" ", args);
        String[] x = query.split("|");
        for(Driver d: Main.main.drivers) {
            if(d.name.contains(x[0]) || x[0].contains(d.name)) {
                d.add(x[1], x[2], x[3], x[4]);
                break;
            }
        }
        Main.main.save();
    }

    @Command("addNote")
    public void addNote(Event event, String[] args) {
        String query = String.join(" ", args);
        String[] x = query.split("|");
        Main.main.notes.put(x[0], x[1]);
        Main.main.save();
    }

    @Command("clearDatabase")
    public void clearDatabase(Event event, String[] args) {
        Main.main.notes = new HashMap<>();
        Main.main.drivers = new ArrayList<>();
        Main.main.save();
    }

    @Command("listDrivers")
    public void listDrivers(Event event, String[] args) {
        StringBuilder b = new StringBuilder();
        for(Driver d: Main.main.drivers) {
            b.append("Driver: " + d.name + " at " + d.url + "\n");
            for(Download dw: d.downloads) {
                b.append(dw.name + " - " + dw.url + " - " + dw.version + " - " + dw.os + "\n");
            }
            b.append("\n");
        }
        System.out.println(b.toString());
    }

}
