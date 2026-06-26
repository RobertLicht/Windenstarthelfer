package com.example.l520.wsh_0_003;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;


public class SensorActivity extends AppCompatActivity implements SensorEventListener, View.OnClickListener{

    // Create output in Logfile in the terminal to follow action
    final String TAG = this.getClass().getName();
    //Log.d(TAG, "value of lx: " + lx);

    // Declare variables
    // -> Text Sensordaten -  Beschleunigung (linear)
    private TextView textBeschLinear, textAxisX, textAxisY, textAxisZ;
    // -> Text Sensordaten -  Beschleunigung (Mittelwert)
    private  TextView textX, textY, textZ;
    // -> Text Sensordate - Gyroskop
    private TextView textGyroX, textGyroY, textGyroZ;
    // -> Text Geschwindigkeit
    private TextView textGeschwindigkeit, textGeschwindigkeitX, textGeschwindigkeitY, textGeschwindigkeitZ;
    private TextView textMittelGeschwX, textMittelGeschwY;
    private byte iArr = 0, countCalm = 0, lenArr = 5;
    //private float [] arrACCx = new float[lenArr];  // Initialize array for ACCx
    //private float [][] arrACC = new float[lenArr][6]; // Initialize 2-D-Array for sensor data
    private long logTimeAccOld = 0, logTimeAcc = 0; // Log output time stamps
    private float sumlx = 0, MeanACCx = 0, sumly = 0, MeanACCy = 0, sumlz = 0, MeanACCz = 0, MeanACC = 0;
    private float dTacc = 0, speedABS = 0;
    private float [] Arrspeed = new float[3];
    private float [] ArrspeedOld = new float[3];
    private float [] ArrspeedRES = new float[3];
    private float SumVx = 0, SumVy = 0,SumVz = 0, MittelX = 0, MittelY = 0,MittelZ = 0;
    private byte iMittel = 0, Mittel = 10;
    private float sumgx = 0, MeanGx = 0, sumgy = 0, MeanGy = 0, sumgz = 0, MeanGz = 0, MeanG = 0;
    private float dTg = 0, angleX = 0, angleY = 0, angleZ = 0;

    private float timestamp, timestampOld = 0;
    private float dT, timeSensor;
    private long countSensor = 0;
    private static final float MS2S = 1.0f / 1000.0f; // Factor for converting milliseconds to seconds
    private static final float NS2S = 1.f / 1000000000.0f; //Factor for converting nanoseconds to seconds

