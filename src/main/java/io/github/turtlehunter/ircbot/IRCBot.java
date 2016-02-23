package io.github.turtlehunter.ircbot;

import org.jibble.pircbot.PircBot;

/**
 * turtlehunter.github.IRCBot - uriel IRCBot 21/2/2016
 */
public class IRCBot extends PircBot {
    public IRCBot() {
        this.setName("UrielsalisBot");
        this.setLogin("urielsalisbot");
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
        Main.received(channel, sender, login, hostname, message);
    }
}
