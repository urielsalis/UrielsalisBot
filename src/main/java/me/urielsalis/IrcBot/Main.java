package me.urielsalis.IrcBot;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import me.urielsalis.IRCApi.EventManager;
import me.urielsalis.IRCApi.IRCApi;
import me.urielsalis.IRCApi.events.Event;
import me.urielsalis.IRCApi.events.OnPrivmsg;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

/**
 * Main class
 * Listeners, save/load and irc load
 *
 * @author Uriel Salischiker
 */
public class Main {

    public static Main main;
    public static IRCApi irc;
    public Kryo kryo;
    public ArrayList<Driver> drivers;
    public HashMap<String, String> notes;
    public JSONParser parser = new JSONParser();
    public String tempOS;
    int count = 0;



    /**
     * Entry point
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        main = new Main();
    }

    public Main() {
        initBot();
        loadOrDownload();
    }

    /**
     * Save drivers and note to file
     */
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

    /**
     * Join EsperNet and wait 1 second
     */
    public void initBot() {
        irc = new IRCApi();
        new Thread() {
            @Override
            public void run() {
                irc.init("irc.esper.net", "Urielsalads");
                irc.run();
            }
        }.start();

        EventManager.commandPrefix = "!";

        System.out.println("Adding Listeners");
        EventManager.addClass(Listeners.class);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Connected: " + irc.isConnected());

    }


