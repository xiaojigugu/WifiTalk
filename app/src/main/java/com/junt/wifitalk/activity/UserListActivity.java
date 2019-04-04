package com.junt.wifitalk.activity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.junt.wifitalk.R;
import com.junt.wifitalk.service.FindUserService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在线用户列表界面
 */
public class UserListActivity extends AppCompatActivity {

    private DiscoverBroadcast discoverBroadcast;
    private static List<Map<String, String>> ipList = new ArrayList<>();
    private static SimpleAdapter ipListAdapter;
    private String chooseAddress, chooseName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

//        Map<String, String> ipMap0 = new HashMap<>();
//        ipMap0.put("name", "本机");
//        ipMap0.put("address", getLocAddress());
//        Map<String, String> ipMap1 = new HashMap<>();
//        ipMap1.put("name", "一加");
//        ipMap1.put("address", "192.168.1.152");
//        Map<String, String> ipMap2 = new HashMap<>();
//        ipMap2.put("name", "红米测试");
//        ipMap2.put("address", "192.168.1.137");
//        ipList.add(ipMap0);
//        ipList.add(ipMap1);
//        ipList.add(ipMap2);

        init();

    }

    private void init() {
        Button btn_offline = findViewById(R.id.btn_offline);
        btn_offline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FindUserService.ACTION_WIFI_INSTRUCTION);
                intent.putExtra("instruction", FindUserService.INSTRUCTION_PEER_OFFLINE);
                sendBroadcast(intent);
            }
        });
        ListView listView = findViewById(R.id.listView);
        ipListAdapter = new SimpleAdapter(
                this,
                ipList,
                R.layout.item_ip_list,
                new String[]{"name", "address"},
                new int[]{R.id.ipName, R.id.ipAddress});

        listView.setAdapter(ipListAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Intent intent = new Intent(UserListActivity.this, AudioActivity.class);
//                intent.putExtra("name", ipList.get(position).get("name"));
//                intent.putExtra("address", ipList.get(position).get("address"));
//                startActivity(intent);
                chooseAddress = ipList.get(position).get("address");
                chooseName = ipList.get(position).get("name");
                Log.e("UserListActivity","选择了用户："+chooseName);
                Intent intent = new Intent(FindUserService.ACTION_WIFI_INSTRUCTION);
                intent.putExtra("instruction", FindUserService.INSTRUCTION_PEER_CONNECT);
                intent.putExtra("address", ipList.get(position).get("address"));
                sendBroadcast(intent);
            }
        });

        checkVoicePermission();
        IntentFilter intentFilter = new IntentFilter(FindUserService.ACTION_DISCOVER);
        discoverBroadcast = new DiscoverBroadcast();
        registerReceiver(discoverBroadcast, intentFilter);

        Intent intent = new Intent(UserListActivity.this, FindUserService.class);
        startService(intent);
    }

    /**
     * 检查录音权限
     */
    private void checkVoicePermission() {
        int hasVoicePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (hasVoicePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                finish();
                Toast.makeText(this, "需要录音权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Service传递过来最新的设备列表
     */
    private class DiscoverBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.e("UserListActivity", "handleMessage,address=" + intent.getStringExtra("address"));
            Map<String, String> map = new HashMap<>();
            map.put("address", intent.getStringExtra("address"));
            map.put("name", intent.getStringExtra("name"));
            int netType = intent.getIntExtra("NetType", -1);
            switch (netType) {
                case FindUserService.ACTION_NET_ONLINE:
                    if (!ipList.contains(map)) {
                        ipList.add(map);
                    }
                    ipListAdapter.notifyDataSetChanged();
                    break;
                case FindUserService.ACTION_NET_OFFLINE:
                    ipList.remove(map);
                    ipListAdapter.notifyDataSetChanged();
                    break;
                case FindUserService.ACTION_NET_CONNECTED:
                    intent = new Intent(UserListActivity.this, AudioActivity.class);
                    intent.putExtra("name", chooseName);
                    intent.putExtra("address", chooseAddress);
                    startActivity(intent);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(discoverBroadcast);
    }
}
