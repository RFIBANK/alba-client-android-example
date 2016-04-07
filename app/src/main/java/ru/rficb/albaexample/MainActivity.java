package ru.rficb.albaexample;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {
    TextView nameField;
    TextView costField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nameField = (TextView)findViewById(R.id.name);
        costField = (TextView)findViewById(R.id.cost);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean checkFields() {
        Resources resources = getResources();

        if (nameField.getText().toString().equals("")) {
            Toast.makeText(this, resources.getString(R.string.validation_error_name_is_empty),
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if (costField.getText().toString().equals("")) {
            Toast.makeText(this, resources.getString(R.string.validation_error_cost_is_empty),
                    Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    public void onClickMc(View view) {
        if (checkFields()) {
            Intent intent = new Intent(this, McActivity.class);
            intent.putExtra("NAME", nameField.getText().toString());
            intent.putExtra("COST", costField.getText().toString());
            startActivity(intent);
        }
    }

    public void onClickCard(View view) {
        if (checkFields()) {
            Intent intent = new Intent(this, CardActivity.class);
            intent.putExtra("NAME", nameField.getText().toString());
            intent.putExtra("COST", costField.getText().toString());
            startActivity(intent);
        }
    }

}
