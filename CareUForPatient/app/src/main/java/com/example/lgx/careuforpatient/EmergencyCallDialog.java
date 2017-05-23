package com.example.lgx.careuforpatient;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Created by LGX on 2017-05-11.
 */
public class EmergencyCallDialog extends Dialog
{
    private Button call112Button, call119Button, callProtectorButton;
    private View.OnClickListener mCall112Listener, mCall119Listener, mCallProtectorListener;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // 다이얼로그 외부 화면 흐리게 표현
        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.dialog_emergencycall);

        call112Button = (Button)findViewById(R.id.call112Image);
        call119Button = (Button)findViewById(R.id.call119Image);
        callProtectorButton = (Button)findViewById(R.id.callProtectorImage);

        call112Button.setOnClickListener(mCall112Listener);
        call119Button.setOnClickListener(mCall119Listener);
        callProtectorButton.setOnClickListener(mCallProtectorListener);

    }

    // 클릭버튼이 확인과 취소 두개일때 생성자 함수로 이벤트를 받는다
    public EmergencyCallDialog( Context context, View.OnClickListener call112Listener, View.OnClickListener call119Listener, View.OnClickListener callProtectorListener)
    {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        mCall112Listener = call112Listener;
        mCall119Listener = call119Listener;
        mCallProtectorListener = callProtectorListener;
    }
}
