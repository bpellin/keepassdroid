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

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

import com.keepassdroid.utils.EmptyUtils;
import com.keepassdroid.utils.UriUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecentFileViewModel extends AndroidViewModel
{
    private final MediatorLiveData<List<String>> files =
            new MediatorLiveData<List<String>>(Collections.emptyList());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
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

        updateFiles();
    }

    public LiveData<List<String>> getRecentFiles() {
        return files;
    }

    private void updateFiles() {
        // Capture the input before resolving names in the background.
        final List<String> databases = new ArrayList<>(fileHistory.getDbList().getValue());
        executor.execute(new Runnable() {
            @Override
            public void run() {
                List<String> displayNames = new ArrayList<>();

                for (String fileName : databases) {
                    String name = UriUtil.getFileName(Uri.parse(fileName), getApplication().getApplicationContext());
                    if (EmptyUtils.isNullOrEmpty(name)) {
                        name = fileName;
                    } else {
                        name = name + " - " + fileName;
                    }

                    displayNames.add(name);
                }

                // Published lists must never be mutated by a subsequent refresh.
                files.postValue(Collections.unmodifiableList(displayNames));
            }
        });
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
        super.onCleared();
    }
}
