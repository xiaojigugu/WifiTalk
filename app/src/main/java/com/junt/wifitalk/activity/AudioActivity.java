package com.junt.wifitalk.activity;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.junt.wifitalk.R;
import com.junt.wifitalk.service.FindUserService;
import com.junt.wifitalk.util.ReceiveSoundsThread;
import com.junt.wifitalk.util.SendSoundsThread;

/**
 * 对讲界面
 */
public class AudioActivity extends AppCompatActivity {
    private Button speakButton;
    private TextView message, tvName;
    private SendSoundsThread sendSoundsThread;
    private ReceiveSoundsThread receiveSoundsThread;
    private boolean isFirst = true;
    private String address;
    //是否正在通话
    public static boolean isCalling;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        isCalling = true;

        String name = getIntent().getStringExtra("name");
        address = getIntent().getStringExtra("address");
        Log.e("AudioActivity", "name=" + name + " address=" + address);
        sendSoundsThread = new SendSoundsThread(address);
        receiveSoundsThread = new ReceiveSoundsThread();
        receiveSoundsThread.start();
        receiveSoundsThread.setRunning(true);

        message = findViewById(R.id.Message);
        tvName = findViewById(R.id.tvName);

        speakButton = findViewById(R.id.speakButton);
        speakButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    message.setText("松开结束");
                    if (isFirst) {
                        sendSoundsThread.start();
                        isFirst = false;
                    }
                    sendSoundsThread.setRunning(true);
                    receiveSoundsThread.setRunning(false);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    message.setText("按下说话");
                    sendSoundsThread.setRunning(false);
                    receiveSoundsThread.setRunning(true);
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isCalling = false;
        receiveSoundsThread.realse();
        receiveSoundsThread.interrupt();
        sendSoundsThread.release();
        sendSoundsThread.interrupt();
        Intent intent = new Intent(FindUserService.ACTION_WIFI_INSTRUCTION);
        intent.putExtra("instruction", FindUserService.INSTRUCTION_PEER_DISCONNECT);
        sendBroadcast(intent);
    }
}