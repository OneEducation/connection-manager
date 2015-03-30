package org.oneedu.uikit;

import android.app.Application;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by dongseok0 on 30/03/15.
 */
public class BaseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/VAG.ttf")
                        .setFontAttrId(org.oneedu.uikit.R.attr.fontPath)
                        .build()
        );
    }
}
