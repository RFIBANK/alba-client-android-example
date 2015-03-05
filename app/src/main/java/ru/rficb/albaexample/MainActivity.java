package ru.rficb.albaexample;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;


public class MainActivity extends ActionBarActivity implements AlbaResultReceiver.Receiver {

    final int CHECK_TIMEOUT = 3500;

    AlbaResultReceiver resultReceiver;
    private Runnable runnable;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resultReceiver = new AlbaResultReceiver(null);

        final TelephonyManager tManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final String phoneNumber = tManager.getLine1Number();

        TextView phoneField = (TextView)findViewById(R.id.phone);
        phoneField.setText(phoneNumber);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void onClick(View arg0) {
        TextView statusField = (TextView)findViewById(R.id.status);
        TextView nameField = (TextView)findViewById(R.id.name);
        TextView costField = (TextView)findViewById(R.id.cost);
        TextView phoneField = (TextView)findViewById(R.id.phone);

        statusField.setText(getResources().getString(R.string.status_start));

        AlbaIntentService.startPayment(
            this,
            resultReceiver,
            nameField.getText().toString(),
            costField.getText().toString(),
            phoneField.getText().toString()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        resultReceiver = new AlbaResultReceiver(new Handler());
        resultReceiver.setReceiver(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        resultReceiver.setReceiver(null);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle data) {
        final int transactionId;
        final String sessionKey;
        final Context context = this;
        final TextView statusField = (TextView)findViewById(R.id.status);

        switch (resultCode) {
            case AlbaIntentService.STATUS_INIT_FINISHED:
                transactionId = data.getInt(AlbaIntentService.DATA_TRANSACTION_ID);
                sessionKey = data.getString(AlbaIntentService.DATA_SESSION_KEY);

                statusField.setText(
                    String.format(getResources().getString(R.string.status_created), transactionId)
                );

                runnable = new Runnable() {
                    public void run() {
                    statusField.setText(
                        String.format(getResources().getString(R.string.status_waiting), transactionId)
                    );
                    if (runnable == this) {
                       // Если пользователь повторно нажимает "оплатить",
                       // то проверяем только статус последней транзакции
                       AlbaIntentService.startCheckStatus(
                           context, resultReceiver, transactionId, sessionKey
                       );
                    }
                    }
                };

                handler.postDelayed(runnable, CHECK_TIMEOUT);
                break;

            case AlbaIntentService.STATUS_CHECK_FINISHED:
                transactionId = data.getInt(AlbaIntentService.DATA_TRANSACTION_ID);
                String transactionStatus = data.getString(AlbaIntentService.DATA_STATUS);

                Log.v(
                    "AlbaIntentService",
                    String.format("Transaction %d status: %s", transactionId, transactionStatus)
                );
                if (transactionStatus.equals("error")) {
                    statusField.setText(
                        String.format(getResources().getString(R.string.status_cant_pay), transactionId)
                    );
                } else if (transactionStatus.equals("payed") || transactionStatus.equals("success")) {
                    statusField.setText(
                        String.format(getResources().getString(R.string.status_paid), transactionId)
                    );
                } else {
                    handler.postDelayed(runnable, CHECK_TIMEOUT);
                }
                break;

            case AlbaIntentService.STATUS_INIT_ERROR:
                statusField.setText(getResources().getString(R.string.status_cant_init));
                break;

            case AlbaIntentService.STATUS_CHECK_ERROR:
                transactionId = data.getInt(AlbaIntentService.DATA_TRANSACTION_ID);
                statusField.setText(
                    String.format(getResources().getString(R.string.status_cant_pay), transactionId)
                );
                break;
        }
    }


}
