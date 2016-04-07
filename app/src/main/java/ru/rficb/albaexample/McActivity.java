package ru.rficb.albaexample;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.TextView;

public class McActivity extends ActionBarActivity {

    AlbaResultReceiver resultReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mc);

        resultReceiver = new AlbaResultReceiver(null);

        final TelephonyManager tManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final String phoneNumber = tManager.getLine1Number();

        TextView phoneField = (TextView)findViewById(R.id.phone);
        phoneField.setText(phoneNumber);
    }

    public void startPayment(View view) {
        Bundle extras = getIntent().getExtras();
        TextView phoneField = (TextView)findViewById(R.id.phone);

        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("NAME", extras.getString("NAME"));
        intent.putExtra("COST", extras.getString("COST"));
        intent.putExtra("PHONE", phoneField.getText().toString());
        intent.putExtra("PAYMENT_TYPE", getResources().getString(R.string.payment_type_mc));
        startActivity(intent);
        finish();

    }
}
