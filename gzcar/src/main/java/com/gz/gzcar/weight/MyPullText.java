package com.gz.gzcar.weight;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gz.gzcar.R;

import java.util.ArrayList;

/**
 * Created by Endeavor on 2016/8/9.
 */
public class MyPullText extends RelativeLayout implements View.OnClickListener {

    private TextView textView;
    private ListView listwiew;
    private PopupWindow pop;
    private View view;

    public MyPullText(Context context) {
        super(context);
        init(context);
    }

    public MyPullText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MyPullText);
    }


    public MyPullText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.MyPullText);
    }

    private void init(Context context) {

        view = LayoutInflater.from(context).inflate(R.layout.mypulltext, this);
        textView = (TextView) view.findViewById(R.id.tv_text);

        view.setOnClickListener(this);


    }

    // 设置字体颜色
    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    // 设置字体大小
    public void setTextSize(float size) {
        textView.setTextSize(size);
    }

    // 设置显示内容
    public void setText(String text) {
        textView.setText(text);
    }

    public String getText() {

        return textView.getText().toString().trim();
    }

    // 设置下拉菜单显示数据
    public void setPopList(ArrayList<String> item) {

        if (item == null)
            return;


        listwiew = new ListView(getContext());

        PopAdapter popAdapter = new PopAdapter(item);

        listwiew.setAdapter(popAdapter);

        measure(0, 0);

        Log.i("my", "this.getWidth()===" + this.getMeasuredWidth());
        pop = new PopupWindow(listwiew, this.getMeasuredWidth(), 200, true);
        pop.setBackgroundDrawable(new BitmapDrawable());
        pop.setOutsideTouchable(true);


        listwiew.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String text = mItem.get(i);
                textView.setText(text);
                pop.dismiss();

                if (mOnTextChangedListener == null) {
                    return;
                } else {
                    mOnTextChangedListener.OnTextChanged();
                }
            }
        });
    }

    OnTextChangedListener mOnTextChangedListener;

    public void setOnTextChangedListener(OnTextChangedListener listener) {
        mOnTextChangedListener = listener;
    }

    public interface OnTextChangedListener {
        void OnTextChanged();
    }

    @Override
    public void onClick(View view) {

        if (listwiew == null) {
            return;
        }
        pop.showAsDropDown(view, 0, 0);

    }

    ArrayList<String> mItem;

    class PopAdapter extends BaseAdapter {


        public PopAdapter(ArrayList<String> item) {
            mItem = item;
        }

        @Override
        public int getCount() {

            return mItem.size();
        }

        @Override
        public String getItem(int i) {
            return mItem.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            ViewHolder holder;
            if (convertView == null) {

                convertView = View.inflate(MyPullText.this.getContext(), R.layout.item_pop, null);
                holder = new ViewHolder();
                holder.tv = (TextView) convertView.findViewById(R.id.tv_pop_text);
                convertView.setTag(holder);
            }else {
                holder = (ViewHolder) convertView.getTag();
            }

            String item = getItem(i);
            holder.tv.setText(item);

            return convertView;
        }
    }

    static class ViewHolder {
        public TextView tv;
    }
}
