package com.newjerseysoftware.robotsimulator;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * Created by Burt Snyder on 6/3/21.
 */
public class MainActivity extends AppCompatActivity {

    private ArrayList<Button> buttons = new ArrayList<Button>();
    private OkHttpClient client = new OkHttpClient();
    private String networkAddress = "";
    private RobotWebSocketListener listen;
    public WebSocket ws;
    public CharSequence alert;
    public String ObstacleCoordinate = "99";

    public String posx = "";
    public String posy = "";

    private void webSocketConnect() {
        listen = new RobotWebSocketListener(buttons);
        Request request = new Request.Builder()
                .url("ws://"+networkAddress)
                .build();

        ws = client.newWebSocket(request, listen);
        client.dispatcher().executorService().shutdown();
        //ws.send("");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            networkAddress = env("production.carrier.ip",this.getBaseContext());
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
                if(ws != null) {
                    ObstacleCoordinate = getUiFromCount(button.getId());
                    ws.send("{\"testOne\":{\"x\":\""+ ObstacleCoordinate.charAt(0) +
                            "\",\"y\":\""+ObstacleCoordinate.charAt(1)+"\"}}");
                }else{
                    alert = "connect to socket first";
                    Toast.makeText(getApplicationContext(), alert, Toast.LENGTH_SHORT).show();
                }
//                for(int i=0;i<buttons.size();i++){
//                    Button b = buttons.get(i);
//                    b.setBackgroundColor(getResources().getColor(R.color.black));
//                    b.setBackgroundResource(R.drawable.button);
//                }
//                button.setBackgroundColor(getResources().getColor(R.color.purple_700));
                //for test
                button.setBackgroundColor(getResources().getColor(R.color.obstacle));
                if(ws != null) {
                    //alert = "obstacle set: don't click again";
                    //Toast.makeText(getApplicationContext(), alert, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private static JSONObject jsonConvert(String json){
        try {
            JSONObject jo = new JSONObject(json);
            return jo;
        } catch (Throwable t) {
            Log.e("JSONObject Error:", "JSON MALFORMED: " + json);
        }
        return null;
    }

    private final class RobotWebSocketListener extends WebSocketListener {

        protected int pos = 20;
        protected ArrayList<Button> buttons;

        RobotWebSocketListener(ArrayList<Button> buttons ){
            super();
            this.buttons = buttons;
        }

        @Override
        public void onOpen(WebSocket ws, Response res) {
        }

        @Override
        public void onMessage(WebSocket ws, String txt) {

            JSONObject obj = jsonConvert(txt);
            if(obj != null){
                try {
                    if(obj.has("Position")) {

                        JSONObject position = obj.getJSONObject("Position");

                        posx = position.getString("x");
                        posy = position.getString("y");
                        pos = convertXyFromUi(posx, posy) - 1;

                        if(pos >= 0 && pos < 25) {
                            //Log.d("pos", "" + pos);
                            Button selectedButton = buttons.get(pos);
                            for (int i = 0; i < buttons.size(); i++) {
                                Button b = buttons.get(i);
                                b.setBackgroundColor(getResources().getColor(R.color.black));
                                b.setBackgroundResource(R.drawable.button);
                            }
                            if(ObstacleCoordinate.equals(getUiFromCount(selectedButton.getId()))){
                                //obstacle collision
                                Log.d("collision: ", "pos:" + pos);
                                ws.send("{\"collision\":{\"x\":\""+ ObstacleCoordinate.charAt(0) +
                                        "\",\"y\":\""+ObstacleCoordinate.charAt(1)+"\"}}");
                                selectedButton.setBackgroundColor(getResources().getColor(R.color.obstacle));
                                //ObstacleCoordinate = "99";//reset
                            }else {
                                selectedButton.setBackgroundColor(getResources().getColor(R.color.purple_700));
                                Log.d("ObstacleCorrd: " , ObstacleCoordinate);
                            }

                        }

                        if(pos == 24){
                            ObstacleCoordinate = "99";
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
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
            Log.i("ws failure: " , t.getMessage());
        }
    }


    private static int convertXyFromUi(String x, String y){
        String in = x + y;
        switch (in){
            case "04":
                return 1;
            case "14":
                return 2;
            case "24":
                return 3;
            case "34":
                return 4;
            case "44":
                return 5;
            case "03":
                return 6;
            case "13":
                return 7;
            case "23":
                return 8;
            case "33":
                return 9;
            case "43":
                return 10;
            case "02":
                return 11;
            case "12":
                return 12;
            case "22":
                return 13;
            case "32":
                return 14;
            case "42":
                return 15;
            case "01":
                return 16;
            case "11":
                return 17;
            case "21":
                return 18;
            case "31":
                return 19;
            case "41":
                return 20;
            case "00":
                return 21;
            case "10":
                return 22;
            case "20":
                return 23;
            case "30":
                return 24;
            case "40":
                return 25;
            default:
                return 0;
        }
    }

    private static String getUiFromCount(int in){
        switch (in){
            case 1:
                return "04";
            case 2:
                return "14";
            case 3:
                return "24";
            case 4:
                return "34";
            case 5:
                return "44";
            case 6:
                return "03";
            case 7:
                return "13";
            case 8:
                return "23";
            case 9:
                return "33";
            case 10:
                return "43";
            case 11:
                return "02";
            case 12:
                return "12";
            case 13:
                return "22";
            case 14:
                return "32";
            case 15:
                return "42";
            case 16:
                return "01";
            case 17:
                return "11";
            case 18:
                return "21";
            case 19:
                return "31";
            case 20:
                return "41";
            case 21:
                return "00";
            case 22:
                return "10";
            case 23:
                return "20";
            case 24:
                return "30";
            case 25:
                return "40";
            default:
                return "";
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