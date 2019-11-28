package com.alan.shanghaibus;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 这个就是登陆界面的activity
 * 注册  登陆   忘记密码    要考虑这几种情况 转其他activity
 */

public class XiaoaiLogin extends AppCompatActivity implements View.OnClickListener,View.OnLongClickListener{
    //声明控件对象
    private EditText et_name;
    private EditText et_pass;
    private Button mLoginButton;
    private Button mLoginError;
    private Button mRegister;
    private Button ONLYTEST;
    private Button bt_username_clear;
    private Button bt_pwd_clear;
    private Button bt_pwd_eye;
    private TextWatcher username_watcher;
    private TextWatcher password_watcher;  //文本监视器

    int slectIndx = 1;
    int tempSelect = slectIndx;
    private boolean flase;
    boolean isReLogin = flase;
    private int SERVER_FLAG = 0;
    private RelativeLayout countryselsct;
    private TextView county_phone_sn,countryName;

    private final static int LOGIN_ENABLE = 0x01;
    private final static int LOGIN_UNABLF = 0x02;
    private final static int PASS_ERROR =  0x03;
    private final static int NAME_ERROR = 0x04;  //上面是消息的常量值

    //主布局中定义了一个按钮和文本
    LoginTask loginTask;

    private String TAG = XiaoaiLogin.class.getSimpleName();

    private String xiaoai_cookie;

