package com.keepassdroid.provider;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import com.android.keepass.R;

/**
 * @author pfn
 */
public class AuthzActivity extends Activity {
    private String token;
    private int uid;
    private Authz authz;
    private boolean finished = false;

    private boolean broadcastSent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authz);

        final TextView timer = (TextView) findViewById(R.id.timer);
        final Handler handler = new Handler();
        final long finishTime =
                System.currentTimeMillis() + DatabaseProvider.AUTHZ_TIMEOUT;
        final Runnable finish = new Runnable() {
            @Override
            public void run() {
                if (!finished)
                    finish();
            }
        };

        final Runnable timerUpdate = new Runnable() {
            @Override
            public void run() {
                long remaining = finishTime - System.currentTimeMillis();
                timer.setText(String.valueOf(remaining / 1000));
                if (!finished)
                    handler.postDelayed(this, 500);
            }
        };
        timerUpdate.run();

        handler.postDelayed(finish, DatabaseProvider.AUTHZ_TIMEOUT);

        uid = getIntent().getIntExtra(DatabaseProvider.EXTRA_UID, 0);
        token = getIntent().getStringExtra(DatabaseProvider.EXTRA_TOKEN);
        final AuthzDatabaseHelper dbHelper = new AuthzDatabaseHelper(this);
        authz = dbHelper.getByUid(uid);

        authz.lastAuthz = System.currentTimeMillis();
        // update signatures field
        authz.checkSignatures(this);
        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.always:
                        authz.remember = true;
                    case R.id.allow:
                        authz.authz = true;
                        break;
                    case R.id.deny:
                        authz.remember = false;
                        authz.authz = false;
                        break;
                }
                dbHelper.save(authz);
                broadcastResult();
                handler.removeCallbacks(finish);
                handler.removeCallbacks(timerUpdate);
                finish();
            }
        };

        findViewById(R.id.allow).setOnClickListener(l);
        findViewById(R.id.deny).setOnClickListener(l);
        findViewById(R.id.always).setOnClickListener(l);
        ((TextView)findViewById(R.id.applications)).setText(authz.appNames);
    }

    private void broadcastResult() {
        Intent i = new Intent(authz.authz ?
                DatabaseProvider.ACTION_AUTHZ_SUCCESS :
                DatabaseProvider.ACTION_AUTHZ_FAIL);
        i.putExtra(DatabaseProvider.EXTRA_TOKEN, token);
        sendBroadcast(i);
    }

    @Override
    protected void onPause() {
        super.onPause();
        finished = true;
        if (!broadcastSent) { // outside click results in a denial
            broadcastResult();
        }
    }
}
