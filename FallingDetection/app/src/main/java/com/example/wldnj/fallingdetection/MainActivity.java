package com.example.wldnj.fallingdetection;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

/**
 * Created by wldnj on 2017-05-26.
 */

public class MainActivity extends AppCompatActivity {

    Button buttonBluetoothSetting;
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonBluetoothSetting = (Button) findViewById( R.id.buttonBLESetting );
        buttonBluetoothSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent( MainActivity.this, BluetoothConnectActivity.class );
                startActivity(intent);
            }
        });
    }
}
