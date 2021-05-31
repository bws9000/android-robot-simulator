package com.newjerseysoftware.robotsimulator;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {

    private ArrayList<Button> buttons = new ArrayList<Button>();
    private OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    private String networkAddress = "";

    private void webSocketConnect() {
        RobotWebSocketListener listen = new RobotWebSocketListener();
        Request request = new Request.Builder()
                .url("ws://"+networkAddress+":2867")
                .header("User-Agent", "OkHttp Headers.java")
                .addHeader("Accept", "application/json; q=0.5")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .build();

        WebSocket ws = client.newWebSocket(request, listen);
        client.dispatcher().executorService().shutdown();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            networkAddress = env("home.network.ip",this.getBaseContext());
        } catch (IOException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_main);
        Button start = (Button) findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webSocketConnect();
            }
        });

        LinearLayout layout = new LinearLayout(this);
        LinearLayout.LayoutParams match_parent = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        layout.setBackgroundColor(getResources().getColor(R.color.black));
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setLayoutParams(match_parent);

        TableLayout tableLayout = findViewById(R.id.tableLayout1);
        int count=0;
        for (int i = 0; i <5; i++) {
            TableRow row= new TableRow(this);
            TableRow.LayoutParams tableRowParams = new TableRow.LayoutParams(143, 143);
            tableRowParams.weight = .100f;
            row.setWeightSum(.20f);
            row.setLayoutParams(tableRowParams);
            for(int j = 0 ; j <5; j++) {
                count++;
                Button btn = new Button(this);
                btn.setId(count);
                btn.setBackgroundResource(R.drawable.button);
                LinearLayout.LayoutParams btnLayoutParams = new LinearLayout.LayoutParams(0,0);
                btn.setLayoutParams(btnLayoutParams);
                buttons.add(btn);
                btn.setOnClickListener(handleOnClick(btn, count));
                row.addView(btn, tableRowParams);
            }
            tableLayout.addView(row,i);
        }

        ViewGroup group = findViewById(R.id.main_parent);
        group.addView(layout);

    }

    View.OnClickListener handleOnClick(final Button button, int count) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                for(int i=0;i<buttons.size();i++){
                    Button b = buttons.get(i);
                    b.setBackgroundColor(getResources().getColor(R.color.black));
                    b.setBackgroundResource(R.drawable.button);
                }
                button.setBackgroundColor(getResources().getColor(R.color.purple_700));
            }
        };
    }

    //https://developer.mozilla.org/en-US/docs/Web/API/CloseEvent#status_codes
    private final static class RobotWebSocketListener extends WebSocketListener {
        //{"active":true,"message":"{\"message\":\"hello from verizon carrier ip\"}","activeArea":8,"Position":{"x":"2","y":"1"},"gridBlock":0,"bounds":4}
        @Override
        public void onOpen(WebSocket ws, Response res) {
            ws.send("hello from Android - Feed me");

//            new Timer().scheduleAtFixedRate(new TimerTask(){
//                @Override
//                public void run(){
//                    ws.send("hello from Android - Feed me");
//                }
//            },0,5000);
//          ws.close(1000, "ws connection closed");
        }

        @Override
        public void onMessage(WebSocket ws, String txt) {
            Log.i("onMessageText: " , txt);
        }
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            Log.i("onMessageBytes: " , String.valueOf(bytes));
        }
        @Override
        public void onClosing(WebSocket ws, int code, String reason) {
            Log.i("ws closing:",
                    "no more messages incoming: code:"+code+" reason:"+reason);
            ws.close(code,null);
        }
        @Override
        public void onClosed(WebSocket ws, int code, String reason) {
            if(ws == null) {
                Log.i("ws closed: code:", code + " reason: " + reason);
            }
        }
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response resp) {
            Log.i("ws failure: " , t.getMessage() + " response:"+resp.toString());
        }
    }

    private static String env(String key, Context context) throws IOException {
        try {
            Properties props = new Properties();
            AssetManager am = context.getAssets();
            InputStream is = am.open("app.properties");
            props.load(is);
            return props.getProperty(key);
        }catch (IOException ex){
            ex.fillInStackTrace();
        }
        return null;
    }
}