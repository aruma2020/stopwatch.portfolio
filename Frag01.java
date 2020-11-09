package com.Stopwatch.StopWatch;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

public class Frag01 extends Fragment {
    private Handler handler =new Handler();

    //時間計算用フィールド変数
    private long starttime = 0;
    private long stoptime = 0;
    private long nowtime=0;
    private long stopkei = 0;

    //起動状態管理用フィールド変数
    private boolean start_flug = false;
    private boolean reset_flug = true;
    private boolean lap_flug = false;
    private boolean lap_get = false;

    //Ｌａｐ回数
    private int lap_count = 0;

    //Lapリスト用
    static ArrayList list = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;

    //時間表示フォーマット
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SS");


    //画面切り替え時にデータ保存→渡し
    @Override
    public void onPause() {
        super.onPause();
        MainActivity ma = (MainActivity)getActivity();
        ma.share_now = this.nowtime;
        ma.laplist = list;
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity ma = (MainActivity)getActivity();
        Log.d("tesl", "laplist" + String.valueOf(ma.laplist));

        //保存リストから読込したときの処理
        if(ma.loadflug == true) {
            //時間リセット
            nowtime = 0;
            stoptime = -ma.share_now;
            reset_flug = true;
            start_flug = false;
            lap_count=0;

            //このフラグメントの変数を読み込んだデータに合わせる
            this.nowtime = ma.share_now;
            list = ma.laplist;
            for(int i = 0;i<ma.laplist.size();i++){
                lap_count++;
            }
            ma.loadflug = false;
            lap_get = true;

            //もし計測を持続中に読込処理した場合、停止状態にする
            start_flug = false;
        }

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                //lapリスト作成
                final ListView listView = getActivity().findViewById(R.id.lap_list);
                arrayAdapter=new ArrayAdapter<>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, list);
                listView.setAdapter(arrayAdapter);

                while(true) {
                    //Lapを取得した状態であればLapリスト更新
                    if (lap_get == true) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("testlap", "List" + String.valueOf(list));
                                ArrayList list2 = (ArrayList) list.clone();
                                arrayAdapter.clear();
                                list = (ArrayList) list2.clone();

                                arrayAdapter = new ArrayAdapter<>(getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, list);
                                listView.setAdapter(arrayAdapter);

                                arrayAdapter.notifyDataSetChanged();
                            }
                        });
                        lap_get = false;
                    }
                }
            }
        });
        thread.start();
    }

    @Nullable
    @Override
    public View onCreateView (@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        /*return inflater.inflate(R.layout.frag01_layout,container,false);*/

        View v = inflater.inflate(R.layout.frag01_layout,container,false);
        final TextView textView = v.findViewById(R.id.textView);

        //時間リアルタイム表示
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                while(true) {
                    //リアルタイムで時間表示
                    if (start_flug == true) {
                        nowtime = (System.currentTimeMillis() - starttime - stoptime);
                        textView.setText(sdf.format(nowtime));
                        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                    }

                    //リセット押したら停止中でもリセット表示
                    if(reset_flug == true){
                        textView.setText(sdf.format(nowtime));
                        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                    }
                    //Lapボタンを押したら時間取得
                    if(lap_flug == true){
                        list.add("Lap" + lap_count + "　　　" + textView.getText());
                        Log.d("testlap", String.valueOf(textView.getText()));
                        lap_get = true;
                        lap_flug = false;
                    }
                }
            }
        });
        thread.start();
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //スタートボタン生成
        Button btnStart = (Button)view.findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new onStartClick());

        //リセットボタン生成
        Button btnReset = (Button)view.findViewById(R.id.btn_reset);
        btnReset.setOnClickListener(new onResetClick());

        //lapボタン生成
        Button btnLap = (Button)view.findViewById(R.id.btn_lap);
        btnLap.setOnClickListener(new onLapClick());

        //リスト表示(読み込み受付）
        ListView listview = (ListView)view.findViewById(R.id.lap_list);

        //広告表示
        MobileAds.initialize(getActivity().getApplicationContext(),"ca-app-pub-8417122983180836~7359391360");
        AdView mAdView = view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();

        mAdView.loadAd(adRequest);
    }

    //スタートボタン処理
    public class onStartClick implements View.OnClickListener{

        @Override
        public void onClick(final View view) {
            Button btnStart = (Button)view.findViewById(R.id.btn_start);
            //時間開始
            if(start_flug == false) {
                start_flug = true;
                if (reset_flug == true){
                    //初回起動　or リセットボタン押した後（０スタート）
                    starttime = System.currentTimeMillis();
                    reset_flug = false;
                }else{
                    //ストップ時間計算　ストップ取得時間～再開時間までの時間を計算
                    stopkei =System.currentTimeMillis() - stopkei;
                    stoptime = stoptime + stopkei;
                }
                btnStart.setText("ストップ");
            //時間ストップ
            }else{
                start_flug = false;
                btnStart.setText("スタート");
                //計算用　ストップした時間を取得
                stopkei = System.currentTimeMillis();
            }
        }
    }

    //Lapボタン処理
    public class onLapClick implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            //lapフラグオン+lapタップ回数+1
            lap_flug = true;
            lap_count++;
        }
    }
    //リセットボタン処理
    public class onResetClick implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            //時間リセット
            reset_flug = true;
            nowtime = 0;
            starttime = System.currentTimeMillis();
            stoptime = 0;
            lap_count = 0;
            arrayAdapter.clear();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        OnBackPressedCallback callback = new OnBackPressedCallback(true ) {
            @Override
            public void handleOnBackPressed() {
                // 戻る押した場合ダイアログ表示処理
                DialogFragment newFragment = new TestDialogFragment();
                newFragment.show(getFragmentManager(), "test");
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }

    public static class TestDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // 戻るボタンでアプリ終了するか詮索させる。
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("アプリを終了します。よろしいですか？")
                    .setPositiveButton("キャンセル", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    })
                    .setNegativeButton("はい", new DialogInterface.OnClickListener() {
                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                        public void onClick(DialogInterface dialog, int id) {
                            getActivity().finishAndRemoveTask();
                        }
                    });
            return builder.create();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //アプリ終了時lapリストクリア
        list.clear();
    }
}
