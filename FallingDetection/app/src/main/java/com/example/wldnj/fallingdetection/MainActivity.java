package com.example.wldnj.fallingdetection;

import android.os.Bundle;
import android.widget.Button;

/**
 * Created by wldnj on 2017-05-23.
 */

public class MainActivity extends BlunoLibrary {
    private Button buttonScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

    }
}
