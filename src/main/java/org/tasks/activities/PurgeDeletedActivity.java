package org.tasks.activities;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.todoroo.andlib.data.TodorooCursor;
import com.todoroo.andlib.sql.Criterion;
import com.todoroo.andlib.sql.Query;
import com.todoroo.andlib.utility.DialogUtilities;
import com.todoroo.astrid.dao.MetadataDao;
import com.todoroo.astrid.data.Task;
import com.todoroo.astrid.gcal.GCalHelper;
import com.todoroo.astrid.service.TaskDeleter;
import com.todoroo.astrid.service.TaskService;

import org.tasks.R;
import org.tasks.injection.InjectingActivity;

import javax.inject.Inject;

public class PurgeDeletedActivity extends InjectingActivity {

    @Inject TaskService taskService;
    @Inject TaskDeleter taskDeleter;
    @Inject GCalHelper gcalHelper;
    @Inject MetadataDao metadataDao;

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DialogUtilities.okCancelDialog(
                this,
                getResources().getString(
                        R.string.EPr_manage_purge_deleted_message),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pd = DialogUtilities.runWithProgressDialog(PurgeDeletedActivity.this, new Runnable() {
                            @Override
                            public void run() {
                                TodorooCursor<Task> cursor = taskService.query(Query.select(Task.ID, Task.TITLE, Task.CALENDAR_URI).where(
                                        Criterion.and(Task.DELETION_DATE.gt(0), Task.CALENDAR_URI.isNotNull())));
                                try {
                                    int length = cursor.getCount();
                                    for (int i = 0; i < length; i++) {
                                        cursor.moveToNext();
                                        Task task = new Task(cursor);
                                        gcalHelper.deleteTaskEvent(task);
                                    }
                                } finally {
                                    cursor.close();
                                }
                                int result = taskDeleter.purgeDeletedTasks();
                                metadataDao.removeDanglingMetadata();
                                showResult(R.string.EPr_manage_purge_deleted_status, result);
                            }
                        });
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });

    }

    @Override
    protected void onPause() {
        DialogUtilities.dismissDialog(this, pd);

        super.onPause();
    }

    protected void showResult(int resourceText, int result) {
        DialogUtilities.okDialog(this, getString(resourceText, result), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
    }
}
