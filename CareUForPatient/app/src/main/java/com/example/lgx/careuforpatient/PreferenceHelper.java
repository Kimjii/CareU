package com.example.lgx.careuforpatient;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by LGX on 2017-05-12.
 */

/**
 * SharedPreferences 목록
 *  First : 최초 실행 판별 변수
 *  patno : 환자 고유번호
 *  prono : 보호자 고유번호
 *  address : 환자의 이전 위치 저장
 *  clock : thread 호출 시 현재 시각 저장( 시각 변화 감지를 위해 )
 */

public class PreferenceHelper extends Application
{
    // 값 불러오기
    public static String getPreferences( Context context, String key)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( context );
        return pref.getString( key, "" );
    }

    // 값 저장하기
    public static void savePreferences(Context context, String key, String value)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( context );
        SharedPreferences.Editor editor = pref.edit();
        editor.putString( key, value );
        editor.commit();
    }

    // 값(Key Data) 삭제하기
    public static void removePreferences(Context context, String key)
    {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences( context );
        SharedPreferences.Editor editor = pref.edit();
        editor.remove( key );
        editor.commit();
    }

}