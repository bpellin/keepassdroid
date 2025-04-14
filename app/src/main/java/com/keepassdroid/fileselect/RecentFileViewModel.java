/*
 * Copyright 2025 Brian Pellin.
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
package com.keepassdroid.fileselect;

import android.app.Application;
import android.net.Uri;
import android.os.Looper;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.UriUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class RecentFileViewModel extends AndroidViewModel
{
    private final MediatorLiveData<List<String>> files =
            new MediatorLiveData<List<String>>(new ArrayList<String>());
    private RecentFileHistory fileHistory;

    public RecentFileViewModel(@NotNull Application application) {
        super(application);
    }

    public RecentFileViewModel(@NotNull Application application, @NotNull RecentFileHistory fileHistory) {
        this(application);

        this.fileHistory = fileHistory;

        files.addSource(fileHistory.getDbList(), new Observer<List<String>>() {

            @Override
            public void onChanged(List<String> strings) {
                updateFiles();
            }
        });

        updateFiles();;
    }

    public LiveData<List<String>> getRecentFiles() {
        return files;
    }

    private void updateFiles() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public synchronized void run() {
                //List<String> displayNames = files.getValue();
                List<String> displayNames = new ArrayList<>();
                displayNames.clear();

                List<String> databases = fileHistory.getDbList().getValue();
                for (String fileName : databases) {
                    String name = UriUtil.getFileName(Uri.parse(fileName), getApplication().getApplicationContext());
                    if (EmptyUtils.isNullOrEmpty(name)) {
                        name = fileName;
                    } else {
                        name = name + " - " + fileName;
                    }

                    displayNames.add(name);
                }

                updateFiles(displayNames);
            }
        });
    }

    /*
    public void updateDbList(RecentFileHistory fileHistory) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                List<String> list = files.getValue();
                if (list != null) {
                    list.clear();
                    //list.addAll(fileHistory.getDbList());
                }
                files.postValue(list);
            }
        });
    }
     */

    private void updateFiles(List<String> dbs) {
        List<String> liveData = files.getValue();
        liveData.clear();
        liveData.addAll(dbs);

        // On the main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            files.setValue(liveData);
        } else {
            files.postValue(liveData);
        }
    }
}