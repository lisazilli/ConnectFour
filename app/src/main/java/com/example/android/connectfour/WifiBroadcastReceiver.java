package com.example.android.connectfour;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class WifiBroadcastReceiver extends BroadcastReceiver {
    private MainActivity activity;
    private WifiP2pManager p2pManager;
    private Channel p2pChannel;

    public WifiBroadcastReceiver(WifiP2pManager p2pManager, Channel p2pChannel, MainActivity activity) {
        super();
        this.p2pManager = p2pManager;
        this.p2pChannel = p2pChannel;
        this.activity = activity;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String intentAction = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(intentAction)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                activity.setIsP2pEnabled(true);
            } else {
                activity.setIsP2pEnabled(false);
            }
            Log.d("ConnectFour", String.format("P2P State Changed %d", state));
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(intentAction)) {
            Log.d("ConnectFour", "Peers changed");
            if (p2pManager != null) {
                p2pManager.requestPeers(p2pChannel, activity.peerListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(intentAction)) {
            Log.d("ConnectFour", "P2P Connection Changed.");
            if (p2pManager == null) {
                return;
            }
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {
                p2pManager.requestConnectionInfo(p2pChannel, activity.connectionListener);
            } else {
                activity.statusView.setText(R.string.no_conn);
                if (activity.disconnectBtn.getVisibility() == View.VISIBLE) {
                    activity.disconnectBtn.setVisibility(View.INVISIBLE);
                }
                if (activity.peerView.getVisibility() != View.VISIBLE) {
                    activity.peerView.setVisibility(View.VISIBLE);
                }
                if (activity.find_btn.getVisibility() != View.VISIBLE) {
                    activity.find_btn.setVisibility(View.VISIBLE);
                }
                Toast.makeText(activity.getApplicationContext(), R.string.disconnected, Toast.LENGTH_SHORT).show();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(intentAction)) {
            Log.d("ConnectFour", "P2P this device changed.");

        }
    }
}
