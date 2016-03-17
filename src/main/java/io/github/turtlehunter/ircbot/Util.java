package io.github.turtlehunter.ircbot;

/**
 * io.github.turtlehunter.ircbot - uriel bot 23/2/2016
 */
public class Util {
    public static String join(String[] strs) {
        String str = "";
        for (int i = 1; i < strs.length; i++) {
            str += strs[i] + " ";
        }
        return str.trim();
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
}