    /**
     * Load settings so they are ready for onRegistered
     * If save.bin is found, load database from there
     * else, download intel database
     */
    public void loadOrDownload() {
        File settings = new File("settings");
        if(!settings.exists()) {
            System.err.println("No settings file!");
            System.exit(-1);
        } else {
            try {
                System.out.println("Loaded settings");
                List<String> lines = Files.readAllLines(settings.toPath(), StandardCharsets.UTF_8);
                if(lines.size() < 3) {
                    System.err.println("Invalid settings file. First line nickserv user, second line nickserv password, next lines channels to join. Min one channel");
                    System.exit(-1);
                }
                Save.nickservUser = lines.get(0);
                Save.nickservPass = lines.get(1);

                for (int i = 2; i < lines.size(); i++) {
                    Save.channels.add(lines.get(i));
                }

                Main.irc.send("Nickserv", "identify " + Save.nickservUser + " " + Save.nickservPass);
                System.out.println("<NickServ> identify ****** ******");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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
            drivers = new ArrayList<>();
            notes = new HashMap<>();
            downloadIntel();
            save();
        }

    }

    /**
     * Downloads database from intel
     */
    public void downloadIntel() {
        try {
            System.out.println("Downloading intel database");
            URL url = new URL("https://downloadcenter.intel.com/product/80939/Graphics-Drivers");
            InputStream is = url.openStream();  // throws an IOException
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean found = false;
            while ((line = br.readLine()) != null) {
                if(!found) {
                    //<button type="button" class="text-left btn btn-primary btn-no-shadow dropdown-toggle">Additional selections available...</button>
                    if(line.contains("Additional selection")) found = true;
                } else {
                    if(line.contains("<li>")) {
                        //<li><a href="/product/88357/Intel-Iris-Pro-Graphics-580-for-6th-Generation-Intel-Core-Processors">Intel&#174; Iris™ Pro Graphics 580 for 6th Generation Intel&#174; Core™ Processors</a></li>
                        String s = line.replace("<li><a href=\"", "");
                        String driverUrl = "https://downloadcenter.intel.com" + s.substring(0, s.indexOf("\">")).trim();
                        String driverName = removeHTML(s.substring(s.indexOf("\">")+2, s.indexOf("</a>")).replace("</a></li>", "")).trim();
                        Driver d = new Driver(driverName, driverUrl);
                        Driver d2 = driverDownloads(d);
                        count += d2.downloads.size();
                        drivers.add(d2);

                    } else if(line.contains("</ul>")) {
                        break;
                    }
                }
            }
            System.out.println("Done " + drivers.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets all downloads for a given driver
     *
     * @param d
     * @return d with all its downloads
     */
    public Driver driverDownloads(Driver d) {
        String empid = d.url.split("/")[4];
        try {
            URL url = new URL("https://downloadcenter.intel.com/json/pageresults?pageNumber=1&&productId="+empid);
            Scanner s = new Scanner(url.openStream(), "UTF-8");
            String json = s.useDelimiter("\\A").next();
            s.close();
            JSONObject obj2 = (JSONObject) parser.parse(json);
            JSONArray array2 = (JSONArray) obj2.get("ResultsForDisplay");
            for(int n = 0; n < array2.size(); n++) {
                JSONObject jsonObject = (JSONObject) array2.get(n);
                String os = (String) jsonObject.get("OperatingSystems");
                String urlDown = "https://downloadcenter.intel.com"+ (String) jsonObject.get("FullDescriptionUrl");
                String nameDown = (String) jsonObject.get("Title");
                String version = (String) jsonObject.get("Version");

                d.add(os, urlDown, nameDown, version);
            }
            return d;
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Removes Some ugly characters from intel names
     *
     * @param replace
     * @return replace with intel characters strip out
     */
    public String removeHTML(String replace) {
        return replace.replace("&#174;", "").replace("™", "").replace("(TM)", "").replace("(tm)", "").replace("TM", "").replace("tm", "");
    }

    public String removeEdition(String replace) {
        String[] strs = replace.split(" ");
        String result = "";
        for(String str: strs) {
            if(str.toLowerCase().equals("windows") || isInteger(str) || str.equals("8.1") || str.toLowerCase().equals("xp") || str.toLowerCase().equals("vista")) result += str + " ";
        }
        return result.substring(0, result.length()-1);
    }

    public boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

    public static void send(String str, Event event) {
        OnPrivmsg e = (OnPrivmsg) event;
        String channel = e.chan;
        if(e.chan.equals("Urielsalads")) {
            channel = e.u.getNick();
        }

        System.out.println(channel + " - " + "<Urielsalads> " + str);

        for(String s: str.split("\n")) {
            irc.send(channel, s);
        }
    }

    public String findOS(String query) {
        for(Driver d: drivers) {
            if(contains(d.name, query)) {
                return ChatFormat.DARK_GREEN + d.url + ChatFormat.NORMAL +"\n" + ChatFormat.YELLOW + checkDrivers(d) + ChatFormat.NORMAL;
            }
        }
        return "Not found";
    }

    public String checkDrivers(Driver driver, String os) {
        boolean bit64 = os.contains("64");
        System.out.println(os);
        String windows = os.split(" ")[1];
        String higher = "Too old";

        for(Download download: driver.downloads) {
            if(!download.name.toLowerCase().contains("inf")) {
                System.out.println(download.os);
                boolean bit642 = download.os.contains("64");
                if (bit64 == bit642 && download.os.contains(windows)) return "true";
                if (download.os.contains("10")) higher = "10"; //bruteforce way but whatever
                else if (download.os.contains("8.1") && !higher.equals("10")) higher = "8.1";
                else if (download.os.contains("8") && !higher.equals("10") && !higher.equals("8.1")) higher = "8";
                else if (download.os.contains("7") && !higher.equals("10") && !higher.equals("8.1") && !higher.equals("8"))
                    higher = "7";
                else if (download.os.contains("Vista") && !higher.equals("10") && !higher.equals("8.1") && !higher.equals("8") && !higher.equals("7"))
                    higher = "Vista";
                else if (download.os.contains("XP") && !higher.equals("10") && !higher.equals("8.1") && !higher.equals("8") && !higher.equals("7") && !higher.equals("Vista"))
                    higher = "XP";
                else if (download.os.contains("2000") && !higher.equals("10") && !higher.equals("8.1") && !higher.equals("8") && !higher.equals("7") && !higher.equals("Vista") && !higher.equals("XP"))
                    higher = "2000";
            }
        }
        return bit64 ? "No drivers for Windows " + windows + " x64. Latest is Windows " + higher : "No drivers for Windows " + windows + ". Latest is Windows " + higher;
    }

    public static String checkDrivers(Driver driver) {
        String higher = "Too old";

        for(Download download: driver.downloads) {
            if(!download.name.toLowerCase().contains("inf")) {
                System.out.println(download.os);
                if (download.os.contains("10")) higher = "10"; //bruteforce way but whatever
                else if (download.os.contains("8.1") && !higher.equals("10")) higher = "8.1";
                else if (download.os.contains("8") && !higher.equals("10") && !higher.equals("8.1")) higher = "8";
                else if (download.os.contains("7") && !higher.equals("10") && !higher.equals("8.1") && !higher.equals("8"))
                    higher = "7";
                else if (download.os.contains("Vista") && !higher.equals("10") && !higher.equals("8.1") && !higher.equals("8") && !higher.equals("7"))
                    higher = "Vista";
                else if (download.os.contains("XP") && !higher.equals("10") && !higher.equals("8.1") && !higher.equals("8") && !higher.equals("7") && !higher.equals("Vista"))
                    higher = "XP";
                else if (download.os.contains("2000") && !higher.equals("10") && !higher.equals("8.1") && !higher.equals("8") && !higher.equals("7") && !higher.equals("Vista") && !higher.equals("XP"))
                    higher = "2000";
            }
        }
        return "Latest is Windows "+higher;
    }

    public boolean contains(String name, String query) {
        return name.toLowerCase().contains(query.toLowerCase()) || query.toLowerCase().contains(name);
    }


    public String findDriver(String driver, String os) {
        driver = driver.trim();
        os = os.trim();
        String result = "";
        int count = 0;
        String windows = os.split(" ")[1];
        boolean showedExe = false;
        for(Driver d: drivers) {
            if(contains(d.name, driver)) {
                String exists = checkDrivers(d, removeEdition(os));
                result += ChatFormat.DARK_BLUE + d.url + ChatFormat.NORMAL + " - " + ChatFormat.RED + d.name + ChatFormat.NORMAL;
                if (exists.equals("true")) {
                    boolean drivbit64 = os.contains("64");
                    for (Download download : d.downloads) {
                        boolean bit64 = download.os.contains("64");
                        if(!download.name.toLowerCase().contains("inf")) {
                            if (!showedExe || !download.url.toLowerCase().contains("zip")) {
                                if(download.url.toLowerCase().contains("exe")) showedExe = true;
                                if (contains(windows, download.os)) {
                                    if (bit64 && drivbit64) {
                                        if (count <= 1) {
                                            result += "\n" + ChatFormat.RED + download.name + ChatFormat.NORMAL + " for " + ChatFormat.BLUE + download.os + ChatFormat.NORMAL + " " + ChatFormat.LIGHT_GRAY + download.version + ChatFormat.NORMAL + "\n" + ChatFormat.YELLOW + download.url + ChatFormat.NORMAL;
                                            count++;
                                        } else {
                                            break;
                                        }
                                    } else if (!bit64 && !drivbit64) {
                                        if (count <= 1) {
                                            result += "\n" + ChatFormat.RED + download.name + ChatFormat.NORMAL + " for " + ChatFormat.BLUE + download.os + ChatFormat.NORMAL + " " + ChatFormat.LIGHT_GRAY + download.version + ChatFormat.NORMAL + "\n" + ChatFormat.BOLD + download.url + ChatFormat.NORMAL;
                                            count++;
                                        } else {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    return exists;
                }
                break;
            }
        }
        if(result.isEmpty()) return "Not found";
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


}
