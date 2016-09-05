package me.urielsalis.urielsalads;

/**
 * Created by urielsalis on 19/08/16.
 */
public class GPU {
    private boolean windows7;
    private boolean windows8;
    private boolean windows81;
    private boolean windows10;
    private String name;
    public int windows7Download = 0;
    public int windows8Download = 0;
    public int windows81Download = 0;
    public int windows10Download = 0;


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

    public GPU(boolean windows7, boolean windows8, boolean windows81, boolean windows10, String name) {

        this.windows7 = windows7;
        this.windows8 = windows8;
        this.windows81 = windows81;
        this.windows10 = windows10;
        this.name = name;
    }

    public GPU(String name) {
        this.name = name;
    }

    public boolean allDownloadsFilled() {
        return !(windows7 && windows7Download == 0) && !(windows8 && windows8Download == 0) && !(windows81 && windows81Download == 0) && !(windows10 && windows10Download == 0);
    }

    public void close() {
        if (windows7Download == 0) windows7 = false;
        if (windows8Download == 0) windows8 = false;
        if (windows81Download == 0) windows81 = false;
        if (windows10Download == 0) windows10 = false;
    }
}
