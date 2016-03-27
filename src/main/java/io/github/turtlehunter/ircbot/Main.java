package io.github.turtlehunter.ircbot;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import me.urielsalis.IRCApi.EventManager;
import me.urielsalis.IRCApi.IRCApi;
import me.urielsalis.IRCApi.events.*;
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
    public ArrayList<Driver> drivers = new ArrayList<>();
    public HashMap<String, String> notes = new HashMap<>();
    public String tempOS = "";
    public IRCApi irc;
    private Kryo kryo;
    public static Main main;
    JSONParser parser = new JSONParser();
    ArrayList<String> channel = new ArrayList<>();

    public static void main(String[] args)
    {
        main = new Main();
    }

    public Main() {
        initBot();
        loadOrDownload();
    }

    public void loadOrDownload() {
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
            send("UrielsalisBot V1.1. Downloading Intel database, this might(will) take a while");
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

            send("Finished setting up. " + count + " drivers loaded");
            save();
        }
    }

    private void send(String s) {
        for(String str: channel) {
            irc.send(str, s);
        }
    }

    public void initBot() {
        irc = new IRCApi();

        new Thread(){
            @Override
            public void run() {
                irc.init("irc.esper.net", 6667, "" , "UrielsalisBot", "UrielsalisBot", "UrielsalisBot", false);
                irc.start();
            }
        }.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Reflection Start...");
        EventManager.commandPrefix = "!";
        EventManager.addClass(Listeners.class);
        System.out.println("Loading Complete!");
        //init done
        System.out.println("Is connected: " + irc.isConnected());
    }

    public void joinChannel(String channel) {
        if(channel == null) System.out.println("null");
        irc.join(channel);
        System.out.println("Joined Channel " + channel);
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
        boolean showed = false;
        String result = "Not found";
        for(Driver drv: drivers) {

            String lowdrv = drv.name.toLowerCase();
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
                        if(drv.name.toLowerCase().contains("inf")) {
                            continue;
                        }
                        String drvos = down.os.toLowerCase();
                        if (os.contains(Util.removeEdition(drvos).split(" ")[1])) {
                            if ((os.contains("32") && drvos.contains("32")) || (!os.contains("64") && !os.contains("64"))) {
                                if (!showed) {
                                    result += ChatFormat.OLIVE + down.version + ChatFormat.NORMAL + " for " + ChatFormat.OLIVE + down.os + ChatFormat.NORMAL + " - " + ChatFormat.BOLD + down.url + ChatFormat.NORMAL + "\n";
                                    for (String str : notes.keySet()) {
                                        if (str.contains(down.name.toLowerCase()) || down.name.toLowerCase().contains(str) || str.contains(down.url.toLowerCase()) || down.url.toLowerCase().contains(str)) {
                                            result += ChatFormat.OLIVE + notes.get(str) + ChatFormat.NORMAL;
                                        }
                                    }
                                    showed = true;
                                }
                            } else if (os.contains("64") && drvos.contains("64")) {
                                if (!showed) {
                                    result += ChatFormat.OLIVE + down.version + ChatFormat.NORMAL + " for " + ChatFormat.OLIVE + down.os + ChatFormat.NORMAL + " - " + ChatFormat.BOLD + down.url + ChatFormat.NORMAL + "\n";
                                    for (String str : notes.keySet()) {
                                        if (str.contains(down.name.toLowerCase()) || down.name.toLowerCase().contains(str) || str.contains(down.url.toLowerCase()) || down.url.toLowerCase().contains(str)) {
                                            result += ChatFormat.OLIVE + notes.get(str) + ChatFormat.NORMAL;
                                        }
                                    }
                                    showed = true;
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

    public String format(String graphiccard) {
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

    public static String filter(String family) {
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
        System.out.println(channel);
        for(String str2: strs) {
            irc.send(channel, str2);
        }
    }

    public static String checkDrivers(Driver driver, String os) {
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
        return bit64 ? "No drivers for Windows " + windows + " x64. Latest is Windows " + higher : "No drivers for Windows " + windows + ". Latest is Windows " + higher;
    }

    public static String checkDrivers(Driver driver) {
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

