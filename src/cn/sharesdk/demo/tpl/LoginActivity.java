package cn.sharesdk.demo.tpl;

import java.util.HashMap;

import org.apache.http.client.HttpClient;

import cn.sharesdk.facebook.Facebook;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.framework.utils.UIHandler;
import cn.sharesdk.sina.weibo.SinaWeibo;
import cn.sharesdk.tencent.qzone.QZone;
import cn.sharesdk.twitter.Twitter;
import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentCallbacks;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler.Callback;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Toast;

public class LoginActivity extends Activity implements Callback, 
		OnClickListener, PlatformActionListener {
	String registerUrl = "http://192.168.0.104:8080/ParkServer/RegisterAction";
	String loginUrl = "http://192.168.0.104:8080/ParkServer/LoginAction";
	private static final int MSG_USERID_FOUND = 1;
	private static final int MSG_LOGIN = 2;
	private static final int MSG_AUTH_CANCEL = 3;
	private static final int MSG_AUTH_ERROR= 4;
	private static final int MSG_AUTH_COMPLETE = 5;
	private static final int MSG_REGISTER = 6;
	
	private Platform plat;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ShareSDK.initSDK(this);
		
		setContentView(R.layout.third_party_login_page);
		findViewById(R.id.tvWeibo).setOnClickListener(this);
		findViewById(R.id.tvQq).setOnClickListener(this);
		findViewById(R.id.tvOther).setOnClickListener(this);
	}
	
	protected void onDestroy() {
		ShareSDK.stopSDK(this);
		super.onDestroy();
	}
	
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.tvWeibo: {
				authorize(new SinaWeibo(this));
			}
			break;
			case R.id.tvQq: {
				authorize(new QZone(this));
			}
			break;
			case R.id.tvOther: {
				authorize(null);
			}
			break;
			case R.id.tvFacebook: {
				Dialog dlg = (Dialog) v.getTag();
				dlg.dismiss();
				authorize(new Facebook(this));
			}
			break;
			case R.id.tvTwitter: {
				Dialog dlg = (Dialog) v.getTag();
				dlg.dismiss();
				authorize(new Twitter(this));
			}
			break;
		}
	}
	
	private void authorize(Platform plat) {
		if (plat == null) {
			popupOthers();
			return;
		}
		
		this.plat = plat;
		
		if(plat.isValid()) {
			String userId = plat.getDb().getUserId();
			String userName = plat.getDb().getUserName();
			if (userId != null) {
				UIHandler.sendEmptyMessage(MSG_USERID_FOUND, this);
				login(userName, userId, null);
				return;
			}
		}
		plat.setPlatformActionListener(this);
		plat.SSOSetting(true);
		plat.showUser(null);
	}
	
	private void popupOthers() {
		Dialog dlg = new Dialog(this);
		View dlgView = View.inflate(this, R.layout.other_plat_dialog, null);
		View tvFacebook = dlgView.findViewById(R.id.tvFacebook);
		tvFacebook.setTag(dlg);
		tvFacebook.setOnClickListener(this);
		View tvTwitter = dlgView.findViewById(R.id.tvTwitter);
		tvTwitter.setTag(dlg);
		tvTwitter.setOnClickListener(this);
		
		dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dlg.setContentView(dlgView);
		dlg.show();
	}
	
	public void onComplete(Platform platform, int action,
			HashMap<String, Object> res) {
		if (action == Platform.ACTION_USER_INFOR) {
			UIHandler.sendEmptyMessage(MSG_AUTH_COMPLETE, this);
			register(platform.getDb().getUserName(), platform.getDb().getUserId());
		}
		System.out.println(">>>>res:" + res);
	}
	
	public void onError(Platform platform, int action, Throwable t) {
		if (action == Platform.ACTION_USER_INFOR) {
			UIHandler.sendEmptyMessage(MSG_AUTH_ERROR, this);
		}
		t.printStackTrace();
	}
	
	public void onCancel(Platform platform, int action) {
		if (action == Platform.ACTION_USER_INFOR) {
			UIHandler.sendEmptyMessage(MSG_AUTH_CANCEL, this);
		}
	}
	
	private void login(String plat, String userId, HashMap<String, Object> userInfo) {
		Message msg = new Message();
		msg.what = MSG_LOGIN;
		msg.obj = new String[]{plat,userId};
		UIHandler.sendMessage(msg, this);
	}
	
	private void register(String userName,String password) {
		Message msg = new Message();
		msg.what = MSG_REGISTER;
		msg.obj = new String[]{userName,password};
		UIHandler.sendMessage(msg, this);
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		switch(msg.what) {
			case MSG_USERID_FOUND: {
				Toast.makeText(this, R.string.userid_found, Toast.LENGTH_SHORT).show();
			}
			break;
			case MSG_LOGIN: {
				String [] str = (String[]) msg.obj;
				new MyAsynTask().execute(new String[]{loginUrl,str[0],str[1]});
			}
			break;
			case MSG_REGISTER:
				String [] str = (String[]) msg.obj;
				new MyAsynTask().execute(new String[]{registerUrl,str[0],str[1]});
				break;
			case MSG_AUTH_CANCEL: {
				Toast.makeText(this, R.string.auth_cancel, Toast.LENGTH_SHORT).show();
			}
			break;
			case MSG_AUTH_ERROR: {
				Toast.makeText(this, R.string.auth_error, Toast.LENGTH_SHORT).show();
			}
			break;
			case MSG_AUTH_COMPLETE: {
				Toast.makeText(this, R.string.auth_complete, Toast.LENGTH_SHORT).show();
			}
			break;
		}
		return false;
	}
	
	class MyAsynTask extends AsyncTask<String, Void, String>{

		@Override
		protected String doInBackground(String... params) {
			String url = params[0];
			String userName = params[1];
			String password = params[2];
			return HttpUtil.submit(userName, password, url);
		}
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if(result != null && result.equals("success")){
				result = "成功";
			}else{
				result = "失败";
				plat.removeAccount();
			}
			Toast.makeText(LoginActivity.this,result,Toast.LENGTH_LONG).show();
		}
	}
	
}
