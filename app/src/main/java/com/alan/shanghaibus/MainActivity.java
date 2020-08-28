package com.alan.shanghaibus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private CharSequence mTitle;
    private ListView infoListView;
    private List<Map<String, Object>> list = new ArrayList<>();
    private ProgressDialog dialog;
    private Toolbar toolbar;
    private TextView mTextMessage;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);


        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        String busNo = preferences.getString(Config.KEY_BUS_NO, "69");
        String bus_direction = preferences.getString(Config.KEY_BUS_DIRECTIONS_LIST, "1");; //O 上行，1下行，默认0
        String direction_m = "上行";
        if(bus_direction.compareTo("1")==0){
            direction_m = "下行";
        }

        //if(Config.debug) Log.i("-----Bus Route", busNo + "路 " + direction_m);
        //设置主标题和颜色
        //toolbar.setLogo(R.drawable.bus);
        toolbar.setTitle(busNo + "路 " + direction_m);

        setSupportActionBar(toolbar);
        // 显示公交信息的ListView
        infoListView = findViewById(R.id.info_list_view);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
            }
        });
    }

    protected void onStart() {
        super.onStart();
        String busNo = preferences.getString(Config.KEY_BUS_NO, "69");
        String bus_direction = preferences.getString(Config.KEY_BUS_DIRECTIONS_LIST, "1");; //O 上行，1下行，默认0
        String direction_m = "上行";
        if(bus_direction.compareTo("1")==0){
            direction_m = "下行";
        }

        //if(Config.debug) Log.i("-----Bus Route", busNo + "路 " + direction_m);
        //设置主标题和颜色
        toolbar.setTitle(busNo + "路 " + direction_m);

        setSupportActionBar(toolbar);
    }

    // 将数据填充到ListView中
    private void show() {
        if(list.isEmpty()) {
            TextView message = findViewById(R.id.message);
            message.setText("目前没有信息");
        } else {
            SimpleAdapter adapter = new SimpleAdapter(this, list, R.layout.my_list_item,
                    new String[]{"item", "station", "time"},
                    new int[]{R.id.item, R.id.station, R.id.time});
            infoListView.setAdapter(adapter);
        }
        dialog.dismiss();  // 关闭窗口
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            String user_agent_default = "Mozilla/5.0 (Linux; Android 5.1; SM-J5008 Build/LMY47O; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.49 Mobile MQQBrowser/6.2 TBS/043613 Safari/537.36 MicroMessenger/6.5.23.1180 NetType/WIFI Language/zh_CN MicroMessenger/6.5.23.1180 NetType/WIFI Language/zh_CN";
            final  String user_agent = preferences.getString(Config.KEY_USER_AGENT_LIST, user_agent_default);
            //String authorization = preferences.getString("KEY_AUTH_CODE", "N/A");
            String busNo;
            busNo = preferences.getString(Config.KEY_BUS_NO, "69");
            busNo = busNo + "路";
            String bus_stop_id;
            bus_stop_id = preferences.getString(Config.KEY_BUS_STOP_ID, "1");
            //Preference connectionPref = findPreference(Config.KEY_BUS_ENABLE_ALL);
            boolean check_all =  preferences.getBoolean(Config.KEY_BUS_ENABLE_ALL, false);

            //if(Config.debug) Log.i("-----Preference Setting Check All", checked);

            String bus_direction; //O 上行，1下行，默认0
            bus_direction = preferences.getString(Config.KEY_BUS_DIRECTIONS_LIST, "1");
            String bus_sid = "62edeaac8b61a263106c09b3fdf9d6de";

            String url_homepage = "https://shanghaicity.openservice.kankanews.com/";
            String query_sid_url = "https://shanghaicity.openservice.kankanews.com/public/bus/get";
            String query_stop_url = "https://shanghaicity.openservice.kankanews.com/public/bus/Getstop";
            String query_router_details_url = "https://shanghaicity.openservice.kankanews.com/public/bus/mes/sid/";
            String query_router_url = "https://shanghaicity.openservice.kankanews.com/public/bus";


            OkHttpClient.Builder mOkHttpClientBuilder = new OkHttpClient.Builder();
            mOkHttpClientBuilder.cookieJar(new MyCookieJar());
            OkHttpClient client_home_page = mOkHttpClientBuilder.build();
            //final OkHttpClient client_home_page = mOkHttpClientBuilder.build();

            //Step 1: Visit Home Page
            if(Config.debug) Log.i("-----Bus Step 1", "Visit Home Page");
            Request request = new Request.Builder()
                    .url(url_homepage)
                    .addHeader("User-Agent", user_agent)
                    .get()
                    .build();

            try {
                Response response_get_cookie = client_home_page.newCall(request).execute();
                //if(Config.debug) Log.i("-----send Reponse Body - Homepage", response_get_cookie.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
            Headers.Builder headersBuilder;
            Headers requestHeaders;

            //Step 2: Get SID for router
            if(Config.debug) Log.i("-----Bus Step 2", "Get SID for router");
            headersBuilder = new Headers.Builder()
                    .add("charset", "utf-8")
                    .add("user-agent", user_agent)
                    .add("referer", url_homepage);
            requestHeaders = headersBuilder.build();

            FormBody.Builder formBodyBuilder = new FormBody.Builder()
                    .add("idnum", busNo);
            RequestBody requestBody = formBodyBuilder.build();

            request = new Request.Builder()
                    .url(query_sid_url)
                    .headers(requestHeaders)
                    .post(requestBody)
                    .build();

            try {
                Response response_get_id = client_home_page.newCall(request).execute();
                String result_get_id = response_get_id.body().string();//4.获得返回结果
                result_get_id = unicodeToUtf8(result_get_id);
                //String result = "Test";
                if(Config.debug) Log.i(TAG,"-----send Reponse Body Bus Sid" + result_get_id);

                try {
                    JSONObject jsonObj = new JSONObject(result_get_id);
                    bus_sid = jsonObj.getString("sid");
                    if(Config.debug) Log.e(TAG, "Bus SID: " + bus_sid);
                } catch (final JSONException e) {
                    final String errmsg = "Test";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Error Message When Get SID: " + errmsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            //bus_sid = get_bus_sid(client_home_page, user_agent, busNo);

            //Step 3: Get Stops for router
            if(Config.debug) Log.i("-----Bus Step 3", "Get Stops for router");

            String url_router_stops = query_router_details_url + bus_sid + "?stoptype=" + bus_direction;
            if(Config.debug) Log.i("-----send Reponse URL", url_router_stops);

            headersBuilder = new Headers.Builder()
                    .add("charset", "utf-8")
                    .add("user-agent", user_agent)
                    .add("referer", query_sid_url);
            requestHeaders = headersBuilder.build();

            Request request_get_stops = new Request.Builder()
                    .url(url_router_stops)
                    .headers(requestHeaders)
                    .get()
                    .build();

            for (String header : request_get_stops.headers("Cookie")) {
                if(Config.debug) Log.e(TAG, "Request Head - Get Stops: " + header);
            }

            try {
                if(Config.debug) Log.i("-----send Reponse Body Bus Stops", "Begin");
                Response response_get_stops = client_home_page.newCall(request_get_stops).execute();
                for (String header : response_get_stops.headers("Set-Cookie")) {
                    if(Config.debug) Log.e(TAG, "Response Head - Get Stops: " + header);
                }

                if(Config.debug) Log.i("-----send Reponse Body Bus Stops", "Execute " + response_get_stops.code());

                assert response_get_stops.body() != null;
                String result_stops = response_get_stops.body().string();
                if(Config.debug) Log.i("-----send Reponse Body Bus Stops", "Executed");
                //if(Config.debug) Log.i("-----send Reponse Body Bus Stops", result_stops);
                result_stops = unicodeToUtf8(result_stops);
                //if(Config.debug) Log.i("-----send Reponse Body Bus Stops", result_stops);
                Document document = Jsoup.parse( result_stops);
                List<Element> stations= document.select("div.station");
                for (Element station : stations) {
                    String stop_id = station
                            .select("span[class=num]")
                            .text();
                    String stop_name = station
                            .select("span[class=name]")
                            .text();
                    //if(Config.debug) Log.i("-----send Reponse Body Bus Stops", stop_id + " :" + stop_name);
                    Map<String, Object> bus_station_item = new HashMap<>();
                    bus_station_item.put("item", stop_id);
                    bus_station_item.put("station", stop_name);
                    String stop_time = "N/A";
                    if(Config.debug) Log.e(TAG, "Stop ID: " + stop_id);
                    if(Config.debug) Log.e(TAG, "Stop ID Asign: " + bus_stop_id);
                    if(check_all){
                        stop_time = get_bus_stop_info(client_home_page, query_stop_url, user_agent, url_router_stops, bus_direction, stop_id, bus_sid);
                    }else {
                        if (bus_stop_id != null && Integer.parseInt(stop_id.replace(".", "")) == Integer.parseInt(bus_stop_id)) {
                            stop_time = get_bus_stop_info(client_home_page, query_stop_url, user_agent, url_router_stops, bus_direction, stop_id, bus_sid);
                        }
                    }
                    bus_station_item.put("time", stop_time);
                    list.add(bus_station_item);
                }
            } catch (IOException e) {
                if(Config.debug) Log.e(TAG, "Error fff: " + e.getMessage());
            }

            // 执行完毕后给handler发送一个空消息
            handler.sendEmptyMessage(0);
        }

        //Step 2: Get Bus Route SID
        private String get_bus_sid(OkHttpClient client_home_page,  String user_agent, String busNo){
            String url_homepage = "https://shanghaicity.openservice.kankanews.com/";
            String query_sid_url = "https://shanghaicity.openservice.kankanews.com/public/bus/get";
            String bus_sid = "62edeaac8b61a263106c09b3fdf9d6de";

            //Step 2: Get SID for router
            if(Config.debug) Log.i("-----Bus Step 2", "Get SID for router");
            Headers.Builder headersBuilder = new Headers.Builder()
                    .add("charset", "utf-8")
                    .add("user-agent", user_agent)
                    .add("referer", url_homepage);
            Headers requestHeaders = headersBuilder.build();

            FormBody.Builder formBodyBuilder = new FormBody.Builder()
                    .add("idnum", busNo);
            RequestBody requestBody = formBodyBuilder.build();

            Request request = new Request.Builder()
                    .url(query_sid_url)
                    .headers(requestHeaders)
                    .post(requestBody)
                    .build();

            try {
                Response response_get_id = client_home_page.newCall(request).execute();
                String result_get_id = response_get_id.body().string();//4.获得返回结果
                result_get_id = unicodeToUtf8(result_get_id);
                //String result = "Test";
                if(Config.debug) Log.i(TAG,"-----send Reponse Body Bus Sid" + result_get_id);

                try {
                    JSONObject jsonObj = new JSONObject(result_get_id);
                    bus_sid = jsonObj.getString("sid");
                    if(Config.debug) Log.e(TAG, "Bus SID: " + bus_sid);
                } catch (final JSONException e) {
                    final String errmsg = "Test";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Error Message When Get SID: " + errmsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bus_sid;
        }

        //Step 4: Get Stop time for stop_id
        private String get_bus_stop_info(OkHttpClient client_home_page, String query_stop_url, String user_agent, String url_router_stops, String bus_direction, String bus_stop_id, String bus_sid){
            String strResult = "等待发车";

            if(Config.debug) Log.i("-----Bus Step 4", "Get Stop time for stop_id " + query_stop_url);
            Headers.Builder headersBuilder = new Headers.Builder()
                    .add("charset", "utf-8")
                    .add("Connection", "keep-alive")
                    .add("Accept-Encoding", "gzip, deflate, br")
                    .add("content-type", "application/x-www-form-urlencoded")
                    .add("user-agent", user_agent)
                    .add("referer", url_router_stops)
                    .add("host", "shanghaicity.openservice.kankanews.com")
                    .add("Origin", "https://shanghaicity.openservice.kankanews.com")
                    .add("X-Requested-With", "XMLHttpRequest");
            Headers requestHeaders = headersBuilder.build();

            FormBody.Builder formBodyBuilder = new FormBody.Builder()
                    .add("stoptype", bus_direction)
                    .add("stopid", bus_stop_id + ".")
                    .add("sid", bus_sid);
            RequestBody requestBody = formBodyBuilder.build();

            Request request_stop_time = new Request.Builder()
                    .url(query_stop_url)
                    .headers(requestHeaders)
                    .post(requestBody)
                    .build();
            for (String header : request_stop_time.headers("Cookie")) {
                if(Config.debug) Log.e(TAG, "Request Head - Get Stop Time: " + header);
            }
            try {
                Response response_get_stop_time = client_home_page.newCall(request_stop_time).execute();

                for (String header : response_get_stop_time.headers("Set-Cookie")) {
                    if(Config.debug) Log.e(TAG, "Response Head - Get Stop Time: " + header);
                }

                String result = null;//4.获得返回结果
                if (response_get_stop_time.body() != null) {
                    result = response_get_stop_time.body().string();
                }

                if(Config.debug) Log.i("-----send Reponse Body Query Stop", result);

                try {
                    JSONObject bus_stop_info = new JSONObject(result);
                    String message = bus_stop_info.getString("error");
                    strResult = "等待发车";
                } catch (final JSONException e) {
                    try{
                        //addNotification("Get Stop Time Fail, please check!");
                        JSONArray jsonarray = new JSONArray(result);
                        for(int i=0; i < jsonarray.length(); i++) {
                            JSONObject jsonObj = jsonarray.getJSONObject(i);
                            final String terminal = jsonObj.getString("terminal");
                            final String distance = jsonObj.getString("distance");
                            final String time = jsonObj.getString("time") + "秒";
                            final String stopdis = jsonObj.getString("stopdis");
                            strResult = terminal + " 还有" + stopdis + "站" + distance + "米，约" + time;
                        }

                    }catch ( final JSONException ex) {
                        if(Config.debug) Log.e(TAG, "Json parsing error 1: " + ex.getMessage());
                        return ex.getMessage();
                    }
                }
                //return resultList;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return strResult;
        }
    };

    public String unicodeToUtf8(String theString) {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len);
        for (int x = 0; x < len;) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = theString.charAt(x++);
                        switch (aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Malformed   \\uxxxx   encoding.");
                        }
                    }
                    outBuffer.append((char) value);
                } else {
                    if (aChar == 't')
                        aChar = '\t';
                    else if (aChar == 'r')
                        aChar = '\r';
                    else if (aChar == 'n')
                        aChar = '\n';
                    else if (aChar == 'f')
                        aChar = '\f';
                    outBuffer.append(aChar);
                }
            } else
                outBuffer.append(aChar);
        }
        return outBuffer.toString();
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            // 收到消息后执行handler
            show();
        }
    };


    // 判断是否有可用的网络连接
    public boolean isNetworkAvailable(Activity activity)
    {
        Context context = activity.getApplicationContext();
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return false;
        else
        {   // 获取所有NetworkInfo对象
            @SuppressLint("MissingPermission") NetworkInfo[] networkInfo = cm.getAllNetworkInfo();
            if (networkInfo != null && networkInfo.length > 0)
            {
                for (NetworkInfo networkInfo1 : networkInfo)
                    if (networkInfo1.getState() == NetworkInfo.State.CONNECTED)
                        return true;  // 存在可用的网络连接
            }
        }
        return false;
    }

    public void toXiaoaiDevices(){
        /* 新建一个Intent对象 */
        Intent intent = new Intent();
        intent.putExtra("name","XiaoAi");
        /* 指定intent要启动的类 */
        intent.setClass(MainActivity.this, XiaoaiDevices.class);
        /* 启动一个新的Activity */
        MainActivity.this.startActivity(intent);
        /* 关闭当前的Activity */
        MainActivity.this.finish();
    }

    // 刷新
    public void refresh() {
        if(isNetworkAvailable(MainActivity.this)) {
            // 显示“正在刷新”窗口
            dialog = new ProgressDialog(this);
            dialog.setMessage("正在刷新...");
            dialog.setCancelable(false);
            dialog.show();
            // 重新抓取
            list.clear();
            new Thread(runnable).start();  // 子线程
        } else {
            // 弹出提示框
            new AlertDialog.Builder(this)
                    .setTitle("刷新")
                    .setMessage("当前没有网络连接！")
                    .setPositiveButton("重试",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            refresh();
                        }
                    }).setNegativeButton("退出",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    System.exit(0);  // 退出程序
                }
            }).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_refresh:
                //Toast.makeText(MainActivity.this, "刷新按钮 TBD", Toast.LENGTH_SHORT).show();
                refresh();
                break;
            case R.id.action_toggle_direction:
                //Toast.makeText(MainActivity.this, "换向按钮 TBD", Toast.LENGTH_SHORT).show();
                //refresh();
                //toXiaoaiTTS();
                toXiaoaiDevices();
                break;
            case R.id.action_settings:
                //popUpMyOverflow();
                getShanghaiBusSettingsFragment(this.getCurrentFocus());
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("退出提示");
        dialog.setMessage("您确定退出应用吗?");
        dialog.setNegativeButton("取消",null);
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                System.exit(0);
            }
        });
        dialog.show();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = "69";
                break;
            case 2:
                mTitle = "700";
                break;
            case 3:
                mTitle = "1031";
                break;
        }
    }

    public void getShanghaiBusSettingsFragment(View view) {
        Intent launchIntent = new Intent(this, ShanghaiBusPreferencesActivity.class);
        if (launchIntent != null) {
            startActivity(launchIntent);//null pointer check in case package name was not found
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

    }
}
