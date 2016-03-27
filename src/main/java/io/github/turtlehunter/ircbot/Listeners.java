package io.github.turtlehunter.ircbot;

import com.esotericsoftware.kryo.io.Output;
import me.urielsalis.IRCApi.events.*;

import java.io.*;
import java.util.HashMap;

/**
 * io.github.turtlehunter.ircbot - uriel bot 26/3/2016
 */
public class Listeners {
    @EventHandler("onInvite")
    public void onInvite(Event event) {
        Main.main.joinChannel(((OnInvite) event).chan);
    }

    @Command("quit")
    public void quit(Event event, String[] args) {
        Main.main.save();
        System.out.println("Quit");
        if(args.length > 0) {
            Main.main.irc.partChannel(args[0], "Leaving");
        } else {
            Main.main.irc.quit("Bai");
        }
        System.exit(0);
    }

    @Command("cleardatabase")
    public void cleardatabase(Event event, String[] args) {
        File file = new File("save.bin");
        file.delete();
        Main.main.drivers.clear();
        Main.main.loadOrDownload();
    }

    @Command("clearnotes")
    public void clearnotes(Event event, String[] args) {
        File file = new File("notes.bin");
        file.delete();
        Main.main.notes = new HashMap<>();
    }

    @Command(".help")
    public void help(Event event, String[] args) {
        Main.main.sendMSG(((OnPrivmsg)event).chan, "!quit Quits, !cleardatabase Clears the database, !clearnotes Clears the notes, !add <pattern> Note <note> Adds a note, !getDriver <pattern> Windows <os> [32/64] Gets info of pattern from OS, !getDrivers sames a !getDriver, !getOS <driver> Get latest version of driver");
    }

    @Command("add")
    public void add(Event event, String[] args) {
        OnPrivmsg e = (OnPrivmsg) event;
        String message = e.msg;
        Main.main.notes.put(message.substring(message.indexOf(" ") + 1, message.indexOf(" Note ") - 1), message.substring(message.indexOf(" Note ") + 6));
    }

    @Command("getOS")
    public void getOS(Event event, String[] args) {
        OnPrivmsg e = (OnPrivmsg) event;
        String message = e.msg;
        String channel = e.chan;
        if(channel.equals("UrielsalisBot")) {
            channel = e.u.getNick();
        }

        String drv = message.substring(7).toLowerCase();
        System.out.println(drv);
        for (Driver d : Main.main.drivers) {
            if (d.name.toLowerCase().contains(drv) || drv.contains(d.name.toLowerCase())) {
                Main.main.sendMSG(channel, "Latest is Windows " + Main.checkDrivers(d) + " from Driver at " + d.url);
                break;
            }
        }
    }

    @Command("getDriver")
    public void getDriver(Event event, String[] args) {
        OnPrivmsg e = (OnPrivmsg) event;
        String message = e.msg;
        String channel = e.chan;
        if(channel.equals("UrielsalisBot")) {
            channel = e.u.getNick();
        }
        String driver = message.substring(message.indexOf(" ") + 1, message.indexOf("Windows") - 1);
        String os = message.substring(message.indexOf("Windows"));

        if (message.toLowerCase().contains("nvidia")) {
            Main.main.sendMSG(channel, "GO TEAM GREEN!!\nSorry, not implemented yet");
        } else if (message.toLowerCase().contains("amd")) {
            Main.main.sendMSG(channel, "GO TEAM RED!!\nSorry, not implemented yet");
        } else {
            Main.main.sendMSG(channel, Main.main.findDriver(driver, os));
        }
    }

    @EventHandler("onPrivmsg")
    public void onMessage(Event event) {
        OnPrivmsg e = (OnPrivmsg) event;
        String message = e.msg;
        String channel = e.chan;
        if(channel.equals("UrielsalisBot")) {
            channel = e.u.getNick();
        }

        if(e.msg.contains("Enum\\PCI\\VEN_8086")) {
            if (message.contains("Graphics card")) {
                String str[] = message.split(", ");
                for (String stR : str) System.out.print(stR + "-");
                String graphics = null;
                if (str[1].contains("HD") || str[1].contains("Graphics")) graphics = str[1];
                if (str[2].contains("HD") || str[2].contains("Graphics")) graphics = str[2];
                if (str[3].contains("HD") || str[3].contains("Graphics")) graphics = str[3];
                if (graphics != null) {
                    String str2 = Main.main.findDriver(graphics, Main.main.tempOS);
                    if (!str2.equals("Not found")) Main.main.sendMSG(channel, str2);
                }
            } else {
                String data[] = message.split("\\|");
                String os2 = Util.removeEdition(data[2].replace("-bit", "")).replace(" 64", "").replace(" 32", "");
                if (data[2].contains("32")) os2 += " 32";
                else if (data[2].contains("64")) os2 += " 64";
                Main.main.tempOS = os2;
                if (data[3].contains("not find card")) return;
                String tmp = data[3].replace("(R)", "");
                String graphics = Main.filter(tmp.substring(tmp.indexOf("(") + 1, tmp.indexOf(")")).replace("(R)", "").replace("Family", "").replace("-Chipsatzfamilie", ""));
                graphics = Main.main.format(graphics);
                System.out.println(os2 + "-" + graphics);
                String str2 = Main.main.findDriver(graphics, os2);
                System.out.println(str2);
                if (!str2.equals("Not found")) Main.main.sendMSG(channel, str2);
            }
        }

    }

    @EventHandler("onRegistered")
    public void onRegistered(Event event) {
        try (BufferedReader br = new BufferedReader(new FileReader(new File("channel.txt")))) {
            String line;
            while ((line = br.readLine()) != null) {
                Main.main.channel.add(line);
                Main.main.joinChannel(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
