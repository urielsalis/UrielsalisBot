package me.urielsalis.urielsalads;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.ircclouds.irc.api.Callback;
import com.ircclouds.irc.api.IRCApi;
import com.ircclouds.irc.api.IRCApiImpl;
import com.ircclouds.irc.api.IServerParameters;
import com.ircclouds.irc.api.domain.IRCServer;
import com.ircclouds.irc.api.state.IIRCState;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by urielsalis on 04/09/16.
 */
public class Main {
    public static IRCApi _api;
    private static final Logger logger = LogManager.getLogger("UrielsalisBOT");
    public static JsonObject jsonObject = new JsonObject();
    public static JsonArray hjt = new JsonArray();
    public static Properties prop = new Properties();

    public static String githubUser;
    public static String githubPass;
    public static String nickServPass;

    public static void main(String[] args) {
        BasicConfigurator.configure();
        loadProperties();
        loadResults();
        loadHJT();

        init();
    }

    private static void loadProperties() {
        try {
            FileInputStream input = new FileInputStream("config.properties");
            prop.load(input);
            input.close();
            githubUser = prop.getProperty("githubUser");
            githubPass = prop.getProperty("githubPass");
            nickServPass = prop.getProperty("nickServPass");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadResults() {
        File json = new File("results.json");
        if(json.exists()) {
            try {
                jsonObject = Json.parse(new FileReader(json)).asObject();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void loadHJT() {
        File json = new File("hjt.json");
        if(json.exists()) {
            try {
                hjt = Json.parse(new FileReader(json)).asArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void init() {
        IntelDatabase.download();
        _api = new IRCApiImpl(true);
        String host = prop.getProperty("host");
        String server = prop.getProperty("server");
        String[] nicks = prop.getProperty("nicks").split(",");

        _api.connect(getServerParams(nicks[0], getList(nicks), nicks[0], host, server, false), new Callback<IIRCState>() {
            public void onSuccess(final IIRCState aIRCState) {
                run();
            }

            public void onFailure(Exception aErrorMessage) {
                throw new RuntimeException(aErrorMessage);
            }
        });
    }

    private static List<String> getList(String[] nicks) {
        ArrayList<String> strs = new ArrayList<>();
        for (int i = 1; i < nicks.length; i++) {
            strs.add(nicks[i]);
        }
        return strs;
    }

    private static void run() {
        for(String channel: prop.getProperty("channels").split(",")) {
            _api.joinChannel(channel);
        }
        _api.addListener(new MessageListener());
    }

    private static IServerParameters getServerParams(final String aNickname, final List<String> aAlternativeNicks, final String aRealname, final String aIdent,
                                                     final String aServerName, final Boolean aIsSSLServer) {
        return new IServerParameters() {
            public IRCServer getServer()
            {
                return new IRCServer(aServerName, aIsSSLServer);
            }

            public String getRealname()
            {
                return aRealname;
            }

            public String getNickname()
            {
                return aNickname;
            }

            public String getIdent()
            {
                return aIdent;
            }

            public List<String> getAlternativeNicknames()
            {
                return aAlternativeNicks;
            }
        };
    }

    public static void save() {
        try {
            IOUtils.write(jsonObject.toString(), new FileOutputStream("results.json"));
            IOUtils.write(hjt.toString(), new FileOutputStream("hjt.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
