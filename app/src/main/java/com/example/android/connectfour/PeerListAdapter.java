package com.example.android.connectfour;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PeerListAdapter extends BaseAdapter {

    List<WifiP2pDevice> peers;

    public PeerListAdapter (List<WifiP2pDevice> peerList){
        peers = peerList;
    }

    @Override
    public int getCount() {
        return peers.size();
    }

    @Override
    public Object getItem(int position) {
        return peers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            WifiP2pDevice peer = peers.get(position);
            Context context = parent.getContext();
            LinearLayout view = new LinearLayout(context);
            view.setOrientation(LinearLayout.HORIZONTAL);
            ImageView imgView = new ImageView(context);
            imgView.setImageResource(R.drawable.device);
            view.addView(imgView);
            TextView deviceTextView = new TextView(context);
            deviceTextView.setText(peer.deviceName+": "+getDeviceStatus(peer.status));
            deviceTextView.setPadding(0, 0, 0, 0);
            view.addView(deviceTextView);
            return view;
        }
        return convertView;
    }

    private static String getDeviceStatus(int deviceStatus) {
        Log.d("ConnectFour", "Peer status:" + deviceStatus);
        switch (deviceStatus) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }
}