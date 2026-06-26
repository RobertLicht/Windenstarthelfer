package com.example.l520.wsh_0_003;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class RecDataActivity extends AppCompatActivity implements SensorEventListener,
             GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
             LocationListener, Runnable {

    // Create output in Logfile in the terminal to follow action
    private final String TAG = this.getClass().getName();
    //Log.d(TAG, "value of lx: " + lx);


    // Declare variables
    // -> Text Sensor Beschleunigung linear
    private TextView textAccX, textAccY, textAccZ, textGeschwGPS, textAtitude, textPressure, textCombiSpeed;
    // -> Button
    //private Button btnDatenAufz, buttonOpenSaveDir;

    //-> Global variables for function onSensorChanged
    private double timestamp, timestampOld = 0;
    private float dT, timeSensorSum = 0, timeSensorSumOld = 0;
    private static final float MS2S = 1.0f / 1000.0f; // Factor for converting milliseconds to seconds
    private static final float NS2S = 1.f / 1000000000.0f; //Factor for converting nanoseconds to seconds

    // -> Global variables for Sensor and Sensor Manager
    private SensorManager wshSensorManager = null;
    private Sensor lin_acc, acc, wshGyroSensor, wshBaro;
    final int SensorTimeDelay = 125 * 100; // 20*1000 [ms] <-> 50 [Hz]// 125*100 [ms] <-> 80 [Hz]

    // Number of runs in if-case for calculating the mean values
    private short numVal = SensorTimeDelay / (125 * 10); // Number of loops for mean value calculation
    private short numCalm = (short) (numVal * 3); // Number of loops to wait for reset

    //-> Global variables for location services
    private GoogleApiClient googleApiClient; // Create an object to the google API Client
    private LocationRequest locationRequest; // Create an object for Settings
    //private static final int MY_PERMISSION_REQUEST_FINE_LOCATION = 101;
    private boolean permissionIsGranted = false;

    //-> Global variables for getLocationData
    private double GPSaltitudeOld = 0;
    private double GPStimestampOld = 0, GPSdT = 0, timeRecGPSsum = 0;
    private long countRecGPS = 0;

    /*
    //-> Global variables for function getAccelerometer
    private float[] ArrSumAcelero = new float[3];
    private short icounterA = 0;
    private float[] OutAcelero = new float[3];
    */

    //-> Global variables for function getLinearAccelerometer
    private float[] ArrVelocityOld = new float[3];
    private float[] ArrSumAcc = new float[3];
    private float[] ArrMeanAcc = new float[3];
    private short icounter = 0, countCalm = 0;
    private float[] ArrVelocityIntgl = new float[3];
    private float[] ArrVeloIntegrlOld = new float[3];

    //-> Global variables for getGyroscop
    private short icounterG = 0;
    private float[] ArrGyroAngOld = new float[3];
    private float[] ArrMeanGyro = new float[3];

    //-> Global variables for getBaroPressure
    private short icounterP = 0;
    private float BaroSUMhPa = 0, MeanBarohPa = 0, BaroVertSpeedOld = 0;
    private double BaroRefPressure = 1013.25;


    //-> Global variables for initStartButton
    // Indicate if the output should be logged to a .csv file
    private boolean logData = false;
    private boolean dataReady = false;
    private Thread thread;

    //-> Global variables for data log
    private long Relationname = 0; // The generation / ID of the log output
    private String log; // Output log
    // Handler for the UI plots so everything plots smoothly
    protected Handler handler;
    protected Runnable runable;
    private static final int REQUEST_READ_PHONE_STATE = 110, REQUEST_ACCESS_FINE_LOCATION = 111, REQUEST_WRITE_STORAGE = 112;

    //=> Global variables available for the log
    private float[] OutArrAcc = new float[3];
    private float[] OutArrVeloRES = new float[3];
    private float[] OutGyroV = new float[3];
    private float[] OutArrGyroAnRES = new float[3];
    private float BaroAltitudeOld = 0;
    private float BaroVertSpeedRES = 0;
    private double OutGPSspeed = 0, OutGPSspeedOnly;
    private double OutGPSaccuracy = 0;

    private float logTimeAccOld = 0;
    private float[] ArrVelocity = new float[3];
    private float[] OutArrVeloInegrlRES = new float[3];
    private float logTimeBaroOld = 0;
    private float absCombiSpeed = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rec_data);
        // Keep the Display and CPU on high level
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // Get an instance of the sensor service, and use that to get an instance of a particular sensor.
        // Create Sensor Manager
        wshSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        //if (wshSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null){
        lin_acc = wshSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        //wshSensorManager.registerListener(this, lin_acc, SensorTimeDelay); // Register at onResume
        //}

        // Necessary for tests on the PC
        //if (wshSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
        acc = wshSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //wshSensorManager.registerListener(this, acc, SensorTimeDelay); // Register at onResume
        //}

        //if (wshSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
        wshGyroSensor = wshSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //wshSensorManager.registerListener(this, wshGyroSensor, SensorTimeDelay); // Register at onResume
        //}

        //if (wshSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
        wshBaro = wshSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        //wshSensorManager.registerListener(this, wshBaro, SensorTimeDelay); // Register at onResume
        //}

        // !!!!Sensors are registert in the android lifecycle!!!


        // Assign TextViews
        // -> Text Sensordaten -  Beschleunigung (linear)
        textAccX = (TextView) findViewById(R.id.textAccX);
        textAccY = (TextView) findViewById(R.id.textAccY);
        textAccZ = (TextView) findViewById(R.id.textAccZ);
        textGeschwGPS = (TextView) findViewById(R.id.textGeschwGPS);
        textAtitude = (TextView) findViewById(R.id.textAtitude);
        textPressure = (TextView) findViewById(R.id.textPressure);
        textCombiSpeed = (TextView) findViewById(R.id.textCombiSpeed);

        // Call function to initiate the button for logging
        initStartButton();
        // TODO write function to initiate UI -> initUI();

        // Check if writing permission is enabled
        //checkWritingPermission();

        // Check if gps is enabled
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        // create builder for location based service
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

        /*
        --------------------------------------------
        Request for permissions
        --------------------------------------------
         *
        boolean hasPermissionPhoneState = (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermissionPhoneState) {
            ActivityCompat.requestPermissions(LoginActivity.this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    REQUEST_READ_PHONE_STATE);
        }
        */
        boolean hasPermissionLocation = (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermissionLocation) {
            ActivityCompat.requestPermissions(RecDataActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION);
        }

        boolean hasPermissionWrite = (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermissionWrite) {
            ActivityCompat.requestPermissions(RecDataActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
        /*
        --------------------------------------------
                Request for permission
        --------------------------------------------
        */


    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not in use
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // Get the timestamp
        timestamp = event.timestamp;
        if (timestampOld != 0) {
            // Calculate dT
            dT = (float) Math.abs((timestamp - timestampOld) * NS2S); // dT in [s]
            // Create time for record
            double freqSensor = (1 / dT); // Frequency in [Hz]
            timeSensorSum += dT; // in [s]
        }
        // Update timestampOld
        timestampOld = timestamp;

        /*
        ----------------------------------------
        Read values from different sensors
        ----------------------------------------
        */
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            // Get event data
            getLinearAccelerometer(event); // Call function to get data, parameter is event

        /*}
        else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER){
          // Get event data
            getAccelerometer(event); // Call function to get data, parameter is event
            */
        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // Get event data
            getGyroscop(event); // Call function to get data, parameter is event

        } else if (sensor.getType() == Sensor.TYPE_PRESSURE) {
            // Get event data
            getBaroPressure(event); /// Call function to get data, parameter is event
        }

    }

    /*
    // Get Data of linear acceleration by function
    private void getAccelerometer(SensorEvent event) // Here parameters are SensorEvent and event
    {
        float[] ArrAcelero = event.values;// Acceleration in [m/s^2]

        // Smooth value by using mean-value
        if (icounterA < numVal) // Add some samples
        {
            ArrSumAcelero[0] += ArrAcelero[0];
            ArrSumAcelero[1] += ArrAcelero[1];
            ArrSumAcelero[2] += ArrAcelero[2];
            icounterA++;
        }

        float[] ArrMeanAcelero = new float[3];
        if (icounterA == numVal) // Calculate mean acceleration
        {
            ArrMeanAcelero[0] = ArrSumAcelero[0] / numVal;
            ArrMeanAcelero[1] = ArrSumAcelero[1] / numVal;
            ArrMeanAcelero[2] = ArrSumAcelero[2] / numVal;
            // Reset Sum and counter
            icounter = 0;
            Arrays.fill(ArrSumAcelero, 0);

            // Output for log
            OutAcelero = ArrMeanAcelero;
        }
    }
    */


    // Get Data of linear acceleration by function
    private void getLinearAccelerometer(SensorEvent event) // Here parameters are SensorEvent and event
    {
        // Get values from SensorEvent
        float[] valuesACC = event.values;
        // linear Movement in [m/s^2]
        float[] ArrLacc = new float[3]; // Create Array with space three entries
        ArrLacc[0] = valuesACC[0];
        ArrLacc[1] = valuesACC[1];
        ArrLacc[2] = valuesACC[2];

        if (icounter < 1){
            Arrays.fill(ArrLacc, 0);
            // Restore counter
            icounter = 0;
        }

        // Output for log
        OutArrAcc = valuesACC;

                    /*
                     * TODO: Geschwindigkeit durch numerische Integration berechnen:
                     * -> Fläche unter der Linie der Funktion entspricht der Geschwindigkeit im Zeitabschnitt
                     * https://de.wikipedia.org/wiki/Trapezregel#Zusammengesetzte_Sehnentrapezformel
                     * https://en.wikipedia.org/wiki/Trapezoidal_rule
                     */
                float a = timeSensorSumOld; // in [s]
                float b = timeSensorSum; // in [s]
                byte n = 12;
                double h = (b - a) / n;


                // Summierte simpsonsche Formel (Keplersche Fassregel) - START
                float[] sumT = new float[3];
                for (byte kT = 1; kT < n; kT++) // Summe der Sehnentrapezregel
                {
                    float xkT = (float) (a + (kT * h));
                    sumT[0] += (ArrLacc[0] * xkT);
                    sumT[1] += (ArrLacc[1] * xkT);
                    sumT[2] += (ArrLacc[2] * xkT);
                }

                float[] sumM = new float[3];
                for (byte kM = 1; kM <= n; kM++) // Summe der Tangententrapezregel
                {
                    float xkM = (float) (a + (kM * h));
                    sumM[0] += (ArrLacc[0] * (0.5 * ((xkM - 1) + xkM)));
                    sumM[1] += (ArrLacc[1] * (0.5 * ((xkM - 1) + xkM)));
                    sumM[2] += (ArrLacc[2] * (0.5 * ((xkM - 1) + xkM)));
                }

                // Berechne: S(f) = (h/6)*(f(a) + 2*(sumT) + f(b) + 4*(sumM))
                ArrVelocityIntgl[0] = (float) ((h / 6) * ((ArrLacc[0] * a) + (2 * (sumT[0])) + (ArrLacc[0] * b) + (4 * (sumM[0]))));
                ArrVelocityIntgl[1] = (float) ((h / 6) * ((ArrLacc[1] * a) + (2 * (sumT[1])) + (ArrLacc[1] * b) + (4 * (sumM[1]))));
                ArrVelocityIntgl[2] = (float) ((h / 6) * ((ArrLacc[2] * a) + (2 * (sumT[2])) + (ArrLacc[2] * b) + (4 * (sumM[2]))));
                // Summierte simpsonsche Formel (Keplersche Fassregel) - END

                // Update Time
                timeSensorSumOld = timeSensorSum;

                /*
                // Zusammengesetzte Tangententrapezformel (Mittelpunktsregel) - START
                float[] sumMittTrapez = new float[3];
                for (byte iM = 1; iM <= n; iM++) {
                    sumMittTrapez[0] += (ArrMeanAcc[0] * (a - (0.5 * h) + (iM * h)));
                    sumMittTrapez[1] += (ArrMeanAcc[1] * (a - (0.5 * h) + (iM * h)));
                    sumMittTrapez[2] += (ArrMeanAcc[2] * (a - (0.5 * h) + (iM * h)));
                }
                // Berechnnung: M(f) = h * (sum(f)(a-(0.5*h)+(i*h))))
                ArrVelocityIntgl[0] = (float) (h * sumMittTrapez[0]);
                ArrVelocityIntgl[1] = (float) (h * sumMittTrapez[1]);
                ArrVelocityIntgl[2] = (float) (h * sumMittTrapez[2]);
                // Zusammengesetzte Tangententrapezformel (Mittelpunktsregel) - END

                /*
                // Zusammengesetzte Sehnentrapezformel - START
                float[] sumTrapez = new float[3];
                for (byte i = 1; i < n; i++){
                    sumTrapez[0] += (ArrMeanAcc[0] * (a + i*h));
                    sumTrapez[1] += (ArrMeanAcc[1] * (a + i*h));
                    sumTrapez[2] += (ArrMeanAcc[2] * (a + i*h));
                }
                // Berechnung: T(f) = h * (0.5*f(a) + 0.5*f(b) + sum(f(a+(i*h))))
                ArrVelocityIntgl[0] = (float) (h*((0.5*ArrMeanAcc[0]*a)+(0.5*ArrMeanAcc[0]*b)+sumTrapez[0]));
                ArrVelocityIntgl[1] = (float) (h*((0.5*ArrMeanAcc[1]*a)+(0.5*ArrMeanAcc[1]*b)+sumTrapez[1]));
                ArrVelocityIntgl[2] = (float) (h*((0.5*ArrMeanAcc[2]*a)+(0.5*ArrMeanAcc[2]*b)+sumTrapez[2]));
                // Zusammengesetzte Sehnentrapezformel - END
                */

        /*
        ---------------------------------------------------------------------------
        START | Code for TextView-Updates
        ---------------------------------------------------------------------------
         */

        // Smooth value by using mean-value
        if (icounter < numVal) // Add some samples
        {
            ArrSumAcc[0] += ArrLacc[0];
            ArrSumAcc[1] += ArrLacc[1];
            ArrSumAcc[2] += ArrLacc[2];
            icounter++;
        }

        if (icounter == numVal) // Calculate mean acceleration
        {
            ArrMeanAcc[0] = ArrSumAcc[0] / numVal;
            ArrMeanAcc[1] = ArrSumAcc[1] / numVal;
            ArrMeanAcc[2] = ArrSumAcc[2] / numVal;

            // Calculate Absolute-vector
            float MeanAccABS = (float) Math.sqrt((ArrMeanAcc[0] * ArrMeanAcc[0]) + (ArrMeanAcc[1] * ArrMeanAcc[1]) + (ArrMeanAcc[2] * ArrMeanAcc[2]));

            // Set acceleration to zero if device is not accelerated
            if (MeanAccABS < 0.04) // Set MeanAccABS and counter to 0
            {
                countCalm++; // Increase counterCalm
                if (countCalm > numCalm) {
                    Arrays.fill(ArrMeanAcc, 0);
                    Arrays.fill(ArrVelocity, 0);
                    Arrays.fill(ArrVelocityOld, 0);
                    Arrays.fill(ArrVelocityIntgl,0);
                    Arrays.fill(ArrVeloIntegrlOld,0);
                    countCalm = 0;
                }
            }
            // Reset Sum and counter
            Arrays.fill(ArrSumAcc, 0);
            icounter = 0;

            if (logTimeAccOld != 0) {
                // Calculate deltaTime
                float dTacc = (timeSensorSum - logTimeAccOld);
                // Code for slow writing log-file
                ArrVelocity[0] = ArrMeanAcc[0] * dTacc;
                ArrVelocity[1] = ArrMeanAcc[1] * dTacc;
                ArrVelocity[2] = ArrMeanAcc[2] * dTacc;


                    /*
                     * TODO: Geschwindigkeit durch numerische Integration berechnen:
                     * -> Fläche unter der Linie der Funktion entspricht der Geschwindigkeit im Zeitabschnitt
                     * https://de.wikipedia.org/wiki/Trapezregel#Zusammengesetzte_Sehnentrapezformel
                     * https://en.wikipedia.org/wiki/Trapezoidal_rule
                     *
                float a = logTimeAccOld; // in [s]
                float b = timeSensorSum; // in [s]
                byte n = 12;
                double h = (b - a) / n;

                /*
                // Summierte simpsonsche Formel (Keplersche Fassregel) - START
                float[] sumT = new float[3];
                for (byte kT = 1; kT < n; kT++) // Summe der Sehnentrapezregel
                {
                    float xkT = (a + (kT * h));
                    sumT[0] += (ArrMeanAcc[0] * xkT);
                    sumT[1] += (ArrMeanAcc[1] * xkT);
                    sumT[2] += (ArrMeanAcc[2] * xkT);
                }

                float[] sumM = new float[3];
                for (byte kM = 1; kM <= n; kM++) // Summe der Tangententrapezregel
                {
                    float xkM = (a + (kM * h));
                    sumM[0] += (ArrMeanAcc[0] * (0.5 * ((xkM - 1) + xkM)));
                    sumM[1] += (ArrMeanAcc[1] * (0.5 * ((xkM - 1) + xkM)));
                    sumM[2] += (ArrMeanAcc[2] * (0.5 * ((xkM - 1) + xkM)));
                }

                // Berechne: S(f) = (h/6)*(f(a) + 2*(sumT) + f(b) + 4*(sumM))
                ArrVelocityIntgl[0] = (h / 6) * ((ArrMeanAcc[0] * a) + (2 * (sumT[0])) + (ArrMeanAcc[0] * b) + (4 * (sumM[0])));
                ArrVelocityIntgl[1] = (h / 6) * ((ArrMeanAcc[1] * a) + (2 * (sumT[1])) + (ArrMeanAcc[1] * b) + (4 * (sumM[1])));
                ArrVelocityIntgl[2] = (h / 6) * ((ArrMeanAcc[2] * a) + (2 * (sumT[2])) + (ArrMeanAcc[2] * b) + (4 * (sumM[2])));
                // Summierte simpsonsche Formel (Keplersche Fassregel) - END
                *

                // Zusammengesetzte Tangententrapezformel (Mittelpunktsregel) - START
                float[] sumMittTrapez = new float[3];
                for (byte iM = 1; iM <= n; iM++) {
                    sumMittTrapez[0] += (ArrMeanAcc[0] * (a - (0.5 * h) + (iM * h)));
                    sumMittTrapez[1] += (ArrMeanAcc[1] * (a - (0.5 * h) + (iM * h)));
                    sumMittTrapez[2] += (ArrMeanAcc[2] * (a - (0.5 * h) + (iM * h)));
                }
                // Berechnnung: M(f) = h * (sum(f)(a-(0.5*h)+(i*h))))
                ArrVelocityIntgl[0] = (float) (h * sumMittTrapez[0]);
                ArrVelocityIntgl[1] = (float) (h * sumMittTrapez[1]);
                ArrVelocityIntgl[2] = (float) (h * sumMittTrapez[2]);
                // Zusammengesetzte Tangententrapezformel (Mittelpunktsregel) - END

                /*
                // Zusammengesetzte Sehnentrapezformel - START
                float[] sumTrapez = new float[3];
                for (byte i = 1; i < n; i++){
                    sumTrapez[0] += (ArrMeanAcc[0] * (a + i*h));
                    sumTrapez[1] += (ArrMeanAcc[1] * (a + i*h));
                    sumTrapez[2] += (ArrMeanAcc[2] * (a + i*h));
                }
                // Berechnung: T(f) = h * (0.5*f(a) + 0.5*f(b) + sum(f(a+(i*h))))
                ArrVelocityIntgl[0] = (float) (h*((0.5*ArrMeanAcc[0]*a)+(0.5*ArrMeanAcc[0]*b)+sumTrapez[0]));
                ArrVelocityIntgl[1] = (float) (h*((0.5*ArrMeanAcc[1]*a)+(0.5*ArrMeanAcc[1]*b)+sumTrapez[1]));
                ArrVelocityIntgl[2] = (float) (h*((0.5*ArrMeanAcc[2]*a)+(0.5*ArrMeanAcc[2]*b)+sumTrapez[2]));
                // Zusammengesetzte Sehnentrapezformel - END
                */
            }
        }
        // Update timestamp
        logTimeAccOld = timeSensorSum;

        // Round values before updating TextViews
        float[]  dispArrMeanAcc = new float[3];
        dispArrMeanAcc[0] = (float) (((int) (ArrMeanAcc[0] * 100)) / 100.0);
        dispArrMeanAcc[1] = (float) (((int) (ArrMeanAcc[1] * 100)) / 100.0);
        dispArrMeanAcc[2] = (float) (((int) (ArrMeanAcc[2] * 100)) / 100.0);
        // Display linear acceleration, update TextView
        textAccX.setText("X [m/s^2]: " + String.valueOf(dispArrMeanAcc[0]));
        textAccY.setText("Y [m/s^2]: " + String.valueOf(dispArrMeanAcc[1]));
        textAccZ.setText("Z [m/s^2]: " + String.valueOf(dispArrMeanAcc[2]));


        /*
        ---------------------------------------------------------------------------
        END | Code for TextView-Updates
        ---------------------------------------------------------------------------
        */

        /*
        // Calculate current velocity vectors in [m/s]
        float[] ArrVelocity = new float[3]; // Create Array with space for three entries
        ArrVelocity[0] = ArrLacc[0] * dT;
        ArrVelocity[1] = ArrLacc[1] * dT;
        ArrVelocity[2] = ArrLacc[2] * dT;
        */

        // Calculate Resulting velocity vectors
        float[] ArrVelocityRES = new float[3]; // Create Array with space three entries
        ArrVelocityRES[0] = ArrVelocity[0] + ArrVelocityOld[0];
        ArrVelocityRES[1] = ArrVelocity[1] + ArrVelocityOld[1];
        ArrVelocityRES[2] = ArrVelocity[2] + ArrVelocityOld[2];

        // Calculate Resulting velocity vectors from trapezodial rule
        float[] ArrIntegrlRES = new float[3];
        ArrIntegrlRES[0] = ArrVelocityIntgl[0] + ArrVeloIntegrlOld[0];
        ArrIntegrlRES[1] = ArrVelocityIntgl[1] + ArrVeloIntegrlOld[1];
        ArrIntegrlRES[2] = ArrVelocityIntgl[2] + ArrVeloIntegrlOld[2];

        // Update arrays
        ArrVelocityOld = ArrVelocityRES;
        ArrVeloIntegrlOld = ArrIntegrlRES;

        // Output for log
        OutArrVeloRES = ArrVelocityRES;
        OutArrVeloInegrlRES = ArrIntegrlRES;

    }



    // Get Data of linear acceleration by function
    private void getGyroscop(SensorEvent event) // Here parameters are SensorEvent and event
    {
        // Get values from SensorEvent
        float[] valuesGy = event.values;
        // Gyroscop calibrated in [rad/s], rotation mathematical positive
        float[] ArrGyroV = new float[3]; // Create Array with space three entries
        ArrGyroV[0] = valuesGy[0];
        ArrGyroV[1] = valuesGy[1];
        ArrGyroV[2] = valuesGy[2];

        /*
        // Output for log
        OutGyroV = valuesGy;
        */

        // Smooth value by using mean-value
        float[] ArrSumGyro = new float[3];
        if (icounterG < numVal) // Add some samples
        {
            ArrSumGyro[0] += ArrGyroV[0];
            ArrSumGyro[1] += ArrGyroV[1];
            ArrSumGyro[2] += ArrGyroV[2];
            icounterG++;
        }

        if (icounterG == numVal) // Calculate mean value
        {

            ArrMeanGyro[0] = ArrSumGyro[0] / numVal;
            ArrMeanGyro[1] = ArrSumGyro[1] / numVal;
            ArrMeanGyro[2] = ArrSumGyro[2] / numVal;

            // Output for log
            OutGyroV = ArrMeanGyro;

            // Reset Sum and counter
            ArrSumGyro[0] = 0; ArrSumGyro[1] = 0; ArrSumGyro[2] = 0;
            icounterG = 0;
        }

        // Calculate current angle along the vectors in [rad]
        float[] ArrGyroAn = new float[3]; // Create Array with space three entries
        ArrGyroAn[0] = ArrGyroV[0] * dT;
        ArrGyroAn[1] = ArrGyroV[1] * dT;
        ArrGyroAn[2] = ArrGyroV[2] * dT;

        // Calculate resulting angle
        float[] ArrGyroAnRES = new float[3];
        ArrGyroAnRES[0]  = ArrGyroAn[0] + ArrGyroAngOld[0];
        ArrGyroAnRES[1]  = ArrGyroAn[0] + ArrGyroAngOld[1];
        ArrGyroAnRES[2]  = ArrGyroAn[0] + ArrGyroAngOld[2];

        // Output for log
        OutArrGyroAnRES = ArrGyroAnRES;

        // Update ArrGyroVOld
        ArrGyroAngOld = ArrGyroAn;
    }

    // Get Data of pressure by function
    private void getBaroPressure(SensorEvent event) // Here parameters are SensorEvent and event
    {
        // Get values from SensorEvent
        // Ambient air pressure in [hPa] or [mbar] (1013.25 [hPa] <-> 1013.25 [mbar] => 1 [hPa] <=> 1 [mbar])
        float valBarohPa = event.values[0];

        // Smooth value by using mean-value
        if (icounterP < numVal) // Add some samples
        {
            BaroSUMhPa += valBarohPa;
            icounterP++;
        }

        if (icounterP == numVal) // Calculate mean pressure
        {
            // Calculate average value
            MeanBarohPa = BaroSUMhPa / numVal;

            // Reset Sum and counter
            BaroSUMhPa = 0;
            icounterP = 0;
            /*
            * Internationale Höhenformel bei Standardatmosphäre
            * Temperatur: 15 [°C] = 288.15 [K], Luftdruck: 1013.25 [hPa]
            * Temperaturgradient: 0.65 [K] / 100 [m] = 0.0065 [K/m]
            */
            // Berechnung des inversen Isentropenkoeffizienten
            double invIsentrKoeffizent = (1.235 - 1) / 1.235; // Kappa = 1.235

            // Barometric formula, relocated to altitude
            double BaroAltitude = (288.15 / 0.0065) * (1 - (Math.pow((MeanBarohPa / BaroRefPressure), (invIsentrKoeffizent))));
            // Out to log
            BaroAltitudeOld = (float) BaroAltitude;

            // Round value (method only available for datatype double)
            BaroAltitude = (Math.round(BaroAltitude * 10) / 10.0);
            MeanBarohPa = (float) (Math.round(MeanBarohPa * 100) / 100.0);
            // Update TextView
            textAtitude.setText("Höhe üNN. [m]: " + String.valueOf(BaroAltitude));
            textPressure.setText("  Luftdruck [hPa]: " + String.valueOf(MeanBarohPa));
            // If-instruction for slow running log file
            if (logTimeBaroOld != 0) {
                float dTbaro = (timeSensorSum - logTimeBaroOld);
                // Calculate vertical speed in [m/s] based on pressure change
                float BaroVertSpeed = ((float) BaroAltitude - BaroAltitudeOld) / dTbaro;

                // Calculating resulting vertical speed [m/s] based on pressure change
                BaroVertSpeedRES = BaroVertSpeed + BaroVertSpeedOld;
            }
            // Update timestamp
            logTimeBaroOld = timeSensorSum;
        }

        // Update value
        BaroVertSpeedOld = BaroVertSpeedRES;
    }


    /*
    ------------------------------------------------------
    Ask user for Permission if OS requires so
    ------------------------------------------------------
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            /*
            case REQUEST_READ_PHONE_STATE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(LoginActivity.this, "Permission granted.", Toast.LENGTH_SHORT).show();
                    //reload my activity with permission granted or use the features what required the permission
                    finish();
                    startActivity(getIntent());
                } else
                {
                    Toast.makeText(LoginActivity.this, "The app was not allowed to get your phone state. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                }
            }
            */
            case REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    //Toast.makeText(RecDataActivity.this, "Zugriff gestattet", Toast.LENGTH_SHORT).show();
                    //reload my activity with permission granted or use the features what required the permission
                    finish();
                    startActivity(getIntent());
                } else
                {
                    //Toast.makeText(getApplicationContext(), "The app was not allowed to get your location. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                    Toast.makeText(getApplicationContext(),
                            "Erlaubnis ist notwendig", Toast.LENGTH_SHORT).show();
                }
            }

            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    //Toast.makeText(RecDataActivity.this, "Zugriff gestattet", Toast.LENGTH_SHORT).show();
                    //reload my activity with permission granted or use the features what required the permission
                    finish();
                    startActivity(getIntent());
                } else
                {
                    //Toast.makeText(getApplicationContext(), "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                    Toast.makeText(getApplicationContext(),
                            "Erlaubnis ist notwendig", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    //TODO better integration of requestLocationUpdates, if possible
    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Hint: ACCESS_FINE_LOCATION includes ACCESS_CROASE_LOCATION!!!
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_ACCESS_FINE_LOCATION);
            }else {
                // set value to true, so older versions still work
                permissionIsGranted = true;
            }
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }
    /*
    ------------------------------------------------------
    End of ask user for Permission if OS requires so
    ------------------------------------------------------
     */


    /*
    ----------------------------------------------------
    Android location related services
    ----------------------------------------------------
     */

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    // Call method and check for permission
        requestLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Not in ues now
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Not in use now
    }

    @Override
    public void onLocationChanged(Location location) {

        // Call function to pass data, parameter is location
        getLocationData(location);
    }

    /*
    ----------------------------------------------
    Get Data of location services by function
    ----------------------------------------------
     */
    private void getLocationData(Location location) // Here parameters are Location and location
    {
        // Count updates
        countRecGPS++;

        // Get timestamps and calculate dT
        float GPStimestamp = System.currentTimeMillis();
        if (GPStimestampOld != 0){
            GPSdT = Math.abs((GPStimestamp - GPStimestampOld) * MS2S);
            // Create time for record
            double freqRecGPS = ( 1 / GPSdT); // Frequency in [Hz]
            timeRecGPSsum += GPSdT;
        }
        // Update GPStimestampOld
        GPStimestampOld = GPStimestamp;

        // Get data form location services
        double[] LocationData = new double[7];
        //LocationData[0] = location.getLatitude();  // Latitude (Breitengrad)
        //LocationData[1] = location.getLongitude(); // Longitude (Längengrad)
        LocationData[2] = location.getAltitude();  // Altitude (Höhe)
        //LocationData[3] = location.getBearing();   // Bearing (Bewegungsrichtung)
        LocationData[4] = location.getSpeed();     // Speed (Geschwindigkeit horizental)
        LocationData[5] = location.getTime();      // Time (GPS/GLONASS - Zeit in UTC)
        LocationData[6] = location.getAccuracy();  // Accuracy horizotal in [m]

        // calculate source format to format: hh, mm, ss, ms
        int seconds = (int) ((LocationData[5] / 1000) % 60);
        int minutes = (int) ((LocationData[5] / (1000 * 60)) % 60);
        int hours = (int)   ((LocationData[5] / (1000 * 60 * 60)) % 24);

        // Calculate vertical speed
        double GPSaltitude = LocationData[2];
        if (GPSaltitudeOld != 0) {
            // Calculate distance between updates in [m]
            double dGPSaltitude = GPSaltitude - GPSaltitudeOld;

            // Compare Result with horizental accuracy
            if (dGPSaltitude < LocationData[6]){
                dGPSaltitude = 0; // Set Delta to zero
            }

            // Catch if GPSdT is zero
            if (GPSdT == 0){GPSdT = 1;}

            // Calculate vertical velocity in [m/s]
            double GPSaltSpeed = dGPSaltitude / GPSdT;

            // Calculating absolute GPS speed in [m/s]
            double GPSabsSpeed = Math.sqrt((GPSaltSpeed * GPSaltSpeed) + (LocationData[4] * LocationData[4]));

            // Convert absolute GPS speed to [km/h]
            GPSabsSpeed *= 3.6;

            // Round value of absolute GPS speed
            //GPSabsSpeed = (double) (((int) (GPSabsSpeed * 100)) / 100.0);
            GPSabsSpeed = (Math.round(GPSabsSpeed * 10) / 10.0);

            // Update TextView textGeschwGPS
            textGeschwGPS.setText(String.valueOf(GPSabsSpeed) + " [km/h]");

            // Out ot log
            OutGPSspeed = GPSabsSpeed;

        }
        // Update GPSaltitudeOld
        GPSaltitudeOld = GPSaltitude;
        // Out ot log
        OutGPSspeedOnly = LocationData[4];
        OutGPSaccuracy = LocationData[6];

        /*
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
        Calculate Velocity by combined Data (GPS horizontal Speed + Baro vertical Speed)
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-
         */
        absCombiSpeed = (float) Math.sqrt((BaroVertSpeedRES * BaroVertSpeedRES) + (LocationData[4] * LocationData[4]));
        // Convert value to [km/h]
        absCombiSpeed *= 3.6;
        // Round value
        absCombiSpeed = (float) (((int) (absCombiSpeed * 10)) / 10.0);
        // Update textView
        textCombiSpeed.setText(String.valueOf(absCombiSpeed) + " [km/h]");
    }


    /*
    ---------------------------------------------------
    Handle buttons
    ---------------------------------------------------
     */
    private void initStartButton()
    {
        final Button btnLogData = (Button) findViewById(R.id.btnDatenAufz);

        btnLogData.setOnClickListener(new View.OnClickListener(){

            public void onClick (View v)
            {

                if (!logData)
                {
                    btnLogData.setBackgroundResource(R.drawable.common_google_signin_btn_text_dark_normal);
                    btnLogData.setText("Aufzeichnen beenden");

                    startDataLog();

                    thread = new Thread(RecDataActivity.this);

                    thread.start();
                }
                else
                {
                    btnLogData.setBackgroundResource(R.drawable.common_google_signin_btn_text_light);
                    btnLogData.setText("Daten aufzeichnen");

                    stopDataLog();
                }
            }
        });

        final Button btnOpenSdir = (Button) findViewById(R.id.buttonOpenSaveDir);

        btnOpenSdir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open folder
                openFolder();
            }
        });

        final  Button btnQFE = (Button) findViewById(R.id.buttonQFE);

        btnQFE.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Set QFE pressure
                BaroRefPressure = MeanBarohPa;
            }
        });
    }

    public void openFolder()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        startActivityForResult(intent, 0);
    }




    private void reset() {

        handler = new Handler();

        runable = new Runnable() {
            @Override
            public void run() {

                handler.postDelayed(this, 100);

                dataReady = true;


            }
        };

    }

    /*
     * Output and logs are run on their own thread to keep the UI from hanging
     * and the output smooth.
     */
    @Override
    public void run()
    {
        while (logData && !Thread.currentThread().isInterrupted())
        {
            logData();
        }

        Thread.currentThread().interrupt();
    }

    /*
     * Log output data to an external .csv file.
     */
    private void logData()
    {
        if (logData && dataReady)
        {
            log += Relationname++ + ",";

            log += timeSensorSum + ",";
            /*
            log += OutArrAcc[0] + ",";
            log += OutArrAcc[1] + ",";
            log += OutArrAcc[2] + ",";
            */
            // Setting due to slow writing speed
            /*
            log += OutAcelero[0] + ",";
            log += OutAcelero[1] + ",";
            log += OutAcelero[2] + ",";
            */
            log += ArrMeanAcc[0] + ",";
            log += ArrMeanAcc[1] + ",";
            log += ArrMeanAcc[2] + ",";
            log += OutArrVeloRES[0] + ",";
            log += OutArrVeloRES[1] + ",";
            log += OutArrVeloRES[2] + ",";
            log += OutArrVeloInegrlRES[0] + ",";
            log += OutArrVeloInegrlRES[1] + ",";
            log += OutArrVeloInegrlRES[2] + ",";
            log += Math.toDegrees(OutGyroV[0]) + ",";
            log += Math.toDegrees(OutGyroV[1]) + ",";
            log += Math.toDegrees(OutGyroV[2]) + ",";
            //log += Math.toDegrees(OutArrGyroAnRES[0]) + ",";
            //log += Math.toDegrees(OutArrGyroAnRES[1]) + ",";
            //log += Math.toDegrees(OutArrGyroAnRES[2]) + ",";
            log += MeanBarohPa + ",";
            log += BaroAltitudeOld + ",";
            log += BaroVertSpeedOld + ",";
            log += absCombiSpeed + ",";
            log += OutGPSspeed + ",";
            log += OutGPSspeedOnly + ",";
            log += GPSaltitudeOld + ",";
            log += OutGPSaccuracy + ",";

            log += "\n";

            dataReady = false;
        }
    }


    /*
     * Begin logging data to an external .csv file.
     */
    private void startDataLog()
    {
        if (logData == false)
        {
            // Reset some values before start of DataLog
            Arrays.fill(ArrVelocity, 0); // Short way to set the whole array to zero
            Arrays.fill(ArrVelocityOld, 0);
            Arrays.fill(ArrVelocityIntgl, 0);
            Arrays.fill(ArrVeloIntegrlOld, 0);

            icounter = 0;

            BaroAltitudeOld = 0;
            BaroVertSpeedRES = 0;

            Relationname = 0; // Reset value of Relationname

            timeSensorSum = 0; // Reset value of added time

            // Set names for the headers of the corresponding column
            String headers = "Relationname" + ",";

            headers += "Time[s]" + ",";
            /*
            headers += "AcceloerometerX[m/s^2]" + ",";
            headers += "AcceloerometerY[m/s^2]" + ",";
            headers += "AcceloerometerZ[m/s^2]" + ",";
            */
            headers += "AccX[m/s^2]" + ",";
            headers += "AccY[m/s^2]" + ",";
            headers += "AccZ[m/s^2]" + ",";
            headers += "VelocityX[m/s]" + ",";
            headers += "VelocityY[m/s]" + ",";
            headers += "VelocityZ[m/s]" + ",";
            headers += "IntgrVelocityX[m/s]" + ",";
            headers += "IntgrVelocityY[m/s]" + ",";
            headers += "IntgrVelocityZ[m/s]" + ",";
            headers += "AroundX[deg/s]" + ",";
            headers += "AroundY[deg/s]" + ",";
            headers += "AroundZ[deg/s]" + ",";
            //headers += "AroundX[deg]" + ",";
            //headers += "AroundY[deg]" + ",";
            //headers += "AroundZ[deg]" + ",";
            headers += "BaroPressure[mbar]" + ",";
            headers += "BaroAltitude[m]" + ",";
            headers += "BaroSpeed[m/s]" + ",";
            headers += "GPScombiBaroSpeed[mk/h]" + ",";
            headers += "GPS-Speed [km/h]" +",";
            headers += "GPS-SpeedOnly[km/h]" + ",";
            headers += "GPS-Altitude[m]" + ",";
            headers += "GPShorizontalAccuracyRadius [m]" +",";

            log = headers;

            //log += System.getProperty("line.separator");
            log += "\n";

            logData = true;

            Toast.makeText(getApplicationContext(),"Aufzeichnen gestartet",Toast.LENGTH_SHORT).show();
        }
    }

    private void stopDataLog()
    {
        if (logData)
        {
            writeLogToFile();
        }

        if (logData && thread != null)
        {
            logData = false;

            thread.interrupt();

            thread = null;
        }
    }

    /*
     * Write the logged data out to a persisted file.
     * TODO: Consider implementing SQliteDatabase for faster logging
     */
    private void writeLogToFile()
    {


        // Create file name from date
        long timeSystem = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String dateString = formatter.format(new Date(timeSystem));

        String filename = "WSH" + dateString + ".csv";
        /*
        File dir = new File("/sdcard/Windenstarthelfer/Logs/");
        File dir = new File("android.os.Environment.getExternalStorageDirectory()/Windenstarthelfer/Logs/");
        */
        File dir = new File(android.os.Environment.getExternalStorageDirectory()
                + File.separator + "Windenstarthelfer" + File.separator
                + "Logs");

        if (!dir.exists())
        {
            dir.mkdirs();
        }

        File file = new File(dir, filename);
        //File file = new File("/sdcard/Windenstarthelfer/Logs/" + filename);


        byte[] data = log.getBytes();
        try
        {
            // Working solution 26.10.2016
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.flush();
            fos.close();


            Toast.makeText(getApplicationContext(), "Aufzeichnung abgeschlossen", Toast.LENGTH_SHORT).show();
        }
        catch (FileNotFoundException e)
        {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
        catch (IOException e)
        {
            // handle exception
        }
        finally
        {
            // Update the MediaStore so we can view the file without rebooting.
            // Note that it appears that the ACTION_MEDIA_MOUNTED approach is
            // now blocked for non-system apps on Android 4.4.
            MediaScannerConnection.scanFile(this, new String[]
                    { file.getPath() }, null,
                    new MediaScannerConnection.OnScanCompletedListener()
                        {
                            @Override
                            public void onScanCompleted( final String path, final Uri uri) {

                            }
                        });
        }
    }


    /*
    ------------------------------------------------
    Call the methods from the android life-cycle
    ------------------------------------------------
     */

    @Override
    protected void onStart() {
        super.onStart();
        // Connect googleApiClient on app startup
        googleApiClient.connect();

        // Reset time counter
        countRecGPS = 0;

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        reset(); // Call reset function
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (permissionIsGranted) {
            if (googleApiClient.isConnected()) {
                requestLocationUpdates();
            }
        }

        // Register Sensors
        wshSensorManager.registerListener(this, lin_acc, SensorTimeDelay);
        wshSensorManager.registerListener(this, acc, SensorTimeDelay);
        wshSensorManager.registerListener(this, wshGyroSensor, SensorTimeDelay);
        wshSensorManager.registerListener(this, wshBaro, SensorTimeDelay);

        handler.post(runable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (permissionIsGranted) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }

        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        handler.removeCallbacks(runable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (permissionIsGranted) {
            googleApiClient.disconnect();
        }

        // Unregister sensors
        wshSensorManager.unregisterListener(this, lin_acc);
        wshSensorManager.unregisterListener(this, acc);
        wshSensorManager.unregisterListener(this, wshGyroSensor);
        wshSensorManager.unregisterListener(this, wshBaro);

        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // onResume follows after this method, so no action is taken here
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister sensors
        wshSensorManager.unregisterListener(this, lin_acc);
        wshSensorManager.unregisterListener(this, acc);
        wshSensorManager.unregisterListener(this, wshGyroSensor);
        wshSensorManager.unregisterListener(this, wshBaro);
    }

}
