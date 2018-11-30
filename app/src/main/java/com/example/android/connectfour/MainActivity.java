package com.example.android.connectfour;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity /*implements PeerListListener*/ {
    WifiManager wifiMgr;
    Channel p2pChannel;
    WifiP2pManager p2pManager;
    BroadcastReceiver p2pReceiver;
    final IntentFilter p2pIntentFilter  = new IntentFilter();
    TextView statusView;
    ListView peerView;
    Switch wifiSwitch;
    List<WifiP2pDevice> peerDevices = new ArrayList<WifiP2pDevice>();
    WifiP2pManager.ConnectionInfoListener connectionListener;
    private boolean isP2pEnabled = false;
    PeerListListener peerListener;
    Button disconnectBtn;
    Button find_btn;
    Button sendBtn;
    WifiP2pInfo p2pInfo;
    SocketServerAsyncTask p2pHost = null;
    SocketServerAsyncTask p2pClient = null;

    static final int P2P_HOST_PORT = 8888;
    static final int P2P_ClIENT_PORT = 8887;

    static final int HANDSHAKE_MSG = 1;
    static final int DATA_MSG = 2;

    // P2P easily gives us the group owner. But not peer address.
    // This variable stores the address after a requestGroupInfo.
    String p2pClientAddress = null;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        wifiMgr = (WifiManager) getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        p2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        p2pChannel = p2pManager.initialize(this, getMainLooper(), null);

        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        statusView = findViewById(R.id.statusView);
        peerView = findViewById(R.id.peerList);
        disconnectBtn = findViewById(R.id.disconnectPeer);
        find_btn = findViewById(R.id.findGame);
        wifiSwitch = findViewById(R.id.wifiSwitch);
        sendBtn = findViewById(R.id.sendMsg);
        if (wifiMgr.isWifiEnabled()) {
            wifiSwitch.setChecked(true);
        } else {
            wifiSwitch.setChecked(false);
        }

        connectionListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                Log.d("ConnectFour", "Received connection information.");
                p2pInfo = info;
                if (p2pInfo.groupFormed && p2pInfo.isGroupOwner) {
                    statusView.setText(R.string.host_conn);
                    /*if ( p2pHost == null || p2pHost.socket == null || !p2pHost.socket.isConnected() ) {
                        p2pHost = new SocketServerAsyncTask(MainActivity.this, P2P_HOST_PORT, true);
                        p2pHost.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
                        //p2pHost.execute();
                    }*/
                    p2pHost = new SocketServerAsyncTask(MainActivity.this, P2P_HOST_PORT, true);
                    p2pHost.execute();
                    sendBtn.setVisibility(View.VISIBLE);
                } else if (p2pInfo.groupFormed) {
                    statusView.setText(R.string.client_conn);
                    /*if ( p2pClient == null || p2pClient.socket == null || !p2pClient.socket.isConnected() ) {
                        p2pClient = new SocketServerAsyncTask(MainActivity.this, P2P_ClIENT_PORT, false);
                        p2pClient.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
                        //p2pClient.execute();
                    }*/
                    p2pClient = new SocketServerAsyncTask(MainActivity.this, P2P_ClIENT_PORT, false);
                    p2pClient.execute();
                    sendBtn.setVisibility(View.VISIBLE);
                }

                if (disconnectBtn.getVisibility() != View.VISIBLE) {
                    // Previous connection was not disconnected. Show the disconnect btn
                    disconnectBtn.setOnClickListener(
                            new View.OnClickListener() {

                                @Override
                                public void onClick(View v) {
                                    disconnect();
                                }
                            });
                    disconnectBtn.setVisibility(View.VISIBLE);
                }
                if (find_btn.getVisibility() == View.VISIBLE) {
                    // Connection exists already.
                    find_btn.setVisibility(View.GONE);
                }
                if (peerView.getVisibility() == View.VISIBLE) {
                    // Connection exists already.
                    peerView.setVisibility(View.GONE);
                }

                /*Intent intent = new Intent( MainActivity.this, PlayActivity.class);
                getApplication().getApplicationContext().startActivity(intent);*/

            }
        };

        sendBtn.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Log.d("ConnectFour", "Send button clicked!!");
                if ( p2pInfo.groupFormed && p2pInfo.isGroupOwner && p2pClientAddress == null ) {
                    Log.d("ConnectFour", "Host can't communicate with client yet until it receives the IP addr!!");
                } else if (p2pInfo.groupFormed && p2pInfo.isGroupOwner) {
                    Log.d("ConnectFour", "Try to write the host message");
                    String testString = new String("Your host says hi!!!!!!!!!!!!!!");
                    new WriteThread (MainActivity.this, p2pClientAddress, P2P_ClIENT_PORT, DATA_MSG, testString, true).start();
                }else if (p2pInfo.groupFormed && p2pClientAddress == null ) {
                    new WriteThread (MainActivity.this, p2pInfo.groupOwnerAddress.getHostAddress(), P2P_HOST_PORT, HANDSHAKE_MSG, null, false).start();
                } else if (p2pInfo.groupFormed ) {
                    Log.d("ConnectFour", "Try to write the client message");
                    String testString = new String("Message from the client....");
                    new WriteThread (MainActivity.this, p2pInfo.groupOwnerAddress.getHostAddress(), P2P_HOST_PORT, DATA_MSG, testString, false).start();
                }
            }
        });

        find_btn.setOnClickListener( new View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View v){
                // Newer devices need to get permission before you can access location info
                if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},0);
                    //wait for callback
                } else {
                    // Either legacy device or already have permission
                    peerDiscovery();
                }
            }
        });

        wifiSwitch.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick(View v){
                wifiControl();
            }
        });

        peerListener = new PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                List<WifiP2pDevice> currentPeers = new ArrayList<>(peerList.getDeviceList());
                Log.d("ConnectFour", String.format("Peers Available [%d]",peerList.getDeviceList().size()));
                if (!currentPeers.equals(peerDevices)) {
                    peerDevices.clear();
                    peerDevices.addAll(currentPeers);

                    BaseAdapter peerAdapter = new PeerListAdapter(currentPeers);
                    peerView.setAdapter(peerAdapter);
                }

                if (peerDevices.size() == 0) {
                    Toast.makeText(MainActivity.this, R.string.no_devices, Toast.LENGTH_SHORT).show();
                    statusView.setText(R.string.no_conn);
                    return;
                }
            }
        };
        peerView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final WifiP2pDevice device = peerDevices.get(position);
                WifiP2pConfig p2pConfig = new WifiP2pConfig();
                p2pConfig.deviceAddress = device.deviceAddress;
                p2pConfig.wps.setup = WpsInfo.PBC;
                p2pManager.connect(p2pChannel, p2pConfig, new ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(MainActivity.this, String.format("%s%s",R.string.success_conn,device.deviceName), Toast.LENGTH_SHORT).show();
                        disconnectBtn.setOnClickListener(
                                new View.OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        disconnect();
                                    }
                                });
                        disconnectBtn.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(MainActivity.this, R.string.failed_conn, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        p2pReceiver = new WifiBroadcastReceiver(p2pManager, p2pChannel, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (p2pHost != null && p2pHost.socket != null) {
            p2pHost.cancel(true);
            try {
                p2pHost.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (p2pClient != null && p2pClient.socket != null) {
            try {
                p2pClient.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public  void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            peerDiscovery();
        }
    }

    public void disconnect() {
        if (p2pHost != null && p2pHost.socket != null) {
            p2pHost.cancel(true);
            try {
                p2pHost.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (p2pClient != null && p2pClient.socket != null) {
            try {
                p2pClient.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        p2pManager.removeGroup(p2pChannel, new ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.d("ConnectFour", "Disconnect failed. Reason:" + reasonCode);

            }

            @Override
            public void onSuccess() {
                findViewById(R.id.disconnectPeer).setVisibility(View.GONE);
            }

        });
    }

    public void wifiControl() {
        if (!wifiMgr.isWifiEnabled()) {
            wifiMgr.setWifiEnabled(true);
        } else {
            wifiMgr.setWifiEnabled(false);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void peerDiscovery(){
        if (!isP2pEnabled() || !wifiMgr.isP2pSupported()) {
            Toast.makeText(MainActivity.this, "Enable Wi-Fi P2P first.", Toast.LENGTH_SHORT).show();
            return;
        }
        statusView.setText(R.string.init_discovery);
        p2pManager.discoverPeers(p2pChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                statusView.setText(R.string.discovery);
                Log.d("ConnectFour", "discoverPeers started.");

            }

            @Override
            public void onFailure(int reasonCode) {
                statusView.setText(R.string.failed_discovery);
                Log.d("ConnectFour", "discoverPeers failed.");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        //p2pReceiver = new WifiBroadcastReceiver(p2pManager, p2pChannel, this);
        registerReceiver(p2pReceiver, p2pIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(p2pReceiver);
    }

    public boolean isP2pEnabled() {
        return isP2pEnabled;
    }

    public void setIsP2pEnabled(boolean p2pEnabled) {
        isP2pEnabled = p2pEnabled;
    }

    public class SocketServerAsyncTask extends AsyncTask<Void, Void, String> {
        MainActivity activity;
        int port = 0;
        ServerSocket serverSocket;
        Socket socket = null;
        ObjectInput in = null;
        String message;
        Boolean isHost;

        public SocketServerAsyncTask (MainActivity activity, int port, Boolean isHost) {
            this.activity = activity;
            this.port = port;
            this.isHost = isHost;
        }

        @Override
        protected String doInBackground(Void... params) {
            try {
                Log.d("ConnectFour", String.format("Start server on port [%d]", port) );
                serverSocket = new ServerSocket(port);
                while ( !isCancelled() ){
                    Log.d("ConnectFour", "Waiting for client...");
                    socket = serverSocket.accept();
                    in = null;
                    DataInputStream dataIn = new DataInputStream(socket.getInputStream());

                    byte messageType = dataIn.readByte();

                    if (messageType == HANDSHAKE_MSG ) {
                        Log.d("ConnectFour", String.format("Received Msg Type [%d]", HANDSHAKE_MSG) );
                        byte byteSize = dataIn.readByte();
                        byte[] byteData = new byte[byteSize];
                        dataIn.read(byteData);
                        ByteArrayInputStream bis = new ByteArrayInputStream(byteData);
                        in = null;

                        in = new ObjectInputStream(bis);
                        p2pClientAddress = (String) in.readObject();
                        in.close();
                        Log.d("ConnectFour", "Successful Handshake!");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity.getApplicationContext(), "Successful Handshake!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else if (messageType == DATA_MSG ) {
                        Log.d("ConnectFour", String.format("Received Msg Type [%d]", DATA_MSG) );
                        byte byteSize = dataIn.readByte();
                        byte[] byteData = new byte[byteSize];
                        dataIn.read(byteData);
                        ByteArrayInputStream bis = new ByteArrayInputStream(byteData);
                        in = null;

                        in = new ObjectInputStream(bis);
                        message = (String) in.readObject();
                        in.close();
                        Log.d("ConnectFour", "Successfully read message.");
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Log.d("ConnectFour", "Dropping invalid message type" );
                    }
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        // ignore close exception
                    }
                }
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    // ignore close exception
                }
            }
            super.onPostExecute(result);
        }
    }

    private class WriteThread extends Thread {
        MainActivity activity;
        Socket socket;
        String message;
        Object clientData;
        String hostAddress;
        int port = 0;
        int msgType;
        Boolean isHost;

        public WriteThread (MainActivity activity, String hostAddress, int port, int msgType, Object clientData, Boolean isHost) {
            this.activity = activity;
            this.hostAddress = hostAddress;
            this.port = port;
            this.clientData = clientData;
            this.msgType = msgType;
            this.isHost = isHost;
        }

        @Override
        public void run() {
            if ( hostAddress == null ) {
                Log.d("ConnectFour", "No address to send to. Exit write thread.");
                return;
            }
            socket = new Socket();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                int retry = 1;
                socket.bind(null);
                do {
                    Log.d("ConnectFour", String.format("Trying to connect to host[%s:%d] attempt[%d]", hostAddress, port, retry));
                    socket.connect(new InetSocketAddress(hostAddress, port), 500);
                } while (!socket.isConnected() && 5 >= retry++);
                Log.d("ConnectFour", "Successfully connected...");
                Log.d("ConnectFour", "Starting to send data...");
                ObjectOutput objectOut = null;

                objectOut = new ObjectOutputStream(bos);
                if ( msgType == DATA_MSG)
                    objectOut.writeObject(clientData);
                else {
                    p2pClientAddress = socket.getLocalAddress().getHostAddress();
                    Log.d("ConnectFour", "My IP addy - "+p2pClientAddress);
                    objectOut.writeObject(p2pClientAddress);
                }
                objectOut.flush();
                byte[] objectBytes = bos.toByteArray();

                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                dataOut.writeByte(msgType);
                dataOut.writeByte(objectBytes.length);
                dataOut.write(objectBytes);
                dataOut.flush();
                dataOut.close();
                Log.d("ConnectFour", "Successfully sent data.");
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
}
