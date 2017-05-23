package com.example.lgx.careuforpatient;

import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GPSService extends Service
{
    ServiceThread thread;

    /* Reverse GeoCoder */
    private GpsInfo gps;
    double latitude = 0, longitude = 0;
    Geocoder geocoder = new Geocoder( this );
    List<Address> list = null;

    /* Now Time & Date */
    long nowTime;
    Date nowDate;
    SimpleDateFormat format = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
    boolean isSameTime = false;

    @Override
    public void onCreate ()
    {
        gps = new GpsInfo( GPSService.this );
        super.onCreate();
    }

    @Override
    public IBinder onBind ( Intent intent )
    {
        return null;
    }

    @Override
    public int onStartCommand ( Intent intent, int flags, int startId )
    {
        gpsServiceHandler handler = new gpsServiceHandler();
        thread = new ServiceThread( handler );
        thread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        thread.stopForever();
        thread = null;//쓰레기 값을 만들어서 빠르게 회수하라고 null을 넣어줌.
    }

    class gpsServiceHandler extends Handler
    {
        String sendMsg;

        @Override
        public void handleMessage(android.os.Message msg)
        {
            try
            {
                 /* 현재 위치 GPS 가져오기 */
                if ( gps.isGetLocation() )
                {
                    latitude = gps.getLatitude();
                    longitude = gps.getLongitude();
                }
                else
                {
                    gps.showSettingsAlert();
                }

                /* 현재 위치 위도, 경도 -> 주소로 변환 */
                list = geocoder.getFromLocation( latitude, longitude, 10 );

                /* 웹 서버 접근 */
                if ( isSameTime == false ) // 시각이 달라짐
                {
                    String result = new UserHomeTask().execute( PreferenceHelper.getPreferences( getApplicationContext(), "patNo" ), "mouvingroute" ,list.get(0).getAddressLine(0), getNowTime() ).get();
                    PreferenceHelper.savePreferences( getApplicationContext(), "address", list.get(0).getAddressLine(0) );
                }
                else
                {
                    if ( PreferenceHelper.getPreferences( getApplicationContext(), "address" ).equals( list.get(0).getAddressLine(0) )) {} // 같은 위치
                    else
                    {
                        String result = new UserHomeTask().execute( PreferenceHelper.getPreferences( getApplicationContext(), "patNo" ), "mouvingroute" ,list.get(0).getAddressLine(0), getNowTime() ).get();
                        PreferenceHelper.savePreferences( getApplicationContext(), "address", list.get(0).getAddressLine(0) );
                    }
                }
            }
            catch ( MalformedURLException e )
            {
                e.printStackTrace();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
            catch ( Exception e ) {}

        }

        private String getNowTime()
        {
            nowTime = System.currentTimeMillis();
            nowDate = new Date( nowTime );

            SimpleDateFormat clockFormat = new SimpleDateFormat( "HH" );
            if ( clockFormat.format( nowDate ).equals( PreferenceHelper.getPreferences( getApplicationContext(), "clock" ) ) ) // 현재 시각과 이전 저장 시각이 같으면
            {
                isSameTime = true;
            }
            else // 다르면
            {
                isSameTime = false;
                PreferenceHelper.savePreferences( getApplicationContext(), "clock", clockFormat.format( nowDate ) );
            }

            return format.format( nowDate );
        }
    }

    class UserHomeTask extends AsyncTask<String, Void, String>
    {
        String sendMsg, receiveMsg;

        @Override
        protected String doInBackground(String... strings)
        {
            try
            {
                String str;
                URL url = new URL("http://192.168.0.11:8080/CareU/mouvingroute_insert_android.jsp"); // ch-Iptime 5G
                // URL url = new URL("http://192.168.0.30:8080/CareU/mouvingroute_insert_android.jsp"); // D444 5G

                sendMsg = "patno="+strings[0]+"&division="+strings[1]+"&address="+strings[2]+"&time="+strings[3];


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
}


