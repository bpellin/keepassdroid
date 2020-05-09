/*
 * Copyright 2009-2013 Brian Pellin.
 *     
 * This file is part of KeePassDroid.
 *
 *  KeePassDroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePassDroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePassDroid.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Handler;

import com.android.keepass.R;
import com.keepassdroid.database.edit.OnFinish;
import com.keepassdroid.database.edit.RunnableOnFinish;

/** Designed to Pop up a progress dialog, run a thread in the background, 
 *  run cleanup in the current thread, close the dialog.  Without blocking 
 *  the current thread.
 *  
 * @author bpellin
 *
 */
public class ProgressTask implements Runnable {
	private Activity mAct;
	private Handler mHandler;
	private RunnableOnFinish mTask;
	private ProgressDialog mPd;
	
	public ProgressTask(Activity act, RunnableOnFinish task, int messageId) {
		mAct = act;
		mTask = task;
		mHandler = new Handler();
		
		// Show process dialog
		mPd = new ProgressDialog(mAct);
		mPd.setCanceledOnTouchOutside(false);
		mPd.setTitle(act.getText(R.string.progress_title));
		mPd.setMessage(act.getText(messageId));

		// Set code to run when this is finished
		mTask.setStatus(new UpdateStatus(act, mHandler, mPd));
		mTask.mFinish = new AfterTask(task.mFinish, mHandler);
		
	}
	
	public void run() {
		// Show process dialog
		mPd.show();

		// Start Thread to Run task
		Thread t = new Thread(mTask);
		t.start();
	}
	
	private class AfterTask extends OnFinish {
		
		public AfterTask(OnFinish finish, Handler handler) {
			super(finish, handler);
		}

		@Override
		public void run() {
			super.run();
			// Remove the progress dialog
			mHandler.post(new CloseProcessDialog());
		}
		
	}
	
	private class CloseProcessDialog implements Runnable {

		public void run() {
			Activity act = mPd.getOwnerActivity();
			if (act != null && act.isFinishing()) {
				return;
			}

			boolean isDestroyed = false;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			    isDestroyed = mAct.isDestroyed();
            }

			if ( (mPd != null) && mPd.isShowing() && !isDestroyed) {
				mPd.dismiss();
			}
		}
		
	}
	
}
