package com.zcm.qrcapture;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.zcm.google.zxing.fragment.CaptureFragment;

public class FragmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        getSupportFragmentManager().beginTransaction().replace(R.id.container,
                new CaptureFragment()).commit();
    }
}
