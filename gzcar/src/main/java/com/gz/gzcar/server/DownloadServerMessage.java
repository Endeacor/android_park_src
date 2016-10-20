package com.gz.gzcar.server;

import android.util.Log;

import com.gz.gzcar.Database.CarInfoTable;
import com.gz.gzcar.Database.CarWeiBindTable;
import com.gz.gzcar.Database.CarWeiTable;
import com.gz.gzcar.Database.MoneyTable;
import com.gz.gzcar.Database.TrafficInfoTable;
import com.gz.gzcar.MyApplication;

import org.json.JSONException;
import org.json.JSONObject;
import org.xutils.DbManager;
import org.xutils.common.Callback.CommonCallback;
import org.xutils.common.util.KeyValue;
import org.xutils.db.sqlite.WhereBuilder;
import org.xutils.ex.DbException;
import org.xutils.http.RequestParams;
import org.xutils.x;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 下载一切需要的记录
 */
public class DownloadServerMessage {
	/**
	 * DB
	 */
	public static DbManager db =x.getDb(MyApplication.daoConfig);

	public static String mycontroller_sn="1";

	public static String url="http://221.204.11.69:3002/api/v1";

	public static Boolean log=false;

	/**
	 * 开始下载全部数据
	 */
	public void getallmessage(){
		try {
			DownloadTimeBean bean=db.selector(DownloadTimeBean.class).findFirst();
			if(bean!=null){
				startdown(bean);
			}else {
				//初始化新至进去
				DownloadTimeBean timeBean=new DownloadTimeBean();
				timeBean.setTime("1970-1-1 01:00:00");
				db.save(timeBean);
				startdown(timeBean);
			}
		} catch (DbException e) {
			e.printStackTrace();
		}
	}
	public void startdown(DownloadTimeBean bean){
		showlog("传入时间为："+bean.getTime());
		//开始下载
		try {
			get_in_out_record_download(url, bean.getTime(), mycontroller_sn);
			get_down_tempfee(url, bean.getTime(), mycontroller_sn);
			get_down_info_stall(url, bean.getTime(), mycontroller_sn);
			get_down_info_vehicle(url, bean.getTime(), mycontroller_sn);
			get_down_record_stall_vehicle(url, bean.getTime(), mycontroller_sn);
			//修改时间
			SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			db.update(DownloadTimeBean.class, WhereBuilder.b("time", "=", bean.getTime()),new KeyValue("time",dateFormat.format(new Date())));
		} catch (DbException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 下载通行记录
	 * @param time   上次记录时间
	 * @param controller_sn  设备号
	 * @param baseurl  base路径
	 */
	private void get_in_out_record_download(String baseurl,String time,String controller_sn){
		RequestParams params=new RequestParams(baseurl+"/in_out_record_download");
		params.addBodyParameter("controller_sn",controller_sn);
		params.addBodyParameter("max_updated_at",time);
		x.http().get(params, new CommonCallback<String>() {
			@Override
			public void onCancelled(CancelledException arg0) {
			}
			@Override
			public void onError(Throwable arg0, boolean arg1) {
			}
			@Override
			public void onFinished() {
			}
			@Override
			public void onSuccess(String arg0) {
				try {
					JSONObject object=new JSONObject(arg0);
					String ref=object.getString("ref");
					if("0".equals(ref)){
						String result=object.getString("result");
						if(result.length()!=2){
							List<DownloadinandoutRecordBean> list=new ArrayList<DownloadinandoutRecordBean>();
							list=com.alibaba.fastjson.JSONObject.parseArray(result, DownloadinandoutRecordBean.class);
							//下一步根据list来存数据库
							for(int c=0;c<list.size();c++){
								//开始存数据
								TrafficInfoTable table=new TrafficInfoTable();
								table.setIn_user(list.get(c).getIn_operator());
								table.setPass_no(list.get(c).getPass_no());
								table.setCar_type(list.get(c).getCard_type());
								table.setCar_no(list.get(c).getCar_no());
								table.setIn_time(DownUtils.getstringtodate(list.get(c).getIn_time()));
								table.setIn_image(list.get(c).getIn_image());
								table.setOut_time(DownUtils.getstringtodate(list.get(c).getOut_time()));
								table.setOut_image(list.get(c).getOut_image());
								table.setOut_user(list.get(c).getOut_operator());
								table.setStall(list.get(c).getStall_code());
								table.setReceivable(DownUtils.getstringtodouble(list.get(c).getFee()));
								table.setActual_money(DownUtils.getstringtodouble(list.get(c).getFact_fee()));
								table.setStall_time(list.get(c).getParked_time()+"");
								table.setUpdateTime(DownUtils.getstringtodate(list.get(c).getUpdated_at()));
								table.setStatus(list.get(c).getStatus());
								table.setModifeFlage(false);
								try {
									db.save(table);
								} catch (DbException e) {
									e.printStackTrace();
								}
							}
							showlog("下载通行记录保存完成");
						}else {
							showlog("下载通行记录无更新");	
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}


	/**
	 * 下传临时车收费明细
	 * @param time   上次记录时间
	 * @param controller_sn  设备号
	 * @param baseurl  base路径
	 */
	private void get_down_tempfee(String baseurl,String time,String controller_sn){
		RequestParams params=new RequestParams(baseurl+"/down_tempfee");
		params.addBodyParameter("controller_sn",controller_sn);
		params.addBodyParameter("max_updated_at",time);
		x.http().get(params, new CommonCallback<String>() {
			@Override
			public void onCancelled(CancelledException arg0) {
			}
			@Override
			public void onError(Throwable arg0, boolean arg1) {
			}
			@Override
			public void onFinished() {
			}
			@Override
			public void onSuccess(String arg0) {
				try {
					JSONObject object=new JSONObject(arg0);
					String ref=object.getString("ref");
					if("0".equals(ref)){
						String result=object.getString("result");
						if(result.length()!=2){
							List<DownTemFeeBean> list=new ArrayList<DownTemFeeBean>();
							list=com.alibaba.fastjson.JSONObject.parseArray(result, DownTemFeeBean.class);
							//下一步根据list来存数据库
							for(int c=0;c<list.size();c++){
								MoneyTable table=new MoneyTable();
								table.setFee_detail_code(list.get(c).getFee_detail_code());
								table.setFee_code(list.get(c).getFee_code());
								table.setFee_name(list.get(c).getFee_name());
								table.setParked_min_time(list.get(c).getParked_min_time());
								table.setParked_max_time(list.get(c).getParked_max_time());
								table.setMoney(DownUtils.getstringtodouble(list.get(c).getMoney()));
								table.setCreated_at(DownUtils.getstringtodate(list.get(c).getCreated_at()));
								table.setUpdated_at(DownUtils.getstringtodate(list.get(c).getUpdated_at()));
								table.setCar_type_code(list.get(c).getCar_type_code());
								table.setCar_type_name(list.get(c).getCar_type_name());
								try {
									db.save(table);
								} catch (DbException e) {
									e.printStackTrace();
								}
							}
							showlog("下传临时车收费明细保存成功");
						}else {
							showlog("下传临时车收费明细无更新");	
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * 下传车位表
	 * @param time   上次记录时间
	 * @param controller_sn  设备号
	 * @param baseurl  base路径
	 */
	private void get_down_info_stall(String baseurl,String time,String controller_sn){
		RequestParams params=new RequestParams(baseurl+"/down_info_stall");
		params.addBodyParameter("controller_sn",controller_sn);
		params.addBodyParameter("max_updated_at",time);
		x.http().get(params, new CommonCallback<String>() {
			@Override
			public void onCancelled(CancelledException arg0) {
			}
			@Override
			public void onError(Throwable arg0, boolean arg1) {
			}
			@Override
			public void onFinished() {
			}
			@Override
			public void onSuccess(String arg0) {
				try {
					JSONObject object=new JSONObject(arg0);
					String ref=object.getString("ref");
					if("0".equals(ref)){
						String result=object.getString("result");
						if(result.length()!=2){
							//开始进行解析
							List<DownInfoStallBean> list=new ArrayList<DownInfoStallBean>();
							list=com.alibaba.fastjson.JSONObject.parseArray(result, DownInfoStallBean.class);
							//下一步根据list来存数据库
							for(int c=0;c<list.size();c++){
								CarWeiTable table=new CarWeiTable();
								table.setStall_code(list.get(c).getStall_code());
								table.setArea_code(list.get(c).getArea_code());
								table.setPrint_code(list.get(c).getPrint_code());
								table.setCreated_at(DownUtils.getstringtodate(list.get(c).getCreated_at()));
								table.setUpdated_at(DownUtils.getstringtodate(list.get(c).getUpdated_at()));
								try {
									db.save(table);
								} catch (DbException e) {
									e.printStackTrace();
								}
							}
							showlog("下传车位表保存成功");
						}else {
							showlog("下传车位表无更新");	
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}
	/**
	 * 下传固定车信息表
	 * @param time   上次记录时间
	 * @param controller_sn  设备号
	 * @param baseurl  base路径
	 */
	private void get_down_info_vehicle(String baseurl,String time,String controller_sn){
		RequestParams params=new RequestParams(baseurl+"/down_info_vehicle");
		params.addBodyParameter("controller_sn",controller_sn);
		params.addBodyParameter("max_updated_at",time);
		x.http().get(params, new CommonCallback<String>() {
			@Override
			public void onCancelled(CancelledException arg0) {
			}
			@Override
			public void onError(Throwable arg0, boolean arg1) {
			}
			@Override
			public void onFinished() {
			}
			@Override
			public void onSuccess(String arg0) {
				try {
					JSONObject object=new JSONObject(arg0);
					String ref=object.getString("ref");
					if("0".equals(ref)){
						String result=object.getString("result");
						if(result.length()!=2){
							//开始进行解析
							List<DownInfoVehicleBean> list=new ArrayList<DownInfoVehicleBean>();
							list=com.alibaba.fastjson.JSONObject.parseArray(result, DownInfoVehicleBean.class);
							//下一步根据list来存数据库
							for(int c=0;c<list.size();c++){
								CarInfoTable table=new CarInfoTable();
								table.setCodeId(list.get(c).getId());
								table.setCar_no(list.get(c).getCar_no());
								table.setCar_type(list.get(c).getCar_type());
								table.setPerson_name(list.get(c).getPerson_name());
								table.setPerson_tel(list.get(c).getPerson_tel());
								table.setPerson_address(list.get(c).getPerson_address());
								table.setStart_date(DownUtils.getstringtoday(list.get(c).getStart_date()));
								table.setStop_date(DownUtils.getstringtoday(list.get(c).getStop_date()));
								table.setCreated_at(DownUtils.getstringtodate(list.get(c).getCreated_at()));
								table.setUpdated_at(list.get(c).getUpdated_at());
								table.setStatus(list.get(c).getStatus());
								try {
									db.save(table);
								} catch (DbException e) {
									e.printStackTrace();
								}
							}
							showlog("下传固定车信息表保存成功");
						}else {
							showlog("下传固定车信息表无更新");	
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}
	/**
	 * 下传车位和车辆绑定表
	 * @param time   上次记录时间
	 * @param controller_sn  设备号
	 * @param baseurl  base路径
	 */
	private void get_down_record_stall_vehicle(String baseurl,String time,String controller_sn){
		RequestParams params=new RequestParams(baseurl+"/down_record_stall_vehicle");
		params.addBodyParameter("controller_sn",controller_sn);
		params.addBodyParameter("max_updated_at",time);
		x.http().get(params, new CommonCallback<String>() {
			@Override
			public void onCancelled(CancelledException arg0) {
			}
			@Override
			public void onError(Throwable arg0, boolean arg1) {
			}
			@Override
			public void onFinished() {
			}
			@Override
			public void onSuccess(String arg0) {
				try {
					JSONObject object=new JSONObject(arg0);
					String ref=object.getString("ref");
					if("0".equals(ref)){
						String result=object.getString("result");
						if(result.length()!=2){
							//开始进行解析
							List<DownRecordStallVehicleBean> list=new ArrayList<DownRecordStallVehicleBean>();
							list=com.alibaba.fastjson.JSONObject.parseArray(result, DownRecordStallVehicleBean.class);
							//下一步根据list来存数据库
							for(int c=0;c<list.size();c++){
								CarWeiBindTable table=new CarWeiBindTable();
								table.setCode(list.get(c).getCode());
								table.setCar_no(list.get(c).getCar_no());
								table.setStall_code(list.get(c).getStall_code());
								table.setBegin_date(DownUtils.getstringtoday(list.get(c).getBegin_date()));
								table.setEnd_date(DownUtils.getstringtoday(list.get(c).getEnd_date()));
								table.setCreated_at(DownUtils.getstringtodate(list.get(c).getCreated_at()));
								table.setUpdated_at(DownUtils.getstringtodate(list.get(c).getUpdated_at()));
								table.setStatus(list.get(c).getStatus());
								try {
									db.save(table);
								} catch (DbException e) {
									e.printStackTrace();
								}
							}
							showlog("下传车位和车辆绑定表保存成功");
						}else {
							showlog("下传车位和车辆绑定表无更新");
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		});
	}
	/**
	 * 打印log
	 * @param msg
	 */
	public void showlog(String msg){
		if(log){
			Log.i("chenghao", msg);
		}
	}
}