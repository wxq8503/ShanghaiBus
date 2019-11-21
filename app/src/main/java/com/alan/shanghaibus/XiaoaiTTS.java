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
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class XiaoaiTTS extends AppCompatActivity {
    private CharSequence mTitle;
    private ListView infoListView;
    private List<Map<String, Object>> device_list = new ArrayList<>();
    private ProgressDialog dialog;
    private Toolbar toolbar;
    private TextView mTextMessage;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private String TAG = XiaoaiTTS.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //toolbar = findViewById(R.id.toolbar);
        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // 显示公交信息的ListView
        infoListView = findViewById(R.id.info_list_view);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                refresh();
            }
        });
        //refresh();
    }

    protected void onStart() {
        super.onStart();

    }

    public boolean saveDirection(String domainName) {
        SharedPreferences.Editor editor = getSharedPreferences("1024", MODE_PRIVATE).edit();
        editor.putString("domain", domainName);
        return editor.commit();
    }

    public String loadDirection() {
        //SharedPreferences editor = getSharedPreferences("1024", MODE_PRIVATE);
        //return editor.getString("domain", "www.t66y.com");
        return preferences.getString(Config.KEY_BUS_DIRECTIONS_LIST, "1");
    }

    // 将数据填充到ListView中
    private void show() {
        if(device_list.isEmpty()) {
            TextView message = findViewById(R.id.message);
            message.setText("目前没有信息");
        } else {
            SimpleAdapter adapter = new SimpleAdapter(this, device_list, R.layout.my_list_item,
                    new String[]{"item", "station", "time"},
                    new int[]{R.id.item, R.id.station, R.id.time});
            infoListView.setAdapter(adapter);
        }
        dialog.dismiss();  // 关闭窗口
    }

    private void addNotification(String showText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
        builder.setSmallIcon(R.drawable.settings_ic_bus);
        builder.setContentTitle("Shanghai Bus");
        builder.setContentText(showText);
        builder.setContentInfo("Info");

        Intent notificationIntent = new Intent(getApplicationContext(), XiaoaiTTS.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            String user_agent_default = "Mozilla/5.0 (Linux; Android 5.1; SM-J5008 Build/LMY47O; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/53.0.2785.49 Mobile MQQBrowser/6.2 TBS/043613 Safari/537.36 MicroMessenger/6.5.23.1180 NetType/WIFI Language/zh_CN MicroMessenger/6.5.23.1180 NetType/WIFI Language/zh_CN";
            final  String user_agent = preferences.getString(Config.KEY_USER_AGENT_LIST, user_agent_default);
            try {
                String login = Login();
            } catch (Exception e) {
                if(Config.debug) Log.e(TAG, "Error fff: " + e.getMessage());
            }

            // 执行完毕后给handler发送一个空消息
            handler.sendEmptyMessage(0);
        }

        private String md5(String string) {
            if (TextUtils.isEmpty(string)) {
                return "";
            }
            MessageDigest md5 = null;
            try {
                md5 = MessageDigest.getInstance("MD5");
                byte[] bytes = md5.digest(string.getBytes());
                String result = "";
                for (byte b : bytes) {
                    String temp = Integer.toHexString(b & 0xff);
                    if (temp.length() == 1) {
                        temp = "0" + temp;
                    }
                    result += temp;
                }
                return result;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return "";
        }

        private byte[] encryptToSHA(String info) {
            byte[] digesta = null;
            try {
                MessageDigest alga = MessageDigest.getInstance("SHA-1");
                alga.update(info.getBytes());
                digesta = alga.digest();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return digesta;
        }

        private String getSHA(String val) throws NoSuchAlgorithmException{
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(val.getBytes());
            byte[] m = sha1.digest();//加密
            return getString(m);
        }

        private String getString(byte[] b){
            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < b.length; i ++){
                sb.append(b[i]);
            }
            return sb.toString();
        }

        private String byte2hex(byte[] b) {
            String hs = "";
            String stmp = "";
            for (int n = 0; n < b.length; n++) {
                stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
                if (stmp.length() == 1) {
                    hs = hs + "0" + stmp;
                } else {
                    hs = hs + stmp;
                }
            }
            return hs;
        }

        private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnmZXCVBNMASDFGHJKLPOIUYTREWQ_";

        private String getRandomString(final int sizeOfRandomString) {
            final Random random=new Random();
            final StringBuilder sb=new StringBuilder(sizeOfRandomString);
            for(int i=0;i<sizeOfRandomString;++i)
                sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
            return sb.toString();
        }

        private String Login(){
            String username = "1358579****";
            String password = "****2019";
            HashMap<String, String> sign = getLoginSign();
            HashMap<String, String> authInfo = serviceAuth(sign, username, password);
            String serviceToken = loginMiAi(authInfo);
            String xiaoai_cookie = getAiCookie(authInfo.get("userId"), serviceToken);
            if(Config.debug) Log.i("-----Cookie", xiaoai_cookie);
            String devices = getLiveDevices(xiaoai_cookie);
            return xiaoai_cookie;
        }

        //Step 1: Get Login Sign
        private HashMap<String, String> getLoginSign(){
            HashMap<String, String> hashMap =  new HashMap<>();
            String SERVICE_LOGIN_URL = "https://account.xiaomi.com/pass/serviceLogin?sid=micoapi&_json=true";

            if(Config.debug) Log.i("-----TTS 1 Get Login Sign", "Get Login Sign");

            OkHttpClient.Builder mOkHttpClientBuilder = new OkHttpClient.Builder();
            mOkHttpClientBuilder.cookieJar(new MyCookieJar());
            OkHttpClient client_home_page = mOkHttpClientBuilder.build();

            String url_service_login = SERVICE_LOGIN_URL;
            if(Config.debug) Log.i("-----TTS 1 send Reponse URL", url_service_login);

            Headers.Builder headersBuilder;
            Headers requestHeaders;
            headersBuilder = new Headers.Builder()
                    .add("Accept", "*/*")
                    .add("Connection", "Keep-Alive")
                    .add("Content-Type", "application/json")
                    .add("User-Agent", " 'APP/com.xiaomi.mico APPV/2.0.10 iosPassportSDK/3.4.1 iOS/12.3'");
            requestHeaders = headersBuilder.build();

            Request request_get_login_sign = new Request.Builder()
                    .url(url_service_login)
                    .headers(requestHeaders)
                    .get()
                    .build();
            try {
                Response response_get_login_sing = client_home_page.newCall(request_get_login_sign).execute();

                String result_get_sign = response_get_login_sing.body().string();//4.获得返回结果
                result_get_sign = unicodeToUtf8(result_get_sign);
                result_get_sign = result_get_sign.replace("&&&START&&&","");
                result_get_sign = result_get_sign.replace("{\"checkSafePhone\":false}","");
                if(Config.debug) Log.i(TAG,"-----TTS 1 send Reponse Body get Sign" + result_get_sign);

                try {
                    JSONObject jsonObj = new JSONObject(result_get_sign);
                    String _sign = jsonObj.getString("_sign");
                    String qs = jsonObj.getString("qs");
                    if(Config.debug) Log.e(TAG, "_sign: " + _sign);
                    if(Config.debug) Log.e(TAG, "qs: " + qs);
                    hashMap.put("_sign",_sign);
                    hashMap.put("qs",qs);

                } catch (final JSONException e) {
                    final String errmsg = "Test";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Error Message When Get sign: " + errmsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (IOException e) {
                if(Config.debug) Log.e(TAG, "Error fff: " + e.getMessage());
            }
            return hashMap;
        }

        //Step 2: Get Service Auth
        private HashMap<String, String> serviceAuth(HashMap<String, String> signData, String user, String pwd){
            String hashed_pwd = md5(pwd).toUpperCase();
            if(Config.debug) Log.i("-----TTS 2 Hashed Password", hashed_pwd);
            HashMap<String, String> hashMap =  new HashMap<>();
            String SERVICE_AUTH = "https://account.xiaomi.com/pass/serviceLoginAuth2";

            if(Config.debug) Log.i("-----TTS 2 Get Service Auth", "Get Service Auth");

            OkHttpClient.Builder mOkHttpClientBuilder = new OkHttpClient.Builder();
            mOkHttpClientBuilder.cookieJar(new MyCookieJar());
            OkHttpClient client_home_page = mOkHttpClientBuilder.build();

            String url_service_auth = SERVICE_AUTH;
            if(Config.debug) Log.i("-----TTS 2 send Reponse URL", url_service_auth);

            Headers.Builder headersBuilder;
            Headers requestHeaders;
            headersBuilder = new Headers.Builder()
                    .add("Accept", "*/*")
                    .add("Connection", "Keep-Alive")
                    .add("Content-Type", "application/x-www-form-urlencoded")
                    .add("User-Agent", " 'APP/com.xiaomi.mico APPV/2.0.10 iosPassportSDK/3.4.1 iOS/12.3'")
                    .add("Cookie", "deviceId=3C861A5820190429;sdkVersion=3.4.1");
            requestHeaders = headersBuilder.build();

            FormBody.Builder formBodyBuilder = new FormBody.Builder()
                    .add("user", user)
                    .add("hash", hashed_pwd)
                    .add("callback",  "https://api.mina.mi.com/sts")
                    .add("sid", "micoapi")
                    .add("_json", "true")
                    .add("_sign", signData.get("_sign"))
                    .add("qs", signData.get("qs"));
            RequestBody requestBody = formBodyBuilder.build();

            Request request_get_login_sign = new Request.Builder()
                    .url(url_service_auth)
                    .headers(requestHeaders)
                    .post(requestBody)
                    .build();
            try {
                Response response_get_service_auth = client_home_page.newCall(request_get_login_sign).execute();
                //for (String header : response_get_service_auth.headers("Set-Cookie")) {
                //    if(Config.debug) Log.e(TAG, "Response Head - Get Stops: " + header);
                //}

                String result_get_service_auth = response_get_service_auth.body().string();//4.获得返回结果
                result_get_service_auth = unicodeToUtf8(result_get_service_auth);
                result_get_service_auth = result_get_service_auth.replace("&&&START&&&","");
                if(Config.debug) Log.i(TAG,"-----TTS 2 send Reponse Body Service Auth" + result_get_service_auth);

                try {
                    JSONObject jsonObj = new JSONObject(result_get_service_auth);
                    String nonce = jsonObj.getString("nonce");
                    String ssecurity = jsonObj.getString("ssecurity");
                    String location = jsonObj.getString("location");
                    String userId =  jsonObj.getString("userId");
                    if(Config.debug) Log.e(TAG, "nonce: " + nonce);
                    if(Config.debug) Log.e(TAG, "ssecurity: " + ssecurity);
                    if(Config.debug) Log.e(TAG, "location: " + location);
                    if(Config.debug) Log.e(TAG, "userId: " + userId);
                    hashMap.put("nonce",nonce);
                    hashMap.put("ssecurity",ssecurity);
                    hashMap.put("location",location);
                    hashMap.put("userId",userId);
                } catch (final JSONException e) {
                    final String errmsg = "Test";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Error Message When Get Service Auth: " + errmsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (IOException e) {
                if(Config.debug) Log.e(TAG, "Error fff: " + e.getMessage());
            }
            return hashMap;
        }

        //Step 3: Login Mi Ai
        private String loginMiAi(HashMap<String, String> authInfo){
            String LOGIN_MI_AI_URL = authInfo.get("location");
            String nonce = authInfo.get("nonce");
            String security = authInfo.get("ssecurity");
            String clientSign = "nonce=" + nonce + "&" + security;
            byte[] hashed_sign = encryptToSHA(clientSign);
            //String testhash = "nonce=6925521015213910016&e0zDPDQzRja/LkfXuDOzjg==";
            //if(Config.debug) Log.i("-----TTS 3 encrypt To SHA1", Base64.encodeToString(encryptToSHA(testhash), Base64.DEFAULT));

            String encodedClientSign = Base64.encodeToString(hashed_sign, Base64.DEFAULT);
            if(Config.debug) Log.i("-----TTS 3 encodedClientSign", encodedClientSign);
            if(Config.debug) Log.i("-----TTS 3 Get Login cookie", "Get Login Cookie");

            OkHttpClient.Builder mOkHttpClientBuilder = new OkHttpClient.Builder();
            mOkHttpClientBuilder.cookieJar(new MyCookieJar());
            OkHttpClient client_home_page = mOkHttpClientBuilder.build();

            String url_login_miai = LOGIN_MI_AI_URL + "&clientSign=" + encodedClientSign;
            if(Config.debug) Log.i("-----TTS 3 send Reponse URL", url_login_miai);

            Headers.Builder headersBuilder;
            Headers requestHeaders;
            headersBuilder = new Headers.Builder()
                    .add("Accept", "*/*")
                    .add("Connection", "Keep-Alive")
                    .add("Content-Type", "application/json")
                    .add("User-Agent", " 'APP/com.xiaomi.mico APPV/2.0.10 iosPassportSDK/3.4.1 iOS/12.3'");
            requestHeaders = headersBuilder.build();

            Request request_get_login_mi_ai = new Request.Builder()
                    .url(url_login_miai)
                    .headers(requestHeaders)
                    .get()
                    .build();
            try {
                Response response_get_login_sing = client_home_page.newCall(request_get_login_mi_ai).execute();

                try {
                    if(response_get_login_sing.code()== 401){
                        return "";
                    }else{
                        for (String header : response_get_login_sing.headers("Set-Cookie")) {
                            //if(Config.debug) Log.e(TAG, "Response Head - Get Headers Cookie: " + header);
                            String regex = "serviceToken=(.*?);";
                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher(header);
                            while(matcher.find()) {
                                String group = matcher.group();
                                if(Config.debug) Log.e(TAG, "Response Head - Get Headers Cookie: " + group);
                                return group;
                                //break;
                            }
                        }
                    }
                } catch (Exception e) {
                    final String errmsg = "Test";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Error Message When Get sign: " + errmsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (IOException e) {
                if(Config.debug) Log.e(TAG, "Error fff: " + e.getMessage());
            }
            return "";
        }

        //Step 4: GetMiAiCookie
        private String getAiCookie(String userId, String serviceToken){
            return "userId=" + userId + ";" + serviceToken;
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
                    if(Config.debug) Log.e(TAG, "Device_List: " + devices_list);
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
                            device_item.put("item", name + " | ");
                            device_item.put("station", presence);
                            device_item.put("time", deviceID);
                            device_list.add(device_item);
                            return deviceID;
                        }
                    }else{
                        Map<String, Object> device_item = new HashMap<>();
                        // adding each child node to HashMap key => value
                        device_item.put("item", "N/A | ");
                        device_item.put("station", "N/A");
                        device_item.put("time", "N/A");
                        device_list.add(device_item);
                    }
                } catch (final JSONException e) {
                    if(Config.debug) Log.e(TAG, "Device_List: " + e.toString());
                    Map<String, Object> device_item = new HashMap<>();
                    device_item.put("item", "N/A | ");
                    device_item.put("station", "N/A");
                    device_item.put("time", "N/A");
                    device_list.add(device_item);
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
                if(Config.debug) Log.e(TAG, "Error fff: " + e.getMessage());
            }
            return "";
        }

        //Step 6: tts
        private void tts(String msg, String cookie, String deviceID){
            String USBS_URL = "https://api.mina.mi.com/remote/ubus";

            OkHttpClient.Builder mOkHttpClientBuilder = new OkHttpClient.Builder();
            mOkHttpClientBuilder.cookieJar(new MyCookieJar());
            OkHttpClient client_home_page = mOkHttpClientBuilder.build();

            String url_tts = USBS_URL + "?method=text_to_speech&path=mibrain&deviceId=" + deviceID + "&requestId="+getRandomString(10);
            url_tts = url_tts + "&message={'text'%3A+'" + msg;

            if(Config.debug) Log.i("-----TTS 3 send Reponse URL", url_tts);

            Headers.Builder headersBuilder;
            Headers requestHeaders;
            headersBuilder = new Headers.Builder()
                    .add("Accept", "*/*")
                    .add("Connection", "Keep-Alive")
                    .add("Content-Type", "application/json")
                    .add("Cookie", cookie)
                    .add("User-Agent", " 'APP/com.xiaomi.mico APPV/2.0.10 iosPassportSDK/3.4.1 iOS/12.3'");
            requestHeaders = headersBuilder.build();

            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            RequestBody requestBody = formBodyBuilder.build();

            Request request_get_login_mi_ai = new Request.Builder()
                    .url(url_tts)
                    .headers(requestHeaders)
                    .post(requestBody)
                    .build();
            try {
                Response response_get_login_sing = client_home_page.newCall(request_get_login_mi_ai).execute();

                try {
                    if(response_get_login_sing.code()== 401){
                    }else{
                        for (String header : response_get_login_sing.headers("Set-Cookie")) {
                            //if(Config.debug) Log.e(TAG, "Response Head - Get Headers Cookie: " + header);
                            String regex = "serviceToken=(.*?);";
                            Pattern pattern = Pattern.compile(regex);
                            Matcher matcher = pattern.matcher(header);
                            while(matcher.find()) {
                                String group = matcher.group();
                                if(Config.debug) Log.e(TAG, "Response Head - Get Headers Cookie: " + group);
                                //return group;
                                //break;
                            }
                        }
                    }
                } catch (Exception e) {
                    final String errmsg = "Test";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Error Message When Get sign: " + errmsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (IOException e) {
                if(Config.debug) Log.e(TAG, "Error fff: " + e.getMessage());
            }
        }

        //Step 7: tts
        private void say(String msg, String deviceID){

            try {

                try {

                } catch (Exception e) {
                    final String errmsg = "Test";
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Error Message When Get sign: " + errmsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                if(Config.debug) Log.e(TAG, "Error fff: " + e.getMessage());
            }
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
        intent.setClass(XiaoaiTTS.this, MainActivity.class);
        /* 启动一个新的Activity */
        XiaoaiTTS.this.startActivity(intent);
        /* 关闭当前的Activity */
        XiaoaiTTS.this.finish();
    }

    // 刷新
    public void refresh() {
        if(isNetworkAvailable(XiaoaiTTS.this)) {
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
                Toast.makeText(XiaoaiTTS.this, "换向按钮 TBD", Toast.LENGTH_SHORT).show();
                //refresh();
                toMainActivity();
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
