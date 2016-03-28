package ru.rficb.albaexample;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.math.BigDecimal;

import ru.rficb.alba.AlbaService;
import ru.rficb.alba.AlbaFatalError;
import ru.rficb.alba.AlbaTemporaryError;
import ru.rficb.alba.InitPaymentRequest;
import ru.rficb.alba.InitPaymentResponse;
import ru.rficb.alba.InitTestType;
import ru.rficb.alba.TransactionDetails;


public class AlbaIntentService extends IntentService {
    ResultReceiver resultReceiver;

    public static final int STATUS_INIT_FINISHED = 1000;
    public static final int STATUS_CHECK_FINISHED = 1001;
    public static final int STATUS_INIT_ERROR = 1002;
    public static final int STATUS_CHECK_ERROR = 1003;

    public static final String DATA_TRANSACTION_ID = "ru.rficb.albaexample.data.transaction_id";
    public static final String DATA_SESSION_KEY = "ru.rficb.albaexample.data.session_Key";
    public static final String DATA_STATUS = "ru.rficb.albaexample.data.status";

    private static final String ACTION_START = "ru.rficb.albaexample.action.start_payment";
    private static final String ACTION_CHECK = "ru.rficb.albaexample.action.check_status";

    private static final String EXTRA_RECEIVER = "ru.rficb.albaexample.extra.receiver";
    private static final String EXTRA_TRANSACTION_ID = "ru.rficb.albaexample.extra.transaction_id";
    private static final String EXTRA_NAME = "ru.rficb.albaexample.extra.name";
    private static final String EXTRA_AMOUNT = "ru.rficb.albaexample.extra.amount";
    private static final String EXTRA_PHONE = "ru.rficb.albaexample.extra.phone";
    private static final String EXTRA_SESSION_KEY = "ru.rficb.albaexample.extra.session_Key";

    private AlbaService alba = null;

    public static void startPayment(Context context, AlbaResultReceiver resultReceiver , String name, String amount, String phone) {
        Intent intent = new Intent(context, AlbaIntentService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_NAME, name);
        intent.putExtra(EXTRA_AMOUNT, amount);
        intent.putExtra(EXTRA_PHONE, phone);
        intent.putExtra(EXTRA_RECEIVER, resultReceiver);
        context.startService(intent);
    }

    public static void startCheckStatus(Context context, AlbaResultReceiver resultReceiver, int TransactionId, String sessionKey) {
        Intent intent = new Intent(context, AlbaIntentService.class);
        intent.setAction(ACTION_CHECK);
        intent.putExtra(EXTRA_TRANSACTION_ID, TransactionId);
        intent.putExtra(EXTRA_SESSION_KEY, sessionKey);
        intent.putExtra(EXTRA_RECEIVER, resultReceiver);
        context.startService(intent);
    }

    public AlbaIntentService() {
        super("AlbaIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        resultReceiver = intent.getParcelableExtra(EXTRA_RECEIVER);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (alba == null) {
                alba = new AlbaService(getResources().getString(R.string.alba_key));
            }

            if (ACTION_START.equals(action)) {
                final String name = intent.getStringExtra(EXTRA_NAME);
                final String amount = intent.getStringExtra(EXTRA_AMOUNT);
                final String phone = intent.getStringExtra(EXTRA_PHONE);
                handleStartPayment(name, new BigDecimal(amount), phone);
            } else if (ACTION_CHECK.equals(action)) {
                final int transactionId = intent.getIntExtra(EXTRA_TRANSACTION_ID, 0);
                final String sessionKey = intent.getStringExtra(EXTRA_SESSION_KEY);
                handleCheckStatus(transactionId, sessionKey);
            }
        }
    }

    private void handleStartPayment(String name, BigDecimal amount, String phone) {
        Log.d("AlbaIntentService", "Start payment: " + name + ", " + amount + ", " + phone);
        try {
            String paymentType = getResources().getString(R.string.alba_payment_type);
            InitPaymentResponse response = alba.initPayment(
                    InitPaymentRequest.builder()
                            .setPaymentType(paymentType)
                            .setCost(amount)
                            .setName(name)
                            .setPhone(phone)
                            // .setTest(InitTestType.OK)
                            .build()
            );
            Log.d("AlbaIntentService", "Transaction created: " + response.getTransactionId());

            Bundle resultData = new Bundle();
            resultData.putInt(DATA_TRANSACTION_ID, response.getTransactionId());
            resultData.putString(DATA_SESSION_KEY, response.getSessionKey());
            resultReceiver.send(STATUS_INIT_FINISHED, resultData);

        } catch (AlbaTemporaryError | AlbaFatalError albaError) {
            albaError.printStackTrace();
            resultReceiver.send(STATUS_INIT_ERROR, new Bundle());
        }
    }

    private void handleCheckStatus(int transactionId, String sessionKey) {
        Bundle resultData = new Bundle();

        try {
            TransactionDetails details = alba.transactionDetails(sessionKey);
            resultData.putInt(DATA_TRANSACTION_ID, transactionId);
            resultData.putString(DATA_STATUS, details.getStatus().toString());
            resultReceiver.send(STATUS_CHECK_FINISHED, resultData);

        } catch (AlbaTemporaryError albaTemporaryError) {
            // игнорирование временной ошибки
            Log.d("AlbaIntentService", "Check status error: " + albaTemporaryError.getMessage());
        } catch (AlbaFatalError albaFatalError) {
            albaFatalError.printStackTrace();
            resultData.putInt(DATA_TRANSACTION_ID, transactionId);
            resultReceiver.send(STATUS_CHECK_ERROR, resultData);
        }
    }
}
