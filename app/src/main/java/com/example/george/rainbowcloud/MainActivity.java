package com.example.george.rainbowcloud;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.example.george.rainbowcloud.db.TaskContract;
import com.example.george.rainbowcloud.db.TaskDbHelper;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivityLOOOOG"; // for logging

    private final CompositeDisposable disposables = new CompositeDisposable();

    private TaskDbHelper mHelper;

    private ListView mTaskListView;

    private ArrayAdapter<String> mAdapter;

    @Override protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mHelper = new TaskDbHelper(this);

        mTaskListView = findViewById(R.id.list_todo);

        this.updateUI();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu); // Render the menu in the main activity
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_task:


                final EditText taskEditText = new EditText(this);
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Add a new task")
                        .setMessage("What do you want to do next?")
                        .setView(taskEditText)
                        .setPositiveButton("Add", (dialog1, which) -> scheduleTaskCreation(taskEditText))
                        .setNegativeButton("Cancel", null)
                        .create();
                dialog.show();


                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void scheduleTaskCreation(EditText currentTask){
        disposables.add(createTask(currentTask)
                // Run on a background thread
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<String>(){

                    @Override public void onComplete() {
                        Log.d(TAG, "onComplete()");
                    }

                    @Override public void onError(Throwable e) {
                        Log.e(TAG, "onError()", e);
                    }

                    @Override public void onNext(String string) {

                        updateUI();

                        Log.d(TAG, "onNext(" + string + ")");
                    }

                }));
    }

    private Observable<String> createTask(EditText currentTask){
        return Observable.defer(() -> {

            String task = String.valueOf(currentTask.getText());

            SQLiteDatabase db = mHelper.getWritableDatabase();

            ContentValues values = new ContentValues();

            values.put(TaskContract.TaskEntry.COL_TASK_TITLE, task);

            db.insertWithOnConflict(
                    TaskContract.TaskEntry.TABLE,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
            );

            db.close();

            return Observable.just(TaskContract.TaskEntry.COL_TASK_TITLE );
        });
    }

    private void updateUI() {

        ArrayList<String> taskList = new ArrayList<>();

        SQLiteDatabase db = mHelper.getReadableDatabase();

        Cursor cursor = db.query(
                TaskContract.TaskEntry.TABLE,
                new String[]{TaskContract.TaskEntry._ID, TaskContract.TaskEntry.COL_TASK_TITLE},
                null, null, null, null, null
        );
        while (cursor.moveToNext()) { // Used to fill the taskList from the database.
            int idx = cursor.getColumnIndex(TaskContract.TaskEntry.COL_TASK_TITLE);
            taskList.add(cursor.getString(idx));
        }

        if (mAdapter == null) {

            mAdapter = new ArrayAdapter<>(this,
                    R.layout.item_todo, // what view to use for the items
                    R.id.task_title, // where to put the String of data
                    taskList); // where to get all the data

            mTaskListView.setAdapter(mAdapter); // set it as the adapter of the ListView


        } else {

            mAdapter.clear();
            mAdapter.addAll(taskList);
            mAdapter.notifyDataSetChanged();

        }

        cursor.close();
        db.close();
    }

    public void deleteTask(View view) {
        View parent = (View) view.getParent();

        TextView taskTextView = parent.findViewById(R.id.task_title);

        String task = String.valueOf(taskTextView.getText());

        SQLiteDatabase db = mHelper.getWritableDatabase();

        db.delete(TaskContract.TaskEntry.TABLE,
                TaskContract.TaskEntry.COL_TASK_TITLE + " = ?",
                new String[]{task});

        db.close();

        updateUI();
    }


}