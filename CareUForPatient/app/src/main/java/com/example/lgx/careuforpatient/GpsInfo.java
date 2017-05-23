package com.example.lgx.careuforpatient;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;

/**
 * Created by LGX on 2017-05-12.
 */
public class GpsInfo extends Service implements LocationListener
{
    private final Context context;

    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean isGetLocation = false;

    Location location;
    double lat; // 위도
    double lon; // 경도

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 최소 GPS 정보 업데이트 거리는 10M
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 최소 GPS 정보 업데이트 시간은 1분

    protected LocationManager locationManager;

    public GpsInfo(Context context)
    {
        this.context = context;
        getLocation();
    }

    public Location getLocation()
    {
        try
        {
            locationManager = ( LocationManager ) context.getSystemService( LOCATION_SERVICE );

            isGPSEnabled = locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER );
            isNetworkEnabled = locationManager.isProviderEnabled( LocationManager.NETWORK_PROVIDER );

            if ( Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission( context, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission( context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }

            if ( !isGPSEnabled && !isNetworkEnabled ) // 사용 불가
            {
            }
            else
            {
                this.isGetLocation = true;
                if ( isNetworkEnabled )
                {
                    locationManager.requestLocationUpdates( LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this );
                    if ( locationManager != null )
                    {
                        location = locationManager.getLastKnownLocation( LocationManager.NETWORK_PROVIDER );
                        if ( location != null )
                        {
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                        }
                    }
                }
                if ( isGPSEnabled )
                {
                    if ( location == null )
                    {
                        locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this );
                        if ( locationManager != null )
                        {
                            location = locationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );
                            if ( location != null )
                            {
                                lat = location.getLatitude();
                                lon = location.getLongitude();
                            }
                        }
                    }
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return location;
    }

    public void stopUsingGPS()
    {
        if ( locationManager != null )
        {
            locationManager.removeUpdates( GpsInfo.this );
        }
    }

    public double getLatitude()
    {
        if ( location != null )
            lat = location.getLatitude();

        return lat;
    }

    public double getLongitude()
    {
        if ( location != null )
            lon = location.getLongitude();

        return lon;
    }

    public boolean isGetLocation()
    {
        return this.isGetLocation;
    }

    public void showSettingsAlert()
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder( context );

        alertDialog.setTitle( "GPS ON" );
        alertDialog.setMessage( "GPS 설정이 필요합니다. \n GPS 설정창으로 이동하시겠습니까?" );

        alertDialog.setPositiveButton( "Settings", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick ( DialogInterface dialog, int which )
            {
                Intent intent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS );
                context.startActivity( intent );
            }
        } );

        alertDialog.setNegativeButton( "Cancel", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick ( DialogInterface dialog, int which )
            {
                dialog.cancel();
            }
        } );

        alertDialog.show();
    }

    @Override
    public void onLocationChanged ( Location location )
    {

    }

    @Override
    public void onStatusChanged ( String provider, int status, Bundle extras )
    {

    }

    @Override
    public void onProviderEnabled ( String provider )
    {

    }

    @Override
    public void onProviderDisabled ( String provider )
    {

    }

    @Nullable
    @Override
    public IBinder onBind ( Intent intent )
    {
        return null;
    }
}
