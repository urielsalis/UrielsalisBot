package me.urielsalis.urielsalisBot;

import me.urielsalis.IRCApi.IRCApi;
import me.urielsalis.IRCApi.events.Event;
import me.urielsalis.IRCApi.events.OnPrivmsg;
import org.json.XML;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

public class Util {
    private static JSONParser parser;
    public static IRCApi irc;
    private static String cpu;

    public static void init(IRCApi irc) {
        parser = new JSONParser();
        Util.irc = irc;
    }

    /**
     * Gets all downloads for a given driver
     *
     * @param d
     * @return d with all its downloads
     */
    public static Driver driverDownloads(Driver d) {
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
     * Removes some ugly characters from intel names
     *
     * @param replace
     * @return replace with intel characters strip out
     */
    public static String removeHTML(String replace) {
        return replace.replace("&#174;", "").replace("™", "").replace("(TM)", "").replace("(tm)", "").replace("TM", "").replace("tm", "");
    }

    public static String removeEdition(String replace) {
        String[] strs = replace.split(" ");
        String result = "";
        for(String str: strs) {
            if(str.toLowerCase().equals("windows") || isInteger(str) || str.equals("8.1") || str.toLowerCase().equals("xp") || str.toLowerCase().equals("vista")) result += str + " ";
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

    public static boolean contains(String name, String query) {
        return format(name.toLowerCase()).contains(format(query.toLowerCase())) || format(query.toLowerCase()).contains(format(name));
    }

    private static String getEpmID(String url) {
        //http://ark.intel.com/products/43529/Intel-Core-i3-350M-Processor-3M-Cache-2_26-GHz
        String[] s = url.replace("http://", "").replace("https://", "").split("/");
        System.out.println(s[2]);
        return s[2];
    }

    public static String format(String graphiccard) {
        graphiccard = removeHTML(graphiccard);
        graphiccard = graphiccard.replace("(R)", "").replace("(r)", "").trim();
        if (graphiccard.contains("45 Express Chipset")) graphiccard = "4 Series";
        if (graphiccard.contains("/")) {
            String words[] = graphiccard.split(" ");
            StringBuilder builder = new StringBuilder();
            for (String str : words) {
                builder.append(str.contains("/") ? str.split("/")[0] + " " : str + " ");
            }
            graphiccard = builder.toString();
        }
        graphiccard = graphiccard.replaceAll("[^\\u0000-\\uFFFF]", "");
        if(graphiccard.contains("for"))
            graphiccard = graphiccard.substring(0, graphiccard.indexOf("for")-1);

        return graphiccard;
    }


    @SuppressWarnings("Since15")
    public static void getCPU(String message) {
        try {

            URL url = new URL(message.replace(".dx ", "").trim());
            InputStream is = url.openStream();  // throws an IOException
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                if(line.contains("Processor: ")) {
                    String tmp = line.replace("Processor: ", "").trim();
                    String[] s = tmp.split(" ");
                    tmp = String.join(" ", Arrays.copyOfRange(s, 2, s.length));
                    cpu = tmp.substring(0, tmp.indexOf("@")-1).trim();
                    System.out.println(cpu);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void downloadIntel() {
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
                        String driverName = Util.removeHTML(s.substring(s.indexOf("\">")+2, s.indexOf("</a>")).replace("</a></li>", "")).trim();
                        Driver d = new Driver(driverName, driverUrl);
                        Driver d2 = Util.driverDownloads(d);
                        Bot.addDriver(d2);

                    } else if(line.contains("</ul>")) {
                        break;
                    }
                }
            }
            System.out.println("Done " + Bot.getDrivers().size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Driver getDriver(String s) {
        for(Driver d: Bot.getDrivers()) {
            if(d.equals(s)) return d;
        }
        return null;
    }

    public static String findOS(String query) {
        for(Driver d: Bot.getDrivers()) {
            if(contains(d.name, query)) {
                return ChatFormat.DARK_GREEN + d.url + ChatFormat.NORMAL +"\n" + ChatFormat.YELLOW + checkDrivers(d) + ChatFormat.NORMAL;
            }
        }
        return "Not found";

    }

    public static String checkDrivers(Driver driver, String os) {
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

    public static String driverFor(String s, String os) {
        String result = "";
        int count = 0;
        String windows = os.split(" ")[1];
        boolean showedExe = false;
        for(Driver d: Bot.getDrivers()) {
            if(d.url.contains(s)) {
                String exists = checkDrivers(d, removeEdition(os));
                result += ChatFormat.DARK_BLUE + d.url + ChatFormat.NORMAL + " - " + ChatFormat.RED + d.name + ChatFormat.NORMAL;
                if (exists.equals("true")) {
                    boolean drivbit64 = os.contains("64");
                    for (Download download : d.downloads) {
                        boolean bit64 = download.os.contains("64");
                        if (!download.name.toLowerCase().contains("inf") && (!showedExe || !download.url.toLowerCase().contains("zip"))) {
                            if (download.url.toLowerCase().contains("exe")) showedExe = true;
                            if (contains(windows, download.os)) {
                                if (bit64 && drivbit64 || !bit64 && !drivbit64) {
                                    if (count <= 1) {
                                        result += "\n" + ChatFormat.RED + download.name + ChatFormat.NORMAL + " for " + ChatFormat.BLUE + download.os + ChatFormat.NORMAL + " " + ChatFormat.LIGHT_GRAY + download.version + ChatFormat.NORMAL + " " + ChatFormat.YELLOW + download.url + ChatFormat.NORMAL;
                                        count++;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    return exists;
                }
            }
        }
        if(result.isEmpty()) {
            return "Error: get from https://downloadcenter.intel.com/product/" + s;
        } else {
            return result;
        }
    }

    public static String findDriver(String driver, String os) {
        driver = driver.trim();
        if(driver.equals("Intel HD Graphics") || driver.equals("Microsoft Basic Display Adapter")) {
            //ark.intel.com
            try {
                String google = "http://www.google.com/search?q=";
                String search = URLEncoder.encode(cpu, "UTF-8");
                String charset = "UTF-8";
                String userAgent = "UrielsalisBot 1.0 (+github.com/turtlehunter/UrielsalisBot) uriel@urielsalis.me"; // Change this to your company's name and bot homepage!

                Elements links = Jsoup.connect(google + URLEncoder.encode(search, charset)).userAgent(userAgent).get().select(".g>.r>a");

                String url = null;

                for (Element link : links) {
                    String title = link.text();
                    url = link.absUrl("href"); // Google returns URLs in format "http://www.google.com/url?q=<url>&sa=U&ei=<someKey>".
                    url = URLDecoder.decode(url.substring(url.indexOf('=') + 1, url.indexOf('&')), "UTF-8");

                    if (!url.startsWith("http")) {
                        continue; // Ads/news/etc.
                    }

                    System.out.println("URL: " + url);
                    if(url.contains("ark.intel.com")) {
                        break;
                    }
                }

                //TODO Broken

                if(url != null && url.contains("ark.intel.com")) {
                    url = url.replace("/es", "");
                    URL url2 = new URL("http://ark.intel.com/compare/" +getEpmID(url) + "?e=t");
                    String graphicscard = "";
                    String family = "";
                    Scanner scanner = new Scanner(url2.openStream(), "UTF-8");
                    scanner.useDelimiter("\\A");
                    String xmldata = scanner.next();
                    scanner.close();

                    org.json.JSONObject soapDatainJsonObject = XML.toJSONObject(xmldata);
                    String json = soapDatainJsonObject.toString().replace("ss:", "");
                    try {
                        JSONObject object = (JSONObject) parser.parse(json);
                        JSONObject workbook = (JSONObject) (object.get("Workbook"));
                        JSONObject workSheet = (JSONObject) workbook.get("Worksheet");
                        JSONObject table = (JSONObject) workSheet.get("Table");
                        JSONArray rows = (JSONArray) table.get("Row");
                        for(Object t: rows) {
                            if(!graphicscard.isEmpty() && !graphicscard.isEmpty()) break;
                            JSONObject row = (JSONObject) t;
                            if(row.get("Cell") instanceof JSONArray) {
                                JSONArray cell = (JSONArray) row.get("Cell");
                                boolean copyNext = false;
                                boolean codename = false;
                                for(Object o: cell) {
                                    if(o instanceof JSONObject) {
                                        JSONObject r = (JSONObject) o;
                                        if (r.containsKey("Data")) {
                                            JSONObject data = (JSONObject) r.get("Data");
                                            if (data.containsKey("content")) {
                                                if(data.get("content") instanceof String) {
                                                    String content = (String) data.get("content");
                                                    if (content.equals("Code Name")) {
                                                        copyNext = true;
                                                        codename = true;
                                                    } else if (content.equals("Processor Graphics \u2021")) {
                                                        copyNext = true;
                                                    } else if (copyNext) {
                                                        if (codename) {
                                                            family = content;
                                                            codename = false;
                                                        } else {
                                                            graphicscard = content;
                                                        }
                                                        copyNext = false;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    System.out.println(family);
                    System.out.println(format(removeHTML(graphicscard)));

                    if(graphicscard.isEmpty() || format(removeHTML(graphicscard)).equals("Intel HD Graphics")) {
                        switch (family) {
                            case "Arrandale":
                                return "Ark: " + url + "\n" + driverFor("81503", os);
                            case "Bay Trail":
                                return "Ark: " + url + "\n" + findDriver("Intel HD Graphics 2500", os);
                            case "Clarkdale":
                                return "Ark: " + url + "\n" + driverFor("81503", os);
                            case "Haswell":
                                return "Ark: " + url + "\n" + findDriver("Intel HD Graphics 4200", os);
                            case "Ivy Bridge":
                                return "Ark: " + url + "\n" + findDriver("Intel HD Graphics 2500", os);
                            case "Sandy Bridge":
                                return "Ark: " + url + "\n" + findDriver("Intel HD Graphics 3000", os);
                            default:
                                return "Ark: " + url + "\n" + "no drivers found for family " + family;

                        }
                    } else {
                        return "Ark: " + url + "\n" + findDriver(format(removeHTML(graphicscard)), os);
                    }

                }


            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                return "Rate-limited, try again in 10 seconds";
            }
        } else {
            os = os.trim();
            String result = "";
            int count = 0;
            String windows = os.split(" ")[1];
            boolean showedExe = false;
            for (Driver d : Bot.getDrivers()) {
                if (contains(d.name, driver)) {
                    String exists = checkDrivers(d, removeEdition(os));
                    result += ChatFormat.DARK_BLUE + d.url + ChatFormat.NORMAL + " - " + ChatFormat.RED + d.name + ChatFormat.NORMAL;
                    if (exists.equals("true")) {
                        boolean drivbit64 = os.contains("64");
                        for (Download download : d.downloads) {
                            boolean bit64 = download.os.contains("64");
                            if (!download.name.toLowerCase().contains("inf")) {
                                if (!showedExe || !download.url.toLowerCase().contains("zip")) {
                                    if (download.url.toLowerCase().contains("exe")) showedExe = true;
                                    if (contains(windows, download.os)) {
                                        if (bit64 && drivbit64) {
                                            if (count <= 1) {
                                                result += "\n" + ChatFormat.RED + download.name + ChatFormat.NORMAL + " for " + ChatFormat.BLUE + download.os + ChatFormat.NORMAL + " " + ChatFormat.LIGHT_GRAY + download.version + ChatFormat.NORMAL + " " + ChatFormat.YELLOW + download.url + ChatFormat.NORMAL;
                                                count++;
                                            } else {
                                                break;
                                            }
                                        } else if (!bit64 && !drivbit64) {
                                            if (count <= 1) {
                                                result += "\n" + ChatFormat.RED + download.name + ChatFormat.NORMAL + " for " + ChatFormat.BLUE + download.os + ChatFormat.NORMAL + " " + ChatFormat.LIGHT_GRAY + download.version + ChatFormat.NORMAL + " " +  ChatFormat.YELLOW + download.url + ChatFormat.NORMAL;
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
        return "Error";
    }

}
