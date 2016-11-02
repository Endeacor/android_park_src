package com.gz.gzcar.searchfragment;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.gz.gzcar.BaseFragment;
import com.gz.gzcar.Database.TrafficInfoTable;
import com.gz.gzcar.MyApplication;
import com.gz.gzcar.R;
import com.gz.gzcar.utils.DateUtils;
import com.gz.gzcar.utils.L;
import com.gz.gzcar.utils.T;

import org.xutils.DbManager;
import org.xutils.ex.DbException;
import org.xutils.x;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by Endeavor on 2016/8/8.
 * <p/>
 * 收费记录
 */
public class PrizeFragment extends BaseFragment {


    @Bind(R.id.et_money_carnumber)
    EditText mCarNumber;
    @Bind(R.id.et_money_starttime)
    TextView mStartTime;
    @Bind(R.id.et_money_endtime)
    TextView mEndTime;
    @Bind(R.id.btn_money_search)
    Button mSearch;
    @Bind(R.id.money_recyclerview)
    RecyclerView rcy;
    @Bind(R.id.tv_money)
    TextView mMoney;
    private DbManager db = x.getDb(MyApplication.daoConfig);
    private List<TrafficInfoTable> allData = new ArrayList<>();
    private MyAdapter myAdapter;
    private int pageIndex = 0;
    private int TAG = 0;
    private String searchNumber;
    private String searchStart;
    private String searchEnd;

