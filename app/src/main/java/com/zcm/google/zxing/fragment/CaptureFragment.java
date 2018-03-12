package com.zcm.google.zxing.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.zcm.google.zxing.camera.CameraManager;
import com.zcm.google.zxing.decoding.CaptureActivityHandler;
import com.zcm.google.zxing.decoding.DecodeHandlerInterface;
import com.zcm.google.zxing.decoding.InfragmentTimer;
import com.zcm.google.zxing.view.ViewfinderView;
import com.zcm.qrcapture.R;

import java.io.IOException;
import java.util.Vector;

/**
 *
 */
public class CaptureFragment extends Fragment implements Callback,
        DecodeHandlerInterface {

    public static CaptureFragment newInstance() {
        Bundle bundle = new Bundle();
        CaptureFragment fragment = new CaptureFragment();
        fragment.setArguments(bundle);
        return fragment;
    }


    public static final String SCAN_RESULT_ACTION = "com.zxing.fragment.ACTION_SCAN_RESULT";

    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private InfragmentTimer infragmentTimer;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private static final float BEEP_VOLUME = 0.10f;
    private boolean vibrate;
    private ImageView cancelScanButton;
    private View view;

    MyBroadcast broadcast;

    /**
     * Called when the fragment is first created.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        view = inflater.inflate(R.layout.activity_scanner, null);
        // ViewUtil.addTopView(getApplicationContext(), this,
        // R.string.scan_card);
        CameraManager.init(getActivity());
        viewfinderView = (ViewfinderView) view
                .findViewById(R.id.viewfinder_content);
        cancelScanButton = view.findViewById(R.id.scanner_toolbar_back);
        hasSurface = false;
        infragmentTimer = new InfragmentTimer(getTargetFragment());


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        broadcast = new MyBroadcast();
        IntentFilter intentFilter = new IntentFilter(
                CaptureFragment.SCAN_RESULT_ACTION);
        getActivity().registerReceiver(broadcast, intentFilter);


        SurfaceView surfaceView = (SurfaceView) view
                .findViewById(R.id.scanner_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
        decodeFormats = null;
        characterSet = null;

        playBeep = true;
        AudioManager audioService = (AudioManager) getActivity()
                .getSystemService(Context.AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;

        // quit the scan view
        cancelScanButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // CaptureFragment.this.finish();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        CameraManager.get().closeDriver();

        Log.d("CaptureFragment", "Receiver" + broadcast);
        if (broadcast != null) {
            getActivity().unregisterReceiver(broadcast);
            broadcast = null;
        }
    }


    @Override
    public void onDestroy() {
        infragmentTimer.shutdown();
        super.onDestroy();
    }

    /**
     * Handler scan result
     *
     * @param result
     * @param barcode
     */
    public void handleDecode(Result result, Bitmap barcode) {
        infragmentTimer.onFragment();
        playBeepSoundAndVibrate();
        String resultString = result.getText();
        // FIXME
        if (resultString.equals("")) {

            Toast.makeText(getActivity(), "Scan failed!", Toast.LENGTH_SHORT)
                    .show();
        } else {

            sendBroadcastToFragment(resultString);
        }
    }

    public void sendBroadcastToFragment(String result) {

        Intent intent = new Intent();
        intent.setAction(SCAN_RESULT_ACTION);
        intent.putExtra("result", result);
        getActivity().sendBroadcast(intent);
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (handler == null) {
            handler = new CaptureActivityHandler(this, decodeFormats,
                    characterSet);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;

    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();

    }

    private void initBeepSound() {
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(
                    R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
    }

    private static final long VIBRATE_DURATION = 200L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getActivity().getSystemService(
                    Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private final OnCompletionListener beepListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };

    /**
     * you should get result like this.
     * <p/>
     * String scanResult = data.getExtras().getString("result");
     */
    @Override
    public void resturnScanResult(int resultCode, Intent data) {
        Toast.makeText(getActivity(), data.getStringExtra("result"), Toast.LENGTH_SHORT).show();

    }

    @Override
    public void launchProductQuary(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivity(intent);
    }


    public class MyBroadcast extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction() != null) {
                if (intent.getAction().equals(CaptureFragment.SCAN_RESULT_ACTION)) {
                    //String str = intent.getExtras().getString("result").trim();
                    String str = intent.getStringExtra("result");


                    Log.i("CaptureFragment===", str);
                    if (TextUtils.isEmpty(str)) {
                        Toast.makeText(getActivity(), "扫码失败", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "扫码成功：" + str, Toast.LENGTH_SHORT).show();
                    }

                }
            }
        }
    }
}