package com.zcm.google.zxing.decoding;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;

import com.google.zxing.Result;
import com.zcm.google.zxing.view.ViewfinderView;


public interface DecodeHandlerInterface {

     int RESULT_STATE_OK = 0;

     void drawViewfinder();

     ViewfinderView getViewfinderView();

     Handler getHandler();

     void handleDecode(Result result, Bitmap barcode);

     void returnScanResult(int resultCode, Intent data);

     void launchProductQuery(String url);
}