    //c7edcc
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search_money, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String start = DateUtils.getCurrentYear() + "-" + DateUtils.getCurrentMonth() + "-" + DateUtils.getCurrentDay() + " 00:00";
        String end = DateUtils.getCurrentDataDetailStr();
//        Log.e("ende", "start==" + start);
//        Log.e("ende", "end==" + end);
        mStartTime.setText(start);
        mEndTime.setText(end);
    }

    @Override
    public void onResume() {
        super.onResume();

        TAG = 0;
        pageIndex = 0;
        allData.clear();
        if (myAdapter != null)
            myAdapter.notifyDataSetChanged();

        initdata();
        initViews();
    }

    private void initViews() {

        //时间选择器
        initDetailTime(getContext(), mStartTime, mEndTime);

        final LinearLayoutManager lm = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        rcy.setLayoutManager(lm);
        myAdapter = new MyAdapter();
        rcy.setAdapter(myAdapter);
        rcy.setOnScrollListener(new RecyclerView.OnScrollListener() {

            int oldY = -1;

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                L.showlogError("dy==" + dy);
                oldY = dy;

            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                int lastVisibleItemPosition = lm.findLastVisibleItemPosition();
                if (newState == 0 && lastVisibleItemPosition == (allData.size() - 1)) {
                    pageIndex += 1;
                    loadMore(pageIndex);

                }
            }
        });


    }

    private void initdata() {
        if (!TextUtils.isEmpty(searchNumber)) {
            TAG = 2;
        }
        searchStart = mStartTime.getText().toString().trim();
        searchEnd = mEndTime.getText().toString().trim();
        loadMore(pageIndex);
        sumMoney();

    }

    private void loadMore(int pageIndex) {
        L.showlogError("TAG===" + TAG);

        if (TAG == 0) {// 时间查询
            L.showlogError("====时间查询===");
            try {
                List<TrafficInfoTable> all = db.selector(TrafficInfoTable.class)
                        .where("update_time", ">", dateFormatDetail.parse(searchStart))
                        .and("update_time", "<", dateFormatDetail.parse(searchEnd))
                        .and("status", "=", "已出")
                        .and("receivable", ">", 0)
                        .limit(15)
                        .offset(15 * pageIndex)
                        .orderBy("update_time", true)
                        .findAll();
                if (all != null && all.size() > 0) {
                    allData.addAll(all);
                    if (myAdapter != null)
                        myAdapter.notifyDataSetChanged();
                } else {
                    T.showShort(getContext(), "没有更多数据了");
                }

            } catch (DbException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (TAG == 2) {// 车号&时间查询
            L.showlogError("====车号&时间查询===");
            try {
                List<TrafficInfoTable> all = db.selector(TrafficInfoTable.class)
                        .where("update_time", ">", dateFormatDetail.parse(searchStart))
                        .and("update_time", "<", dateFormatDetail.parse(searchEnd))
                        .and("car_number", "like", "%" + searchNumber + "%")
                        .and("receivable", ">", 0)
                        .and("status", "=", "已出")
                        .limit(15)
                        .offset(15 * pageIndex)
                        .orderBy("update_time", true)
                        .findAll();
                if (all != null && all.size() > 0) {
                    allData.addAll(all);
                    if (myAdapter != null)
                        myAdapter.notifyDataSetChanged();
                } else {
                    T.showShort(getContext(), "没有更多数据了");
                }

            } catch (DbException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }


    }


    @OnClick({R.id.et_money_starttime, R.id.et_money_endtime, R.id.btn_money_search})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.et_money_starttime:
                startTimeShow();
                break;
            case R.id.et_money_endtime:
                endTimeShow();
                break;

            case R.id.btn_money_search:

                search();

                break;
        }
    }

    private void search() {
        searchNumber = mCarNumber.getText().toString().trim();
        searchStart = mStartTime.getText().toString().trim();
        searchEnd = mEndTime.getText().toString().trim();
        pageIndex = 0;
        allData.clear();
        myAdapter.notifyDataSetChanged();

        if (TextUtils.isEmpty(searchNumber)) {
            TAG = 0;
            loadMore(pageIndex);
            sumMoney();

        } else {
            TAG = 2;
            loadMore(pageIndex);
            sumMoney();

        }
    }

    private void sumMoney() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                switch (TAG) {

                    case 0:

                        try {
                            List<TrafficInfoTable> all = db.selector(TrafficInfoTable.class)
                                    .where("update_time", ">", dateFormatDetail.parse(searchStart))
                                    .and("update_time", "<", dateFormatDetail.parse(searchEnd))
                                    .and("status", "=", "已出")
                                    .and("receivable", ">", 0)
                                    .orderBy("update_time", true)
                                    .findAll();

                            double toteMoney = 0;
                            if (all != null) {

                                for (int i = 0; i < all.size(); i++) {

                                    double money = all.get(i).getActual_money();
                                    toteMoney += money;
                                }
                            }

                            Message message = Message.obtain();
                            message.obj = toteMoney;
                            handler.sendMessage(message);

                        } catch (DbException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }


                        break;
                    case 2:

                        try {
                            List<TrafficInfoTable> all = db.selector(TrafficInfoTable.class)
                                    .where("update_time", ">", dateFormatDetail.parse(searchStart))
                                    .and("update_time", "<", dateFormatDetail.parse(searchEnd))
                                    .and("car_number", "like", "%"+searchNumber+"%")
                                    .and("receivable", ">", 0)
                                    .and("status", "=", "已出")
                                    .orderBy("update_time", true)
                                    .findAll();
                            double toteMoney = 0;
                            if (all != null) {

                                for (int i = 0; i < all.size(); i++) {

                                    double money = all.get(i).getActual_money();
                                    toteMoney += money;
                                }
                            }

                            Message message = Message.obtain();
                            message.obj = toteMoney;
                            handler.sendMessage(message);

                        } catch (DbException e) {
                            e.printStackTrace();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        break;
                }

            }
        }.start();


    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            double toteMoney = (double) msg.obj;

            mMoney.setText("合计金额:" + toteMoney + " 元");

        }
    };

    private class MyAdapter extends RecyclerView.Adapter<MyHolder> {

        @Override
        public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(getActivity()).inflate(R.layout.item_search_free, parent, false);
            MyHolder myHolder = new MyHolder(itemView);
            return myHolder;
        }

        @Override
        public void onBindViewHolder(MyHolder holder, int position) {

            final TrafficInfoTable free = allData.get(position);
            holder.mId.setText(position + 1 + "");
            holder.mCarNum.setText(free.getCar_no());
            holder.mMoney.setText(free.getActual_money() + "");
            holder.mParkingtime.setText(free.getStall_time());
            holder.mType.setText(free.getCar_type());
            Date inTime = free.getIn_time();
            if (inTime != null) {
                holder.mInTime.setText(dateFormatDetail.format(inTime));
            }
            if (free.getOut_time() != null) {

                holder.mOuttime.setText(dateFormatDetail.format(free.getOut_time()));
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getContext(), ImageDetailActivity.class);
                    intent.putExtra("in_image", free.getIn_image() + "");
                    intent.putExtra("out_image", free.getOut_image() + "");

                    intent.putExtra("carNumber", free.getCar_no() + "");

                    if (free.getCar_type() == null)
                        intent.putExtra("carType", "未知");
                    else
                        intent.putExtra("carType", free.getCar_type() + "");

                    if (free.getStall() == null)
                        intent.putExtra("stall", "无");//占用车位
                    else
                        intent.putExtra("stall", free.getStall() + "");//占用车位
                    if (free.getReceivable() == null)
                        intent.putExtra("receivable", "未出场");//应收费用
                    else
                        intent.putExtra("receivable", free.getReceivable() + "");//应收费用
                    if (free.getActual_money() == null)
                        intent.putExtra("actual_money", "未出场");//实收费用
                    else
                        intent.putExtra("actual_money", free.getActual_money() + "");//实收费用
                    intent.putExtra("status", free.getStatus() + "");  //通行状态
                    if (free.getOut_user() == null)
                        intent.putExtra("out_user", "未知");
                    else
                        intent.putExtra("out_user", free.getOut_user() + "");
                    if (free.getIn_user() == null)
                        intent.putExtra("in_user", "未知");
                    else
                        intent.putExtra("in_user", free.getIn_user() + "");


                    intent.putExtra("in_time", DateUtils.date2StringDetail(free.getIn_time()));
                    if (free.getOut_time() == null)
                        intent.putExtra("out_time", "未出场");
                    else
                        intent.putExtra("out_time", DateUtils.date2StringDetail(free.getOut_time()));

                    if (free.getStall_time() == null)
                        intent.putExtra("stall_time", "未知");//停车时长
                    else
                        intent.putExtra("stall_time", free.getStall_time() + "");//停车时长
                    intent.putExtra("update_time", DateUtils.date2StringDetail(free.getUpdateTime()));
                    startActivity(intent);
                }
            });

        }

        @Override
        public int getItemCount() {
            return allData.size();
        }

    }


    class MyHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.item_free_id)
        TextView mId;
        @Bind(R.id.item_free_num)
        TextView mCarNum;
        @Bind(R.id.item_free_type)
        TextView mType;
        @Bind(R.id.item_free_intime)
        TextView mInTime;
        @Bind(R.id.item_free_outtime)
        TextView mOuttime;
        @Bind(R.id.item_free_parkingtime)
        TextView mParkingtime;
        @Bind(R.id.item_free_money)
        TextView mMoney;

        public MyHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }

}
