package hse.edu.myapplication2.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import javax.microedition.khronos.egl.EGLConfig;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.widget.RadioButton;
import android.widget.TextView;

import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class MainActivity extends ActionBarActivity implements SensorEventListener{

    TextView XtextView;
    TextView YtextView;
    TextView ZtextView;
    SurfaceView mSurfaceView;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Sensor mLight;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        XtextView = (TextView) findViewById(R.id.XtextView);
        YtextView = (TextView) findViewById(R.id.YtextView);
        ZtextView = (TextView) findViewById(R.id.ZtextView);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private float[] rMatrix = new float[9];
    /**
     * @param result the array of Euler angles in the order: yaw, roll, pitch
     * @param rVector the rotation vector
     */
    public void calculateAngles(float[] result, float[] rVector){
        //caculate rotation matrix from rotation vector first
        SensorManager.getRotationMatrixFromVector(rMatrix, rVector);

        //calculate Euler angles now
        SensorManager.getOrientation(rMatrix, result);

        //The results are in radians, need to convert it to degrees
        convertToDegrees(result);
    }
    private void convertToDegrees(float[] vector){
        for (int i = 0; i < vector.length; i++){
            vector[i] = Math.round(Math.toDegrees(vector[i]));
        }
    }

    Lock lock = new ReentrantLock(true);
    int temp = 0;
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor != null) {
            if (sensorEvent.sensor.getType() == mSensor.getType()) {
                float[] result = new float[3];
                calculateAngles(result, sensorEvent.values);
                XtextView.setText(String.format("%.2f", result[0]));
                YtextView.setText(String.format("%.2f", result[1]));
                ZtextView.setText(String.format("%.2f", result[2]));
                    if (lock.tryLock()) {
                        final double freqChange = result[0];
                        // Use a new tread as this can take a while
                        final Thread thread = new Thread(new Runnable() {
                            public void run() {
                                genTone(freqChange);
                                handler.post(new Runnable() {

                                    public void run() {
                                        playSound();
                                    }
                                });
                            }
                        });
                        thread.start();
                        lock.unlock();
                    }
                }
            }
        }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

        }

    private final double duration = 0.5; // seconds
    private final int sampleRate = 8000;
    private final int numSamples = (int)(duration * sampleRate);
    private final double sample[] = new double[numSamples];
    private final double freqOfTone = 440; // hz

    private final byte generatedSnd[] = new byte[2 * numSamples];

    Handler handler = new Handler();

    void genTone(double freqChange){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/(freqOfTone+freqChange)));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }

    void playSound(){
        final AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, numSamples,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }
}
