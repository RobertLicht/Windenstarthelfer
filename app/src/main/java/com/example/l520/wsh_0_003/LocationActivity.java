package com.example.l520.wsh_0_003;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;



public class LocationActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    // Create output in Logfile in the terminal to follow action
    private final String TAG = this.getClass().getName();
    //Log.d(TAG, "value of lx: " + lx);

    // declare variables
    TextView textLatitude;
    TextView textLongitude;
    TextView textAccuracyGPS;
    TextView textAltitude;
    TextView textBearing;
    TextView textSpeed;
    TextView textTimeUTC;
    private FusedLocationProviderApi locationProvider = LocationServices.FusedLocationApi;
    private GoogleApiClient googleApiClient; // Create an object to the google API Client
    private LocationRequest locationRequest; // Create an object for Settings
    private LocationManager locationManager;
    private String provider;
    private double myLatitude;
    private double myLongitude;
    private double myAltitude;
    private float  myBearing;
    private float  myAccuracy;
    private float  mySpeed;
    private long   myTimeUTC;
    private static final int MY_PERMISSION_REQUEST_FINE_LOCATION = 101;
    private static final int MY_PERMISSION_REQUEST_CROASE_LOCATION = 102;
    private boolean permissionIsGranted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        // Check if gps is enabled
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        // initialize  variables and wire up with xml file activity_location
        textLatitude = (TextView) findViewById(R.id.tvLatitude);
        textLongitude = (TextView) findViewById(R.id.tvLongitude);
        textAltitude = (TextView) findViewById(R.id.tvAltitudeText);
        textBearing = (TextView) findViewById(R.id.tvBearingText);
        textSpeed = (TextView) findViewById(R.id.tvSpeedText);
        textAccuracyGPS = (TextView) findViewById(R.id.tvAccuracyGPS);
        textTimeUTC = (TextView) findViewById(R.id.tvTimeUTCText);

        // create builder for location based service
        // ATTENTION: This "addApi(AppIndex.API)"was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this) // pressed Alt+Enter to make the implements above
                .addOnConnectionFailedListener(this)
                .addApi(AppIndex.API).build();

        locationRequest = new LocationRequest();
        locationRequest.setInterval(500); // Interval of updates in [ms]
        locationRequest.setFastestInterval(500); // Interval of updates in [ms]
        // Several modes like PRIORITY_BALANCED_POWER_ACCURACY or PRIORITY_LOW_POWER are available
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Get the location Manager
        /*locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);*/

    }


    public void onConnected(@Nullable Bundle bundle) {
        // Create methode
        requestLocationUpdates();

    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Hint: ACCESS_FINE_LOCATION includes ACCESS_CROASE_LOCATION!!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_REQUEST_FINE_LOCATION);

                /*/ Define the criteria how to select the location provider
                Criteria criteria = new Criteria();
                provider = locationManager.getBestProvider(criteria, false);
                Location location = locationManager.getLastKnownLocation(provider);

                locationManager.requestLocationUpdates(provider, 50, 1, (android.location.LocationListener) this);
                */

            }else {
                // set value to true, so older versions still work
                permissionIsGranted = true;
            }
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        // Update textViews
        myLatitude = location.getLatitude();
        myLongitude = location.getLongitude();
        myAltitude = location.getAltitude();
        myBearing = location.getBearing();
        mySpeed = location.getSpeed();
        myTimeUTC = location.getTime();
        myAccuracy = location.getAccuracy();// Get information about gps accuracy
        textLatitude.setText("Breitengrad: " + String.valueOf(myLatitude) + " [deg]");
        textLongitude.setText("Längengrad: " + String.valueOf(myLongitude) + " [deg]");
        textAltitude.setText("Höhe: " + String.valueOf(myAltitude) + " [m]");
        textBearing.setText("Bewegungsrichtung: " + String.valueOf(myBearing) + " [deg]");
        textSpeed.setText("Geschwindigkeit: " + String.valueOf(mySpeed) + " [m/s]");
        textAccuracyGPS.setText("Genauigkeit GPS: " + String.valueOf(myAccuracy) + " [m] " +
                "(68%, Radius nur horizontal)");
        // calculate source format to format: hh, mm, ss, ms
        int seconds = (int) ((myTimeUTC / 1000) % 60);
        int minutes = (int) ((myTimeUTC / (1000 * 60)) % 60);
        int hours = (int)   ((myTimeUTC / (1000 * 60 * 60)) % 24);
        // update textView Time (UTC)
        textTimeUTC.setText("Zeit (UTC)-> " + String.valueOf(hours) + "" +
                " : " + String.valueOf(minutes) + " : " + String.valueOf(seconds));

    }



    // Call the methods from the android life-cycle

    @Override
    protected void onStart() {
        super.onStart();
        // Connect googleApiClient on app startup
        googleApiClient.connect();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (permissionIsGranted) {
            if (googleApiClient.isConnected()) {
                requestLocationUpdates();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (permissionIsGranted) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (permissionIsGranted) {
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // Method for handling the permission result

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case MY_PERMISSION_REQUEST_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // permission granted
                    permissionIsGranted = true;
                } else {
                    // permission denied
                    permissionIsGranted = false;
                    Toast.makeText(getApplicationContext(), "Erlaubnis notwendig", Toast.LENGTH_SHORT).show();
                    textLatitude.setText("Längengrad: Keine Erlaubnis zur Positionsbestimmung");
                    textLongitude.setText("Breitengrad: Keine Erlaubnis zur Positionsbestimmung");
                    textAltitude.setText("Höhe: Keine Erlaubnis zur Positionsbestimmung");
                    textBearing.setText("Bewegungsrichtung:  Keine Erlaubnis zur Positionsbestimmung");
                    textSpeed.setText("Geschwindigkeit:  Keine Erlaubnis zur Positionsbestimmung");
                    textAccuracyGPS.setText("Genauigkeit GPS: Keine Erlaubnis zur Positionsbestimmung");
                    textTimeUTC.setText("Zeit (UTC):  Keine Erlaubnis zur Positionsbestimmung");
                    requestLocationUpdates();
                }
                break;
            case MY_PERMISSION_REQUEST_CROASE_LOCATION:
                // Do something in corase location mode
                Toast.makeText(getApplicationContext(), "Bitte in Modus hohe Genauigkeit wechseln", Toast.LENGTH_SHORT).show();
                break;
        }
    }


}
