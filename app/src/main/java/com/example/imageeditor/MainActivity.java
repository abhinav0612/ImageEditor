package com.example.imageeditor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.ahmedadeltito.photoeditorsdk.PhotoEditorSDK;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    ImageView imageView;
    float[] lastEvent = null;
    float d = 0f,temp = 0f,targetAngle = 30f,currentAngle = 0f;
    float newRot = 0f;
    private boolean isZoomAndRotate;
    private boolean isOutSide;
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;
    private PointF start = new PointF();
    private PointF mid = new PointF();
    float oldDist = 1f;
    private float xCoOrdinate, yCoOrdinate;
    private ConstraintLayout layout;
    private Button selectImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        layout = findViewById(R.id.constraintLayout);
        imageView = findViewById(R.id.imageView);
        selectImage = findViewById(R.id.button);

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            takePermission();
        }

        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ConstraintLayout view = (ConstraintLayout) v;
                view.bringToFront();
                viewTransformation(view, event);
                return true;
            }
        });

        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent,2);
            }
        });
    }

    private void takePermission() {
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
    }

    private void viewTransformation(View view, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                xCoOrdinate = view.getX() - event.getRawX();
                yCoOrdinate = view.getY() - event.getRawY();

                start.set(event.getX(), event.getY());
                isOutSide = false;
                mode = DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    midPoint(mid, event);
                    mode = ZOOM;
                }

                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                d = rotation(event);
                break;
            case MotionEvent.ACTION_UP:
                isZoomAndRotate = false;
                if (mode == DRAG) {
                    float x = event.getX();
                    float y = event.getY();
                }
            case MotionEvent.ACTION_OUTSIDE:
                isOutSide = true;
                mode = NONE;
                lastEvent = null;
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isOutSide) {
                    if (mode == DRAG) {
                        isZoomAndRotate = false;
                        view.animate().x(event.getRawX() + xCoOrdinate).y(event.getRawY() + yCoOrdinate).setDuration(0).start();
                    }
                    if (mode == ZOOM && event.getPointerCount() == 2) {
                        float newDist1 = spacing(event);
                        if (newDist1 > 10f) {
                            float scale = newDist1 / oldDist * view.getScaleX();
                            view.setScaleX(scale);
                            view.setScaleY(scale);
                        }
                        if (lastEvent != null) {
                            currentAngle = view.getRotation();
                            Log.e(TAG,"Rotation Angle before rotation: " + currentAngle);
                            newRot = rotation(event);

                            temp = view.getRotation() + (newRot - d);
                            if (temp <0)
                                temp =temp*-1;
                            Log.e(TAG,"Rotation Angle during rotation: "+ temp);

                            Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            if(temp >= targetAngle) {
                                vibe.vibrate(100);
                                Toast.makeText(this,"You reached "+Math.floor(temp) +" degrees",Toast.LENGTH_SHORT)
                                        .show();
//                                Log.e(TAG,"Vibrated");
//                                view.setRotation((float) targetAngle);
//                                long downTime = SystemClock.uptimeMillis();
//                                long eventTime = SystemClock.uptimeMillis() + 100;
//                                float x = 0.0f;
//                                float y = 0.0f;
//                                int metaState = 0;
//                                final MotionEvent motionEvent = MotionEvent.obtain(
//                                        downTime,
//                                        eventTime,
//                                        MotionEvent.ACTION_UP,
//                                        x,
//                                        y,
//                                        metaState
//                                );
//                                layout.dispatchTouchEvent(motionEvent);
                                layout.setEnabled(false);
                                targetAngle=targetAngle +30;
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        layout.setEnabled(true);
                                    }
                                },1500);

//                                layout.setEnabled(false);

                            }
                            else{
                                view.setRotation((float) (view.getRotation() + (newRot - d)));
                            }

                        }
                    }
                }
                break;
        }
    }

    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (int) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode ==1 ){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2 && resultCode == RESULT_OK) {
            try {
                InputStream in = getContentResolver().openInputStream(data.getData());
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
                imageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

    }
}
