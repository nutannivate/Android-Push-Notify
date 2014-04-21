package com.javapapers.android;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class RegisterActivity extends Activity {

	Button btnGCMRegister;
	Button btnAppShare;
	GoogleCloudMessaging gcm;
	Context context;
	String regId;
	public static final String REG_ID = "regId";
	private static final String APP_VERSION = "appVersion";
	static final String TAG = "Register Activity";
	static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
	static final int REQUEST_CODE_PICK_ACCOUNT = 1002;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);

		if (checkGooglePlayServices() && checkUserAccount()) {
			// Then we're good to go!
			context = getApplicationContext();

			btnGCMRegister = (Button) findViewById(R.id.btnGCMRegister);
			btnGCMRegister.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (TextUtils.isEmpty(regId)) {
						regId = registerGCM();
						Log.d("RegisterActivity", "GCM RegId: " + regId);
					} else {
						Toast.makeText(getApplicationContext(),
								"Already Registered with GCM Server!",
								Toast.LENGTH_LONG).show();
					}
				}
			});

			btnAppShare = (Button) findViewById(R.id.btnAppShare);
			btnAppShare.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					if (TextUtils.isEmpty(regId)) {
						Toast.makeText(getApplicationContext(), "RegId is empty!",
								Toast.LENGTH_LONG).show();
					} else {
						Intent i = new Intent(getApplicationContext(),
								MainActivity.class);
						i.putExtra("regId", regId);
						Log.d("RegisterActivity",
								"onClick of Share: Before starting main activity.");
						startActivity(i);
						finish();
						Log.d("RegisterActivity", "onClick of Share: After finish.");
					}
				}
			});
		}
	}

	public String registerGCM() {

		gcm = GoogleCloudMessaging.getInstance(this);
		regId = getRegistrationId(context);

		if (TextUtils.isEmpty(regId)) {

			registerInBackground();

			Log.d("RegisterActivity",
					"registerGCM - successfully registered with GCM server - regId: "
							+ regId);
		} else {
			Toast.makeText(getApplicationContext(),
					"RegId already available. RegId: " + regId,
					Toast.LENGTH_LONG).show();
		}
		return regId;
	}

	private String getRegistrationId(Context context) {
		final SharedPreferences prefs = getSharedPreferences(
				MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		String registrationId = prefs.getString(REG_ID, "");
		if (registrationId.isEmpty()) {
			Log.i(TAG, "Registration not found.");
			return "";
		}
		int registeredVersion = prefs.getInt(APP_VERSION, Integer.MIN_VALUE);
		int currentVersion = getAppVersion(context);
		if (registeredVersion != currentVersion) {
			Log.i(TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}

	private static int getAppVersion(Context context) {
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		} catch (NameNotFoundException e) {
			Log.d("RegisterActivity",
					"I never expected this! Going down, going down!" + e);
			throw new RuntimeException(e);
		}
	}

	private void registerInBackground() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				try {
					if (gcm == null) {
						gcm = GoogleCloudMessaging.getInstance(context);
					}
					regId = gcm.register(Config.GOOGLE_PROJECT_ID);
					Log.d("RegisterActivity", "registerInBackground - regId: "
							+ regId);
					msg = "Device registered, registration ID=" + regId;

					storeRegistrationId(context, regId);
				} catch (IOException ex) {
					msg = "Error :" + ex.getMessage();
					Log.d("RegisterActivity", "Error: " + msg);
				}
				Log.d("RegisterActivity", "AsyncTask completed: " + msg);
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
				Toast.makeText(getApplicationContext(),
						"Registered with GCM Server." + msg, Toast.LENGTH_LONG)
						.show();
			}
		}.execute(null, null, null);
	}

	private void storeRegistrationId(Context context, String regId) {
		final SharedPreferences prefs = getSharedPreferences(
				MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
		int appVersion = getAppVersion(context);
		Log.i(TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(REG_ID, regId);
		editor.putInt(APP_VERSION, appVersion);
		editor.commit();
	}

	private boolean checkGooglePlayServices() {
		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (status != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
				showErrorDialog(status);
			} else {
				Toast.makeText(this, "This device is not supported.", Toast.LENGTH_LONG).show();
				finish();
			}
			return false;
		}
		return true;
	}
	
	void showErrorDialog(int code) {
		GooglePlayServicesUtil.getErrorDialog(code, this, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
	}
	
	private boolean checkUserAccount() {
		String accountName = AccountUtils.getAccountName(this);
		if (accountName == null) {
			// Then the user was not found in the SharedPreferences. Either the
			// application deliberately removed the account, or the application's
			// data has been forcefully erased.
			showAccountPicker();
			return false;
		}
		Account account = AccountUtils.getGoogleAccountByName(this, accountName);
		if (account == null) {
			// Then the account has since been removed.
			AccountUtils.removeAccount(this);
			showAccountPicker();
			return false;
		}
		return true;
	}
	
	private void showAccountPicker() {
		Intent pickAccountIntent = AccountPicker.newChooseAccountIntent(null, null,
				new String[] { GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE }, true, null, null, null, null);
		startActivityForResult(pickAccountIntent, REQUEST_CODE_PICK_ACCOUNT);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_CODE_RECOVER_PLAY_SERVICES:
			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "Google Play Services must be installed and up-to-date.",
						Toast.LENGTH_SHORT).show();
				finish();
			}
			return;
		case REQUEST_CODE_PICK_ACCOUNT:
			if (resultCode == RESULT_OK) {
				String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				AccountUtils.setAccountName(this, accountName);
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "This application requires a Google account.",
						Toast.LENGTH_SHORT).show();
				finish();
			}
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
