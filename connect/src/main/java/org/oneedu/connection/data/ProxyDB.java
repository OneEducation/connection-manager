package org.oneedu.connection.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;

/**
 * Created by dongseok0 on 17/03/15.
 */
public class ProxyDB {

    private SQLiteDatabase database;
    private ProxyDBHelper dbHelper;
    private String[] allColumns = {
            ProxyDBHelper.COLUMN_SSID,
            ProxyDBHelper.COLUMN_HOST,
            ProxyDBHelper.COLUMN_PORT,
            ProxyDBHelper.COLUMN_USERNAME,
            ProxyDBHelper.COLUMN_PASSWORD,
            ProxyDBHelper.COLUMN_STATUS
    };
    private static ProxyDB mInstance;

    public static ProxyDB getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new ProxyDB(context);
            try {
                mInstance.open();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return mInstance;
    }

    private ProxyDB(Context context) {
        dbHelper = new ProxyDBHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public void addOrUpdateProxy(String ssid, String host, int port, String username, String password) {
        ContentValues values = new ContentValues();
        values.put(ProxyDBHelper.COLUMN_SSID, ssid);
        values.put(ProxyDBHelper.COLUMN_HOST, host);
        values.put(ProxyDBHelper.COLUMN_PORT, port);
        values.put(ProxyDBHelper.COLUMN_USERNAME, username);
        values.put(ProxyDBHelper.COLUMN_PASSWORD, password);
        values.put(ProxyDBHelper.COLUMN_STATUS, 0);
        
        database.insertWithOnConflict(ProxyDBHelper.TABLE_PROXIES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Proxy getProxy(String ssid) {
        Proxy res = null;
        Cursor cursor = database.query(ProxyDBHelper.TABLE_PROXIES, allColumns, ProxyDBHelper.COLUMN_SSID + " = " + DatabaseUtils.sqlEscapeString(ssid), null, null, null, null);
        if (cursor.moveToFirst()) {
            res = new Proxy(cursor);
        }
        cursor.close();

        return res;
    }

    public void deleteProxy(String ssid) {
        database.delete(ProxyDBHelper.TABLE_PROXIES, ProxyDBHelper.COLUMN_SSID + " = '" + ssid + "'", null);
    }

    public void updateInternetConnectStatus(String ssid, int status) {
        ContentValues values = new ContentValues();
        values.put(ProxyDBHelper.COLUMN_STATUS, status);
        database.update(ProxyDBHelper.TABLE_PROXIES, values, ProxyDBHelper.COLUMN_SSID + " = " + DatabaseUtils.sqlEscapeString(ssid), null);
    }

}
