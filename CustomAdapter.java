package com.Stopwatch.StopWatch;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;

public class CustomAdapter extends BaseExpandableListAdapter{

    //親リスト
    private HashMap<Integer, Long> groups;
    //子リスト
    private ArrayList<ArrayList<HashMap<String, String>>> children;
    //親layout
    private int nowlayout_id;
    //子layout
    private int laplayout_id;
    //コメントリスト
    private HashMap<Integer, String> comentl;

    private LayoutInflater inflater;
    //生成時のみコメント適用するための変数
    private int first_count;

    //レコード削除の行数　削除ボタン時に代入される
    private int delnum;
    //レコード読込の行数　読込ボタン時に代入される
    private int loadnum;
    //レコード保存（更新）の行数　保存ボタン時に代入される
    private int savenum;
    //コメントされた行数　コメント設定時に代入される
    private int comentnum;

    //コメント取得用
    private String coment;

    //時間表示フォーマット
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SS");



    static class  ViewHolder{
        Button saveButton;
        TextView textView;
        EditText nowtextview;
        Button loadButton;
        Button deleteButton;
        LinearLayout layout;
        LinearLayout mainlayout;
    }
    public CustomAdapter(Context context, int nowlayout_id, int laplayout_id , HashMap<Integer, Long> groups, ArrayList<ArrayList<HashMap<String, String>>> children,HashMap<Integer, String> comentl){
        this.groups = groups;
        this.children = children;
        this.comentl = comentl;
        this.nowlayout_id = nowlayout_id;
        this.laplayout_id = laplayout_id;
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    //保存・削除・読込押した場所の取得用
    public int getSavenum(){
        return this.savenum;
    }
    public int getDelnum(){
        return this.delnum;
    }
    public int getLoadnum(){
        return this.loadnum;
    }
    public int getComentnum(){
        return this.comentnum;
    }
    public String getComent(){return this.coment;}
    public void setDelnum(int i){
        this.delnum = i;
    }
    public void setSavenum(int i){
        this.savenum = i;
    }
    public void setLoadnum(int i){
        this.loadnum = i;
    }
    public void setComentnum(int i){this.comentnum = i;}
    public void setComent(String s){this.coment =s;}


    @Override
    public int getGroupCount() {
        return groups.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return children.get(i).size();
    }

    @Override
    public Object getGroup(int i) {
        return groups.get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return children.get(i).get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    //現時刻view設定
    @Override
    public View getGroupView(final int position, boolean b, View view, final ViewGroup parent) {
        ViewHolder holder;
        if(view == null){
            //各テキスト・ボタン生成
            view = inflater.inflate(nowlayout_id,parent,false);
            holder = new ViewHolder();
            holder.textView = view.findViewById(R.id.textView);
            holder.nowtextview = view.findViewById(R.id.nowtime_text);
            holder.saveButton = view.findViewById(R.id.btn_save);
            holder.loadButton = view.findViewById(R.id.btn_load);
            holder.deleteButton = view.findViewById(R.id.btn_del);
            holder.layout = view.findViewById(R.id.save_lilst_textview);
            holder.mainlayout = view.findViewById(R.id.main_time_list);
            view.setTag(holder);
        }
            holder = (ViewHolder)view.getTag();

        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        //保存時刻表示
        try {
            holder.textView.setText(sdf.format(groups.get(position + 1)));
        }catch (IllegalArgumentException e){
            Log.d("groups", String.valueOf(groups.get(position + 1)));
        }

        //テキストクリック時lapリスト展開
        final View finalView = view;
        final ExpandableListView mExpandableListView = (ExpandableListView) parent;

        //コメント入力の検出
        final ViewHolder finalHolder = holder;
        TextWatcher watchHandler = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            //入力時にDBに保存
            @Override
            public void afterTextChanged(Editable s) {
                Log.d("TAG", "afterTextChanged()");
                coment = String.valueOf(finalHolder.nowtextview.getText());
                comentnum = position+1;
            }
        };

        holder.nowtextview.addTextChangedListener(watchHandler);

        //生成時のみコメント適応（DBから読込）
        //これがないと入力時適応を繰り返し入力できないため
        if(first_count < position+1) {
            holder.nowtextview.setText(comentl.get(position + 1));
            coment = comentl.get(position+1);
        }
        first_count++;

        //コメント確定時キーボードを隠す
        holder.nowtextview.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                        // ソフトキーボードを隠す
                        Context co = textView.getContext();
                        ((InputMethodManager)co.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(textView.getWindowToken(), 0);
                    return true;
            }
        });

        //時間・左のマーククリックで子グループ展開
        final boolean bb = b;
        holder.mainlayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bb == false) {
                    mExpandableListView.expandGroup(position);
                }else{
                    mExpandableListView.collapseGroup(position);
                }
            }
        });
        //保存ボタン処理　現時間に更新
        holder.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savenum = position+1;
            }
        });
        //読込ボタン処理　押したら計測画面に適応
        holder.loadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                loadnum = position+1;
            }
        });

        //削除ボタン処理　押したらDBレコード削除指令（変数代入）
        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delnum = position+1;
            }
        });
        return finalView;
    }

    @Override
    //lapリストのview設定　i=親要素位置　i1=子要素位置
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup parent) {
        View v;
        if (view == null) {
            v = inflater.inflate(laplayout_id,parent,false);
        } else {
            v = view;
        }
        TextView textView = v.findViewById(R.id.lap_text);
        textView.setText(String.valueOf(children.get(i).get(i1).get("lap")));
        return v;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return false;
    }
}
