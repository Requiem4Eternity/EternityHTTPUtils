package com.beenvip.fqpassengergd.EternityHTTPUtil;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Administrator on 2017/2/23 0023.
 */

public class EternityHTTPUtil {

    //如果需要在发布版打印链接及数据，此处设置为true
    private boolean forceLog=false;


    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final int NETWORK_ERROR = 0;
    public static final int NOT_A_JSON = Integer.MIN_VALUE;
    private static final int CONN_OK = 200;
    private Thread connThread;
    private Handler hdl_callback;
    private HttpURLConnection connRec;
    private Boolean isDebug;
    private Context context;

    public EternityHTTPUtil(Context context) {
        this.context=context;
        try {
            ApplicationInfo info= context.getApplicationInfo();
            isDebug= (info.flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0;
        } catch (Exception e) {
            isDebug=false;
        }
    }

    //创建连接
    public void createConn(final String url, final String method, final HashMap<String, String> params, final IConnCreateCallback iConnCreateCallback) {

        if (isDebug||forceLog) {
            Log.i("url", url);
        }
        if (connThread != null) {
            shutDownStream(connRec);
        }
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    URL connUrl;
                    HttpURLConnection conn;
                    String trueUrl=url;
                    if (params != null && params.size() > 0) {
                        StringBuilder sb_params = new StringBuilder();
                        Iterator<String> itKey = params.keySet().iterator();
                        for (int i = 0; i < params.size(); i++) {
                            if (i != 0) {
                                sb_params.append("&");
                            }
                            String key = itKey.next();
                            sb_params.append(key);
                            sb_params.append("=");
                            sb_params.append(params.get(key));
                            if (isDebug||forceLog) {
                                Log.i("post", key + "=" + params.get(key));
                            }
                        }
                        if (method.equals("POST")) {
                            connUrl=new URL(trueUrl);
                            conn= (HttpURLConnection) connUrl.openConnection();
                            conn.setRequestMethod(POST);
                            OutputStream os = conn.getOutputStream();
                            os.write(sb_params.toString().getBytes());
                            os.flush();
                        } else {
                            trueUrl+="?"+sb_params;
                            connUrl=new URL(trueUrl);
                            conn= (HttpURLConnection) connUrl.openConnection();
                        }
                    }else {
                        connUrl=new URL(trueUrl);
                        conn= (HttpURLConnection) connUrl.openConnection();
                    }
                    connRec = conn;
                    conn.setRequestMethod(method);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    iConnCreateCallback.onConnCreated(conn);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    private void startConn(HttpURLConnection conn) {
        try {
            if (conn.getRequestMethod().equals("POST")) {
                OutputStream os = conn.getOutputStream();
                os.flush();
                os.close();
            }
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int len;
                byte[] data = new byte[4096];
                while ((len = is.read(data)) != -1) {
                    baos.write(data, 0, len);
                }
                is.close();
                String result = new String(baos.toByteArray());

                if (isDebug||forceLog) {
                    Log.i("eternityResult", result);
                }


                baos.close();
                if (hdl_callback != null) {
                    Message msg = new Message();
                    msg.what = CONN_OK;
                    msg.obj = result;
                    hdl_callback.sendMessage(msg);
                }
                conn.disconnect();
            } else {
                if (hdl_callback != null) {
                    hdl_callback.sendEmptyMessage(responseCode);
                }
            }
        } catch (SocketTimeoutException e) {
            if (hdl_callback != null) {
                hdl_callback.sendEmptyMessage(NETWORK_ERROR);
            }
        } catch (UnknownHostException e) {
            if (hdl_callback != null) {
                hdl_callback.sendEmptyMessage(NETWORK_ERROR);
            }
        } catch (ConnectException e) {
            if (hdl_callback != null) {
                hdl_callback.sendEmptyMessage(NETWORK_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            shutDownStream(conn);
        }
    }

    //开始连接，返回JSON
    public void startConnForJSON(HttpURLConnection conn, final IJSONCallback callback) {
        startConn(conn, new IConnCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    callback.onSuccess(new JSONObject(str));
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.onError(NOT_A_JSON);
                }
            }

            @Override
            public void onError(int reason) {
                callback.onError(reason);
            }

            @Override
            public void onNetworkCrashed() {
                callback.onNetworkCrashed();
            }
        });
    }

    //开始连接
    public void startConn(final HttpURLConnection conn, final IConnCallBack callback) {
        try {
            hdl_callback = new Handler(context.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    try {
                        switch (msg.what) {
                            case CONN_OK:
                                callback.onSuccess(msg.obj.toString());
                                break;
                            case NETWORK_ERROR:
                                callback.onNetworkCrashed();
                                break;
                            default:
                                callback.onError(msg.what);
                                break;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            };
            connThread = new Thread() {
                @Override
                public void run() {
                    super.run();
                    if (conn == null) {
                        Log.e("EternityHTTP", "您还没有创建连接，请先用createConn()方法创建连接！");
                    } else {
                        startConn(conn);
                    }
                }
            };
            connThread.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //创建并开始连接
    public void conn(final String url, final String method, @Nullable final HashMap<String, String> params, final IConnCallBack callback) {
        try {
            hdl_callback = new Handler(context.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    try {
                        switch (msg.what) {
                            case CONN_OK:
                                callback.onSuccess(msg.obj.toString());
                                break;
                            case NETWORK_ERROR:
                                callback.onNetworkCrashed();
                                break;
                            default:
                                callback.onError(msg.what);
                                break;
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            };
            connThread = new Thread() {
                @Override
                public void run() {
                    super.run();
                    createConn(url, method, params, new IConnCreateCallback() {
                        @Override
                        public void onConnCreated(HttpURLConnection connection) {
                            startConn(connection);
                        }
                    });
                }
            };
            connThread.start();
        } catch (NullPointerException e) {
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //创建并开始连接，返回JSON
    public void connForJSON(final String url, final String method, @Nullable final HashMap<String, String> params, final IJSONCallback callback) {
        conn(url, method, params, new IConnCallBack() {
            @Override
            public void onSuccess(String str) {
                try {
                    if (str != null && str.length() != 0) {
                        callback.onSuccess(new JSONObject(str));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    callback.onError(NOT_A_JSON);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(int reason) {
                callback.onError(reason);
            }

            @Override
            public void onNetworkCrashed() {
                callback.onNetworkCrashed();
            }
        });
    }

    //取消连接回调
    public void shutDown() {
        hdl_callback = null;
    }

    //关闭流
    private void shutDownStream(HttpURLConnection conn) {
        if (conn != null) {
            conn.disconnect();
            try {
                if (conn.getOutputStream() != null) {
                    conn.getOutputStream().close();
                }
                if (conn.getInputStream() != null) {
                    conn.getInputStream().close();
                }
            } catch (Exception e) {
            }
        }
        connThread = null;
    }
}
