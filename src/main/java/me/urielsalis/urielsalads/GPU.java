package me.urielsalis.urielsalads;

/**
 * Created by urielsalis on 19/08/16.
 */
public class GPU {
    private boolean windows7;
    private boolean windows8;
    private boolean windows81;
    private boolean windows10;
    private boolean windows764;
    private boolean windows864;
    private boolean windows8164;
    private boolean windows1064;
    private String name;
    public int windows7Download = 0;
    public int windows8Download = 0;
    public int windows81Download = 0;
    public int windows10Download = 0;

    public int windows764Download = 0;
    public int windows864Download = 0;
    public int windows8164Download = 0;
    public int windows1064Download = 0;


    public boolean isWindows7() {
        return windows7;
    }

    public void setWindows7(boolean windows7) {
        this.windows7 = windows7;
    }

    public boolean isWindows8() {
        return windows8;
    }

    public void setWindows8(boolean windows8) {
        this.windows8 = windows8;
    }

    public boolean isWindows81() {
        return windows81;
    }

    public void setWindows81(boolean windows81) {
        this.windows81 = windows81;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isWindows10() {
        return windows10;
    }

    public void setWindows10(boolean windows10) {
        this.windows10 = windows10;
    }

    public GPU(boolean windows7, boolean windows8, boolean windows81, boolean windows10, boolean windows764, boolean windows864, boolean windows8164, boolean windows1064, String name, int windows7Download, int windows8Download, int windows81Download, int windows10Download, int windows764Download, int windows864Download, int windows8164Download, int windows1064Download) {
        this.windows7 = windows7;
        this.windows8 = windows8;
        this.windows81 = windows81;
        this.windows10 = windows10;
        this.windows764 = windows764;
        this.windows864 = windows864;
        this.windows8164 = windows8164;
        this.windows1064 = windows1064;
        this.name = name;
        this.windows7Download = windows7Download;
        this.windows8Download = windows8Download;
        this.windows81Download = windows81Download;
        this.windows10Download = windows10Download;
        this.windows764Download = windows764Download;
        this.windows864Download = windows864Download;
        this.windows8164Download = windows8164Download;
        this.windows1064Download = windows1064Download;
    }

    public GPU(String name) {
        this.name = name;
    }

    public boolean allDownloadsFilled() {
        return !(windows764 && windows764Download == 0) && !(windows864 && windows864Download == 0) && !(windows8164 && windows8164Download == 0) && !(windows1064 && windows1064Download == 0)     && !(windows764 && windows764Download == 0) && !(windows864 && windows864Download == 0) && !(windows8164 && windows8164Download == 0) && !(windows1064 && windows1064Download == 0);
    }

    public boolean isWindows764() {
        return windows764;
    }

    public void setWindows764(boolean windows764) {
        this.windows764 = windows764;
    }

    public boolean isWindows864() {
        return windows864;
    }

    public void setWindows864(boolean windows864) {
        this.windows864 = windows864;
    }

    public boolean isWindows8164() {
        return windows8164;
    }

    public void setWindows8164(boolean windows8164) {
        this.windows8164 = windows8164;
    }

    public boolean isWindows1064() {
        return windows1064;
    }

    public void setWindows1064(boolean windows1064) {
        this.windows1064 = windows1064;
    }

    public void close() {
        if (windows7Download == 0) windows7 = false;
        if (windows8Download == 0) windows8 = false;
        if (windows81Download == 0) windows81 = false;
        if (windows10Download == 0) windows10 = false;
        if (windows764Download == 0) windows764 = false;
        if (windows864Download == 0) windows864 = false;
        if (windows8164Download == 0) windows8164 = false;
        if (windows1064Download == 0) windows1064 = false;
    }
}
