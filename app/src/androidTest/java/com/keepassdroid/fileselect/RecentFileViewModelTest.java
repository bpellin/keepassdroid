package com.keepassdroid.fileselect;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelStore;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RecentFileViewModelTest {
    @Test
    public void refreshDoesNotMutatePreviouslyPublishedLists() throws Exception {
        Application application = (Application) InstrumentationRegistry.getInstrumentation()
                .getTargetContext().getApplicationContext();
        MutableLiveData<List<String>> databases = new MutableLiveData<>(Collections.emptyList());
        RecentFileHistory history = new RecentFileHistory(application) {
            @Override
            public LiveData<List<String>> getDbList() {
                return databases;
            }
        };
        BlockingQueue<List<String>> updates = new LinkedBlockingQueue<>();
        Observer<List<String>> observer = updates::add;
        ViewModelStore store = new ViewModelStore();
        RecentFileViewModel[] model = new RecentFileViewModel[1];

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            model[0] = new RecentFileViewModel(application, history);
            store.put("recentFiles", model[0]);
            model[0].getRecentFiles().observeForever(observer);
        });
        try {
            setDatabases(databases, Collections.singletonList("file:///first.kdbx"));
            List<String> first = awaitUpdate(updates,
                    Collections.singletonList("first.kdbx - file:///first.kdbx"));

            setDatabases(databases, Collections.singletonList("file:///second.kdbx"));
            List<String> second = awaitUpdate(updates,
                    Collections.singletonList("second.kdbx - file:///second.kdbx"));
            assertEquals(Collections.singletonList("first.kdbx - file:///first.kdbx"), first);

            setDatabases(databases, Collections.emptyList());
            awaitUpdate(updates, Collections.emptyList());
            assertEquals(Collections.singletonList("first.kdbx - file:///first.kdbx"), first);
            assertEquals(Collections.singletonList("second.kdbx - file:///second.kdbx"), second);
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                model[0].getRecentFiles().removeObserver(observer);
                store.clear();
            });
        }
    }

    private static void setDatabases(MutableLiveData<List<String>> databases, List<String> value) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> databases.setValue(value));
    }

    private static List<String> awaitUpdate(BlockingQueue<List<String>> updates,
                                          List<String> expected) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (true) {
            List<String> value = updates.poll(Math.max(0, deadline - System.nanoTime()),
                    TimeUnit.NANOSECONDS);
            assertNotNull("Timed out waiting for recent files: " + expected, value);
            if (expected.equals(value)) {
                return value;
            }
        }
    }
}
