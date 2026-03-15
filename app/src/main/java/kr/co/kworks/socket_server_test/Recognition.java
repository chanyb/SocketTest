package kr.co.kworks.socket_server_test;

import androidx.annotation.NonNull;

import java.util.Locale;

public class Recognition {
    String id;
    String datetime;
    float x1, y1;
    float x2, y2;
    float accuracy;
    int type;

    public Recognition() {
        this.id = "";
        this.datetime = "";
        this.x1 = -1f;
        this.y1 = -1f;
        this.x2 = -1f;
        this.y2 = -1;
        accuracy = -1f;
        type = -1;
    }

    public Recognition(String id, String datetime, float x1, float y1, float x2, float y2, float accuracy, int type) {
        this.id = id;
        this.datetime = datetime;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.accuracy = accuracy;
        this.type = type;
    }

    @NonNull
    public String toString() {
        return String.format(Locale.KOREA, "Recog(%s,%s,%f,%f,%f,%f,%f,%d)", id, datetime, x1, y1, x2, y2, accuracy, type);
    }
}
