package me.urielsalis.urielsalads;

import com.eclipsesource.json.JsonValue;
import com.ircclouds.irc.api.domain.messages.ChanJoinMessage;
import com.ircclouds.irc.api.domain.messages.ChannelPrivMsg;
import com.ircclouds.irc.api.listeners.VariousMessageListenerAdapter;
import me.urielsalis.urielsalads.Main;
import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.GistFile;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;

import java.io.IOException;
import java.util.Collections;

import static me.urielsalis.urielsalads.Main._api;

public class MessageListener extends VariousMessageListenerAdapter {
    public void onChannelMessage(ChannelPrivMsg aMsg) {
        System.out.println(aMsg.getText());
        if(!aMsg.getText().startsWith(".")) return;
        String[] command = aMsg.getText().split("\\s+");
        if(command[0].equals(".report")) {
            _api.message("urielsalis", "Report: " + "#" + aMsg.getChannelName() + ": <" + aMsg.getSource().getNick() + ">" + aMsg.getText());
        } else if(command[0].equals(".dx")) {
            String link = command[1];
            _api.message(aMsg.getChannelName(), IntelSearch.parseDxdiag(link));
        } else if(command[0].equals(".quit")) {
            _api.disconnect("Bai");
        } else if(command[0].equals(".getReport")) {
            GitHubClient client = new GitHubClient().setCredentials(Main.githubUser, Main.githubPass);
            Gist gist = new Gist().setDescription("Automated Report");
            GistFile file = new GistFile().setContent(Main.jsonObject.toString());
            gist.setFiles(Collections.singletonMap("report.json", file));
            try {
                gist = new GistService(client).createGist(gist);
                _api.message(aMsg.getChannelName(), gist.getHtmlUrl());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if(command[0].equals(".addHJT")) {
            String add = fullString(command);
            Main.hjt.add(add);
            Main.save();
            _api.message(aMsg.getChannelName(), IntelSearch.parseDxdiag(add + " added to database"));
        } else if(command[0].equals(".rmHJT")) {
            String compare = command[1].toLowerCase();
            for (int i = 0; i < Main.hjt.size(); i++) {
                String s = Main.hjt.get(i).asString();
                if(s.split("=")[0].toLowerCase().equals(compare)) {
                    Main.hjt.remove(i);
                    Main.save();
                    _api.message(aMsg.getChannelName(), IntelSearch.parseDxdiag(s + " removed!"));
                    return;
                }
            }
            _api.message(aMsg.getChannelName(), IntelSearch.parseDxdiag("Not found in hjt database!"));
        } else if(command[0].equals(".hjt")) {
            String link = command[1];
            _api.message(aMsg.getChannelName(), HJT.parse(link));
        }

    }

    private String fullString(String[] command) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < command.length; i++) {
            builder.append(" ").append(command[i]);
        }
        return builder.toString().substring(1);
    }

    public void onChannelJoin(ChanJoinMessage aMsg) {
        _api.message("NickServ", "identify "+Main.nickServPass);
    }

}
