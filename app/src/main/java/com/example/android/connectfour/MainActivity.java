package com.example.android.connectfour;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
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
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiMgr = (WifiManager) getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        p2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        p2pChannel = p2pManager.initialize(this, getMainLooper(), null);
        p2pReceiver = new WifiBroadcastReceiver(p2pManager, p2pChannel, this);

        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        p2pIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        statusView = findViewById(R.id.statusView);
        peerView = findViewById(R.id.peerList);
        disconnectBtn = findViewById(R.id.disconnectPeer);
        find_btn = findViewById(R.id.findGame);
        wifiSwitch = findViewById(R.id.wifiSwitch);
        if (wifiMgr.isWifiEnabled()) {
            wifiSwitch.setChecked(true);
        } else {
            wifiSwitch.setChecked(false);
        }

        connectionListener = new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo info) {
                if (info.groupFormed && info.isGroupOwner) {
                    statusView.setText(R.string.host_conn);
                } else if (info.groupFormed) {
                    statusView.setText(R.string.client_conn);
                }

                if (disconnectBtn.getVisibility() != View.VISIBLE) {
                    // Previous connection was not disconnected. Show the disconnect btn
                    // ???? Should we auto start a game in this scenario
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
                    find_btn.setVisibility(View.INVISIBLE);
                }
                if (peerView.getVisibility() == View.VISIBLE) {
                    // Connection exists already.
                    peerView.setVisibility(View.INVISIBLE);
                }
            }
        };

        find_btn.setOnClickListener( new View.OnClickListener(){
            @Override
            public void onClick(View v){
                peerDiscovery();

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
                Log.d("ConnectFour", "Begin Peers Available");

                List<WifiP2pDevice> currentPeers = new ArrayList<>(peerList.getDeviceList());
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
    }

    public void disconnect() {

        p2pManager.removeGroup(p2pChannel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d("ConnectFour", "Disconnect failed. Reason:" + reasonCode);

            }

            @Override
            public void onSuccess() {
                findViewById(R.id.disconnectPeer).setVisibility(View.INVISIBLE);
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
        p2pReceiver = new WifiBroadcastReceiver(p2pManager, p2pChannel, this);
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
}
