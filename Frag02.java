package com.Stopwatch.StopWatch;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.Stopwatch.StopWatch.ui.main.TimeWatchOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import static java.util.Arrays.asList;

public class Frag02 extends Fragment {



    //時間表示フォーマット
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SS");

    private long nowtime_save;
    private String laplist_save;
    private String coment = "";
    private boolean saveflug = false;
    private boolean changeflug = false;
    private static CustomAdapter customAdapter;

    //DB用
    private TimeWatchOpenHelper helper;
    private SQLiteDatabase db;

    //コメントリスト
    HashMap<Integer, String> comentlist = new HashMap<Integer, String>();

    //親リスト設定（現時間）
    HashMap<Integer, Long> nowtime = new HashMap<Integer, Long>();

    //子リスト設定（lapリスト）
    ArrayList<ArrayList<HashMap<String, String>>> laptimelist = new ArrayList<ArrayList<HashMap<String,String>>>();



    //UI変更用スレッド処理
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final MainActivity ma = (MainActivity)getActivity();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    //新規保存処理　ＤＢへ保存後リスト更新
                    if (saveflug == true) {
                        insertData(db,nowtime_save,laplist_save,coment);
                        changeUI();
                        saveflug = false;
                    }

                    //リスト内の削除ボタン押された時の処理
                    if(customAdapter.getDelnum()>0){
                        //DBからデータ取得
                        Cursor cursor = readData();
                        //押した行のレコードを選択
                        cursormove(cursor);
                        //レコード削除
                        db.delete("testdb","_id=" + cursor.getInt(2), null);
                        cursor.close();
                        //削除作業終了のため、変数リセット
                        customAdapter.setDelnum(0);
                        //保存リスト表示更新
                        changeUI();

                    }

                    //リスト内の保存ボタン押された時の処理（レコード更新）
                    if(customAdapter.getSavenum()>0){
                        //DBからデータ取得
                        Cursor cursor = readData();

                        //押した行のレコードを選択
                        cursormove(cursor);

                        //DB指定した行の更新
                        ContentValues ct1 = new ContentValues();
                        ct1.put("nowtime",nowtime_save);
                        ct1.put("laptime",laplist_save);
                        coment = customAdapter.getComent();
                        Log.d("test+++", coment);
                        ct1.put("coment",coment);
                        db.update("testdb",ct1,"_id=?",new String[]{String.valueOf(cursor.getInt(2))});
                        cursor.close();

                        //保存作業終了のため、変数リセット
                        customAdapter.setSavenum(0);
                        //保存リスト表示更新
                        changeUI();

                    }

                    //リスト内の読込ボタン押された時の処理（ストップウォッチ画面移動時に読込したデータを適応）
                    if(customAdapter.getLoadnum()>0){

                        //DBからデータ取得
                        Cursor cursor = readData();

                        //押した行のレコードを選択
                        cursormove(cursor);

                        ma.laplist.clear();

                        //共有現在時刻更新
                        ma.share_now = cursor.getLong(0);

                        //共有lap更新
                        String str = cursor.getString(1);
                        List<String> str2 = new ArrayList<>();
                        if(str !=null) {
                            str2 = asList(str.split(","));
                            for(int i = 0;i<str2.size();i++){
                                ma.laplist.add(str2.get(i));
                            }
                        }

                        //読込フラグオン
                        ma.loadflug = true;

                        //読込作業終了のため、変数リセット
                        customAdapter.setLoadnum(0);
                        //保存リスト表示更新
                        changeUI();
                    }

