package com.sadharan.indoor_positioning.client;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class PositioningNavigationActivity extends AppCompatActivity implements View.OnClickListener {
    private WifiDetails wifiDetails;
    private WifiManager wifiManager;
    PositioningNavigationViewModel positioningNavigationViewModel;
    private LocationManager locationManager;
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (!isGranted) {
            System.exit(-1);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    public void initialize() {
        setContentView(R.layout.positioning_navigation_activity);
        this.positioningNavigationViewModel = new ViewModelProvider(this).get(PositioningNavigationViewModel.class);
        if (getIntent().getLongExtra(getString(R.string.block_id_field), -1) >= 0) {
            this.positioningNavigationViewModel.block_id = getIntent().getLongExtra(getString(R.string.block_id_field), -1);
        }
        findViewById(R.id.get_position_button).setOnClickListener(this);
        getIntent().removeExtra(getString(R.string.block_id_field));
        LocalSurveyDatabase localSurveyDatabase = new LocalSurveyDatabase();
        this.positioningNavigationViewModel.setLocalSurveyDatabase(localSurveyDatabase);
        if (this.positioningNavigationViewModel.datapoints == null) {
            try {
                Toast.makeText(getApplicationContext(), "Fetching Datapoints...", Toast.LENGTH_LONG).show();
                this.positioningNavigationViewModel.fetchDatapoints();
                Toast.makeText(getApplicationContext(), R.string.datapoints_fetch_successful, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), R.string.datapoints_fetch_failure, Toast.LENGTH_SHORT).show();
            }
        }
        //Handle Permissions
        if (!(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        //Get managers
        this.wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        this.locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        //Update initial results
        this.wifiDetails = new ViewModelProvider(this).get(WifiDetails.class);
    }

    @Override
    public void onClick(View view) {
        //Check if wifi is on!
        if (!this.wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), R.string.turn_on_wifi, Toast.LENGTH_SHORT).show();
            return;
        }
        //Check if location is on!
        if (!this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(getApplicationContext(), R.string.turn_on_location, Toast.LENGTH_SHORT).show();
            return;
        }
        wifiDetails.scanWifi(this.wifiManager);
        JSONObject signalStrengthJSON;
        try {
            signalStrengthJSON = new JSONObject();
            int count = 0;
            for (ScanResult scanResult : wifiDetails.scanResults) {
                if (scanResult.level >= -70) {
                    count++;
                    signalStrengthJSON.put(scanResult.BSSID, scanResult.level);
                }
            }
            if (count <= 0) {
                Toast.makeText(getApplicationContext(), R.string.ap_error, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.determining_location, Toast.LENGTH_SHORT).show();
            }
            //((TextView) findViewById(R.id.ap_signal_strength_json)).setText(signalStrengthJSON.toString(4));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), R.string.signal_strength_error, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            LocationElement location = determineLocation(signalStrengthJSON);
            ((TextView)findViewById(R.id.current_position)).setText(location.toString());
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), R.string.location_error, Toast.LENGTH_SHORT).show();
        }
    }

    public LocationElement determineLocation(JSONObject signalStrengthJSON) throws JSONException {
        int n=this.positioningNavigationViewModel.datapoints.length();
        JSONObject datapoint;
        JSONObject current_signal_strength_json;
        Iterator<String> ap_bssids;
        String cur_bssid;
        int cost,min_cost=30000,min_cost_index=-1;
        for(int i=0;i<n;i++){
            datapoint=this.positioningNavigationViewModel.datapoints.getJSONObject(i);
            current_signal_strength_json=new JSONObject(datapoint.getString("APSignalStrengthsJSON"));
            ap_bssids=current_signal_strength_json.keys();
            cost=0;
            while (ap_bssids.hasNext()){
                cur_bssid=ap_bssids.next();
                if(signalStrengthJSON.has(cur_bssid)){
                    cost+=Math.abs(signalStrengthJSON.getInt(cur_bssid)-current_signal_strength_json.getInt(cur_bssid));
                }else{
                    cost+=70;
                }
                if(cost>min_cost){
                    break;
                }
            }
            if(cost<min_cost){
                min_cost=cost;
                min_cost_index=i;
            }
        }
        datapoint=this.positioningNavigationViewModel.datapoints.getJSONObject(min_cost_index);
        return new LocationElement((float)datapoint.getDouble("X_Coordinate"),(float)datapoint.getDouble("Y_Coordinate"),datapoint.getInt("FloorID"));
    }
}