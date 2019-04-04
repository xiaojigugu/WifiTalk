package com.junt.wifitalk.util;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

/**
 * description :接收语音线程
 *
 * @author Junt
 * @date :2019/3/29 22:26
 */
public class ReceiveSoundsThread extends Thread {
    private AudioTrack player = null;
    private boolean isRunning = false;
    private byte[] recordBytes = new byte[1024];
    @SuppressWarnings("resource")
    private DatagramSocket serverSocket;

    public ReceiveSoundsThread() {
        int playerBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        player = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, playerBufferSize * 10, AudioTrack.MODE_STREAM);
    }

    @Override
    public synchronized void run() {
        super.run();
        try {
            serverSocket = new DatagramSocket(8888);
            while (true) {
                if (isRunning) {
                    DatagramPacket receivePacket = new DatagramPacket(recordBytes, recordBytes.length);
                    serverSocket.receive(receivePacket);
                    byte[] data = receivePacket.getData();

                    player.write(data, 0, data.length);
                    player.play();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    public void realse(){
        isRunning=false;
        if (serverSocket!=null){
            serverSocket.close();
        }
        if (player!=null){
            player.release();
            player=null;
        }
    }
}
