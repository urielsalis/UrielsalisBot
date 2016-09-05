package me.urielsalis.urielsalads;

/**
 * Created by urielsalis on 04/09/16.
 */
public class Util {


    public static boolean matches(String name, String name1) {
        if(name==null || name1==null) return false;
        name = separateSlash(name.toLowerCase().replace("™", "").replace("(r)", "").replace("®", "").replace("intel", "").replace("express", "").replace("chipset", "").replace("with", "").replace("hd", "").replace("graphics", "").replace("family", "").replace("processor", "").trim());
        name1 = separateSlash(name1.toLowerCase().replace("™", "").replace("(r)", "").replace("®", "").replace("intel", "").replace("express", "").replace("chipset", "").replace("with", "").replace("hd", "").replace("graphics", "").replace("family", "").replace("processor", "").trim());
        return name.contains(name1) || name1.contains(name) || name.equals(name1);
    }

    private static String separateSlash(String trim) {
        if(trim.contains("(")) {
            trim = trim.substring(0, trim.indexOf("(")-1);
        }
        if(trim.contains("/")) {
            String[] strs = trim.split("\\s+");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < strs.length; i++) {
                if(strs[i].contains("/")) {
                    builder.append(strs[i].split("/")[0]+ " ");
                } else {
                    builder.append(strs[i] + " ");
                }
            }
            return builder.toString();
        } else {
            return trim;
        }
    }
}
