package kr.co.kworks.socket_server_test.model;

import java.util.UUID;

import kr.co.kworks.socket_server_test.CalendarHandler;

public class WeatherSensor {
    public String id;
    public String datetime;
    public float airtempAvg;
    public float wsRunAvg;
    public float wdRunAvg;
    public float wdGust;
    public float wsGust;
    public float airPressureAvg;
    public float rhAvg;
    public float rhRunAvg;

    private transient CalendarHandler calendarHandler;

    public WeatherSensor() {
        calendarHandler = new CalendarHandler();
        id = String.valueOf(UUID.randomUUID());
        datetime = calendarHandler.getCurrentDatetimeString();
        airtempAvg = 22.0f;
        wsRunAvg = 0f;
        wdRunAvg = 0f;
        wdGust = 0f;
        wsGust = 0f;
        airPressureAvg = 0f;
        rhAvg = 0f;
        rhRunAvg = 0f;
    }
}
