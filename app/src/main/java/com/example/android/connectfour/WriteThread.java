package com.example.android.connectfour;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class WriteThread extends Thread {
    MainActivity activity;
    Socket socket;
    String message;
    Object clientData;
    String hostAddress;
    int port = 0;
    int msgType;

    public WriteThread (MainActivity activity, String hostAddress, int port, int msgType, Object clientData) {
        this.activity = activity;
        this.hostAddress = hostAddress;
        this.port = port;
        this.clientData = clientData;
        this.msgType = msgType;
    }

    @Override
    public void run() {
        if ( hostAddress == null ) {
            Log.d("ConnectFour", "No address to send to. Exit write thread.");
            return;
        }
        socket = new Socket();
        int retry = 0;
        while (!socket.isConnected() && 5 >= retry++ ) {
            try {
                socket.bind(null);
                Log.d("ConnectFour", String.format("Trying to connect to host[%s:%d] attempt[%d]", hostAddress, port, retry));
                socket.connect(new InetSocketAddress(hostAddress, port), 5000);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!socket.isConnected() ) {
            Log.d("ConnectFour", String.format("All attempts failed to connect with [%s:%s]", hostAddress, port));
            return;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            Log.d("ConnectFour", "Successfully connected...");
            Log.d("ConnectFour", String.format("Starting to send data. msgType[%d]",msgType));
            DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
            dataOut.writeByte(msgType);
            if (msgType != MainActivity.RESET_MSG) {
                ObjectOutput objectOut = null;
                objectOut = new ObjectOutputStream(bos);
                if ( msgType == MainActivity.TEXT_MSG)
                    objectOut.writeObject(clientData);
                else if ( msgType == MainActivity.DATA_MSG)
                    objectOut.writeObject(clientData);
                else {
                    activity.p2pClientAddress = socket.getLocalAddress().getHostAddress();
                    Log.d("ConnectFour", "My IP addy - "+activity.p2pClientAddress);
                    objectOut.writeObject(activity.p2pClientAddress);
                }
                objectOut.flush();
                byte[] objectBytes = bos.toByteArray();
                dataOut.writeByte(objectBytes.length);
                dataOut.write(objectBytes);
            }
            dataOut.flush();
            dataOut.close();
            Log.d("ConnectFour", "Successfully sent data.");
            if ( msgType == MainActivity.HANDSHAKE_MSG && activity.progressDialog != null && activity.progressDialog.isShowing())
                activity.progressDialog.dismiss();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                bos.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
