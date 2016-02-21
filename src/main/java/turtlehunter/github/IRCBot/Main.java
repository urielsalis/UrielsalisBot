package turtlehunter.github.IRCBot;

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

/**
 * turtlehunter.github.IRCBot - uriel IRCBot 21/2/2016
 */

class Main
{
    private ArrayList<String> devices = new ArrayList<String>();
    private ArrayList<Driver> drivers = new ArrayList<Driver>();
    private IRCBot ircBot;
    private Kryo kryo;
    private static Main main;
    String channel = "#minecrafthelp.breakroom";
    private String tempOS;

    public static void main(String[] args)
    {
        main = new Main();
    }

    public Main() {
        ircBot = new IRCBot();
        try {
            ircBot.connect("irc.esper.net");
        } catch (IOException | IrcException e) {
            e.printStackTrace();
        }
        System.out.println("Connected: " + ircBot.isConnected());
        ircBot.joinChannel(channel);
        kryo = new Kryo();
        if(new File("save.bin").exists()) {
            try {
                Input input = new Input(new FileInputStream("save.bin"));
                drivers = kryo.readObject(input, ArrayList.class);
                input.close();
                ircBot.sendMessage(channel, "UrielsalisBot V1.0. Loaded from file");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            ircBot.sendMessage(channel, "UrielsalisBot V1.0. Downloading Intel database, this might(will) take a while");
            updateDatabase();
            getVersion();
            try {
                Output output = new Output(new FileOutputStream("save.bin"));
                kryo.writeObject(output, drivers);
                output.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
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

    private void updateDatabase() {
        URL url;
        InputStream is = null;
        BufferedReader br;
        String line;
        boolean startCopying = false;
        boolean firstTRSkipped = false;
        boolean copyNext = false;

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
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ioe) {
                // nothing to see here
            }
        }

        for(String str: devices) {
            System.out.println(str);
        }


    }

    public void getVersion() {

        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

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
        return strs[0] + " " + strs[1] + " " + strs[3];
    }

    private String findDriver(String graphiccard, String os) {
        for(Driver drv: drivers) {
            if(drv.name.contains(graphiccard) || graphiccard.contains(drv.name)) {
                String result = "\u000304" + drv.url + "\u000f";
                for(Download down: drv.downloads) {
                    if (down.os.contains(os.replace(" 32", "").replace(" 64", "")) && !down.version.contains("Previously Released")) {
                        if (os.contains("32") && down.os.contains("32") || os.contains("64") && down.os.contains("64")) {
                            result += "\n\u000307" + down.version + "\u000f for \u000307" + down.os + "\u000f - \u0002" + down.url + "\u000f";
                        } else if (os.contains("64") && !down.os.contains("64") || os.contains("32") && !down.os.contains("32")) {
                            //do nothing
                        } else {
                            result += "\n\u000307" + down.version + "\u000f for \u000307" + down.os + "\u000f - \u0002" + down.url + "\u000f";
                        }
                    }
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
        if(message.equals("!quit")) {
            System.out.println("Quit");
            ircBot.disconnect();
            ircBot.dispose();
            System.exit(0);
        } else if(message.equals("!cleardatabase")) {
            File file = new File("save.bin");
            file.delete();
            ircBot.disconnect();
            ircBot.dispose();
            System.exit(0);
        }
        String command = message.substring(0, message.indexOf(" "));
        String driver = message.substring(message.indexOf(" ")+1, message.indexOf("Windows")-1);
        String os = message.substring(message.indexOf("Windows"));
        if(command.equals("!getDriver") || command.equals("!getDrivers")) {
            sendMSG(channel, findDriver(driver, os));
        } else if(user.equals("PangeaBot") || user.equals("urielsalis")) {
            //<PangeaBot> (webrosc) n/a | n/a | Windows 7 Professional 64-bit | Enum\PCI\VEN_8086&DEV_29B2&SUBSYS_02111028&REV_02 (Intel(R) Q35 Express Chipset Family)
            if(message.contains("Graphics card")) {
                System.out.println(message);
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
                String data[] = message.split(" \\| ");
                String os2 = removeEdition(data[2].replace("-bit", "")).replace("64", "").replace("32", "");
                if (data[2].contains("32")) os2 += "32";
                else if (data[2].contains("64")) os2 += "64";
                tempOS = os2;
                String tmp = data[3].replace("(R)", "");
                String graphics = tmp.substring(tmp.indexOf("(") + 1, tmp.indexOf(")")).replace("(R)", "").replace("Family", "").replace("-Chipsatzfamilie", "");
                String str2 = findDriver(graphics, os2);
                if (!str2.equals("Not found")) sendMSG(channel, str2);
            }
        }
    }

    private void sendMSG(String channel, String str) {
        String strs[] = str.split("\n");
        for(String str2: strs) ircBot.sendMessage(channel, str2);
    }
}

