package com.example.android.connectfour;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
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
    LinearLayout discoverLayout;
    LinearLayout gameAreaLayout;
    WifiP2pInfo p2pInfo;
    SocketServerAsyncTask p2pHost = null;
    SocketServerAsyncTask p2pClient = null;
    ProgressDialog progressDialog = null;
    PlayView playView = null;
    GridLayout gameboard = null;

    static final int P2P_HOST_PORT = 8888;
    static final int P2P_ClIENT_PORT = 8887;

    static final int HANDSHAKE_MSG = 1;
    static final int DATA_MSG = 2;
    static final int TEXT_MSG = 3;
    static final int RESET_MSG = 4;

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
        discoverLayout = findViewById(R.id.p2pDiscoverLayout);
        gameAreaLayout = findViewById(R.id.gameArea);
        find_btn = findViewById(R.id.findGame);
        wifiSwitch = findViewById(R.id.wifiSwitch);
        sendBtn = findViewById(R.id.sendMsg);
        if (wifiMgr.isWifiEnabled()) {
            wifiSwitch.setChecked(true);
        } else {
            wifiSwitch.setChecked(false);
        }

        gameboard = findViewById(R.id.boardview);
        gameboard.getViewTreeObserver().addOnGlobalLayoutListener(boardLayoutListener());

        connectionListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo info) {
                Log.d("ConnectFour", "Received connection information.");
                p2pInfo = info;
                setupGameArea(true);
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
                    String message = checkMessage();
                    if (message != null)
                        new WriteThread (MainActivity.this, p2pClientAddress, P2P_ClIENT_PORT, TEXT_MSG, message).start();
                } else if (p2pInfo.groupFormed && p2pClientAddress == null ) {
                    new WriteThread (MainActivity.this, p2pInfo.groupOwnerAddress.getHostAddress(), P2P_HOST_PORT, HANDSHAKE_MSG, null).start();
                } else if (p2pInfo.groupFormed ) {
                    Log.d("ConnectFour", "Try to write the client message");
                    String message = checkMessage();
                    if (message != null)
                        new WriteThread (MainActivity.this, p2pInfo.groupOwnerAddress.getHostAddress(), P2P_HOST_PORT, TEXT_MSG, message).start();
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
                        progressDialog = ProgressDialog.show(MainActivity.this, "Pairing Device To",
                                device.deviceName, true, false);
                        Log.d("ConnectFour", String.format("Successully initiated connection with: %s",device.deviceName));
                        disconnectBtn.setOnClickListener(
                                new View.OnClickListener() {

                                    @Override
                                    public void onClick(View v) {
                                        disconnect();
                                    }
                                });
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

    public String checkMessage ( ) {
        TextView msgTextView = findViewById(R.id.message_field);
        String message = msgTextView.getText().toString();

        if ( message == null || message.isEmpty()){
            Toast.makeText(MainActivity.this, "Type a message before sending", Toast.LENGTH_SHORT).show();
            return null;
        }
        return message;
    }

    public void setupGameArea(boolean isGroupConnecting) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (p2pInfo.groupFormed && p2pInfo.isGroupOwner) {
            if (isGroupConnecting) {
                statusView.setText(R.string.host_conn);
                p2pHost = new SocketServerAsyncTask(MainActivity.this, P2P_HOST_PORT, true);
                p2pHost.execute();
            }

            progressDialog = ProgressDialog.show(MainActivity.this, getString(R.string.waiting_title),
                    getString(R.string.waiting_body), true, false);
        } else if (p2pInfo.groupFormed) {
            if (isGroupConnecting) {
                statusView.setText(R.string.client_conn);
                p2pClient = new SocketServerAsyncTask(MainActivity.this, P2P_ClIENT_PORT, false);
                p2pClient.execute();
            }

            // Initiate first handshake if necessary
            if (p2pInfo.groupFormed && p2pClientAddress == null ) {
                progressDialog = ProgressDialog.show(MainActivity.this, "Game Initializing",
                        "Setting up the game", true, false);
                new WriteThread (MainActivity.this, p2pInfo.groupOwnerAddress.getHostAddress(), P2P_HOST_PORT, HANDSHAKE_MSG, null).start();
            }
        }

        if (!isGroupConnecting) {
            // game reset done
            return;
        }
        if (gameAreaLayout.getVisibility() != View.VISIBLE) {
            // Show game area
            disconnectBtn.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            disconnect();
                        }
                    });
            gameAreaLayout.setVisibility(View.VISIBLE);
            playView = new PlayView( MainActivity.this );
        }
        if (discoverLayout.getVisibility() == View.VISIBLE) {
            // Connection exists close discovery layout.
            discoverLayout.setVisibility(View.GONE);
        }

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (p2pHost != null && p2pHost.socket != null) {
            p2pHost.cancel(true);
            //p2pHost.gracefulExit();
        }
        if (p2pClient != null && p2pClient.socket != null) {
            p2pClient.cancel(true);
            //p2pClient.gracefulExit();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public  void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            peerDiscovery();
        }
    }

    public  void clearGame ( ) {
        if (p2pHost != null && p2pHost.socket != null) {
            p2pHost.cancel(true);
            //p2pHost.gracefulExit();
        }
        if (p2pClient != null && p2pClient.socket != null) {
            p2pClient.cancel(true);
            //p2pClient.gracefulExit();
        }
        if ( playView != null) {
            playView.reset();
        }
    }

    public void disconnect() {
        clearGame();
        p2pManager.removeGroup(p2pChannel, new ActionListener() {
            @Override
            public void onFailure(int reasonCode) {
                Log.d("ConnectFour", "Disconnect failed. Reason:" + reasonCode);

            }

            @Override
            public void onSuccess() {
                // Do nothing. Receiver will handle the disconnect
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
        ServerSocket serverSocket = null;
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
                        if (p2pClientAddress != null ) {
                            Log.d("ConnectFour", String.format("We already have the client address. This is a reset...") );
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String temp = new String(p2pClientAddress);
                                    playView.reset();
                                    // After reset set the client address again to keep the previous handshake.
                                    p2pClientAddress = temp;
                                }
                            });
                        } else {
                            byte byteSize = dataIn.readByte();
                            byte[] byteData = new byte[byteSize];
                            dataIn.read(byteData);
                            ByteArrayInputStream bis = new ByteArrayInputStream(byteData);
                            in = null;

                            in = new ObjectInputStream(bis);
                            p2pClientAddress = (String) in.readObject();
                            in.close();
                            Log.d("ConnectFour", "Successfully setup game!");
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(activity.getApplicationContext(), "Successful setup game!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    } else if (messageType == TEXT_MSG ) {
                        Log.d("ConnectFour", String.format("Received Msg Type [%d]", TEXT_MSG) );
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
                        String [] splitMsg = message.split(":");
                        if (splitMsg.length != 3) {
                            Log.d("ConnectFour", String.format("Failed reading message[%s].",message));
                        } else {
                            try {
                                int row = Integer.parseInt(splitMsg[0]);
                                int column = Integer.parseInt(splitMsg[1]);
                                int color = Integer.parseInt(splitMsg[2]);
                                Log.d("ConnectFour", String.format("Successfully read message[%s] row[%d] column[%d] color[%d].",
                                        message,row,column, color ));
                                GameService.getInstance().getCell(row, column).setColor(color);
                                CellView cellView = GameService.getInstance().getCell(row, column);
                                final CellView finalCellView = cellView;
                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        finalCellView.invalidate();
                                    }
                                });
                                Integer integerColor = new Integer(color);
                                checkGameStatus(integerColor);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }

                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                    } else if (messageType == RESET_MSG ) {
                        Log.d("ConnectFour", String.format("Received Msg Type [%d]. Perform Host Response Reset.", RESET_MSG) );
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                playView.respondToHostReset();
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
                gracefulExit ();
            }
            return "";
        }

        private void checkGameStatus(Integer color) {
            final Integer finalColor = color;
            Log.d("ConnectFour", "Checking game status after other players turn." );
            if (GameService.getInstance().isWon(color)) {
                Log.d("ConnectFour", "Other play won game!!!!!" );
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity.getApplicationContext(),
                                String.format("%s Player wins!", GameService.getColorName(finalColor)),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (GameService.getInstance().isFull()) {
                Log.d("ConnectFour", "Other players last move filled board!!!!" );
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity.getApplicationContext(), "Board is full. No winners.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        @Override
        protected void onPostExecute(String result) {
            gracefulExit ();
            super.onPostExecute(result);
        }
        public void gracefulExit (){
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
        @Override
        protected void finalize() throws Throwable {
            gracefulExit ();
            super.finalize();
        }
    }

    ViewTreeObserver.OnGlobalLayoutListener boardLayoutListener()
    {
        return new ViewTreeObserver.OnGlobalLayoutListener()
        {

            @Override
            public void onGlobalLayout()
            {
                if ( playView != null ) {
                    Log.d("ConnectFour", "Layout listener invoked. Create gameboard.");
                    playView.createBoard();
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        gameboard.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                    else
                    {
                        gameboard.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                } else {
                    Log.d("ConnectFour", "Can't create gameboard yet. Create the play view first!!!!!!!!!!");
                }
            }
        };
    }
}