    // Variables for Sensor and Sensor Manager
    private SensorManager wshSensorManager = null;
    private Sensor lin_acc, acc, wshGyroSensor;
    final int SensorTimeDelay = 20*1000; // 20*1000 [ms] <-> 50 [Hz]// 100*1000 [ms] <-> 10 [Hz]
    //final int SensorTimeDelayACC = 12500; // 12.5*1000 [ms] <-> 80 [Hz]// 100*1000 [ms] <-> 10 [Hz]



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);


        // Create Sensor Manager
        wshSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        //if (wshSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null){
            lin_acc = wshSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            //wshSensorManager.registerListener(this, lin_acc, SensorTimeDelay);
       //}

        // Necessary for tests on the PC
        //if (wshSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
            acc = wshSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            //wshSensorManager.registerListener(this, acc, SensorTimeDelay);
            //}

        //if (wshSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
            wshGyroSensor = wshSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            //wshSensorManager.registerListener(this, wshGyroSensor, SensorTimeDelay);
        //}

        // Assign TextViewas
        // -> Text Sensordaten -  Beschleunigung (linear)
        textBeschLinear = (TextView) findViewById(R.id.textBeschLinear);
        textAxisX = (TextView) findViewById(R.id.textAxisX);
        textAxisY = (TextView) findViewById(R.id.textAxisY);
        textAxisZ = (TextView) findViewById(R.id.textAxisZ);
        // -> Text Sensordaten -  Beschleunigung (Filter)
        textX = (TextView) findViewById(R.id.textX);
        textY = (TextView) findViewById(R.id.textY);
        textZ = (TextView) findViewById(R.id.textZ);
        // -> Text Sensordate - Gyroskop
        textGyroX = (TextView) findViewById(R.id.textGyroX);
        textGyroY = (TextView) findViewById(R.id.textGyroY);
        textGyroZ = (TextView) findViewById(R.id.textGyroZ);
        // -> Text Geschwindigkeit
        textGeschwindigkeit = (TextView) findViewById(R.id.textGeschwindigkeit);
        textGeschwindigkeitX = (TextView) findViewById(R.id.textGeschwindigkeitX);
        textGeschwindigkeitY = (TextView) findViewById(R.id.textGeschwindigkeitY);
        textGeschwindigkeitZ = (TextView) findViewById(R.id.textGeschwindigkeitZ);
        //
        textMittelGeschwX = (TextView) findViewById(R.id.textMittelGeschwX);
        textMittelGeschwY = (TextView) findViewById(R.id.textMittelGeschwY);

        // Implement Button and listen for clicks
        Button btnDataRec = (Button) findViewById(R.id.buttonDataRec);
        btnDataRec.setOnClickListener(this);


    }


        @Override
        public void onAccuracyChanged (Sensor sensor,int accuracy){
            // Not in use
        }

        @Override
        public void onSensorChanged (SensorEvent event){

            // Get the timestamp
            timestamp = event.timestamp;
            dT = (timestamp - timestampOld) * NS2S;
            if (timestampOld != 0) {
                // Create time for record
                timeSensor = (countSensor++ / dT);
            }
            timestampOld = timestamp;

            /*
            // If sensor is unreliabe, return void
            if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE){
                return;
            }
            */

            // Read values from different sensors
            Sensor sensor = event.sensor;

            if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                // Get event data
                getLinearAccelerometer(event); // Call function to get Data

            }else if (sensor.getType() == Sensor.TYPE_GYROSCOPE){
                // Get event Data
                getGyroscop(event); // Call function to get Data
            }

        }



    // Get Data by function
    private void getLinearAccelerometer(SensorEvent event) {

        float[] values = event.values;
        // linear Movement
        float lx = values[0];
        float ly = values[1];
        float lz = values[2];

        //Log.d(TAG, "value of lx: " + lx);

        textAxisX.setText("X-Achse [m/s^2]: " + String.valueOf(lx));
        textAxisY.setText("Y-Achse [m/s^2]: " + String.valueOf(ly));
        textAxisZ.setText("Z-Achse [m/s^2]: " + String.valueOf(lz));


        // Calculate mean acceleration of lenArr values
            if (iArr < lenArr) {
                /* Demo save data to array
                arrACC[iArr] [0] = lx;
                arrACC[iArr] [1] = ly;
                arrACC[iArr] [2] = lz;
                */

                sumlx += lx;// add with shorthand: sumlx += lx <-> sumlx = sumlx + lx;
                sumly += ly;
                sumlz += lz;
                // increase counter
                iArr++;
            }
            if (iArr == lenArr) {
                MeanACCx = sumlx / lenArr;
                MeanACCy = sumly / lenArr;
                MeanACCz = sumlz / lenArr;

                // Calculate absolute vector
                MeanACC = (float) Math.sqrt((MeanACCx * MeanACCx) + (MeanACCy * MeanACCy) + (MeanACCz * MeanACCz));
                // Smooth acceleration if device is not moving
                if (MeanACC < 0.06) {
                    countCalm++;
                    MeanACCx = 0;
                    MeanACCy = 0;
                    MeanACCz = 0;
                }
                // Wait some loops, then reset Speed
                if (countCalm > 40) {
                    Arrspeed[0] = 0;
                    Arrspeed[1] = 0;
                    Arrspeed[2] = 0;
                    countCalm = 0;
                }


                logTimeAcc = System.currentTimeMillis();
                dTacc = logTimeAcc - logTimeAccOld;

        // Get timestamp in nanoseconds and convert to seconds
        logTimeAcc = event.timestamp;
        dT = (event.timestamp - timestamp) * NS2S;

                // Check if a real value exist for logTimeOld
                if (logTimeAccOld != 0) {
                    dTacc = dTacc * MS2S; // Convert milliseconds to seconds
                    // Calculating speed
                    Arrspeed[0] = (MeanACCx * dTacc);
                    Arrspeed[1] = (MeanACCy * dTacc);
                    Arrspeed[2] = (MeanACCz * dTacc);


                    // Resulting vector
                    ArrspeedRES[0] = ArrspeedOld[0] + Arrspeed[0];
                    ArrspeedRES[1] = ArrspeedOld[1] + Arrspeed[1];
                    ArrspeedRES[2] = ArrspeedOld[2] + Arrspeed[2];


                    // Mean value of ArrspeedRES
                    if (iMittel < Mittel) {
                        SumVx += ArrspeedRES[0];
                        SumVy += ArrspeedRES[1];
                        SumVz += ArrspeedRES[2];
                        iMittel++;
                    }
                    if (iMittel == Mittel) {
                        MittelX = SumVx / Mittel;
                        MittelY = SumVy / Mittel;
                        MittelZ = SumVz / Mittel;
                        // Calculate absolute vector
                        speedABS = (float) Math.sqrt((MittelX * MittelX) + (MittelY * MittelY) + (MittelZ * MittelZ));
                        // Update textView
                        textMittelGeschwX.setText("X-mittelere Geschwindigkeit [km/h]: " + String.valueOf(MittelX));
                        textMittelGeschwY.setText("Y-mittelere Geschwindigkeit [km/h]: " + String.valueOf(MittelY));

                        // reset Sum and counter
                        SumVx = 0; SumVy = 0; SumVz = 0; iMittel=0;
                    }

                }

                // Round some values
                //float rund = (float)(((int)(zahl*100))/100.0);
                speedABS = (float) (((int) (speedABS * 100)) / 100.0);
                float GeschwABS = (float) (speedABS * 3.6);
                GeschwABS = (float) (((int) (GeschwABS * 100)) / 100.0);

                // update textView - Sensordaten
                textBeschLinear.setText("Sensordaten - Beschleunigung (linear) | " + String.valueOf(MeanACC));
                // update textViews - Beschleunigung (Mittelwert)
                textX.setText("X-Achse [m/s^2]: " + String.valueOf(MeanACCx));
                textY.setText("Y-Achse [m/s^2]: " + String.valueOf(MeanACCy));
                textZ.setText("Z-Achse [m/s^2]: " + String.valueOf(MeanACCz));
                // update textViews - Geschwindigkeit
                textGeschwindigkeit.setText("Geschwindigkeit | [m/s]: " + String.valueOf(speedABS)
                        + " | [km/h]: " + String.valueOf(GeschwABS));
                textGeschwindigkeitX.setText("X-Geschwindigkeit [m/s]: " + String.valueOf(ArrspeedRES[0]));
                textGeschwindigkeitY.setText("Y-Geschwindigkeit [m/s]: " + String.valueOf(ArrspeedRES[1]));
                textGeschwindigkeitZ.setText("Z-Geschwindigkeit [m/s]: " + String.valueOf(ArrspeedRES[2]));

                // reset sum and counter
                sumlx = 0; sumly = 0; sumlz = 0; iArr = 0;

                // Save last logTime
                logTimeAccOld = logTimeAcc;
                // Save last entries of Arrspeed
                ArrspeedOld[0] = Arrspeed[0];
                ArrspeedOld[1] = Arrspeed[1];
                ArrspeedOld[2] = Arrspeed[2];

            }
    }


    private void getGyroscop(SensorEvent event) {
        float[] values = event.values;
        // Gyroscop calibrated
        float gx = values[0];
        float gy = values[1];
        float gz = values[2];

        //Log.d(TAG, "value of gx: " + gx);

        // Calculate mean acceleration of lenArr values
        if (iArr < lenArr) {
                /* Demo save data to array
                arrACC[iArr] [0] = lx;
                arrACC[iArr] [1] = ly;
                arrACC[iArr] [2] = lz;
                */
            sumgx += gx;
            sumgy += gy;
            sumgz += gz;
            // increase counter
            iArr++;
        }
        if (iArr == lenArr){
            MeanGx = sumgx / lenArr;
            MeanGy = sumgy / lenArr;
            MeanGz = sumgz / lenArr;
            // Calculate absolute vector
            MeanG = (float) Math.sqrt((MeanGx * MeanGx) + (MeanGy * MeanGy) + (MeanGz * MeanGz));
            //Log.d(TAG, "Betrag Winkeländerung: " + MeanG);
            // Smooth angular acceleration if device is not moving
            if (MeanG < 0.08){
                MeanGx = 0;
                MeanGy = 0;
                MeanGz = 0;
            }

            /*
            logTimeAcc = System.currentTimeMillis();
            dTg = logTimeAcc - logTimeAccOld;

            // Check if a real value exist for logTimeOld
            if (logTimeAccOld != 0) {
                // Compute angle
                dTg = dTg * MS2S; // Convert milliseconds to seconds
                angleX = (MeanGx * dTg) + angleX;
                angleY = (MeanGy * dTg) + angleY;
                angleZ = (MeanGx * dTg) + angleZ;
            }
            */

            // update textViews - Gyroskpop
            textGyroX.setText("Um X-Achse [deg/s]: " + String.valueOf(Math.toDegrees(MeanGx)));
            textGyroY.setText("Um Y-Achse [deg/s]: " + String.valueOf(Math.toDegrees(MeanGy)));
            textGyroZ.setText("Um Z-Achse [deg/s]: " + String.valueOf(Math.toDegrees(MeanGz)));
            // reset sum and counter
            sumgx = 0; sumgy = 0; sumgz = 0;
            iArr = 0;
            // Save last logTime
            //logTimeAccOld = logTimeAcc;
        }


    }


    // OnClicklistener
    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.buttonDataRec:
                DataRecClicked();
                break;
            /*
            case R.id.buttonDataConvert:
                DataConvertClicked();
                break;
             */
        }
    }

    // Intent of buttonDataRec
    public void DataRecClicked(){
        // execute intent
        Intent act_DataRec = new Intent(SensorActivity.this, RecDataActivity.class);
        startActivity(act_DataRec);
    }

    private void stopSensors(){
        wshSensorManager.unregisterListener(this, lin_acc);
        wshSensorManager.unregisterListener(this, acc);
        wshSensorManager.unregisterListener(this, wshGyroSensor);

    }


     /*
    ------------------------------------------------
    Call the methods from the android life-cycle
    ------------------------------------------------
     */

    @Override
    protected void onStart() {
        super.onStart();

    }


    @Override
    protected void onResume() {
        super.onResume();

        // Register Sensors
        wshSensorManager.registerListener(this, lin_acc, SensorTimeDelay);
        wshSensorManager.registerListener(this, acc, SensorTimeDelay);
        wshSensorManager.registerListener(this, wshGyroSensor, SensorTimeDelay);

    }

    @Override
    protected void onPause() {
        super.onPause();

        stopSensors();

    }

    @Override
    protected void onStop() {
        super.onStop();

        stopSensors();

    }


}
