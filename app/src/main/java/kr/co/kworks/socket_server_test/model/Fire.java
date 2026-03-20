package kr.co.kworks.socket_server_test.model;

import androidx.annotation.NonNull;

public class Fire {
    public String id;
    public String datetime;

    @NonNull
    @Override
    public String toString() {
        return id + " / " + datetime;
    }
}
