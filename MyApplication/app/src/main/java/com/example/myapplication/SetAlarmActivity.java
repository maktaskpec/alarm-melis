package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SetAlarmActivity extends AppCompatActivity {
    private Button setAlarmButton;
    private Button gun;
    private Button etiket;
    private TimePicker timePicker;
    private boolean[] selectedDays;
    private TextView currentTimeTextView;
    private Runnable updateTimeRunnable;
    private TextView currentDateTextView;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_alarm);
        handler = new Handler(Looper.getMainLooper());
        timePicker = findViewById(R.id.timePicker1);
        setAlarmButton = findViewById(R.id.setAlarmButton);
        gun = findViewById(R.id.gun);
        etiket = findViewById(R.id.etiket);
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        currentDateTextView = findViewById(R.id.day);

        setAlarmButton.setOnClickListener(new MyOnClickListener());
        gun.setOnClickListener(new MyOnClickListener());
        etiket.setOnClickListener(new MyOnClickListener());

        gun.setText(getString(R.string.tag));
        etiket.setText(getString(R.string.etiket));

        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentTimeTextView();
                updateCurrentDateTextView();
                handler.postDelayed(this, 1000); // Her 1 saniyede bir tekrar et
            }
        };
        handler.post(updateTimeRunnable);

        // selectedDays dizisini başlangıçta false değerleri ile oluştur
        selectedDays = new boolean[7];
    }

    public class AlarmData implements Serializable {
        private int hour;
        private int minute;
        private boolean[] selectedDays;

        public AlarmData(int hour, int minute, boolean[] selectedDays) {
            this.hour = hour;
            this.minute = minute;
            this.selectedDays = selectedDays;
        }

        public int getHour() {
            return hour;
        }

        public int getMinute() {
            return minute;
        }

        public boolean[] getSelectedDays() {
            return selectedDays;
        }
    }

    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.setAlarmButton) {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedDays", selectedDays);
                resultIntent.putExtra("selectedHour", hour);
                resultIntent.putExtra("selectedMinute", minute);

                setResult(RESULT_OK, resultIntent);
                finish();
            } else if (view.getId() == R.id.gun) {
                showRepeatDialog();
            } else if (view.getId() == R.id.etiket) {
                showLabelDialog();
            }
        }
    }
    private void updateCurrentTimeTextView() {
        Calendar currentTime = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        currentTimeTextView.setText(sdf.format(currentTime.getTime()));
    }
    private void updateCurrentDateTextView() {
        Calendar currentDate = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        String formattedDate = sdf.format(currentDate.getTime());
        currentDateTextView.setText(formattedDate);
    }
    private void showRepeatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alarm Tekrarlama");

        // Seçenekleri tanımla
        String[] repeatOptions = {"Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};

        // Seçenekleri gösteren bir çoklu seçim listesi oluştur
        boolean[] checkedItems = new boolean[repeatOptions.length];

        builder.setMultiChoiceItems(repeatOptions, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                // Kullanıcının seçimlerini takip et
                checkedItems[which] = isChecked;
            }
        });
        builder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Gün seçilip seçilmediğini kontrol et
                boolean isDaySelected = false;
                for (boolean checkedItem : checkedItems) {
                    if (checkedItem) {
                        isDaySelected = true;
                        break;
                    }
                }

                // Eğer gün seçildiyse, selectedDays dizisini güncelle
                if (isDaySelected) {
                    selectedDays = checkedItems.clone();
                }

                // AlarmData nesnesini oluştur ve MainActivity'e gönder
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                AlarmData alarmData = new AlarmData(hour, minute, isDaySelected ? selectedDays : null);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("alarmData", alarmData);

                setResult(RESULT_OK, resultIntent);
            }
        });

        builder.setNegativeButton("İptal", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // İptal durumunda yapılacaklar
            }
        });
        builder.create().show();
    }

    private void showLabelDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Etiket Girişi");

        final EditText etiketEditText = new EditText(this);
        etiketEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(etiketEditText);

        builder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("İptal", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }
}
