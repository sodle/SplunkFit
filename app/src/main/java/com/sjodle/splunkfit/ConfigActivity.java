package com.sjodle.splunkfit;

import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.auth.api.signin.GoogleSignIn;

public class ConfigActivity extends AppCompatActivity {
    final GoogleFitClient fitClient = new GoogleFitClient(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        final Context context = this;

        final EditText hecUrlField = findViewById(R.id.hec_url);
        final EditText hecTokenField = findViewById(R.id.hec_token);
        Button applyConfigButton = findViewById(R.id.apply_button);

        SharedPreferences appData = context.getSharedPreferences(Constants.PREFS_FILE_KEY, Context.MODE_PRIVATE);
        hecUrlField.setText(appData.getString(Constants.PREFS_HEC_URL_KEY, ""));
        hecTokenField.setText(appData.getString(Constants.PREFS_HEC_TOKEN_KEY, ""));

        applyConfigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hecUrl = hecUrlField.getText().toString();
                String hecToken = hecTokenField.getText().toString();
                new HecClient(context).updateConnectionInfo(hecUrl, hecToken);
                scheduleIngestJob();
            }
        });

        if (fitClient.checkPermissions()) {
            scheduleIngestJob();
        } else {
            GoogleSignIn.requestPermissions(
                    this,
                    Constants.GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    Constants.getFitnessOptions()
            );
        }
    }

    private void scheduleIngestJob() {
        final JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo jobInfo = jobScheduler.getPendingJob(Constants.INGEST_JOB_ID);
        if (jobInfo == null) {
            final ComponentName name = new ComponentName(this, DataIngestionService.class);
            jobInfo = new JobInfo.Builder(Constants.INGEST_JOB_ID, name)
                    .setPeriodic(Constants.INGEST_JOB_INTERVAL)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setPersisted(true)
                    .build();
            final int result = jobScheduler.schedule(jobInfo);

            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d("JobScheduleSuccess", "Scheduled.");
            } else {
                Log.e("JobScheduleError", "Couldn't schedule for some reason.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Constants.GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                scheduleIngestJob();
            }
        }
    }
}
