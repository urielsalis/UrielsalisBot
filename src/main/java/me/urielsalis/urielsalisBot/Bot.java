package me.urielsalis.urielsalisBot;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import me.urielsalis.IRCApi.EventManager;
import me.urielsalis.IRCApi.IRCApi;
import me.urielsalis.IRCApi.events.OnPrivmsg;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bot {
    private static String tempOS;
    private boolean running = false;
    private IRCApi irc;
    private static Kryo kryo;
    private static ArrayList<Driver> drivers;
    private static HashMap<String, Driver> notes;
    private static HashMap<String, String> regex;

    public boolean isRunning() {
        return running;
    }

    public void init() {
        initIRC();
        Util.init(irc);
        loadSettings();
        login();
        loadDrivers();
    }

    public void loop() {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("> ");
            String s = br.readLine();
            if(s.equals("quit")) {
                running = false;
            }

            String first = s.split(" ")[0];
            String extra = s.replace(first + " ", "");

            if(s.startsWith("raw")) {
                irc.sendRaw(extra);
            } else {

                irc.send(first, extra);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {
    }

    private void initIRC() {
        irc = new IRCApi();

        new Thread() {
            @Override
            public void run() {
                try {
                    irc.init("irc.esper.net", "Urielsalads");
                    irc.run();
                } catch(Exception e) {
                    running = false;
                    e.printStackTrace();
                }
            }
        }.start();

        EventManager.commandPrefix = "!";

        System.out.println("Adding Listeners");
        EventManager.addClass(Listeners.class);

        try { //sleep to make sure we are connected
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Connected: " + irc.isConnected());
    }

    private void loadSettings() {
        File settings = new File("settings");
        if(!settings.exists()) {
            System.err.println("No settings file!");
            System.exit(-1);
        } else {
            try {
                System.out.println("Loaded settings");
                List<String> lines = Files.readAllLines(settings.toPath(), StandardCharsets.UTF_8);
                if(lines.size() < 3) {
                    System.err.println("Invalid settings file. First line nickserv user, second line nickserv password, next lines channels to join. Min one channel");
                    System.exit(-1);
                }
                Save.nickservUser = lines.get(0);
                Save.nickservPass = lines.get(1);

                for (int i = 2; i < lines.size(); i++) {
                    Save.channels.add(lines.get(i));
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void login() {
        irc.send("Nickserv", "identify " + Save.nickservUser + " " + Save.nickservPass);
        System.out.println("<NickServ> identify ****** ******");
    }

    private void loadDrivers() {
        kryo = new Kryo();
        if(new File("save.bin").exists()) {
            try {
                Input input = new Input(new FileInputStream("save.bin"));
                drivers = kryo.readObject(input, ArrayList.class);
                input.close();
                Input input1 = new Input(new FileInputStream("notes.bin"));
                notes = kryo.readObject(input1, HashMap.class);
                input1.close();
                Input input2 = new Input(new FileInputStream("regex.bin"));
                regex = kryo.readObject(input2, HashMap.class);
                input2.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            drivers = new ArrayList<>();
            notes = new HashMap<String, Driver>();
            regex = new HashMap<String, String>();
            Util.downloadIntel();
            save();
        }
    }

    public static void save() {
        try {
            Output output = new Output(new FileOutputStream("save.bin"));
            kryo.writeObject(output, drivers);
            output.close();
            Output output1 = new Output(new FileOutputStream("notes.bin"));
            kryo.writeObject(output1, notes);
            output1.close();
            Output output2 = new Output(new FileOutputStream("regex.bin"));
            kryo.writeObject(output2, regex);
            output2.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public static ArrayList<Driver> getDrivers() {
        return drivers;
    }

    public static void setDrivers(ArrayList<Driver> drivers) {
        Bot.drivers = drivers;
    }

    public static HashMap<String, Driver> getNotes() {
        return notes;
    }

    public static void setNotes(HashMap<String, Driver> notes) {
        Bot.notes = notes;
    }

    public static HashMap<String, String> getRegex() {
        return regex;
    }

    public static void setRegex(HashMap<String, String> regex) {
        Bot.regex = regex;
    }

    public static void addDriver(Driver driver) {
        drivers.add(driver);
    }

    public static void addNote(String regex, Driver driver) {
        notes.put(regex, driver);
    }

    public static void addRegex(String regex, String message) {
        Bot.regex.put(regex, message);
    }

    public static void message(OnPrivmsg e, String message) {
        for(Map.Entry<String, String> entry: Bot.getRegex().entrySet()) {
            if(message.matches(entry.getKey())) {
                Util.send(entry.getValue(), e);
            }
        }
        if(message.contains("(PCI\\VEN_8086")) {
            if(message.contains("Enum\\PCI\\VEN_8086")) {
                if (message.contains("Graphics card")) {
                    String str[] = message.split(", ");
                    for (String stR : str) System.out.print(stR + "-");
                    String graphics = null;
                    if (str[1].contains("HD") || str[1].contains("Graphics")) graphics = str[1];
                    if (str[2].contains("HD") || str[2].contains("Graphics")) graphics = str[2];
                    if (str[3].contains("HD") || str[3].contains("Graphics")) graphics = str[3];
                    if (graphics != null) {
                        String str2 = Util.findDriver(Util.format(graphics), tempOS);
                        if (!str2.equals("Not found")) Util.send(str2, e);
                    }
                }
                String graphics = message.substring(message.indexOf("Intel") - 1, message.indexOf("(PCI") - 1).trim();
                System.out.println(graphics);
                String str[] = message.split(", ");
                System.out.println(tempOS);
                System.out.println(Util.format(graphics));
                String str2 = Util.findDriver(Util.format(graphics), tempOS);
                if (!str2.equals("Not found")) Util.send(str2, e);
            } else {
                String data[] = message.split("\\|");
                String os2 = Util.removeEdition(data[2].replace("-bit", "")).replace(" 64", "").replace(" 32", "");
                if (data[2].contains("32")) os2 += " 32";
                else if (data[2].contains("64")) os2 += " 64";
                tempOS = os2;
                if (data[3].contains("not find card")) return;
                String tmp = data[3].replace("(R)", "");
                String graphics = Util.removeHTML(Util.format(tmp.substring(tmp.indexOf("(") + 1, tmp.indexOf(")")).replace("(R)", "").replace("Family", "").replace("-Chipsatzfamilie", "").replace("Familia", "")));
                graphics = Util.format(graphics);
                System.out.println(os2 + "-" + graphics);
                String str2 = Util.findDriver(graphics, os2);
                System.out.println(str2);
                if (!str2.equals("Not found")) Util.send(str2, e);
            }
        } else if(message.contains("Enum\\ROOT\\BASICDISPLAY") || message.contains("Microsoft Basic Display")) {
            String data[] = message.split("\\|");
            String os2 = Util.removeEdition(data[2].replace("-bit", "")).replace(" 64", "").replace(" 32", "");
            if (data[2].contains("32")) os2 += " 32";
            else if (data[2].contains("64")) os2 += " 64";
            tempOS = os2;
        } else if(message.startsWith(".dx")) {
            //get CPU
            Util.getCPU(message);
        }
    }
}
