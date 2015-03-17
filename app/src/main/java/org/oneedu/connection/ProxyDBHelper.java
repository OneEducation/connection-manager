package org.oneedu.connection;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by dongseok0 on 17/03/15.
 */
public class ProxyDBHelper extends SQLiteOpenHelper {
    public static final String TABLE_PROXIES = "proxies";
    public static final String COLUMN_SSID = "ssid";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_HOST = "host";
    public static final String COLUMN_PORT = "port";

    private static final String DATABASE_NAME = "proxies.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_PROXIES + "("
            + COLUMN_SSID + " text primary key, "
            + COLUMN_USERNAME + " text not null, "
            + COLUMN_HOST + " text not null, "
            + COLUMN_PASSWORD + " text not null, "
            + COLUMN_PORT + " integer"
            +");";

    public ProxyDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }
}
