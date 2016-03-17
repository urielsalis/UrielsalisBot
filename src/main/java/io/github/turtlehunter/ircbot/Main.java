package io.github.turtlehunter.ircbot;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jibble.pircbot.IrcException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * turtlehunter.github.IRCBot - uriel IRCBot 21/2/2016
 */

class Main
{
    private ArrayList<Driver> drivers = new ArrayList<>();
    private HashMap<String, String> notes = new HashMap<>();
    private String tempOS = "";
    private IRCBot ircBot;
    private Kryo kryo;
    public static Main main;
    JSONParser parser = new JSONParser();
    String channel;

    public static void main(String[] args)
    {
        main = new Main();
    }

    public Main() {
        initBot();
        loadOrDownload();
    }

    private void loadOrDownload() {
        kryo = new Kryo();
        if(new File("save.bin").exists()) {
            try {
                Input input = new Input(new FileInputStream("save.bin"));
                drivers = kryo.readObject(input, ArrayList.class);
                input.close();
                Input input1 = new Input(new FileInputStream("notes.bin"));
                notes = kryo.readObject(input1, HashMap.class);
                input1.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            ircBot.sendMessage(channel, "UrielsalisBot V1.1. Downloading Intel database, this might(will) take a while");
            updateDatabase();

            int count = 0;
            for(Driver dri: drivers) {
                System.out.println(dri.name + " - " + dri.url);
                count += dri.downloads.size();
                for(Download down: dri.downloads) {
                    System.out.println(down.name + " - " + down.version + " for " + down.os + " at " + down.url);
                }
                System.out.println();
            }

            ircBot.sendMessage(channel, "Finished setting up. " + count + " drivers loaded");
            save();
        }
    }

    private void initBot() {
        try {
            channel = new Scanner(new File("channel.txt")).useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ircBot = new IRCBot();
        try {
            ircBot.connect("irc.esper.net");
        } catch (IOException | IrcException e) {
            e.printStackTrace();
        }
        System.out.println("Connected: " + ircBot.isConnected());
        ircBot.joinChannel(channel);
    }

    private void updateDatabase() {
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;

        //Intel

        try {
            url = new URL("http://www.intel.com/content/www/us/en/support/graphics-drivers.html");
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null) {
                if(line.contains("var familyProduct")) {
                    String str = line.replace("var familyProduct = '[", "").replace("]';", "").trim();
                    try {
                        JSONObject obj = (JSONObject) new JSONParser().parse(str);
                        JSONArray array = (JSONArray) obj.get("productFamily");
                        for(Object o: array) {
                            JSONObject tmp = (JSONObject) o;
                            Driver driver = new Driver(((String) tmp.get("displayName")).replace("®", "").replace("™", ""), "http://www.intel.com/content/www/us/en/support/graphics-drivers/"+ tmp.get("shortname") +".html");

                            URL url3 = new URL("https://downloadcenter.intel.com/json/pageresults?pageNumber=1&&productId="+tmp.get("epmid"));
                            InputStream is3 = url3.openStream();
                            BufferedReader br3 = new BufferedReader(new InputStreamReader(is3));
                            String json = br3.readLine(); //json is only 1 line
                            System.out.println(json);
                            br3.close();
                            is3.close();

                            JSONObject obj2 = (JSONObject) parser.parse(json);
                            JSONArray array2 = (JSONArray) obj2.get("ResultsForDisplay");
                            for(int n = 0; n < array2.size(); n++) {
                                JSONObject jsonObject = (JSONObject) array2.get(n);
                                String os = (String) jsonObject.get("OperatingSystems");
                                String urlDown = "https://downloadcenter.intel.com"+ (String) jsonObject.get("FullDescriptionUrl");
                                String nameDown = (String) jsonObject.get("Title");
                                String version = (String) jsonObject.get("Version");

                                driver.add(os, urlDown, nameDown, version);
                            }
                            drivers.add(driver);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
            save();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ioe) {
                // nothing to see here
            }
        }

    }


    public String findDriver(String graphiccard, String os) {
        graphiccard = graphiccard.toLowerCase().replace("(r)", "");
        os = os.toLowerCase();
        graphiccard = format(graphiccard);
        int showed = 0;
        String result = "Not found";
        for(Driver drv: drivers) {

            String lowdrv = drv.name.toLowerCase();
            System.out.println(lowdrv + " " + graphiccard);
            if(lowdrv.contains(graphiccard) || graphiccard.contains(lowdrv)) {
                result = ChatFormat.RED + drv.url + ChatFormat.NORMAL + "\n";
                for (String str : notes.keySet()) {
                    if (str.contains(drv.name.toLowerCase()) || drv.name.toLowerCase().contains(str) || str.contains(drv.url.toLowerCase()) || drv.url.toLowerCase().contains(str)) {
                        result += "\n\u000312" + notes.get(str) + ChatFormat.NORMAL;
                    }
                }
                String exists = checkDrivers(drv, os);
                System.out.println(exists);
                if (exists.equals("true")) {
                    for (Download down : drv.downloads) {
                        String drvos = down.os.toLowerCase();
                        if (os.contains(Util.removeEdition(drvos).split(" ")[1])) {
                            if ((os.contains("32") && drvos.contains("32")) || (!os.contains("64") && !os.contains("64"))) {
                                if (showed < 2) {
                                    result += ChatFormat.OLIVE + down.version + ChatFormat.NORMAL + " for " + ChatFormat.OLIVE + down.os + ChatFormat.NORMAL + " - " + ChatFormat.BOLD + down.url + ChatFormat.NORMAL + "\n";
                                    for (String str : notes.keySet()) {
                                        if (str.contains(down.name.toLowerCase()) || down.name.toLowerCase().contains(str) || str.contains(down.url.toLowerCase()) || down.url.toLowerCase().contains(str)) {
                                            result += ChatFormat.OLIVE + notes.get(str) + ChatFormat.NORMAL;
                                        }
                                    }
                                    showed++;
                                }
                            } else if (os.contains("64") && drvos.contains("64")) {
                                if (showed < 2) {
                                    result += ChatFormat.OLIVE + down.version + ChatFormat.NORMAL + " for " + ChatFormat.OLIVE + down.os + ChatFormat.NORMAL + " - " + ChatFormat.BOLD + down.url + ChatFormat.NORMAL + "\n";
                                    for (String str : notes.keySet()) {
                                        if (str.contains(down.name.toLowerCase()) || down.name.toLowerCase().contains(str) || str.contains(down.url.toLowerCase()) || down.url.toLowerCase().contains(str)) {
                                            result += ChatFormat.OLIVE + notes.get(str) + ChatFormat.NORMAL;
                                        }
                                    }
                                    showed++;
                                }
                            }
                        }
                    }
                } else {
                    result += exists;
                }
            }
        }
        return result;
    }


    public void received(String channel, String user, String login, String hostname, String message) {
        switch (message) {
            case "!quit":
                save();
                System.out.println("Quit");
                ircBot.disconnect();
                ircBot.dispose();
                System.exit(0);

            case "!cleardatabase": {
                File file = new File("save.bin");
                file.delete();
                drivers.clear();
                loadOrDownload();
            }
            case "!clearnotes": {
                File file = new File("notes.bin");
                file.delete();
                notes = new HashMap<>();
                break;
            }
            case ".!help":
                sendMSG(channel, "!quit Quits, !cleardatabase Clears the database, !clearnotes Clears the notes, !add <pattern> Note <note> Adds a note, !getDriver <pattern> Windows <os> [32/64] Gets info of pattern from OS, !getDrivers sames a !getDriver, !getOS <driver> Get latest version of driver");
                break;
        }
        String command = message.substring(0, message.indexOf(" "));
        String driver = "";
        String os = "";
        System.out.println(message);
        if (!command.equals("!add") && !command.equals("!getOS") && !command.equals(".dx")) {
            driver = message.substring(message.indexOf(" ") + 1, message.indexOf("Windows") - 1);
            os = message.substring(message.indexOf("Windows"));
        }
        switch (command) {
            case "!add":
                notes.put(message.substring(message.indexOf(" ") + 1, message.indexOf(" Note ") - 1), message.substring(message.indexOf(" Note ") + 6));
            case "!getOS":
                String drv = message.substring(7).toLowerCase();
                System.out.println(drv);
                for (Driver d : drivers) {
                    if (d.name.toLowerCase().contains(drv) || drv.contains(d.name.toLowerCase())) {
                        sendMSG(channel, "Latest is Windows " + checkDrivers(d) + " from Driver at " + d.url);
                        break;
                    }
                }
            case "!getDriver":
                if (message.toLowerCase().contains("nvidia")) {
                    sendMSG(channel, "GO TEAM GREEN!!\nSorry, not implemented yet");
                } else if (message.toLowerCase().contains("amd")) {
                    sendMSG(channel, "GO TEAM RED!!\nSorry, not implemented yet");
                } else {
                    sendMSG(channel, findDriver(driver, os));
                }

        }

        if (user.equals("PangeaBot") || user.equals("urielsalis")) {
            //<PangeaBot> (webrosc) n/a | n/a | Windows 7 Professional 64-bit | Enum\PCI\VEN_8086&DEV_29B2&SUBSYS_02111028&REV_02 (Intel(R) Q35 Express Chipset Family)
            if (message.contains("Graphics card")) {
                String str[] = message.split(", ");
                for (String stR : str) System.out.print(stR + "-");
                String graphics = null;
                if (str[1].contains("HD") || str[1].contains("Graphics")) graphics = str[1];
                if (str[2].contains("HD") || str[2].contains("Graphics")) graphics = str[2];
                if (str[3].contains("HD") || str[3].contains("Graphics")) graphics = str[3];
                if (graphics != null) {
                    String str2 = findDriver(graphics, tempOS);
                    if (!str2.equals("Not found")) sendMSG(channel, str2);
                }
            } else {
                String data[] = message.split("\\|");
                String os2 = Util.removeEdition(data[2].replace("-bit", "")).replace(" 64", "").replace(" 32", "");
                if (data[2].contains("32")) os2 += " 32";
                else if (data[2].contains("64")) os2 += " 64";
                tempOS = os2;
                if (data[3].contains("not find card")) return;
                String tmp = data[3].replace("(R)", "");
                String graphics = filter(tmp.substring(tmp.indexOf("(") + 1, tmp.indexOf(")")).replace("(R)", "").replace("Family", "").replace("-Chipsatzfamilie", ""));
                graphics = format(graphics);
                System.out.println(os2 + "-" + graphics);
                String str2 = findDriver(graphics, os2);
                System.out.println(str2);
                if (!str2.equals("Not found")) sendMSG(channel, str2);
            }
        }
    }

    private String format(String graphiccard) {
        if (graphiccard.contains("45 Express Chipset")) graphiccard = "4 Series";
        if (graphiccard.contains("/")) {
            String words[] = graphiccard.split(" ");
            StringBuilder builder = new StringBuilder();
            for (String str : words) {
                builder.append(str.contains("/") ? str.split("/")[0] + " " : str + " ");
            }
            graphiccard = builder.toString();
        }
        return graphiccard;
    }


    public void save() {
        try {
            Output output = new Output(new FileOutputStream("save.bin"));
            kryo.writeObject(output, drivers);
            output.close();
            Output output1 = new Output(new FileOutputStream("notes.bin"));
            kryo.writeObject(output1, notes);
            output1.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String filter(String family) {
        String str[] = family.split(" ");
        String result = "";
        for(String stri: str) {
            if(!stri.contains("/")) {
                result += stri + " ";
            } else {
                result += stri.split("/")[0] + " ";
            }
        }
        return result.substring(0, result.length()-1);
    }

    public void sendMSG(String channel, String str) {
        String strs[] = str.split("\n");
        for(String str2: strs) ircBot.sendMessage(channel, str2);
    }

    public String checkDrivers(Driver driver, String os) {
        boolean bit64 = os.contains("64");
        System.out.println(os);
        String windows = os.split(" ")[1];
        String higher = "Too old";

        for(Download download: driver.downloads) {
            System.out.println(download.os);
            boolean bit642 = download.os.contains("64");
            if(bit64==bit642 && download.os.contains(windows)) return "true";
            if(download.os.contains("10")) higher = "10"; //bruteforce way but whatever
            else if(download.os.contains("8.1") && !higher.equals("10")) higher = "8.1";
            else if(download.os.contains("8") && !higher.equals("10") && !higher.equals("8.1")) higher = "8";
            else if(download.os.contains("7") && !higher.equals("10") && !higher.equals("8.1")&& !higher.equals("8")) higher = "7";
            else if(download.os.contains("Vista") && !higher.equals("10") && !higher.equals("8.1")&& !higher.equals("8")&& !higher.equals("7")) higher = "Vista";
            else if(download.os.contains("XP") && !higher.equals("10") && !higher.equals("8.1")&& !higher.equals("8")&& !higher.equals("7") && !higher.equals("Vista")) higher = "XP";
            else if(download.os.contains("2000") && !higher.equals("10") && !higher.equals("8.1")&& !higher.equals("8")&& !higher.equals("7") && !higher.equals("Vista") && !higher.equals("XP")) higher = "2000";
        }
        return "No drivers for Windows "+windows+". Latest is Windows "+higher;
    }

    public String checkDrivers(Driver driver) {
        String higher = "Too old";

        for(Download download: driver.downloads) {
            System.out.println(download.os);
            if(download.os.contains("10")) higher = "10"; //bruteforce way but whatever
            else if(download.os.contains("8.1") && !higher.equals("10")) higher = "8.1";
            else if(download.os.contains("8") && !higher.equals("10") && !higher.equals("8.1")) higher = "8";
            else if(download.os.contains("7") && !higher.equals("10") && !higher.equals("8.1")&& !higher.equals("8")) higher = "7";
            else if(download.os.contains("Vista") && !higher.equals("10") && !higher.equals("8.1")&& !higher.equals("8")&& !higher.equals("7")) higher = "Vista";
            else if(download.os.contains("XP") && !higher.equals("10") && !higher.equals("8.1")&& !higher.equals("8")&& !higher.equals("7") && !higher.equals("Vista")) higher = "XP";
            else if(download.os.contains("2000") && !higher.equals("10") && !higher.equals("8.1")&& !higher.equals("8")&& !higher.equals("7") && !higher.equals("Vista") && !higher.equals("XP")) higher = "2000";
        }
        return "Latest is Windows "+higher;
    }
}

