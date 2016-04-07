package ru.rficb.albaexample;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.List;

import static android.text.Html.escapeHtml;

public class ResultActivity extends ActionBarActivity implements AlbaResultReceiver.Receiver  {

    AlbaResultReceiver resultReceiver;
    private Runnable runnable;
    private Handler handler = new Handler();

    @Override
    protected void onResume() {
        super.onResume();

        if (resultReceiver == null) {
            resultReceiver = new AlbaResultReceiver(new Handler());
            resultReceiver.setReceiver(this);
            Bundle extras = getIntent().getExtras();

            AlbaIntentService.startPayment(
                    this,
                    resultReceiver,
                    extras.getString("PAYMENT_TYPE"),
                    extras.getString("NAME"),
                    extras.getString("COST"),
                    extras.getString("PHONE"),
                    extras.getString("TOKEN")
            );

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        Bundle extras = getIntent().getExtras();

        TextView nameField = (TextView)findViewById(R.id.name);
        TextView costField = (TextView)findViewById(R.id.cost);
        TextView paymentTypeField = (TextView)findViewById(R.id.payment_type);

        nameField.setText(extras.getString("NAME"));
        costField.setText(extras.getString("COST"));
        paymentTypeField.setText(extras.getString("PAYMENT_TYPE"));

    }

    private void onStatusInitFinished(Bundle data) {
        final TextView statusField = (TextView)findViewById(R.id.status);
        final int transactionId;
        final String sessionKey;
        final Context context = this;
        Resources resources = getResources();

        transactionId = data.getInt(AlbaIntentService.DATA_TRANSACTION_ID);
        sessionKey = data.getString(AlbaIntentService.DATA_SESSION_KEY);

        String acquireUrl;
        if(resources.getString(R.string.payment_type_card).equals("spg_test")) {
            acquireUrl = resources.getString(R.string.spg_test_acquire_url);
        } else {
            acquireUrl = resources.getString(R.string.spg_acquire_url);
        }
        String termUrl = String.format(acquireUrl,
                resources.getInteger(R.integer.alba_service_id),
                transactionId);

        statusField.setText(
                String.format(getResources().getString(R.string.status_created), transactionId)
        );
        if (data.getString("3DS_ACSUrl") != null) {
            Log.v("ResultActivity", "Waiting for 3ds");
            post3ds(data.getString("3DS_ACSUrl"),
                    data.getString("3DS_MD"),
                    data.getString("3DS_PaReq"),
                    termUrl);
        }

        runnable = new Runnable() {
            public void run() {
                statusField.setText(
                        String.format(getResources().getString(R.string.status_waiting), transactionId)
                );
                if (runnable == this) {
                        AlbaIntentService.startCheckStatus(
                                context, resultReceiver, transactionId, sessionKey
                        );
                }
            }
        };

        handler.postDelayed(runnable, getResources().getInteger(R.integer.check_timeout));
    }

    public void onStatusCheckFinished(Bundle data) {
        final TextView statusField = (TextView)findViewById(R.id.status);
        final int transactionId = data.getInt(AlbaIntentService.DATA_TRANSACTION_ID);
        Resources resources = getResources();
        String transactionStatus = data.getString(AlbaIntentService.DATA_STATUS);

        Log.v("ResultActivity", String.format("Transaction %d status: %s", transactionId, transactionStatus));

        if (transactionStatus.equals("error")) {
            statusField.setText(
                    String.format(resources.getString(R.string.status_cant_pay), transactionId)
            );
        } else if (transactionStatus.equals("payed") || transactionStatus.equals("success")) {
            statusField.setText(
                    String.format(resources.getString(R.string.status_paid), transactionId)
            );
        } else {
            handler.postDelayed(runnable, resources.getInteger(R.integer.check_timeout));
        }
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle data) {
        final TextView statusField = (TextView)findViewById(R.id.status);
        Log.d("ResultActivity", "Result code: " + String.valueOf(resultCode));

        switch (resultCode) {
            case AlbaIntentService.STATUS_INIT_ERROR:
                statusField.setText(data.getString("ERROR_MESSAGE"));
                break;
            case AlbaIntentService.STATUS_INIT_FINISHED:
                onStatusInitFinished(data);
                break;
            case AlbaIntentService.STATUS_CHECK_FINISHED:
                onStatusCheckFinished(data);
                break;
            default:
                Log.e("ResultActivity", "Unknown result code: " + String.valueOf(resultCode));
                break;
        }
    }

    public void nextPayment(View view) {
        resultReceiver.setReceiver(null);
        resultReceiver = null;
        finish();
    }

    private ComponentName getBrowserComponentName() {
        String url = "http://www.example.com";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(Uri.parse(url));

        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(browserIntent, 0);
        browserIntent.setAction(Intent.ACTION_VIEW);
        for (ResolveInfo resolveInfo : list) {
            Log.v("ResultActivity", "Found browser -  " + resolveInfo.activityInfo.packageName + ":" + resolveInfo.activityInfo.name);
            return new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        }
        return null;
    }

    private void post3ds(String acsUrl, String md, String paReq, String tempUrl) {
        ComponentName browserComponentName = getBrowserComponentName();
        Intent browserIntent = new Intent();
        // browserIntent.setComponent(new ComponentName("com.android.browser", "com.android.browser.BrowserActivity"));
        browserIntent.setComponent(browserComponentName);
        browserIntent.setAction(Intent.ACTION_VIEW);

        String html = readTrimRawTextFile(this, R.raw.card_3ds_from);
        html = html.replace("${ACSUrl}", escapeHtml(acsUrl));
        html = html.replace("${PaReq}", escapeHtml(paReq));
        html = html.replace("${MD}", escapeHtml(md));
        html = html.replace("${TermUrl}", escapeHtml(tempUrl));

        String dataUri = "data:text/html," + URLEncoder.encode(html).replaceAll("\\+","%20");
        browserIntent.setData(Uri.parse(dataUri));
        startActivity(browserIntent);
    }

    private static String readTrimRawTextFile(Context ctx, int resId) {
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader streamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(streamReader);
        String line;
        StringBuilder text = new StringBuilder();
        try {
            while ((line = bufferedReader.readLine()) != null) {
                text.append(line.trim());
            }
        }
        catch (IOException e) {
            return null;
        }
        return text.toString();
    }







}
