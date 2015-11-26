package org.oneedu.connection.data;

import android.database.Cursor;

/**
 * Created by dongseok0 on 17/03/15.
 */
public class Proxy {
    private String ssid;
    private String host;
    private int port;
    private String username;
    private String password;
    private int status;
    private String pac_url;

    public Proxy(String ssid, String host, int port, String username, String password) {
        this.ssid = ssid;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.status = 0;
    }

    public Proxy(String ssid, String pac_url, String username, String password) {
        this.ssid = ssid;
        this.pac_url = pac_url;
        this.username = username;
        this.password = password;
        this.status = 0;
    }

    public Proxy(Cursor cursor) {
        this.ssid = cursor.getString(0);
        this.host = cursor.getString(1);
        this.port = cursor.getInt(2);
        this.username = cursor.getString(3);
        this.password = cursor.getString(4);
        this.status = cursor.getInt(5);
        this.pac_url = cursor.getString(6);
    }

    public String getSsid() {
        return ssid;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public int getStatus() {
        return status;
    }

    public String getPacUrl() {
        return pac_url;
    }

    public String toString() {
        return ssid + " | " + host + " | " + port + " | " + username + " | " + password;
    }
}
