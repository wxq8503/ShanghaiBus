package com.alan.shanghaibus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class XiaoaiDevices extends AppCompatActivity {
    private ListView deviceListView;
    private List<Map<String, Object>> device_list = new ArrayList<>();
    private ProgressDialog dialog;
    private Toolbar toolbar;
    private TextView mTextMessage;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private String TAG = XiaoaiDevices.class.getSimpleName();
    private String xiaoai_cookie;
    private String deviceID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xiaoaidevices);
        //preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // 显示Device 信息的ListView
        deviceListView = findViewById(R.id.device_list_view);

        SharedPreferences settings = getSharedPreferences("XiaoAiUserInfo", 0);
        xiaoai_cookie = settings.getString("cookie", "Null").toString();
        deviceID = settings.getString("livedeviceId", "Null").toString();
        if(Config.debug) Log.e(TAG, "cookie: " + xiaoai_cookie);
        if(Config.debug) Log.e(TAG, "Device ID: " + deviceID);

        xiaoai_cookie = "Null";
        if(xiaoai_cookie.equals("Null")){
            toXiaomiLogin();
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
            }
        });
        refresh();
    }

    protected void onStart() {
        super.onStart();
        SharedPreferences settings = getSharedPreferences("XiaoAiUserInfo", 0);
        xiaoai_cookie = settings.getString("cookie", "Null").toString();
    }

    // 将数据填充到ListView中
    private void show() {
        if(Config.debug) Log.e(TAG, "Show Device List: ");
        if(device_list.isEmpty()) {
            if(Config.debug) Log.e(TAG, "No Device Found:");
        } else {
            //if(Config.debug) Log.e(TAG, "Device List: " + device_list);
            SimpleAdapter adapter = new SimpleAdapter(this, device_list, R.layout.device_list_item,
                    new String[]{"name", "status", "device_id"},
                    new int[]{R.id.name, R.id.status, R.id.device_id});
            deviceListView.setAdapter(adapter);
            deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ListView listView = (ListView)parent;
                    HashMap<String, String> map = (HashMap<String, String>) listView.getItemAtPosition(position);
                    String name = map.get("name");
                    String status = map.get("status");
                    String device_id = map.get("device_id");
                    //Toast.makeText(XiaoaiDevices.this, name +" , "+ status +" , "+ device_id  ,Toast.LENGTH_LONG).show();

                    // TODO Auto-generated method stub
                    Bundle bundle = new Bundle();

                    bundle.putString("device_id", device_id);
                    bundle.putString("device_name", name);
                    bundle.putString("device_status", status);
                    Intent intent = new Intent(XiaoaiDevices.this, XiaoaiChat.class);
                    intent.putExtras(bundle);
                    finish();
                    startActivity(intent);
                }
            });

        }
        dialog.dismiss();  // 关闭窗口
    }


    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                String devices = getLiveDevices(xiaoai_cookie);
                if(devices.equals("failure")){
                    toXiaomiLogin();
                }
            } catch (Exception e) {
                if(Config.debug) Log.e(TAG, "Error when get Live Devices: " + e.getMessage());
            }
            // 执行完毕后给handler发送一个空消息
            handler.sendEmptyMessage(0);
        }

        private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnmZXCVBNMASDFGHJKLPOIUYTREWQ_";

        private String getRandomString(final int sizeOfRandomString) {
            final Random random=new Random();
            final StringBuilder sb=new StringBuilder(sizeOfRandomString);
            for(int i=0;i<sizeOfRandomString;++i)
                sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
            return sb.toString();
        }

        //Step 5: Get Live Devices
        private String getLiveDevices(String cookie){
            String random_requestID = getRandomString(30);
            String Device_List_URL = "https://api.mina.mi.com/admin/v2/device_list";

            OkHttpClient.Builder mOkHttpClientBuilder = new OkHttpClient.Builder();
            mOkHttpClientBuilder.cookieJar(new MyCookieJar());
            OkHttpClient client_home_page = mOkHttpClientBuilder.build();

            String url_device_list = Device_List_URL + "?master=1&requestId=" + random_requestID;
            if(Config.debug) Log.i("-----send Reponse URL", url_device_list);

            Headers.Builder headersBuilder;
            Headers requestHeaders;
            headersBuilder = new Headers.Builder()
                    .add("Accept", "*/*")
                    .add("Connection", "Keep-Alive")
                    .add("Content-Type", "application/json")
                    .add("Cookie", cookie)
                    .add("User-Agent", "APP/com.xiaomi.mico APPV/2.0.10 iosPassportSDK/3.4.1 iOS/12.3");

            requestHeaders = headersBuilder.build();

            Request request_get_device_list = new Request.Builder()
                    .url(url_device_list)
                    .headers(requestHeaders)
                    .get()
                    .build();
            try {
                Response response_get_device_list = client_home_page.newCall(request_get_device_list).execute();
                String result_get_device_list = response_get_device_list.body().string();//4.获得返回结果
                result_get_device_list = unicodeToUtf8(result_get_device_list);
                //String result = "Test";
                if(Config.debug) Log.i(TAG,"-----send Reponse Body Devices List: " + result_get_device_list);
                try {
                    JSONObject jsonObj = new JSONObject(result_get_device_list);
                    String success_flag = jsonObj.getString("message");
                    if(Config.debug) Log.e(TAG, "success_flag: " + success_flag);

                    // Getting JSON Array node
                    JSONArray devices_list = jsonObj.getJSONArray("data");
                    //if(Config.debug) Log.e(TAG, "Device_List: " + devices_list);
                    if(devices_list.length()>0){
                        for (int i = 0; i < devices_list.length(); i++) {
                            JSONObject device = devices_list.getJSONObject(i);
                            if(Config.debug) Log.e(TAG, "Device_Info: " + device);
                            String deviceID = device.getString("deviceID");
                            String serialNumber = device.getString("serialNumber");
                            String name = device.getString("name");
                            String alias = device.getString("alias");
                            String presence = device.getString("presence");
                            String address = device.getString("address");
                            // tmp hash map for single contact
                            Map<String, Object> device_item = new HashMap<>();
                            // adding each child node to HashMap key => value
                            device_item.put("name", name + " | ");
                            device_item.put("status", presence);
                            device_item.put("device_id", deviceID);
                            device_list.add(device_item);
                            //return deviceID;
                        }
                        return "Success";
                    }else{
                        Map<String, Object> device_item = new HashMap<>();
                        // adding each child node to HashMap key => value
                        device_item.put("name", "N/A | ");
                        device_item.put("status", "N/A");
                        device_item.put("device_id", "N/A");
                        device_list.add(device_item);
                    }
                } catch (final JSONException e) {
                    if(Config.debug) Log.e(TAG, "Device_List: " + e.toString());
                    Map<String, Object> device_item = new HashMap<>();
                    device_item.put("name", "N/A | ");
                    device_item.put("status", "N/A");
                    device_item.put("device_id", "N/A");
                    device_list.add(device_item);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String errmsg = "Test";
                            errmsg = e.getMessage();
                            Toast.makeText(getApplicationContext(),
                                    "Error Message When Get Devices List: " + errmsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (IOException e) {
                if(Config.debug) Log.e(TAG, "Error when get device list: " + e.getMessage());
            }
            return "failure";
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

    public void toMainActivity(){
        /* 新建一个Intent对象 */
        Intent intent = new Intent();
        intent.putExtra("name","LeiPei");
        /* 指定intent要启动的类 */
        intent.setClass(XiaoaiDevices.this, MainActivity.class);
        /* 启动一个新的Activity */
        XiaoaiDevices.this.startActivity(intent);
        /* 关闭当前的Activity */
        XiaoaiDevices.this.finish();
    }


    public void toXiaomiLogin(){
        /* 新建一个Intent对象 */
        Intent intent = new Intent();
        intent.putExtra("name","LeiPei");
        /* 指定intent要启动的类 */
        intent.setClass(XiaoaiDevices.this, XiaoaiLogin.class);
        /* 启动一个新的Activity */
        XiaoaiDevices.this.startActivity(intent);
        /* 关闭当前的Activity */
        XiaoaiDevices.this.finish();
    }

    // 刷新
    public void refresh() {
        if(isNetworkAvailable(XiaoaiDevices.this)) {
            // 显示“正在刷新”窗口
            dialog = new ProgressDialog(this);
            dialog.setMessage("正在刷新...");
            dialog.setCancelable(false);
            dialog.show();
            // 重新抓取
            device_list.clear();
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
                //Toast.makeText(XiaoaiDevices.this, "换向按钮 TBD", Toast.LENGTH_SHORT).show();
                toMainActivity();
                break;
            case R.id.action_settings:
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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(dialog!=null&&dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public void getShanghaiBusSettingsFragment(View view) {
        Intent launchIntent = new Intent(this, ShanghaiBusPreferencesActivity.class);
        if (launchIntent != null) {
            startActivity(launchIntent);//null pointer check in case package name was not found
        }
    }

}
