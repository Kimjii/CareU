package com.example.lgx.careuforpatient;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

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
import java.util.List;

public class MainActivity extends Activity
{
    Button goHomeButton, emergencyCallButton;
    private EmergencyCallDialog dialog;

    double homeLatitude, homeLongitude; // 위도, 경도
    Geocoder geocoder = new Geocoder( this );

    private GpsInfo gps;

    @Override
    protected void onCreate ( Bundle savedInstanceState )
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );

        Intent serviceIntent = new Intent( MainActivity.this, GPSService.class );

        if ( PreferenceHelper.getPreferences( getApplicationContext(), "First" ).equals( "0" ) ) // 설치 후 최초 실행
        {
            Intent intent = new Intent( getApplication(), LoginActivity.class );
            startActivity( intent );
        }

        else
        {
            stopService( serviceIntent );
        }

        startService( serviceIntent );

        /* 집 안내 */
        goHomeButton = (Button)findViewById(R.id.goHomeButton);
        goHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                /* 현재 위치 GPS 가져오기 */
                double latitude = 0, longitude = 0;
                gps = new GpsInfo( MainActivity.this );

                if ( gps.isGetLocation() )
                {
                    latitude = gps.getLatitude();
                    longitude = gps.getLongitude();
                }
                else
                {
                    gps.showSettingsAlert();
                }

                try
                {
                    String result = new UserTask().execute( "patAddress", PreferenceHelper.getPreferences( getApplicationContext(), "patNo" ) ).get();
                    String[][] parsedData = jsonParserList(result);
                    addressToLatLon(parsedData[0][1]);

                    String url = "daummaps://route?sp=" + latitude + "," + longitude + "&ep=" + homeLatitude + "," + homeLongitude + "&by=FOOT";
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);

                }
                catch ( Exception e ) {}
            }
        });

        /* 비상연락 */
        emergencyCallButton = (Button)findViewById(R.id.emergencyCallButton);
        emergencyCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog = new EmergencyCallDialog( MainActivity.this, call112Listener, call119Listener, callProtectorListener );
                dialog.show();
            }
        });

    }

    public void addressToLatLon(String address)
    {
        try
        {
            List<Address> addressList = geocoder.getFromLocationName( address, 10 );
            homeLatitude = addressList.get(0).getLatitude();
            homeLongitude = addressList.get(0).getLongitude();
        }
        catch ( Exception e ) {}
    }

    /* 다이얼로그 관련 */
    private View.OnClickListener call112Listener = new View.OnClickListener() {
        public void onClick(View v) {
            dialog.dismiss();
            startActivity( new Intent( "android.intent.action.CALL_PRIVILEGED", Uri.parse("tel:112") ) );
        }
    };

    private View.OnClickListener call119Listener = new View.OnClickListener() {
        public void onClick(View v) {
            dialog.dismiss();
            startActivity( new Intent( "android.intent.action.CALL_PRIVILEGED", Uri.parse("tel:119") ) );
        }
    };

    private View.OnClickListener callProtectorListener = new View.OnClickListener() {
        public void onClick(View v) {
            try
            {
                String result = new UserTask().execute( "proPhoneNum", PreferenceHelper.getPreferences( getApplicationContext(), "proNo" ) ).get();
                String[][] parsedData = jsonParserList(result);
                startActivity( new Intent( "android.intent.action.CALL", Uri.parse("tel:"+parsedData[0][1]) ) );
            }
            catch ( Exception e ) {}

        }
    };

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

                // url = new URL("http://192.168.0.11:8080/CareU/patient_main_android.jsp"); // ch-Iptime 5G
                url = new URL("http://192.168.0.30:8080/CareU/patient_main_android.jsp"); // D444 5G

                sendMsg = "num="+strings[1]+"&type="+strings[0];

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

            String[] jsonName = {"type","data"};
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
