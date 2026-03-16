package kr.co.kworks.socket_server_test.model;

import androidx.annotation.NonNull;

import java.util.Locale;

public class Ack {
    public String type;
    public String command;
    public String message;
    public String commandId;

    @NonNull
    public String toString() {
        return String.format(Locale.KOREA, "Ack(commandId=%s,command=%s,type=%s,message=%s)", commandId, command, type, message);
    }
}
