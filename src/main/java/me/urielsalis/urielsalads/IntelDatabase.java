package me.urielsalis.urielsalads;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class IntelDatabase {
    public static ArrayList<GPU> gpus = new ArrayList<>();

    public static void download() {
        try {
            Document document = Jsoup.connect("https://downloadcenter.intel.com/product/80939/Graphics-Drivers").get();
            Element search = document.body().getElementById("download-search");
            Element table = search.getElementById("product-selector").getElementsByClass("dropdown-menu").first();
            Elements items = table.select("li");
            for(Element element: items) {
                Element a = element.select("a").first();
                String driver = a.text().replace("Graphics Drivers for ", "");
                String url = a.attr("href");
                String epmID = url.split("/")[2];
                GPU gpu = getDriver(driver, epmID);
                if(gpu==null) continue;
                gpus.add(gpu);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static GPU getDriver(String driver, String epmID) {
        if(epmID.equals("0")) return null;
        try {
            URL url = new URL("https://downloadcenter.intel.com/json/pageresults?pageNumber=1&&productId=" + epmID);
            String json = IOUtils.toString(url);
            GPU gpu = new GPU(driver);

            JsonObject jsonObj = Json.parse(json).asObject();
            JsonArray os = jsonObj.get("OSFilter").asArray();
            for(JsonValue t: os) {
                JsonObject result = (JsonObject) t;
                if (result.get("Label").asString().contains("7")) gpu.setWindows7(true);
                else if (result.get("Label").asString().contains("8") && !result.get("Label").asString().contains("8.1")) gpu.setWindows8(true);
                else if (result.get("Label").asString().contains("8.1")) gpu.setWindows81(true);
                else if (result.get("Label").asString().contains("10")) gpu.setWindows10(true);
            }

            JsonArray results = jsonObj.get("ResultsForDisplay").asArray();
            for(JsonValue t: results) {
                JsonObject result = (JsonObject) t;
                JsonArray osSet = result.get("OperatingSystemSet").asArray();
                int download = result.get("Id").asInt();
                for(JsonValue t2: osSet) {
                    String osName = t2.asString();
                    if((osName.contains("7, 32-bit") || osName.contains("7 32")) && gpu.windows7Download==0 ) gpu.windows7Download = download;
                    if((osName.contains("8, 32-bit") || osName.contains("8 32")) && gpu.windows8Download==0 ) gpu.windows8Download = download;
                    if((osName.contains("8.1, 32-bit") || osName.contains("8.1 32")) && gpu.windows81Download==0 ) gpu.windows81Download = download;
                    if((osName.contains("10, 32-bit") || osName.contains("10 32")) && gpu.windows10Download==0 ) gpu.windows10Download = download;
                    if((osName.contains("7, 64-bit") || osName.contains("64 32")) && gpu.windows764Download==0 ) gpu.windows764Download = download;
                    if((osName.contains("8, 64-bit") || osName.contains("64 32")) && gpu.windows864Download==0 ) gpu.windows864Download = download;
                    if((osName.contains("8.1, 64-bit") || osName.contains("64 32")) && gpu.windows8164Download==0 ) gpu.windows8164Download = download;
                    if((osName.contains("10, 64-bit") || osName.contains("64 32")) && gpu.windows1064Download==0 ) gpu.windows1064Download = download;

                }
                if(gpu.allDownloadsFilled()) break;
            }

            if(epmID.equals("81507")) {
                gpu.windows8Download = 0;
                gpu.setWindows8(false);
            }

            return gpu;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
