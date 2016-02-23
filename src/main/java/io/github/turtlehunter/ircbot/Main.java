package io.github.turtlehunter.ircbot;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.background.JavaScriptJobManager;
import org.apache.commons.logging.LogFactory;
import org.jibble.pircbot.IrcException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * turtlehunter.github.IRCBot - uriel IRCBot 21/2/2016
 */

class Main
{
    private ArrayList<String> devices = new ArrayList<>();
    private ArrayList<Driver> drivers = new ArrayList<>();
    private HashMap<String, String>  notes = new HashMap<>();
    private String tempOS = "";
    private IRCBot ircBot;
    private Kryo kryo;
    private static Main main;
    String channel;

    public static void main(String[] args)
    {
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
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
            getVersion();

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
        }
    }

    public void initBot() {
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
        boolean startCopying = false;
        boolean firstTRSkipped = false;
        boolean copyNext = false;

        //Intel

        try {
            url = new URL("http://www.intel.com/content/www/us/en/support/graphics-drivers/000005526.html");
            is = url.openStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null) {
                if(startCopying) {
                    if(copyNext) {
                        if(line.contains("href")) devices.add(line.trim().substring(48).replaceAll("&reg;", "").replaceAll("&trade;", "").replaceAll("</a></td>", "").replaceAll("\">", " "));
                        copyNext = false;
                    }
                    if(line.contains("<tr>")) {
                        if(!firstTRSkipped) firstTRSkipped = true; else copyNext = true;
                    }
                    if(line.contains("</tbody>")) {
                        firstTRSkipped = false;
                        startCopying = false;
                    }
                } else {
                    if (line.contains("<a name=\"core\"></a>") || line.contains("<a name=\"pentium\"></a>") || line.contains("<a name=\"celeron\"></a>") || line.contains("<a name=\"atom\"></a>") || line.contains("<a name=\"legacy\"></a>")) {
                        startCopying = true;
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

        //AMD


        for(String str: devices) {
            System.out.println(str);
        }


    }

    public void getVersion() {
        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_38);
        webClient.getOptions().setRedirectEnabled(true);
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setAppletEnabled(false);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        for(String str: devices) {
            String strs[] = str.split(" ");
            String url = strs[0];
            String name = join(strs);
            Driver driver = new Driver(name, url);
            try {

                HtmlPage page = webClient.getPage(url);
                JavaScriptJobManager manager = page.getEnclosingWindow().getJobManager();
                while (manager.getJobCount() > 0) {
                    if(manager.getJobCount()<=6) {
                        manager.removeAllJobs();
                        manager.shutdown();
                    }
                    Thread.sleep(1000);
                }

                parseWebpage2(driver, page.getBody().asXml());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void parseWebpage2(Driver driver, String body) {
        System.out.print("Parsing " + driver.name + " " + driver.url);
        String str[] = body.split("\n");
        boolean save = false;
        for (int i = 0; i < str.length; i++) {
            if(save) {
                if(str[i].contains("a data-wap_ref")) {
                    String url = str[i].substring(str[i].indexOf("ng-href")+9, str[i].indexOf("\"", str[i].indexOf("ng-href")+9)).trim();
                    String name = str[i+1].trim();
                    String version = str[i+14].trim();
                    String os = str[i+20].trim().replace("®", "").replace("™", "");
                    driver.add(os, url, name, version);
                    drivers.add(driver);
                    i += 20;
                } else if(str[i].contains("container show-more-container show")) {
                    save = false;
                }
            } else {
                if(str[i].contains("<!-- ngRepeat: item in downloadResult | limitTo:10 -->")) save = true;
            }
        }
        System.out.println(" Found " + driver.downloads.size() + " drivers");
    }

    private String join(String[] strs) {
        String str = "";
        for (int i = 1; i < strs.length; i++) {
            str += strs[i] + " ";
        }
        return str;
    }

    private String removeEdition(String replace) {
        String[] strs = replace.split(" ");
        String result = "";
        for(String str: strs) {
            if(str.equals("Windows") || isInteger(str)) result += str + " ";
        }
        return result.substring(0, result.length()-1);
    }

    public static boolean isInteger(String s) {
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

    private String findDriver(String graphiccard, String os) {
        graphiccard = graphiccard.toLowerCase().replace("(r)", "");
        os = os.toLowerCase();
        if(graphiccard.replaceAll("\\s+","").equals("intelhdgraphics")) return "Search in http://www.intel.com/content/www/us/en/support/graphics-drivers/000005538.html";
        for(Driver drv: drivers) {
            if(drv.name.toLowerCase().contains(graphiccard) || graphiccard.contains(drv.name.toLowerCase())) {
                String result = "\u000304" + drv.url + "\u000f";
                for(String str: notes.keySet()) {
                    if(str.contains(drv.name.toLowerCase()) || drv.name.toLowerCase().contains(str) || str.contains(drv.url.toLowerCase()) || drv.url.toLowerCase().contains(str)) {
                        result += "\n\u000312" + notes.get(str) + "\u000F";
                    }
                }
                String exists = checkDrivers(drv, os);
                if(exists.equals("true")) {
                    for(Download down: drv.downloads) {
                        if (down.os.toLowerCase().contains(os.replace(" 32", "").replace(" 64", "")) && !down.url.toLowerCase().contains("inf") && !down.url.toLowerCase().contains("zip") && !down.version.toLowerCase().contains("previously released")) {
                            if (os.contains("32") && down.os.toLowerCase().contains("32") || os.contains("64") && down.os.toLowerCase().contains("64")) {
                                result += "\n\u000307" + down.version + "\u000f for \u000307" + down.os + "\u000f - \u0002" + down.url + "\u000f";
                                for(String str: notes.keySet()) {
                                    if(str.contains(down.name.toLowerCase()) || down.name.toLowerCase().contains(str) || str.contains(down.url.toLowerCase()) || down.url.toLowerCase().contains(str)) {
                                        result += "\n\u000312" + notes.get(str) + "\u000F";
                                    }
                                }
                            } else if ((!os.contains("64") || down.os.contains("64")) && (!os.contains("32") || down.os.contains("32"))) {
                                result += "\n\u000307" + down.version + "\u000f for \u000307" + down.os + "\u000f - \u0002" + down.url + "\u000f";
                                for(String str: notes.keySet()) {
                                    if(str.toLowerCase().contains(down.name.toLowerCase()) || down.name.toLowerCase().contains(str.toLowerCase()) || str.toLowerCase().contains(down.url.toLowerCase()) || down.url.toLowerCase().contains(str.toLowerCase())) {
                                        result += "\n\u000312" + notes.get(str) + "\u000F";
                                    }
                                }
                            }
                        }
                    }
                } else {
                    return "\n\u000307" + exists + "\u000F";
                }
                return result;
            }
        }
        return "Not found";
    }

    public static void received(String channel, String user, String login, String hostname, String message) {
        main._received(channel, user, login, hostname, message);
    }

    private void _received(String channel, String user, String login, String hostname, String message) {
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
                ircBot.disconnect();
                ircBot.dispose();
                System.exit(0);
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
        if (!command.equals("!add") && !command.equals("!getOS")) {
            driver = message.substring(message.indexOf(" ") + 1, message.indexOf("Windows") - 1);
            os = message.substring(message.indexOf("Windows"));
        }
        if(command.equals("!add") && !command.equals("!getOS")) {
            notes.put(message.substring(message.indexOf(" ")+1, message.indexOf(" Note ")-1), message.substring(message.indexOf(" Note ")+6));
        } else if(command.equals("!getDriver") || command.equals("!getDrivers")) {
            if (message.toLowerCase().contains("nvidia")) {
                sendMSG(channel, "GO TEAM GREEN!!\nSorry, not implemented yet");
            } else if (message.toLowerCase().contains("amd")) {
                sendMSG(channel, "GO TEAM RED!!\nSorry, not implemented yet");
            } else {
                sendMSG(channel, findDriver(driver, os));
            }
        } else if(command.equals("!getOS")) {
            String drv = message.substring(7).toLowerCase();
            System.out.println(drv);
            for(Driver d: drivers) {
                if(d.name.toLowerCase().contains(drv) || drv.contains(d.name.toLowerCase())) {
                    sendMSG(channel, "Latest is Windows "+checkDrivers(d) + " from Driver at " + d.url);
                    break;
                }
            }
        } else if(user.equals("PangeaBot") || user.equals("urielsalis")) {
            //<PangeaBot> (webrosc) n/a | n/a | Windows 7 Professional 64-bit | Enum\PCI\VEN_8086&DEV_29B2&SUBSYS_02111028&REV_02 (Intel(R) Q35 Express Chipset Family)
            if(message.contains("Graphics card")) {
                String str[] = message.split(", ");
                for(String stR: str) System.out.print(stR + "-");
                String graphics = null;
                if(str[1].contains("HD") || str[1].contains("Graphics")) graphics = str[1];
                if(str[2].contains("HD") || str[2].contains("Graphics")) graphics = str[2];
                if(str[3].contains("HD") || str[3].contains("Graphics")) graphics = str[3];
                if(graphics != null) {
                    String str2 = findDriver(graphics, tempOS);
                    if (!str2.equals("Not found")) sendMSG(channel, str2);
                }
            } else {
                String data[] = message.split("\\|");
                String os2 = removeEdition(data[2].replace("-bit", "")).replace(" 64", "").replace(" 32", "");
                if (data[2].contains("32")) os2 += " 32";
                else if (data[2].contains("64")) os2 += " 64";
                tempOS = os2;
                if(data[3].contains("not find card")) return;
                String tmp = data[3].replace("(R)", "");
                String graphics = filter(tmp.substring(tmp.indexOf("(") + 1, tmp.indexOf(")")).replace("(R)", "").replace("Family", "").replace("-Chipsatzfamilie", ""));
                if(graphics.contains("45 Express Chipset")) graphics = "4 Series";
                if(graphics.contains("/")) {
                    String words[] = graphics.split(" ");
                    StringBuilder builder = new StringBuilder();
                    for(String str: words) {
                        builder.append(str.contains("/") ? str.split("/")[0] + " " : str + " ");
                    }
                    graphics = builder.toString();
                }
                System.out.println(os2 + "-" + graphics);
                String str2 = findDriver(graphics, os2);
                System.out.println(str2);
                if (!str2.equals("Not found")) sendMSG(channel, str2);
            }
        }
    }

    private void save() {
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

    private String filter(String family) {
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

    private void sendMSG(String channel, String str) {
        String strs[] = str.split("\n");
        for(String str2: strs) ircBot.sendMessage(channel, str2);
    }

    public String checkDrivers(Driver driver, String os) {
        boolean bit64 = os.contains("64");
        String windows = os.split(" ")[1];
        String higher = "Too old";

        for(Download download: driver.downloads) {
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

