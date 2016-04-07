package ru.rficb.albaexample;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.os.Handler;

public class CardActivity extends ActionBarActivity implements AlbaResultReceiver.Receiver {

    AlbaResultReceiver resultReceiver;
    TextView cardNumberField;
    TextView cardHolderField;
    TextView monthField;
    TextView yearField;
    TextView cvcField;

    TextView cardNumberErrorsField;
    TextView cardHolderErrorsField;
    TextView monthErrorsField;
    TextView yearErrorsField;
    TextView cvcErrorsField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);

        resultReceiver = new AlbaResultReceiver(null);

        cardNumberField = (TextView)findViewById(R.id.card_number);
        cardHolderField = (TextView)findViewById(R.id.card_holder);
        monthField = (TextView)findViewById(R.id.month);
        yearField = (TextView)findViewById(R.id.year);
        cvcField = (TextView)findViewById(R.id.cvc);

        cardNumberErrorsField = (TextView)findViewById(R.id.card_number_errors);
        cardHolderErrorsField = (TextView)findViewById(R.id.card_holder_errors);
        monthErrorsField = (TextView)findViewById(R.id.month_errors);
        yearErrorsField = (TextView)findViewById(R.id.year_errors);
        cvcErrorsField = (TextView)findViewById(R.id.cvc_errors);

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

    private void clearErrors() {
        cardNumberErrorsField.setText("");
        cardHolderErrorsField.setText("");
        cvcErrorsField.setText("");
        monthErrorsField.setText("");
        yearErrorsField.setText("");
    }

    public void startPayment(View view) {

        clearErrors();

        AlbaIntentService.createToken(
                this,
                resultReceiver,
                cardNumberField.getText().toString(),
                cardHolderField.getText().toString(),
                monthField.getText().toString(),
                yearField.getText().toString(),
                cvcField.getText().toString()
        );
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle data) {

        switch (resultCode) {
            case AlbaIntentService.STATUS_TOKEN_CREATED:
                Bundle extras = getIntent().getExtras();
                String paymentType = getResources().getString(R.string.payment_type_card);
                finish();

                Intent intent = new Intent(this, ResultActivity.class);
                intent.putExtra("NAME", extras.getString("NAME"));
                intent.putExtra("COST", extras.getString("COST"));
                intent.putExtra("TOKEN", data.getString(AlbaIntentService.DATA_TOKEN));
                intent.putExtra("PAYMENT_TYPE", paymentType);
                startActivity(intent);

                break;
            case AlbaIntentService.STATUS_TOKEN_CREATE_FAILED:
                cardNumberErrorsField.setText(data.getString(AlbaIntentService.DATA_TOKEN_ERROR_CARD_NUMBER));
                cardHolderErrorsField.setText(data.getString(AlbaIntentService.DATA_TOKEN_ERROR_HOLDER));
                cvcErrorsField.setText(data.getString(AlbaIntentService.DATA_TOKEN_ERROR_CVC));
                monthErrorsField.setText(data.getString(AlbaIntentService.DATA_TOKEN_ERROR_MONTH));
                yearErrorsField.setText(data.getString(AlbaIntentService.DATA_TOKEN_ERROR_YEAR));

                break;
        }
    }

}
