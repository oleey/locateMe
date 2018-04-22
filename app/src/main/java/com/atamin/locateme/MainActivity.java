package com.atamin.locateme;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    ProgressDialog progressDialog;
    Context context;
    double longitude, latitude;
    LocationManager locationManager;
    TextView currentLocation;
    private DatabaseReference mFirebaseDatabase;
    private FirebaseDatabase mFirebaseInstance;
    String userCity = "";
    LocationDB locationDB;
    String fullLocation;
    Activity activity;
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
//    String TAG = "LocateMe";
    Location location;
    boolean canGetLocation = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        activity = this;
        progressDialog = new ProgressDialog(this);

        locationDB = new LocationDB();
        currentLocation = (TextView) findViewById(R.id.currentLocation);

        mFirebaseInstance = FirebaseDatabase.getInstance();

        // get reference to 'location' node
        mFirebaseDatabase = mFirebaseInstance.getReference("locationDB");
//        showSettingsAlert();

        getLocation();

    }

    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);

        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");

        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(intent);
            }
        });

        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Showing Alert Message
        alertDialog.show();
    }

    /**
     * Location found , get the new location of the user
     *
     * @param userLocation user's location
     */
    @Override
    public void onLocationChanged(Location userLocation) {

        if (userLocation == null) {
            Log.e("userlocation is null: ", "NO Internet");
            return;
        }

        if (canGetLocation){
            longitude = userLocation.getLongitude();
            Log.v("Logitude ", String.valueOf(longitude));

            latitude = userLocation.getLatitude();
            Log.v("Latitude ", String.valueOf(latitude));
        }else{
            showSettingsAlert();
        }


        /*------- To get city name from coordinates -------- */
        String cityName = null;
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        Log.e("Locale ", Locale.getDefault() + " :locale");
        List<Address> addresses;


        try {
            addresses = gcd.getFromLocation(userLocation.getLatitude(), userLocation.getLongitude(), 1);
            if (addresses.size() > 0) {
                cityName = addresses.get(0).getLocality();
                Log.e("CityNAme ", cityName + "city");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (cityName == null){
            setLoading("Finding Location");
        }else {
            hideLoading();
            String fullLocation = "Longitude : " + userLocation.getLongitude() + "\nLatitude : " + userLocation.getLatitude() + "\n\nMy Current City is: " + cityName;
            Log.e("Full location ", fullLocation);
            currentLocation.setText(fullLocation);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Disabled Provider ", "provider disabled : " + provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Enabled provider" , "provider enabled : " + provider);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Status changed ", "status (" + status + ") changed : " + provider);
    }

    public void setLoading(String message) {
        progressDialog.setIndeterminate(true);
        if (message == null) {
            progressDialog.setMessage("Loading..");
        } else {
            progressDialog.setMessage(message);
        }
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
    }

    public void hideLoading() {
        progressDialog.hide();
    }


    protected void onPause() {
        if (locationManager != null)
        {
            locationManager.removeUpdates(this);
            locationManager = null;
        }
        super.onPause();
    }

    protected void onResume() {
        // ask for location once app resumed
        getLocation();
        super.onResume();
    }

    public void onDestroy() {
        if (locationManager != null)
        {
            // clean services
            locationManager.removeUpdates(this);
            locationManager = null;
        }
        super.onDestroy();
        finish();
    }

    public void getLocation() {
        if (ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        100);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
//            // Permission has already been granted
////            //init
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (locationManager != null) {
                // getting GPS status
                isGPSEnabled = locationManager
                        .isProviderEnabled(LocationManager.GPS_PROVIDER);
                // getting network status
                isNetworkEnabled = locationManager
                        .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (!isGPSEnabled && !isNetworkEnabled) {
                    // no network provider is enabled
                    showSettingsAlert();
                } else {
                    this.canGetLocation = true;
                    // First get location from Network Provider
                    if (isNetworkEnabled) {
                        // if there is a Network provider, request the location of the user
                        if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER)) {
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);

                            Log.d("Network", "Network");
                            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            onLocationChanged(location);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }

                    // if there is a GPS provider, request the location of the user
                    if (isGPSEnabled) {
                        if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER)) {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                            Log.d("GPS Enabled", "GPS Enabled");
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            onLocationChanged(location);
                            // if GPS Enabled get lat/long using GPS Services
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }

                }
            }
        }
    }
}




