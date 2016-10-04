package me.urielsalis.urielsalads;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;

import static me.urielsalis.urielsalads.Main.APIKEY;

/**
 * Created by urielsalis on 04/09/16.
 */
public class IntelSearch {

    public static String lastDriver(GPU gpu) {
        if(gpu.isWindows1064()) return "Not found: Latest: Windows 10 64";
        if(gpu.isWindows8164()) return "Not found: Latest: Windows 8.1 64";
        if(gpu.isWindows864()) return "Not found: Latest: Windows 8 64";
        if(gpu.isWindows764()) return "Not found: Latest: Windows 7 64";
        if(gpu.isWindows10()) return "Not found: Latest: Windows 10 32";
        if(gpu.isWindows81()) return "Not found: Latest: Windows 8.1 32";
        if(gpu.isWindows8()) return "Not found: Latest: Windows 8 32";
        if(gpu.isWindows7()) return "Not found: Latest: Windows 7 32";
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
                JsonObject json = Json.parse(IOUtils.toString(new URL("http://odata.intel.com/API/v1_0/Products/Processors()?api_key=" + APIKEY + "&$select=ProductId,CodeNameEPMId,GraphicsModel&$filter=substringof(%27" + cpu + "%27,ProductName)&$format=json"))).asObject();
                JsonObject cpuInfo = json.get("d").asArray().get(0).asObject();
                JsonValue graphics = cpuInfo.get("GraphicsModel");
                String graphicsModel = null;
                if(!graphics.isNull()) graphicsModel = graphics.asString();
                int codeNameEPMId = cpuInfo.get("CodeNameEPMId").asInt();
                if(graphicsModel != null && !graphicsModel.equals("Intel® HD Graphics")) {
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
                    JsonObject json2 = Json.parse(IOUtils.toString(new URL("http://odata.intel.com/API/v1_0/Products/CodeNames()?api_key=" + APIKEY + "&$select=CodeNameText&$filter=CodeNameId%20eq%20" + codeNameEPMId + "&$format=json"))).asObject();
                    String codename = json2.get("d").asArray().get(0).asObject().get("CodeNameText").asString();
                    switch (codename) {
                        case "Braswell":
                            if(os.equals("8")) return "Not avaliable for Windows 8";
                            switch (os) {
                                case "10":
                                    return "64bit: " + show(25176) + "   32bit: " + show(25149);
                                case "7":
                                    return show(25235);
                                case "8.1":
                                    return show(25235);
                            }
                        case "Arrandale":
                            if(os.equals("10") || os.equals("8.1") || os.equals("8")) return "Latest: Windows 7";
                            return show(81503);
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
                            return show(81503);
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
            boolean is64 = false;
            boolean other = false;
            for(String str: lines) {
                if(str.trim().startsWith("Processor: ")) {
                    cpu = str.substring(str.lastIndexOf(":")+1).trim();
                    cpus.add(cpu);
                }
                if(str.trim().startsWith("Operating System")) {
                    is64 = str.contains("64");
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
            if(name == null || name.isEmpty() || name.equals("Intel(R) HD Graphics Family") || name.equals("Intel(R) HD Graphics")|| name.contains("Microsoft Basic Display")) {
                if(cpu == null) return "No driver found";
                return findCPU(cpu, os);
            } else {
                //gpu
                if(name.startsWith("Mobile Intel(R) 45 Express Chipset Family")) name="Mobile Intel(R) 4 Express Chipset Family";
                if(name.contains("/")) name = splitSlash(name);
                for(Object tmp2: IntelDatabase.gpus) {
                    GPU gpu = (GPU) tmp2;
                    if(Util.matches(gpu.getName(), name)) {
                        if(is64) {
                            switch (os) {
                                case "10":
                                    return gpu.isWindows1064() ? show(gpu.windows1064Download) : lastDriver(gpu);
                                case "8.1":
                                    return gpu.isWindows8164() ? show(gpu.windows8164Download) : lastDriver(gpu);
                                case "8":
                                    return gpu.isWindows864() ? show(gpu.windows864Download) : lastDriver(gpu);
                                case "7":
                                    return gpu.isWindows764() ? show(gpu.windows764Download) : lastDriver(gpu);
                            }
                        } else {
                            switch (os) {
                                case "10":
                                    return gpu.isWindows10() ? show(gpu.windows10Download) : lastDriver(gpu);
                                case "8.1":
                                    return gpu.isWindows81() ? show(gpu.windows81Download) : lastDriver(gpu);
                                case "8":
                                    return gpu.isWindows8() ? show(gpu.windows8Download) : lastDriver(gpu);
                                case "7":
                                    return gpu.isWindows7() ? show(gpu.windows7Download) : lastDriver(gpu);
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

    private static String splitSlash(String name) {
        return name.split("/")[0];
    }

}
