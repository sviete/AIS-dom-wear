package pl.sviete.dom;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.NotificationCompat.WearableExtender;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static pl.sviete.dom.AisCoreUtils.REQUEST_LOCATION_PERMISSION;

public class AisFuseLocationService extends Service{
    private static final String TAG = "AisFuseLocationService";

    private static final int UPDATE_INTERVAL_IN_MILLISECONDS = 30000; // 30 sec
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final float LOCATION_CHANGE_IN_DISTANCE_TO_NOTIFY = 10f;
    private static final int LOCATION_ACCURACY_SUITABLE_TO_REPORT = 30;

    private Handler mHandler;
    private Context mContext;

    // fuse
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private String mCurrentAddress;
    private LocationRequest mLocationRequest;
    //
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public IBinder onBind(Intent arg0) {
        Log.d(TAG, "onBind");
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        try {
            stopLocationUpdates();
        } catch (Exception ex){
            Log.e(TAG, ex.getMessage());
        }

        // fuse restart
        Log.d(TAG, "startLocationUpdates");
        startLocationUpdates();

        // Return START_NOT_STICKY so we can ensure that if the
        // service dies for some reason, it should not start back.
        return Service.START_NOT_STICKY;
    }


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        //
        mHandler = new Handler(); // this is attached to the main thread and the main looper
        mContext = this.getApplicationContext();

        initializeFuseLocationManager();
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        //
        createWifiBrodcastReceiver();
    }

    private void createWifiBrodcastReceiver(){
        //
        // handler for received data from service
        mBroadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                    // report location
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            reportLocationToAisGate();
                        }
                    }, 3500);
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    private void buildLocationSettingsRequest() {
        Log.d(TAG, "buildLocationSettingsRequest");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
    }

    private void createLocationRequest() {
        Log.d(TAG, "createLocationRequest");
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        // Request the most precise location possible
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // No location updates if the device does not move or cross that distance.
        mLocationRequest.setSmallestDisplacement(LOCATION_CHANGE_IN_DISTANCE_TO_NOTIFY);

        //
    }



    // send location info to gate and show in notification
    private void reportLocationToAisGate() {
        // report location to AIS gate only if it's precise 30m
        try {
            if (mCurrentLocation.hasAccuracy() && mCurrentLocation.getAccuracy() <= LOCATION_ACCURACY_SUITABLE_TO_REPORT) {
                DomWebInterface.updateDeviceLocation(getApplicationContext(), mCurrentLocation);
            }
        }
        catch (Exception ex) {
            // oppo report in Google Play java.lang.NullPointerException:
            Log.e(TAG, "reportLocationToAisGate error", ex);
        }
    }

    /**
     * Creates a callback for receiving location events.
     */
    private void createLocationCallback() {
        Log.d(TAG, "createLocationCallback");
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // This is your most accurate location.
                mCurrentLocation = locationResult.getLastLocation();
                reportLocationToAisGate();

                // get the address from location
                getAddressFromLocation(mCurrentLocation, getApplicationContext(), new GeocoderHandler());
            }
        };
    }

    /**
     * Requests location updates from the FusedLocationApi. Note: we don't call this unless location
     * runtime permission has been granted.
     */
    private void startLocationUpdates() {
        // Begin by checking if the device has the necessary location settings.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        } catch (Exception ex) {
            Log.e(TAG, "fail to stopLocationUpdates", ex);
        }
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        // fuse
        stopLocationUpdates();
        //
        unregisterReceiver(mBroadcastReceiver);

        //
        super.onDestroy();
    }

    private void initializeFuseLocationManager() {
        Log.d(TAG, "initializeFuseLocationManager");
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }


    // get the Geocoder answer without blocking
    public static void getAddressFromLocation(
            final Location location, final Context context, final Handler handler) {
        Thread thread = new Thread() {
            @Override public void run() {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                String result = null;
                try {
                    List<Address> list = geocoder.getFromLocation(
                            location.getLatitude(), location.getLongitude(), 1);
                    if (list != null && list.size() > 0) {
                        Address address = list.get(0);

                        result = address.getAddressLine(0);
                        if (address.getMaxAddressLineIndex() > 0) {
                            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                                result = result + " " + address.getAddressLine(i);
                            }
                        } else {
                            // sending back first address line
                            result = address.getAddressLine(0);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Impossible to connect to Geocoder", e);
                } finally {
                    Message msg = Message.obtain();
                    msg.setTarget(handler);
                    if (result != null) {
                        msg.what = 1;
                        Bundle bundle = new Bundle();
                        bundle.putString("address", result);
                        msg.setData(bundle);
                    } else
                        msg.what = 0;
                    msg.sendToTarget();
                }
            }
        };
        thread.start();
    }

    // handler to show the address in the notification
    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String result;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    mCurrentAddress = bundle.getString("address");
                    DomWebInterface.updateDeviceAddress(getApplicationContext(), mCurrentAddress);
                    break;
                default:
                    mCurrentAddress = "";
            }
        }
    }

}