    @SuppressLint("HandlerLeak")
    final Handler UiMangerHandler = new Handler(){        //处理UI的操作的
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case LOGIN_ENABLE:
                    mLoginButton.setClickable(true);
                    break;
                case LOGIN_UNABLF:
                    mLoginButton.setClickable(false);
                    break;
                case PASS_ERROR:
                    break;
                case NAME_ERROR:
                    break;

            }
            super.handleMessage(msg);
        }
    };

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        et_name = findViewById(R.id.username);
        et_pass = findViewById(R.id.password);
        bt_username_clear = findViewById(R.id.bt_usename_clear);
        bt_pwd_clear = findViewById(R.id.bt_pwd_clear);
        bt_pwd_eye = findViewById(R.id.bt_pwd_eyes);

        bt_username_clear.setOnClickListener(this);
        bt_pwd_eye.setOnClickListener(this);
        bt_pwd_clear.setOnClickListener(this);
        initWatcher();

        mLoginButton = findViewById(R.id.login);
        mLoginError  = findViewById(R.id.login_error);
        mRegister    = findViewById(R.id.register);

        mLoginButton.setOnClickListener(this);
        mLoginError.setOnClickListener(this);
        mRegister.setOnClickListener(this);

    }
    private void initWatcher(){
        username_watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                et_pass.setText("");
                if (editable.toString().length()>0){
                    bt_username_clear.setVisibility(View.VISIBLE);
                }else{
                    bt_username_clear.setVisibility(View.INVISIBLE);
                }

            }
        };
        password_watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                if (editable.toString().length()>0){
                    bt_pwd_clear.setVisibility(View.VISIBLE);
                }else{
                    bt_pwd_clear.setVisibility(View.INVISIBLE);
                }

            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            //登陆activity
            case R.id.login:
                login(et_name.getText().toString(),
                        et_pass.getText().toString());
                break;
            //忘记密码
            case R.id.login_error:
                break;
            //注册
            case R.id.register:
                break;

        }
    }

    private void login(String username, String password){
        //步骤3:创建AsyncTask子类的实例
        //注意每次需new一个实例,新建的任务只能执行一次,否则会出现异常
        loginTask = new LoginTask();
        XiaoaiUser user = new XiaoaiUser(username, password);
        //步骤4:调用AsyncTask子类的实例的execute(Params... params)方法执行异步任务
        loginTask.execute(user);
    }

    private static class XiaoaiUser {
        String username;
        String password;

        XiaoaiUser(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    /*步骤1：创建AsyncTask的子类，并为三个泛型参数制定类型
    在特定场合下，并不是所有类型都被使用，如果没有被使用，可以用java.lang.Void类型代替
    //三种泛型类型分别代表
        启动任务执行的输入参数:String类型
        后台任务执行的进度：Integer类型
        后台计算结果的类型：String类型*/
    private class LoginTask extends AsyncTask<XiaoaiUser, Integer, Boolean> {

        //步骤2. 根据需要，实现AsyncTask的方法
        //onPreExecute方法用于在执行后台任务前做一些UI操作
        @Override
        protected void onPreExecute() {
            //text.setText("加载中");
        }

        //doInBackground方法内部执行后台任务,不可在此方法内修改UI
        @Override
        protected Boolean doInBackground(XiaoaiUser... user) {
            String username = user[0].username;
            String password = user[0].password;
            boolean Login_result = Login_TSK(username, password);
            return Login_result;
        }

        public Boolean Login_TSK(String username, String password){
            try {
                HashMap<String, String> sign = getLoginSign();
                if(sign == null) {
                    if (Config.debug) Log.i("-----Login Error - getLoginSign", "");
                    return false;
                }
                if(sign.isEmpty()) {
                    if (Config.debug) Log.i("-----Login Error - getLoginSign", "");
                    return false;
                }
                if(sign == null || sign.isEmpty()) {
                    if (Config.debug) Log.i("-----Login Error - getLoginSign", "");
                    return false;
                }

                if(sign.equals("failure")){
                    return false;
                }
                HashMap<String, String> authInfo = serviceAuth(sign, username, password);
                if(authInfo == null) {
                    if (Config.debug) Log.i("-----Login Error - serviceAuth", "");
                    return false;
                }
                if(authInfo.isEmpty()) {
                    if (Config.debug) Log.i("-----Login Error - serviceAuth", "");
                    return false;
                }
                if(authInfo == null || authInfo.isEmpty()) {
                    if (Config.debug) Log.i("-----Login Error - serviceAuth", "");
                    return false;
                }

                if (authInfo.get("userId") .equals("failure")) {
                    if (Config.debug) Log.i("-----Login Error", authInfo.get("desc"));
                    return false;
                }
                String serviceToken = loginMiAi(authInfo);
                if (serviceToken == "401") {
                    if (Config.debug) Log.i("-----Login Error", serviceToken);
                    return false;
                }
                xiaoai_cookie = getAiCookie(authInfo.get("userId"), serviceToken);
                if (Config.debug) Log.i("-----Cookie", xiaoai_cookie);

                SharedPreferences settings = getSharedPreferences("XiaoAiUserInfo", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("username", username);
                editor.putString("password", password);
                editor.putString("cookie", xiaoai_cookie);
                //editor.putString("livedeviceId", devices);
                editor.commit();
            }catch (Exception e){
                if (Config.debug) Log.i("-----Login failured", "");
                return false;
            }
            return true;
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
                if(Config.debug) Log.e(TAG, "Error when get Login Sign: " + e.getMessage());
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
                    String login_status_desc = jsonObj.getString("desc");
                    if(!login_status_desc.equals("成功")){
                        if(Config.debug) Log.i(TAG,"-----TTS 2 Login status:" + login_status_desc);
                        hashMap.put("nonce","failure");
                        hashMap.put("ssecurity","failure");
                        hashMap.put("location","failure");
                        hashMap.put("userId","failure");
                        hashMap.put("desc",login_status_desc);
                        return hashMap;
                    }
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
                    hashMap.put("desc",login_status_desc);
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
                if(Config.debug) Log.e(TAG, "Error When get service Auth: " + e.getMessage());
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
                        return "401";
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
                if(Config.debug) Log.e(TAG, "Error when get service Token cookie: " + e.getMessage());
            }
            return "401";
        }

        //Step 4: GetMiAiCookie
        private String getAiCookie(String userId, String serviceToken){
            return "userId=" + userId + ";" + serviceToken;
        }


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

        //onProgressUpdate方法用于更新进度信息
        @Override
        protected void onProgressUpdate(Integer... progresses) {

        }

        //onPostExecute方法用于在执行完后台任务后更新UI,显示结果
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(!result){
                toXiaoaiDevices();
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Login faliure, please check username and password!: ",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        public void toXiaoaiDevices(){
            /* 新建一个Intent对象 */
            Intent intent = new Intent();
            intent.putExtra("name","XiaoAi");
            /* 指定intent要启动的类 */
            intent.setClass(XiaoaiLogin.this, XiaoaiDevices.class);
            /* 启动一个新的Activity */
            XiaoaiLogin.this.startActivity(intent);
            /* 关闭当前的Activity */
            XiaoaiLogin.this.finish();
        }

        //onCancelled方法用于在取消执行中的任务时更改UI
        @Override
        protected void onCancelled() {

        }
    }

    @Override
    public boolean onLongClick(View view) {
        switch (view.getId()){
            case R.id.register:
                if(SERVER_FLAG > 9){
                    break;
                }
        }
        return true;
    }

    /**
     * 监听back的那块
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK){
            if (isReLogin){

            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
