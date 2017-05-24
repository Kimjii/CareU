package com.example.lgx.careuforpatient;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by LGX on 2017-05-12.
 */
public class LoginActivity extends Activity
{
    EditText idInput, passwordInput;
    Button loginButton, joinButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        idInput = (EditText) findViewById(R.id.emailInput);
        passwordInput = (EditText) findViewById(R.id.passwordInput);

        loginButton = (Button) findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    String result = new UserTask().execute( "login", idInput.getText().toString(), passwordInput.getText().toString() ).get();

                    String[][] parsedData = jsonParserList(result);

                    if ( parsedData[0][0].equals( "true" ) )
                    {
                        Toast.makeText(LoginActivity.this,"로그인",Toast.LENGTH_SHORT).show();
                        PreferenceHelper.savePreferences( getApplicationContext(), "First", "1" );
                        PreferenceHelper.savePreferences( getApplicationContext(), "proNo", parsedData[0][1] );
                        PreferenceHelper.savePreferences( getApplicationContext(), "patNo", parsedData[0][2] );
                        Log.i("확인1", PreferenceHelper.getPreferences( getApplicationContext(), "proNo" ));
                        Log.i("확인2", PreferenceHelper.getPreferences( getApplicationContext(), "patNo" ));
                        Log.i("확인3", PreferenceHelper.getPreferences( getApplicationContext(), "First" ));
                        finish();
                    }
                    else if ( parsedData[0][0].equals( "false" ) )
                    {
                        Toast.makeText(LoginActivity.this,"아이디 또는 비밀번호가 틀렸음",Toast.LENGTH_SHORT).show();
                        idInput.setText( "" );
                        passwordInput.setText( "" );
                    }
                    else if ( parsedData[0][0].equals( "noId" ) )
                    {
                        Toast.makeText(LoginActivity.this,"존재하지 않는 아이디",Toast.LENGTH_SHORT).show();
                        idInput.setText( "" );
                        passwordInput.setText( "" );
                    }
                }
                catch ( Exception e ) {}
            }
        });

        joinButton = (Button) findViewById(R.id.signupButton);
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 회원가입 Activity로 이동!
            }
        });
    }

    class UserTask extends AsyncTask<String, Void, String>
    {
        String sendMsg, receiveMsg;

        @Override
        protected String doInBackground(String... strings)
        {
            try
            {
                String str;
                URL url;
                if ( strings[0].equals( "login" ) )
                {
                    // url = new URL("http://192.168.0.11:8080/CareU/login_android.jsp"); // ch-Iptime 5G
                    url = new URL("http://192.168.0.30:8080/CareU/login_android.jsp"); // D444 5G
                    sendMsg = "&type="+strings[0]+"&id="+strings[1]+"&pwd="+strings[2];
                }
                else // 회원가입
                {
                    url = new URL("http://192.168.0.11:8080/CareU/login_android.jsp"); // ch-Iptime 5G
                }
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setRequestProperty( "content-type", "application/x-www-form-urlencoded" );
                conn.setRequestMethod( "POST" );
                OutputStreamWriter osw = new OutputStreamWriter( conn.getOutputStream() );
                osw.write( sendMsg );
                osw.flush();

                if(conn.getResponseCode() == conn.HTTP_OK)
                {
                    InputStreamReader isr = new InputStreamReader( conn.getInputStream(), "UTF-8" );
                    BufferedReader reader = new BufferedReader( isr );
                    StringBuffer buffer = new StringBuffer();

                    while( (str = reader.readLine()) != null )
                    {
                        buffer.append(str);
                    }
                    receiveMsg = buffer.toString();
                    Log.i("통신결과", receiveMsg);
                }
                else
                {
                    Log.i("통신결과", conn.getResponseCode()+"에러");
                }
            }
            catch ( MalformedURLException e )
            {
                e.printStackTrace();
            }
            catch (IOException e )
            {
                e.printStackTrace();
            }
            return receiveMsg;
        }
    }

    public String[][] jsonParserList(String result)
    {
        try
        {
            JSONObject json = new JSONObject(result);
            JSONArray jArr = json.getJSONArray("List");

            String[] jsonName = {"type","prono","patno"};
            String[][] parseredData = new String[jArr.length()][jsonName.length];

            for(int i = 0; i < jArr.length(); i++)
            {
                json = jArr.getJSONObject(i);

                for (int j=0; j < jsonName.length; j++)
                {
                    parseredData[i][j] = json.getString(jsonName[j]);
                }
            }
            return parseredData;
        }
        catch(JSONException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
