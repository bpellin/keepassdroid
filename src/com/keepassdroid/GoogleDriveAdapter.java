package com.keepassdroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;


public class GoogleDriveAdapter {
	private static final String TAG = "PasswordActivity";
	private static final int REQUEST_AUTHORIZATION = 2;
	private static final int REQUEST_ACCOUNT_PICKER = 3;
	private static final int CHOOSE_ACCOUNT_AND_LOAD_FILE = 4;
	private static final String[] ACCOUNT_TYPE = new String[] {GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE};
	private String driveFileId;
	private com.google.api.services.drive.model.File driveFile;
	private Drive driveService;
	private String localDriveFileName;
	private GoogleAccountCredential credential;
	
	
	public String getDriveFileId() {
		return driveFileId;
	}

	public void setDriveFileId(String driveFileId) {
		this.driveFileId = driveFileId;
	}
	
	public void clearDriveFileInfo() {
		driveFileId = null;
		driveFile = null;
		localDriveFileName = null;
	}
	
	public boolean handleDriveOpenAction(String action, String driveFileId, Activity activity) {
		boolean result = false;
		if (driveFileId == null || driveFileId.length() == 0) {
    		Log.e(TAG, "Failed to retrieve the Drive file ID.");
    		result = false;
	    } else {
	    	this.setDriveFileId(driveFileId);
	    	Intent accountPickerIntent = AccountPicker.newChooseAccountIntent(null, null, ACCOUNT_TYPE, false, null, null, null, null);
	    	activity.startActivityForResult(accountPickerIntent, CHOOSE_ACCOUNT_AND_LOAD_FILE);
	    	Log.i(TAG, "GoogleDriveAdapter handling open drive file after starting choose account activity");
	    	result = true;
	    }
		return result;
	}
	
	public boolean handleActivityResult(int requestCode, int resultCode,
			Intent data, Activity activity) {
		boolean result = false;
		switch (requestCode) {
		case REQUEST_AUTHORIZATION:
			if (resultCode == Activity.RESULT_OK) {
				loadDriveFile(activity);
			} else {
				activity.startActivityForResult(
						credential.newChooseAccountIntent(),
						REQUEST_ACCOUNT_PICKER);
			}
			result = true;
			break;
		case REQUEST_ACCOUNT_PICKER:
			if (resultCode == Activity.RESULT_OK && data != null
					&& data.getExtras() != null) {
				String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
				if (accountName != null) {
					credential.setSelectedAccountName(accountName);
					driveService = buildDriveService(credential);
					loadDriveFile(activity);
				}
			}
			result = true;
			break;
		case CHOOSE_ACCOUNT_AND_LOAD_FILE: // Needed for Google Drive
			String accountName = null;
			if (resultCode == Activity.RESULT_OK) {
				accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
			}
			if(accountName == null || accountName.length() <= 0) {
				activity.setResult(Activity.RESULT_CANCELED);
				activity.finish();
			}
			if (driveService == null) {
				try {

					credential = GoogleAccountCredential.usingOAuth2(activity, DriveScopes.DRIVE_FILE);
					credential.setSelectedAccountName(accountName);
					driveService = buildDriveService(credential);
				} catch (Exception e) {
					Log.e(TAG, "Error getting access to the user's drive service", e);
					if (e instanceof UserRecoverableAuthException) {
						//Try to recover by requesting authorization and then loading the file
						activity.startActivityForResult(
								((UserRecoverableAuthException) e).getIntent(),
								REQUEST_AUTHORIZATION);
					}
				}
			}
			//Load the file from Google Drive
			loadDriveFile(activity);
			result = true;
			break;
		}
		return result;
	}

	
	private Drive buildDriveService(GoogleAccountCredential credential) {
		return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
	}
	
	
	private static InputStream downloadFile(Drive service,
			com.google.api.services.drive.model.File file) {
		if (file.getDownloadUrl() != null && file.getDownloadUrl().length() > 0) {
			try {
				HttpResponse resp = service.getRequestFactory()
						.buildGetRequest(new GenericUrl(file.getDownloadUrl()))
						.execute();
				return resp.getContent();
			} catch (IOException e) {
				Log.e(TAG, "Error occurred downloading file", e);
				return null;
			}
		} else {
			// The file doesn't have any content stored on Drive.
			return null;
		}
	}

	
	private void loadDriveFile(final Activity activity) {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					credential.getToken();

					// First retrieve the file from the API.
					driveFile = driveService.files().get(driveFileId).execute();
					
					//Then download the file
					InputStream inputStream = downloadFile(driveService, driveFile);

					//Store the downloaded file to the external storage directory
					String mediaStorageDir = Environment.getExternalStorageDirectory().getPath();
					localDriveFileName = mediaStorageDir
							+ java.io.File.separator
							+ driveFile.getOriginalFilename();
					
					OutputStream outputStream = null;

					try {

						//Write the inputStream to a FileOutputStream
						outputStream = new FileOutputStream(new java.io.File(localDriveFileName));

						int read = 0;
						byte[] bytes = new byte[1024];

						while ((read = inputStream.read(bytes)) != -1) {
							outputStream.write(bytes, 0, read);
						}

					} catch (IOException e) {
						Log.e(TAG, "Error occurred writing downloaded file to disk", e);
					} finally {
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch (IOException e) {
							}
						}
						if (outputStream != null) {
							try {
								// outputStream.flush();
								outputStream.close();
							} catch (IOException e) {
							}

						}
					}

					//Update the GUI in the activity with the path to the downloaded file
					((PasswordActivity)activity).setFileName(localDriveFileName, true);
					
				} catch (Exception e) {
					Log.e(TAG, "Error occurred trying to load file from google drive", e);
					if (e instanceof UserRecoverableAuthException) {

						activity.startActivityForResult(
								((UserRecoverableAuthException) e).getIntent(),
								REQUEST_AUTHORIZATION);
					}
				}
			}
		});
		t.start();

	}

	public void update(File updatedFile) throws IOException {
		if(driveFile != null && driveFileId != null) {
			FileContent mediaContent = new FileContent(driveFile.getMimeType(), updatedFile);
		    //Update the file in google drive
			@SuppressWarnings("unused")
			com.google.api.services.drive.model.File updatedDriveFile = driveService.files().update(driveFileId, driveFile, mediaContent).execute();
		}
	}
}
