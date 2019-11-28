package com.alan.shanghaibus;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class XiaoaiChat extends AppCompatActivity {
    private List<Msg> msgList = new ArrayList<>();
    private EditText inputText;
    private Button send;
    private RecyclerView msgRecyclerView;
    private MsgAdapter adapter;
    private String TAG = XiaoaiChat.class.getSimpleName();
    private String xiaoai_cookie;
    private String deviceID;
    ChatTask chatTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences settings = getSharedPreferences("XiaoAiUserInfo", 0);
        xiaoai_cookie = settings.getString("cookie", "Null").toString();

        Bundle b=getIntent().getExtras();
        //获取Bundle的信息
        deviceID=b.getString("device_id");
        if(Config.debug) Log.e(TAG, deviceID);
        setContentView(R.layout.activity_xiaoaitts);
        initMsgs();
        inputText = (EditText) findViewById(R.id.input_text);
        send = (Button) findViewById(R.id.send);
        msgRecyclerView = (RecyclerView) findViewById(R.id.msg_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        msgRecyclerView.setLayoutManager(layoutManager);
        adapter = new MsgAdapter(msgList);
        msgRecyclerView.setAdapter(adapter);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = inputText.getText().toString();
                if (!"".equals(content)) {
                    chat_with_xiaoai(content, deviceID);
                    Msg msg = new Msg(content,Msg.TYPE_SENT);
                    msgList.add(msg);
                    adapter.notifyItemInserted(msgList.size()-1);
                    msgRecyclerView.scrollToPosition(msgList.size()-1);
                    inputText.setText("");
                }
            }
        });
    }

    private void chat_with_xiaoai(String content, String deviceID){
        //步骤3:创建AsyncTask子类的实例
        //注意每次需new一个实例,新建的任务只能执行一次,否则会出现异常
        chatTask = new XiaoaiChat.ChatTask();
        XiaoaiParams params = new XiaoaiParams(content, deviceID);
        //步骤4:调用AsyncTask子类的实例的execute(Params... params)方法执行异步任务
        chatTask.execute(params);
    }

    private static class XiaoaiParams {
        String msg;
        String deviceID;

        XiaoaiParams(String msg, String deviceID) {
            this.msg = msg;
            this.deviceID = deviceID;
        }
    }

    /*步骤1：创建AsyncTask的子类，并为三个泛型参数制定类型
    在特定场合下，并不是所有类型都被使用，如果没有被使用，可以用java.lang.Void类型代替
    //三种泛型类型分别代表
        启动任务执行的输入参数:String类型
        后台任务执行的进度：Integer类型
        后台计算结果的类型：String类型*/
    private class ChatTask extends AsyncTask<XiaoaiParams, Object, Boolean> {

        //步骤2. 根据需要，实现AsyncTask的方法
        //onPreExecute方法用于在执行后台任务前做一些UI操作
        @Override
        protected void onPreExecute() {
            //text.setText("加载中");
        }

        //doInBackground方法内部执行后台任务,不可在此方法内修改UI
        @Override
        protected Boolean doInBackground(XiaoaiParams... xiaoaiParams) {
            String msg = xiaoaiParams[0].msg;
            String deviceID = xiaoaiParams[0].deviceID;
            boolean rr = say(msg, deviceID);
            return rr;
        }

        //onPostExecute方法用于在执行完后台任务后更新UI,显示结果
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            String say_result = "Success";
            if(!result){
                say_result = "Fail";
            }
            Msg msg = new Msg(say_result,Msg.TYPE_RECEIVED);
            if(Config.debug) Log.e(TAG, "Chat Result: " + say_result);
            msgList.add(msg);
            adapter.notifyItemInserted(msgList.size()-1);
            msgRecyclerView.scrollToPosition(msgList.size()-1);
            inputText.setText("");
        }

        //onCancelled方法用于在取消执行中的任务时更改UI
        @Override
        protected void onCancelled() {

        }
        private static final String ALLOWED_CHARACTERS ="0123456789qwertyuiopasdfghjklzxcvbnmZXCVBNMASDFGHJKLPOIUYTREWQ_";

        private String getRandomString(final int sizeOfRandomString) {
            final Random random=new Random();
            final StringBuilder sb=new StringBuilder(sizeOfRandomString);
            for(int i=0;i<sizeOfRandomString;++i)
                sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
            return sb.toString();
        }


        //Step 6: tts
        private boolean tts(String msg, String cookie, String deviceID){
            String USBS_URL = "https://api.mina.mi.com/remote/ubus";
            OkHttpClient.Builder mOkHttpClientBuilder = new OkHttpClient.Builder();
            mOkHttpClientBuilder.cookieJar(new MyCookieJar());
            OkHttpClient client_home_page = mOkHttpClientBuilder.build();

            String url_tts = USBS_URL + "?deviceId=" + deviceID ;
            try
            {
                String str_msg = "{'text':'" + msg + "'}";
                if(Config.debug) Log.i("-----TTS 3 str_msg:" , str_msg);
                String str = new String(("{'text': '" + msg + "'}").getBytes(), "UTF-8");
                String urlencoded_msg = URLEncoder.encode(str, "UTF-8");
                url_tts = url_tts + "&message=" + urlencoded_msg;
                url_tts = url_tts + "&method=text_to_speech&path=mibrain";
                url_tts = url_tts + "&requestId="+getRandomString(25);
            } catch (Exception localException) {
                if(Config.debug) Log.i("-----TTS 3 toURLEncoded error:" , url_tts);
            }

            if(Config.debug) Log.i("-----TTS 3 send Reponse URL", url_tts);

            if(Config.debug) Log.i("-----TTS 3 send Cookie", cookie);

            Headers.Builder headersBuilder;
            Headers requestHeaders;
            headersBuilder = new Headers.Builder()
                    .add("Accept", "*/*")
                    .add("Connection", "Keep-Alive")
                    .add("Content-Type", "application/x-www-form-urlencoded")
                    .add("Cookie", cookie)
                    .add("User-Agent", " 'APP/com.xiaomi.mico APPV/2.0.10 iosPassportSDK/3.4.1 iOS/12.3'");
            requestHeaders = headersBuilder.build();

            FormBody.Builder formBodyBuilder = new FormBody.Builder();
            RequestBody requestBody = formBodyBuilder.build();

            Request request_run_tts = new Request.Builder()
                    .url(url_tts)
                    .headers(requestHeaders)
                    .post(requestBody)
                    .build();
            try {
                Response response_run_tts = client_home_page.newCall(request_run_tts).execute();
                if(Config.debug) Log.e(TAG, "TTS Function response_code: " + response_run_tts.code());
                try {
                    if(response_run_tts.code()== 401){
                        return false;
                    }
                } catch (Exception e) {
                    final String errmsg = e.getMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    "Error Message When Get TTS: " + errmsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (IOException e) {
                if(Config.debug) Log.e(TAG, "Error when run tts function: " + e.getMessage());
            }
            return true;
        }

        //Step 7: tts
        private boolean say(String msg, String deviceID){
            try {
                try {
                    if(deviceID.equals("")){
                        return false;
                    }
                    if(Config.debug) Log.e(TAG, "xiaoai_cookie: " + xiaoai_cookie);
                    if(Config.debug) Log.e(TAG, "deviceID: " + deviceID);
                    return tts(msg, xiaoai_cookie, deviceID);
                } catch (Exception e) {
                    final String errmsg = e.toString();
                    if(Config.debug) Log.e(TAG, "Error Message When Say Hello: " + errmsg);
                }
            } catch (Exception e) {
                if(Config.debug) Log.e(TAG, "Error when say something: " + e.getMessage());
            }
            return true;
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        toXiaoaiDevices();
    }

    public void toXiaoaiDevices(){
        /* 新建一个Intent对象 */
        Intent intent = new Intent();
        intent.putExtra("name","XiaoAi");
        /* 指定intent要启动的类 */
        intent.setClass(XiaoaiChat.this, XiaoaiDevices.class);
        /* 启动一个新的Activity */
        XiaoaiChat.this.startActivity(intent);
        /* 关闭当前的Activity */
        XiaoaiChat.this.finish();
    }

    private void initMsgs() {
        Msg msg1 = new Msg("Hello XiaoAi",Msg.TYPE_RECEIVED);
        msgList.add(msg1);
        Msg msg2 = new Msg("锄禾日当午，汗滴禾下土，谁知盘中餐粒粒皆辛苦！",Msg.TYPE_RECEIVED);
        msgList.add(msg2);
    }
}