                    //コメント設定したときの処理 (コメントのみ更新）
                    if(customAdapter.getComentnum()>0){
                        //DBからデータ取得
                        Cursor cursor = readData();

                        //押した行のレコードを選択
                        cursormove(cursor);
                        ContentValues ct1 = new ContentValues();
                        coment = customAdapter.getComent();
                        ct1.put("coment",coment);
                        db.update("testdb",ct1,"_id=?",new String[]{String.valueOf(cursor.getInt(2))});
                        cursor.close();
                        customAdapter.setComentnum(0);
                        Log.d("comenttest", coment);
                        try {
                            changeUI();
                        }catch (NullPointerException e){

                        }
                    }

                }
            }
        });
        thread.start();
    }

    @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {

            final View v =  inflater.inflate(R.layout.frag02_layout,container,false);
            final ExpandableListView listView = (ExpandableListView)v.findViewById(R.id.save_list);
            final TextView textView = v.findViewById(R.id.now_textview);
            final MainActivity ma = (MainActivity)getActivity();
            final Button btn = v.findViewById(R.id.btn_nowtime_save);

            nowtime_save = ma.share_now;
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            textView.setText(sdf.format(nowtime_save));
            btn.setOnClickListener(new onSavelaplist());

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        //計測時間が変更されている場合の処理　保存用時間変数に代入
                        if(nowtime_save != ma.share_now){
                            nowtime_save = ma.share_now;
                            if(ma.laplist.size() != 0) {
                                laplist_save = String.valueOf(ma.laplist.get(0));
                                for (int i = 1; i < ma.laplist.size(); i++) {
                                    laplist_save += "," + ma.laplist.get(i);
                                }
                            }else{
                                laplist_save = "";
                            }
                            //現計測時間を表示
                            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                            textView.setText(sdf.format(nowtime_save));
                            }

                        //タブ切り替え時に表示更新
                        if(changeflug == true){
                            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                            textView.setText(sdf.format(nowtime_save));
                            changeflug = false;
                        }


                    }

                    }
            });
            thread.start();

            //DB読み込み
            if(helper == null){
                helper = new TimeWatchOpenHelper(getActivity().getApplicationContext());
            }
            if(db == null){
                db = helper.getWritableDatabase();
            }
            listData();
            // 親リスト、子リストを含んだAdapterを生成
            customAdapter = new CustomAdapter(getActivity().getApplicationContext(), R.layout.list, R.layout.laplist,nowtime, laptimelist,comentlist);
            listView.setAdapter(customAdapter);

        return v;
        }

        //保存ボタン押したら新規保存（フラグ切り替え）
        public class onSavelaplist implements View.OnClickListener {
            @Override
            public void onClick(View view) {
                saveflug = true;
            }
        }

    @Override
    public void onResume() {
        super.onResume();
        changeflug = true;

        MainActivity ma = (MainActivity)getActivity();
        //表示された時に保存用時間を確保
        nowtime_save = ma.share_now;
        if(ma.laplist.size() !=0) {
            laplist_save = String.valueOf(ma.laplist.get(0));
            for (int i = 1; i < ma.laplist.size(); i++) {
                laplist_save += "," + ma.laplist.get(i);
            }
        }
    }

    private void listData(){
        nowtime.clear();
        laptimelist.clear();
        comentlist.clear();
        //DBからデータ取得
        Cursor cursor = readData();
        for (int i = 0; i < cursor.getCount(); i++) {


            if(cursor.getString(3)!=null) {
                comentlist.put(i + 1, cursor.getString(3));
            }

            //現在時刻を取得→リストへ
            nowtime.put(i+1, cursor.getLong(0));

            //lapを取得→リスト変換
            ArrayList<HashMap<String, String>> laplap = new ArrayList<HashMap<String, String>>();
            String str = cursor.getString(1);
            List<String> str2 = new ArrayList<>();
            //lapリスト登録されていた場合、,区切りで文字を分けてリストを作成
            if(str !=null) {
                str2 = asList(str.split(","));
            }
            for(int j=0;j<str2.size();j++) {
                HashMap<String, String> laplist = new HashMap<String, String>();
                laplist.put("lap", str2.get(j));
                laplap.add(laplist);
            }

            laptimelist.add(laplap);
            cursor.moveToNext();
        }
        cursor.close();
    }

    //DBにデータ登録
    private void insertData(SQLiteDatabase db, long now, String lap,String coment){
        ContentValues values = new ContentValues();
        values.put("nowtime", now);
        values.put("laptime", lap);
        values.put("coment",coment);
        db.insert("testdb", null, values);
    }
    //起動時読み込み用
    private Cursor readData(){
        if(helper == null){
            helper = new TimeWatchOpenHelper(getActivity().getApplicationContext());
        }
        if(db == null){
            db = helper.getReadableDatabase();
        }
        Cursor cursor = db.query(
                "testdb",
                new String[] {  "nowtime", "laptime" ,"_ID","coment"},
                null,
                null,
                null,
                null,
                null
        );
        cursor.moveToFirst();
        Log.d("dbtest", String.valueOf(cursor));
        return cursor;
    }

    //DBのcursorリスト内ボタン押した行数まで移動（保存・読込・削除・コメント入力時）
    private void cursormove(Cursor cursor){
        if(customAdapter.getDelnum()>1) {
            for (int i = 1; i < customAdapter.getDelnum(); i++) {
                cursor.moveToNext();
            }
        }
        if(customAdapter.getSavenum()>1) {
            for (int i = 1; i < customAdapter.getSavenum(); i++) {
                cursor.moveToNext();
            }
        }
        if(customAdapter.getLoadnum()>1) {
            for (int i = 1; i < customAdapter.getLoadnum(); i++) {
                cursor.moveToNext();
            }
        }
        if(customAdapter.getComentnum()>1) {
            for (int i = 1; i < customAdapter.getComentnum(); i++) {
                cursor.moveToNext();
            }
        }

    }
    //リスト表示更新処理
    public void changeUI(){
        final MainActivity ma = (MainActivity)getActivity();

        ma.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listData();
                customAdapter.notifyDataSetChanged();
            }
        });
    }
}

