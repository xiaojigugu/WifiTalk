package com.junt.wifitalk.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * description :发送语音线程
 *
 * @author Junt
 * @date :2019/3/29 22:26
 */
public class SendSoundsThread extends Thread {
    private AudioRecord recorder = null;
    private boolean isRunning = false;
    private byte[] recordBytes = new byte[1024];
    private String address;
    private DatagramSocket clientSocket;
    //    private String address="192.168.1.152";
//    private String address="192.168.1.137";

    public SendSoundsThread(String ipAddress) {
        super();
        Log.e("Send","constructor:"+ipAddress);
        this.address=ipAddress;
        int recordBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, 44100, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, recordBufferSize * 10);
    }

    @Override
    public synchronized void run() {
        super.run();
        recorder.startRecording();
        while (true) {
            if (isRunning) {
                try {
                    clientSocket = new DatagramSocket();
                    InetAddress IP = InetAddress.getByName(address);
                    Log.e("Send",IP.getHostAddress()+" "+IP.getHostName());
                    recorder.read(recordBytes, 0, recordBytes.length);
                    DatagramPacket sendPacket = new DatagramPacket(recordBytes, recordBytes.length, IP, 8888);
                    clientSocket.send(sendPacket);
                    clientSocket.close();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public void release(){
        isRunning=false;
        if (clientSocket!=null){
            clientSocket.close();
        }
       if (recorder!=null){
           recorder.release();
           recorder=null;
       }
    }
}
