package org.oneedu.uikit.activites;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by dongseok0 on 20/03/15.
 */
public class BaseActivity extends ActionBarActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}
