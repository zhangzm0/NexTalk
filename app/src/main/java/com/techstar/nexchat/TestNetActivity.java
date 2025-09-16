package com.techstar.nexchat;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.*;
import com.techstar.nexchat.api.ApiClient;

public class TestNetActivity extends AppCompatActivity {

    private EditText edit;
    private TextView text;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout lay = new LinearLayout(this);
        lay.setOrientation(LinearLayout.VERTICAL);

        edit = new EditText(this);
        Button btn = new Button(this);
        btn.setText("Send");
        text = new TextView(this);
        text.setTextIsSelectable(true);

        lay.addView(edit);
        lay.addView(btn);
        lay.addView(text);
        setContentView(lay);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { doSend(); }
        });
    }

    private void doSend() {
        final String q = edit.getText().toString().trim();
        new AsyncTask<Void, Void, String>() {
            @Override protected String doInBackground(Void... voids) {
                return new ApiClient(TestNetActivity.this).send(q);
            }
            @Override protected void onPostExecute(String r) {
                text.setText(r);
            }
        }.execute();
    }
}
