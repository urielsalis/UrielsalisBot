package me.urielsalis.urielsalads;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;

/**
 * Created by urielsalis on 04/09/16.
 */
public class IntelSearch {
    public static String API_KEY = ""
    public static String lastDriver(GPU gpu) {
        if(gpu.isWindows81()) return "Not found. Latest: Windows 8.1";
        if(gpu.isWindows8()) return "Not found. Latest: Windows 8";
        if(gpu.isWindows7()) return "Not found. Latest: Windows 7";
        return "Not found. Latest below Windows 7";
    }

    public static String show(int windows10Download) {
        return "Driver: https://downloadcenter.intel.com/download/"+windows10Download;
    }

    public static String findCPU(String tmp, String os) {
        //ark.intel.com
        String[] strs = tmp.split("\\s+");
        String cpu = null;
        for(String str: strs) {
            if(Character.isLetter(str.charAt(0)) && Character.isDigit(str.charAt(1))) {
                cpu = str;
                break;
            }
        }
        try {
            if(cpu != null) {
                JsonObject json = Json.parse(IOUtils.toString(new URL("http://odata.intel.com/API/v1_0/Products/Processors()?api_key="+ API_KEY +"&$select=ProductId,CodeNameEPMId,GraphicsModel&$filter=substringof(%27" + cpu + "%27,ProductName)&$format=json"))).asObject();
                JsonObject cpuInfo = json.get("d").asArray().get(0).asObject();
                JsonValue graphics = cpuInfo.get("GraphicsModel");
                String graphicsModel = null;
                if(!graphics.isNull()) graphicsModel = graphics.asString();
                int codeNameEPMId = cpuInfo.get("CodeNameEPMId").asInt();
                if(graphicsModel != null && !Util.matches("Intel HD Graphics", graphicsModel)) {
                    for(Object tmp2: IntelDatabase.gpus) {
                        GPU gpu = (GPU) tmp2;
                        if(Util.matches(gpu.getName(), graphicsModel)) {
                            if(os==null) return "Os is null: Work it yourself you lazy! GPU: "+graphicsModel;
                            switch (os) {
                                case "10":
                                    if(gpu.isWindows10()) {
                                        return show(gpu.windows10Download);
                                    } else {
                                        return lastDriver(gpu);
                                    }
                                case "8.1":
                                    if(gpu.isWindows81()) {
                                        return show(gpu.windows81Download);
                                    } else {
                                        return lastDriver(gpu);
                                    }
                                case "8":
                                    if(gpu.isWindows8()) {
                                        return show(gpu.windows8Download);
                                    } else {
                                        return lastDriver(gpu);
                                    }
                                case "7":
                                    if(gpu.isWindows7()) {
                                        return show(gpu.windows7Download);
                                    } else {
                                        return lastDriver(gpu);
                                    }
                            }
                            break;
                        }
                    }
                } else {
                    //find codename
                    JsonObject json2 = Json.parse(IOUtils.toString(new URL("http://odata.intel.com/API/v1_0/Products/CodeNames()?api_key=" + API_KEY + "&$select=CodeNameText&$filter=CodeNameId%20eq%20" + codeNameEPMId + "&$format=json"))).asObject();
                    String codename = json2.get("d").asArray().get(0).asObject().get("CodeNameText").asString();
                    switch (codename) {
                        case "Arrandale":
                            if(os.equals("10") || os.equals("8.1") || os.equals("8")) return "Latest: Windows 7";
                            return "Find driver in https://downloadcenter.intel.com/product/81503 (Latest Windows 7)";
                        case "Bay Trail":
                            for(Object tmp2: IntelDatabase.gpus) {
                                GPU gpu = (GPU) tmp2;
                                if (gpu.getName().contains("2500")) {
                                    switch (os) {
                                        case "10":
                                            return show(gpu.windows10Download);
                                        case "8.1":
                                            return show(gpu.windows81Download);
                                        case "8":
                                            return show(gpu.windows8Download);
                                        case "7":
                                            return show(gpu.windows7Download);
                                    }
                                }
                            }
                        case "Clarkdale":
                            if(os.equals("10") || os.equals("8.1") || os.equals("8")) return "Latest: Windows 7";
                            return "Find driver in https://downloadcenter.intel.com/product/81503 (Latest Windows 7)";
                        case "Haswell":
                            for(Object tmp2: IntelDatabase.gpus) {
                                GPU gpu = (GPU) tmp2;
                                if (gpu.getName().contains("4600")) {
                                    switch (os) {
                                        case "10":
                                            return show(gpu.windows10Download);
                                        case "8.1":
                                            return show(gpu.windows81Download);
                                        case "8":
                                            return show(gpu.windows8Download);
                                        case "7":
                                            return show(gpu.windows7Download);
                                    }
                                }
                            }
                        case "Ivy Bridge":
                            if(os.equals("10")) return "Latest: Windows 10";
                            for(Object tmp2: IntelDatabase.gpus) {
                                GPU gpu = (GPU) tmp2;
                                if (gpu.getName().contains("2500")) {
                                    switch (os) {
                                        case "8.1":
                                            return show(gpu.windows81Download);
                                        case "8":
                                            return show(gpu.windows8Download);
                                        case "7":
                                            return show(gpu.windows7Download);
                                    }
                                }
                            }
                        case "Sandy Bridge":
                            if(os.equals("10")) return "Latest: Windows 10";
                            for(Object tmp2: IntelDatabase.gpus) {
                                GPU gpu = (GPU) tmp2;
                                if (gpu.getName().contains("2000")) {
                                    switch (os) {
                                        case "8.1":
                                            return show(gpu.windows81Download);
                                        case "8":
                                            return show(gpu.windows8Download);
                                        case "7":
                                            return show(gpu.windows7Download);
                                    }
                                }
                            }
                        case "Wolfdale":
                            return "CPU might not meet minimum requirements";
                        case "Penryn":
                            return "CPU might not meet minimum requirements";
                    }
                    return codename + " is not in the database. Probably too old(report with .report if not!";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Cant find in ark";

    }

    public static String parseDxdiag(String url) {
        try {
            String link = IOUtils.toString(new URL(url));
            String[] lines = link.split("\\R");
            String cpu = null;
            String name = null;
            String os = null;
            JsonObject tmp = new JsonObject();
            tmp.add("link", url);
            JsonArray cpus = new JsonArray();
            JsonArray gpus = new JsonArray();
            JsonArray oss = new JsonArray();
            boolean other = false;
            for(String str: lines) {
                if(str.trim().startsWith("Processor: ")) {
                    cpu = str.substring(str.lastIndexOf(":")+1).trim();
                    cpus.add(cpu);
                }
                if(str.trim().startsWith("Operating System")) {
                    os = str.substring(str.lastIndexOf(":")+1).trim().split("\\s+")[1];
                    oss.add(os);
                }
                if(str.trim().startsWith("Card name")) {
                    if (str.substring(str.lastIndexOf(":")+1).trim().contains("amd") || str.substring(str.lastIndexOf(":")+1).trim().contains("nvidia")) other=true;
                    if(str.substring(str.lastIndexOf(":")+1).trim().equals("Intel(R) HD Graphics Family")) continue;
                    name = str.substring(str.lastIndexOf(":")+1).trim();
                    gpus.add(name);
                }
                if(cpu != null && name !=null && os != null) break;
            }
            tmp.add("CPUs", cpus);
            tmp.add("GPUs", gpus);
            tmp.add("OSs", oss);
            if(name != null) {
                if(other) {
                    tmp.add("GPUInUse", "AMD or Nvidia");
                    Main.jsonObject.add(url, tmp);
                    Main.save();
                    return "AMD/Nvidia card present. Ignoring dxdiag";
                }
                tmp.add("GPUInUse", name);
                if (name.toLowerCase().contains("amd") || name.toLowerCase().contains("nvidia")) {
                    Main.jsonObject.add(url, tmp);
                    Main.save();
                    return "AMD/Nvidia card present. Ignoring dxdiag";
                }
            }
            if(Main.jsonObject.get(url) == null) {
                Main.jsonObject.add(url, tmp);
                Main.save();
            }
            if(name == null || name.isEmpty() || name.equals("Intel(R) HD Graphics Family") || name.contains("Microsoft Basic Display")) {
                if(cpu == null) return "No driver found";
                return findCPU(cpu, os);
            } else {
                //gpu
                for(Object tmp2: IntelDatabase.gpus) {
                    GPU gpu = (GPU) tmp2;
                    if(Util.matches(gpu.getName(), name)) {
                        switch (os) {
                            case "10":
                                if(gpu.isWindows10()) {
                                    return show(gpu.windows10Download);
                                } else {
                                    return lastDriver(gpu);
                                }
                            case "8.1":
                                if(gpu.isWindows81()) {
                                    return show(gpu.windows81Download);
                                } else {
                                    return lastDriver(gpu);
                                }
                            case "8":
                                if(gpu.isWindows8()) {
                                    return show(gpu.windows8Download);
                                } else {
                                    return lastDriver(gpu);
                                }
                            case "7":
                                if(gpu.isWindows7()) {
                                    return show(gpu.windows7Download);
                                } else {
                                    return lastDriver(gpu);
                                }
                        }
                        break;
                    }
                }
                return findCPU(cpu, os);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }

}
