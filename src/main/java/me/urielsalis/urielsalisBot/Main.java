package me.urielsalis.urielsalisBot;

import me.urielsalis.IRCApi.IRCApi;

public class Main {
    public static Main main;
    public static Bot bot;

    /**
     * Main method. Inits main and bot, then inits bot, does the main loop and calls cleanup
     *
     * @param args arguments passed via command line
     */
    public static void main(String[] args) {
        main = new Main();
        bot = new Bot();
        bot.init();
        while(bot.isRunning()) {
            bot.loop();
        }
        bot.cleanup();
    }
}
