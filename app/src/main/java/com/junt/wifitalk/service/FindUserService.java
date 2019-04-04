package com.junt.wifitalk.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.junt.wifitalk.activity.AudioActivity;
import com.junt.wifitalk.model.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static android.text.format.Formatter.formatIpAddress;
import static com.junt.wifitalk.activity.AudioActivity.isCalling;

/**
 * description :管理用户上下线及接通对讲
 *
 * @author Junt
 * @date :2019/4/1 13:33
 */
public class FindUserService extends Service {
    private final String TAG = "FindUserService";
    private final int CONNECTION_PORT = 8888;
    private final int DISCOVER_PORT = 9999;
    public static final String ACTION_DISCOVER = "ACTION_DISCOVER";
    public static final String ACTION_WIFI_INSTRUCTION = "ACTION_WIFI_INSTRUCTION";
    public static final int ACTION_NET_ONLINE = 0;
    public static final int ACTION_NET_OFFLINE = 1;
    public static final int ACTION_NET_CONNECTED = 2;
    public static final int ACTION_NET_DISCONNECTED = 3;
    public static final int INSTRUCTION_PEER_CONNECT = 0;
    public static final int INSTRUCTION_PEER_DISCONNECT = 1;
    public static final int INSTRUCTION_PEER_OFFLINE = 2;
    private ServiceReceiver serviceReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate()");
        serviceReceiver = new ServiceReceiver();
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ACTION_WIFI_INSTRUCTION);
        registerReceiver(serviceReceiver, mIntentFilter);

        new PeerDiscoverServerThread().start();
        new PeerDiscoverClientOnlineThread().start();
        new ConnectServerThread(CONNECTION_PORT).start();
    }

    /**
     * 接收上线提醒
     */
    class PeerDiscoverServerThread extends Thread {
        DatagramSocket datagramSocket;

        public PeerDiscoverServerThread() {
            try {
                datagramSocket = new DatagramSocket(DISCOVER_PORT, InetAddress.getByName("0.0.0.0"));
                datagramSocket.setBroadcast(true);
            } catch (Exception e) {
                Log.e(TAG, "PeerDiscoverServerThread:ConstructorError=" + e.getMessage());
            }
        }

        @Override
        public void run() {
            super.run();
            while (true) {
                byte buf[] = new byte[1024];
                // 接收数据
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    datagramSocket.receive(packet);
                    String data = new String(packet.getData()).trim();
                    JSONObject jsonObject = new JSONObject(data);
                    String type = jsonObject.getString("type");
                    if (type.equals("ONLINE") &&
                            !packet.getAddress().toString().equals("/" + getLocalIPAddress())) {

                        JSONObject responseJsonObject = createJson("RESPONSE");

                        byte[] feedback = responseJsonObject.toString().getBytes();
                        // 发送收到上线回馈
                        DatagramPacket sendPacket = new DatagramPacket(feedback, feedback.length,
                                packet.getAddress(), DISCOVER_PORT);
                        datagramSocket.send(sendPacket);
                        // 发送消息
                        sendBroadCastToUserList(ACTION_NET_ONLINE, packet.getAddress().toString().substring(1), jsonObject.getString("name"));
                    } else if (type.equals("OFFLINE") &&
                            !packet.getAddress().toString().equals("/" + getLocalIPAddress())) {
                        sendBroadCastToUserList(ACTION_NET_OFFLINE, packet.getAddress().toString().substring(1), jsonObject.getString("name"));
                    } else if (type.equals("RESPONSE") &&
                            !packet.getAddress().toString().equals("/" + getLocalIPAddress())) {
                        // 发送消息
                        sendBroadCastToUserList(ACTION_NET_ONLINE, packet.getAddress().toString().substring(1), jsonObject.getString("name"));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "PeerDiscoverServerThread:RunError=" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 向列表页发送广播信息
     *
     * @param type  类型
     * @param argus 0-地址 1-用户名
     */
    private void sendBroadCastToUserList(int type, String... argus) {
        Intent intent = new Intent(ACTION_DISCOVER);
        intent.putExtra("NetType", type);
        if (argus.length > 0) {
            intent.putExtra("address", argus[0]);
            intent.putExtra("name", argus[1]);
        }
        sendBroadcast(intent);
    }

    /**
     * 发送在线提醒
     */
    class PeerDiscoverClientOnlineThread extends Thread {
        InetAddress inetAddress;
        DatagramSocket datagramSocket;
        JSONObject jsonObject;

        public PeerDiscoverClientOnlineThread() {
            try {
                jsonObject = createJson("ONLINE");
                inetAddress = InetAddress.getByName("255.255.255.255");
                datagramSocket = new DatagramSocket();
                datagramSocket.setBroadcast(true);
            } catch (Exception e) {
                Log.e(TAG, "PeerDiscoverClientOnlineThread:ConstructorError=" + e.getMessage());
                e.printStackTrace();
            }
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        public void run() {
            super.run();
            try {
                new AsyncTask<String, Integer, String>() {
                    @Override
                    protected String doInBackground(String... paramVarArgs) {
                        Log.e(TAG, "PeerDiscoverClientOnlineThread:content=" + paramVarArgs[0] + " localIp=" + getLocalIPAddress());
                        byte[] data = paramVarArgs[0].getBytes();
                        DatagramPacket dataPacket = new DatagramPacket(
                                data, data.length, inetAddress, DISCOVER_PORT);
                        try {
                            datagramSocket.send(dataPacket);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Log.e(TAG, "PeerDiscoverClientOnlineThread:RunError=" + e.getMessage());
                            return "Failure";
                        }
                        return "Success";
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        super.onPostExecute(result);
                    }
                }.execute(jsonObject.toString());
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 创建通信数据Json
     *
     * @param type ONLINE-上线 OFFLINE-离线 RESPONSE-收到回信
     * @return json对象
     */
    private JSONObject createJson(String type) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", type);
            jsonObject.put("name", User.getInstance().getName());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * 发送下线提醒
     */
    class PeerDiscoverClientOfflineThread extends Thread {
        InetAddress inetAddress;
        DatagramSocket datagramSocket;
        JSONObject jsonObject;

        public PeerDiscoverClientOfflineThread() {
            try {
                jsonObject = createJson("OFFLINE");
                inetAddress = InetAddress.getByName("255.255.255.255");
                datagramSocket = new DatagramSocket();
                datagramSocket.setBroadcast(true);
            } catch (Exception e) {
                Log.e(TAG, "PeerDiscoverClientOfflineThread:ConstructorError=" + e.getMessage());
                e.printStackTrace();
            }
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        public void run() {
            super.run();
            new AsyncTask<String, Integer, String>() {
                @Override
                protected String doInBackground(String... paramVarArgs) {
                    Log.e(TAG, "PeerDiscoverClientOfflineThread:content=" + paramVarArgs[0]);
                    byte[] data = paramVarArgs[0].getBytes();
                    DatagramPacket dataPacket = new DatagramPacket(
                            data, data.length, inetAddress, DISCOVER_PORT);
                    try {
                        datagramSocket.send(dataPacket);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "PeerDiscoverClientOfflineThread:RunError=" + e.getMessage());
                        return "Failure";
                    }
                    return "Success";
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                }
            }.execute(jsonObject.toString());
        }
    }

    /**
     * Receiver(接收状态信息，连接断连指令)
     */
    public class ServiceReceiver extends BroadcastReceiver {
        public ServiceReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            assert action != null;
            switch (action) {
                //收到连接或断开指令
                case ACTION_WIFI_INSTRUCTION:
                    if (intent.getIntExtra("instruction", -1) == INSTRUCTION_PEER_CONNECT) {
                        Log.e(TAG, "连接服务端" + intent.getStringExtra("address"));
                        new ConnectClientThread(intent.getStringExtra("address"), CONNECTION_PORT).start();
                    } else if (intent.getIntExtra("instruction", -1) == INSTRUCTION_PEER_DISCONNECT) {
//                        asd
                    } else if (intent.getIntExtra("instruction", -1) == INSTRUCTION_PEER_OFFLINE) {
                        new PeerDiscoverClientOfflineThread().start();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 连接监听客户端
     */
    class ConnectClientThread extends Thread {
        private String address;
        private int connectionPort;

        public ConnectClientThread(String address, int connectionPort) {
            Log.e(TAG, "ConnectClientThread：ConnectClientThread:address=" + address);
            this.address = address;
            this.connectionPort = connectionPort;
        }

        @Override
        public void run() {
            super.run();
            try {
                Socket socket = new Socket(InetAddress.getByName(address), connectionPort);
                socket.setSoTimeout(10000);
                if (socket.isConnected()) {
                    OutputStream outputStream = socket.getOutputStream();
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", User.getInstance().getName());
                    jsonObject.put("address", getLocalIPAddress());
                    outputStream.write(jsonObject.toString().getBytes());
                    outputStream.flush();
                    sendBroadCastToUserList(ACTION_NET_CONNECTED);
                    socket.close();
                    outputStream.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "ConnectClientThread：runError=" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 监听连接服务端
     */
    class ConnectServerThread extends Thread {
        private ServerSocket serverSocket;

        public ConnectServerThread(int connectionPort) {
            try {
                serverSocket = new ServerSocket(connectionPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            while (true) {
                if (!isCalling) {
                    try {
                        Socket socket = serverSocket.accept();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String data = reader.readLine();
                        JSONObject jsonObject = new JSONObject(data);
                        startToAudioActivity(jsonObject.getString("name"), jsonObject.getString("address"));
                        socket.close();
                        reader.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 跳转到通话界面
     */
    private void startToAudioActivity(String name, String address) {
        Intent intent = new Intent(FindUserService.this, AudioActivity.class);
        intent.putExtra("name", name);
        intent.putExtra("address", address);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    /**
     * 获取本地ip地址
     *
     * @return String
     */
    //获取本地IP函数
    public String getLocalIPAddress() {
        //获取wifi服务
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //判断wifi是否开启
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        return formatIpAddress(ipAddress);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(serviceReceiver);
    }
}
