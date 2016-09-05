package me.urielsalis.urielsalads;

import com.eclipsesource.json.JsonValue;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;

/**
 * Created by urielsalis on 05/09/16.
 */
public class HJT {
    public static String parse(String link) {
        try {
            String hjt = IOUtils.toString(new URL(link));
            StringBuilder builder = new StringBuilder();
            for(JsonValue temp: Main.hjt) {
                String compare = temp.asString().split("=")[0];
                if(hjt.toLowerCase().contains(compare.toLowerCase())) {
                    builder.append(", "+temp.asString().split("=")[1]);
                }
            }
            if(builder.toString().isEmpty()) return "Nothing :) (Im in beta. Please add things with .addHJT thingToMatch=thingToShow)";
            return "Found: "+builder.toString().substring(2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Nothing :) (Im in beta. Please add things with .addHJT thingToMatch=thingToShow)";
    }
}
