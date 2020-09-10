package dev.eyosiyas.smsblocker.view;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import dev.eyosiyas.smsblocker.R;
import dev.eyosiyas.smsblocker.adapter.MessageListAdapter;
import dev.eyosiyas.smsblocker.model.Message;
import dev.eyosiyas.smsblocker.util.Constant;
import dev.eyosiyas.smsblocker.util.Core;

import static dev.eyosiyas.smsblocker.util.Constant.FILTER_SMS_DELIVERED;
import static dev.eyosiyas.smsblocker.util.Constant.FILTER_SMS_SENT;

public class DetailSmsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MessageListAdapter messageListAdapter;
    private Button send;
    private EditText message;
    private String number, KEY;
    private boolean isNumber;

    private final BroadcastReceiver sentBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "Message sent successfully.", Toast.LENGTH_SHORT).show();
                    messageListAdapter = new MessageListAdapter(DetailSmsActivity.this, getMessages(KEY));
                    recyclerView.setAdapter(messageListAdapter);
                    resetUI();
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(context, "RESULT_ERROR_GENERIC_FAILURE", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Toast.makeText(context, "ERROR_NO_SERVICE", Toast.LENGTH_SHORT).show();

                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Toast.makeText(context, "ERROR_NULL_PDU", Toast.LENGTH_SHORT).show();
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Toast.makeText(context, "ERROR_RADIO_OFF", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final BroadcastReceiver deliveredBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "Message delivered to recipient.", Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(context, "Message not received by recipient.", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(sentBroadcastReceiver, new IntentFilter(FILTER_SMS_SENT));
        registerReceiver(deliveredBroadcastReceiver, new IntentFilter(FILTER_SMS_DELIVERED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(sentBroadcastReceiver);
        unregisterReceiver(deliveredBroadcastReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_sms);
        send = findViewById(R.id.btnSendMessage);
        message = findViewById(R.id.editMessageBox);
        Toolbar toolbar = findViewById(R.id.detailToolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        KEY = getIntent().getStringExtra("KEY");
        String DISPLAY = getIntent().getStringExtra("DISPLAY");
        if (KEY == null)
            onBackPressed();
        toolbar.setTitle(DISPLAY != null ? DISPLAY : KEY);
        messageListAdapter = new MessageListAdapter(this, getMessages(KEY));
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        recyclerView = findViewById(R.id.detailMessageRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageListAdapter);
        message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validation();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    public List<Message> getMessages(String search) {
        List<Message> messages = new ArrayList<>();
        Message message;
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/"), null, "address=?", new String[]{search}, null);
        while (cursor.moveToNext()) {
            message = new Message();
            number = cursor.getString(cursor.getColumnIndex("address"));
            isNumber = number.matches("[0-9]+") && number.length() > 2;
            message.setSender(number);
            message.setDisplayName(Core.getDisplayName(this, cursor.getString(cursor.getColumnIndex("address"))));
            message.setBody(cursor.getString(cursor.getColumnIndex("body")));
            message.setRead(cursor.getInt(cursor.getColumnIndex("read")) != 0);
            message.setSeen(cursor.getInt(cursor.getColumnIndex("seen")) != 0);
            message.setTimestamp(cursor.getLong(cursor.getColumnIndex("date")));
            message.setType(cursor.getInt(cursor.getColumnIndex("type")) == 1 ? "received" : "sent");
            messages.add(message);
        }
        cursor.close();
        return messages;
    }

    private void validation() {
        send.setEnabled(isNumber && message.getText().length() > 0);
    }

    private void sendMessage() {
        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(FILTER_SMS_SENT), 0);
        PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(FILTER_SMS_DELIVERED), 0);
        if (ContextCompat.checkSelfPermission(DetailSmsActivity.this, Constant.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, message.getText().toString(), sentPendingIntent, deliveredPendingIntent);
        } else
            Toast.makeText(DetailSmsActivity.this, "Permission missing.", Toast.LENGTH_SHORT).show();
    }

    private void resetUI() {
        message.setText("");
        send.setEnabled(false);
    }
}