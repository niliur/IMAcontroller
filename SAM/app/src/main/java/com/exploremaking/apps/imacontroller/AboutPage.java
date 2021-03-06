package com.exploremaking.apps.imacontroller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


/**
 * Created by David on 2016-02-06.
 */
public class AboutPage extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popupabout);

        String link = getString(R.string.website_address);
        final Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        Button quit = (Button) findViewById(R.id.quit_id);
        Button website = (Button) findViewById(R.id.website_id);

        quit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        website.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(browserIntent);
            }
        });
    }
}
