package com.example.myapplication;

import android.app.AlarmManager;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import android.os.Looper;
import android.os.Handler;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import android.view.MotionEvent;
import android.widget.Toast;
import java.util.List;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity{
    private static AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private Handler handler;
    private Runnable updateTimeRunnable;
    private TextView currentTimeTextView;
    private Button tosetAlarmButton;
    private ViewPager viewPager;
    private ImageView imageView;
    private MediaPlayer mediaPlayer;
    private CountDownTimer countDownTimer;
    List<AlarmInfo> alarmList = new ArrayList<>();
    private ArrayAdapter<AlarmInfo> adapter;
    private ListView alarmListView;
    private TextView countdownTextView;
    private static final String PREF_NAME = "AlarmPreferences";
    private static final String ALARM_LIST_KEY = "alarmList";
    private TextView currentDateTextView;
    ImagePagerAdapter imagePagerAdapter = new ImagePagerAdapter(this);
    private AlarmReceiver alarmReceiver = new AlarmReceiver();
    private static final int SET_ALARM_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        handler = new Handler(Looper.getMainLooper());
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        currentTimeTextView = findViewById(R.id.currentTimeTextView);
        alarmListView = findViewById(R.id.alarmListView);
        tosetAlarmButton = findViewById(R.id.tosetAlarmButton);
        mediaPlayer = MediaPlayer.create(this, R.raw.horoz_ses1);
        countdownTextView = findViewById(R.id.countdownTextView);
        tosetAlarmButton.setOnClickListener(new MyOnClickListener());
        currentDateTextView = findViewById(R.id.day);
        viewPager = findViewById(R.id.viewPager);
        imageView = findViewById(R.id.imageView2);
        loadAlarms();
        updateListView();

        final String urlToOpen = "https://www.kpec-gmbh.de";

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Belirtilen URL'yi açmak için tarayıcıyı başlat
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen));
                startActivity(browserIntent);
            }
        });

        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentTimeTextView();
                updateCurrentDateTextView();
                handler.postDelayed(this, 1000); // Her 1 saniyede bir tekrar et
            }
        };

        IntentFilter filter = new IntentFilter("com.example.myapplication.ALARM_TRIGGERED");
        registerReceiver(alarmReceiver, filter);
        alarmListView.setAdapter(adapter);
        handler.post(updateTimeRunnable);
        AlarmInfo nextAlarm = getNextAlarm();
        if(viewPager != null){
            viewPager.setAdapter(imagePagerAdapter);

            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    // onPageScrolled event'i
                }
                @Override
                public void onPageSelected(int position) {
                    // Sayfa değiştirildiğinde çağrılan event
                    updateAlarmSound(position);
                }
                @Override
                public void onPageScrollStateChanged(int state) {
                    // onPageScrollStateChanged event'i
                }
            });
        }
    }
    public static class AlarmInfo {
        private int hour;
        private int minute;
        private boolean isEnabled;
        private CountDownTimer countDownTimer;
        private List<String> selectedDays;
        public AlarmInfo(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
            this.isEnabled = true;
            this.selectedDays = new ArrayList<>();
        }
        public CountDownTimer getCountDownTimer() {
            return countDownTimer;
        }
        public void setCountDownTimer(CountDownTimer countDownTimer) {
            this.countDownTimer = countDownTimer;
        }
        public int getHour() {
            return hour;
        }
        public int getMinute() {
            return minute;
        }
        public boolean isEnabled() {
            return isEnabled;
        }
        public void setEnabled(boolean enabled) {
            isEnabled = enabled;
        }
        public List<String> getSelectedDays() {
            return selectedDays;
        }
        public void setHour(int hour) {
            this.hour = hour;
        }
        public void setMinute(int minute) {
            this.minute = minute;
        }
        public void setSelectedDays(List<String> selectedDays) {
            this.selectedDays = selectedDays;
        }
        @Override
        public String toString() {
            return String.format(Locale.getDefault(), "%02d:%02d - %s", hour, minute, TextUtils.join(", ", selectedDays));
        }
    }
    private void updateCurrentDateTextView() {
        Calendar currentDate = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        String formattedDate = sdf.format(currentDate.getTime());
        currentDateTextView.setText(formattedDate);
    }
    private void updateAlarmSound(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(this, imagePagerAdapter.getSoundResource(position));

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mediaPlayer.start();
            }
        });
    }
    private void saveAlarms() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String alarmListJson = gson.toJson(alarmList);
        editor.putString(ALARM_LIST_KEY, alarmListJson);
        editor.apply();
    }
    private void loadAlarms() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedAlarmListJson = sharedPreferences.getString(ALARM_LIST_KEY, "");
        if (!savedAlarmListJson.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<AlarmInfo>>() {}.getType();
            alarmList = gson.fromJson(savedAlarmListJson, type);
        }
    }
    private class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            // Intent oluştur
            Intent setAlarmIntent = new Intent(MainActivity.this, SetAlarmActivity.class);
            // Intent'i başlat ve onActivityResult metodunu bekleyerek başlat
            startActivityForResult(setAlarmIntent, SET_ALARM_REQUEST_CODE);
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SET_ALARM_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                // SetAlarmActivity'den dönen verileri al
                int selectedHour = data.getIntExtra("selectedHour", 0);
                int selectedMinute = data.getIntExtra("selectedMinute", 0);
                boolean[] selectedDays = data.getBooleanArrayExtra("selectedDays");
                List<String> selectedDaysStringList = booleanArrayToStringList(selectedDays);
                // Diğer verileri de alabilirsiniz, gerekirse

                // Verileri kullanarak alarmı kur
                MainActivity.AlarmInfo newAlarm = new MainActivity.AlarmInfo(selectedHour, selectedMinute);
                newAlarm.setSelectedDays(selectedDaysStringList);

                alarmList.add(newAlarm);
                updateListView();
                startCountdown(newAlarm);
                scheduleAlarm(newAlarm);
                startCountdown(newAlarm);
                handleSelectedDays(newAlarm, selectedDays);
                saveAlarms();
            }
        }
    }
    private List<String> booleanArrayToStringList(boolean[] selectedDays) {
        List<String> selectedDaysStringList = new ArrayList<>();

        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) {
                // Eğer gün seçilmişse, gün adını ekleyin
                String dayName = getDayName(i);
                selectedDaysStringList.add(dayName);
            }
        }

        return selectedDaysStringList;
    }
//       private class MyOnClickListener implements View.OnClickListener {
//        @Override
//        public void onClick(View v) {
//            Calendar calendar = Calendar.getInstance();
//            TimePickerDialog timePickerDialog = new TimePickerDialog(
//                    MainActivity.this,
//                    new TimePickerDialog.OnTimeSetListener() {
//                        @Override
//                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
//                            AlarmInfo newAlarm = new AlarmInfo(hourOfDay, minute);
//                            alarmList.add(newAlarm);
//                            updateListView();
//                            showRepeatDialog(newAlarm);
//                            scheduleAlarm(newAlarm);
//                        }
//                    },
//                    calendar.get(Calendar.HOUR_OF_DAY),
//                    calendar.get(Calendar.MINUTE),
//                    true
//            );
//            timePickerDialog.show();
//
//        }
//    }
    private void scheduleAlarm(AlarmInfo alarm) {
        // Alarmı çalıştırmak için bir Intent oluştur
        Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
        // Alarm ID'si olarak saat ve dakikayı kullanabilirsiniz
        int alarmId = alarm.getHour() * 100 + alarm.getMinute();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // AlarmManager'ı al ve belirli bir zamanda çalıştır
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Calendar nesnesi oluştur ve alarmın çalması gereken zamanı ayarla
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        alarmTime.set(Calendar.MINUTE, alarm.getMinute());
        alarmTime.set(Calendar.SECOND, 0);

        // Eğer bu zaman şu andan geçmişse, bir sonraki günü seç
        if (alarmTime.getTimeInMillis() <= System.currentTimeMillis()) {
            alarmTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Alarmı bir kere çalıştırmak istiyorsanız set() kullanabilirsiniz
        // alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime.getTimeInMillis(), pendingIntent);

        // Eğer tekrarlı bir alarm istiyorsanız setRepeating() kullanabilirsiniz
        // Bu örnekte bir gün arayla çalışacak şekilde ayarlandı
        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                alarmTime.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,  // Bir gün arayla tekrar et
                pendingIntent
        );
    }
    private AlarmInfo getNextAlarm() {
        AlarmInfo nextAlarm = null;
        long minTime = Long.MAX_VALUE;
        long currentTime = System.currentTimeMillis();

        for (AlarmInfo alarm : alarmList) {
            if (alarm.isEnabled()) {
                long alarmTime = calculateNextAlarmTime(alarm);
                if (alarmTime < minTime && alarmTime > currentTime) {
                    minTime = alarmTime;
                    nextAlarm = alarm;
                }
            }
        }

        return nextAlarm;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
//private void showRepeatDialog(final AlarmInfo alarm) {
//    AlertDialog.Builder builder = new AlertDialog.Builder(this);
//    builder.setTitle("Alarm Tekrarlama");
//
//    // Seçenekleri tanımla
//    String[] repeatOptions = {"Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};
//
//    // Seçenekleri gösteren bir çoklu seçim listesi oluştur
//    boolean[] checkedItems = new boolean[repeatOptions.length];
//
//    builder.setMultiChoiceItems(repeatOptions, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
//        @Override
//        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
//            // Kullanıcının seçimlerini takip et
//            checkedItems[which] = isChecked;
//        }
//    });
//    builder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
//        @Override
//        public void onClick(DialogInterface dialog, int which) {
//            // Gün seçilip seçilmediğini kontrol et
//            boolean isDaySelected = false;
//            for (boolean checkedItem : checkedItems) {
//                if (checkedItem) {
//                    isDaySelected = true;
//                    break;
//                }
//            }
//            // Seçilen günleri işle
//            if (isDaySelected) {
//                handleSelectedDays(alarm, checkedItems);
//                startCountdown(alarm);
//                saveAlarms();
//                updateListView();
//                scheduleAlarm(alarm);
//            } else {
//                // Gün seçilmediyse, alarmı kurulan gün içinde çalacak şekilde ayarla
//                handleSelectedDays(alarm, checkedItems);
//                startCountdown(alarm);
//                saveAlarms();
//                updateListView();
//                scheduleAlarm(alarm);
//            }
//        }
//    });
//    builder.setNegativeButton("İptal", new DialogInterface.OnClickListener() {
//        @Override
//        public void onClick(DialogInterface dialog, int which) {
//            // İptal durumunda yapılacaklar
//        }
//    });
//    builder.create().show();
//}
    private void handleSelectedDays(AlarmInfo alarm, boolean[] selectedDays) {
        List<String> selectedDayList = new ArrayList<>();
        boolean isDaySelected = false;
        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) {
                selectedDayList.add(getDayName(i));
                isDaySelected = true;
            }
        }
        if(!isDaySelected){
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, alarm.getHour());
            calendar.set(Calendar.MINUTE, alarm.getMinute());
            calendar.set(Calendar.SECOND, 0);

            alarm.setHour(calendar.get(Calendar.HOUR_OF_DAY));
            alarm.setMinute(calendar.get(Calendar.MINUTE));
            selectedDayList.add(getDayName(calendar.get(Calendar.DAY_OF_WEEK)-2));
            selectedDayList.add("Bir kez çal");
        }
        alarm.setSelectedDays(selectedDayList);
    }
    private String getDayName(int dayIndex) {
        if (dayIndex >= 0 && dayIndex < 7) {
            String[] days = {"Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};
            return days[dayIndex];
        } else {
            return "Bir kez çalsın";
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private void updateCurrentTimeTextView() {
        Calendar currentTime = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        currentTimeTextView.setText(sdf.format(currentTime.getTime()));
    }
    private void showAlert(final Context context, String title, String message, final CountDownTimer countDownTimer, final PendingIntent pendingIntent, final AlarmInfo alarm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Duraklat", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        stopAlarm(context, countDownTimer, pendingIntent);
                        alarm.setEnabled(false);
                        updateListView();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    private void stopAlarm(Context context, CountDownTimer countDownTimer, PendingIntent pendingIntent) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.cancel();
        }
        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
            alarmManager.cancel(pendingIntent);
        }
    }

    private void updateListView() {
        adapter = new ArrayAdapter<AlarmInfo>(MainActivity.this, R.layout.list_item, R.id.textView, alarmList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                Switch switchButton = view.findViewById(R.id.switchButton);
                Button deleteButton = view.findViewById(R.id.deleteButton);
                TextView textView = view.findViewById(R.id.textView);

                String alarmInfoText = alarmList.get(position).toString();
                textView.setText(alarmInfoText);

                switchButton.setChecked(alarmList.get(position).isEnabled());
                switchButton.setOnCheckedChangeListener(null);

                switchButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        AlarmInfo alarm = alarmList.get(position);
                        alarm.setEnabled(isChecked);
                        if (!isChecked) {
                            stopCountdown(alarm);
                        }
                        else {
                            startCountdown(alarm);
                        }
                        updateListView();
                    }
                });
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlarmInfo alarm = alarmList.get(position);
                        alarmList.remove(position);
                        saveAlarms();
                        updateListView();
                        stopCountdown(alarm);
                    }
                });
                AlarmInfo alarm = alarmList.get(position);
                String timeAndDays = String.format("%02d:%02d - %s", alarm.getHour(), alarm.getMinute(), TextUtils.join(", ", alarm.getSelectedDays()));
                textView.setText(timeAndDays);
                return view;
            }
        };
        alarmListView.setAdapter(adapter);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateTimeRunnable);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmReceiver);

    }
    private void startCountdown(AlarmInfo alarm) {
        if (alarm.getCountDownTimer() == null) {
            long nextAlarmTime = calculateNextAlarmTime(alarm);
            long timeUntilNextAlarm = nextAlarmTime - System.currentTimeMillis();

            countDownTimer = new CountDownTimer(timeUntilNextAlarm, 1000) {
                public void onTick(long millisUntilFinished) {
                    updateCountdownTextView(millisUntilFinished);
                }
                public void onFinish() {
                    showAlert(MainActivity.this, "ALARM", "Alarmı durdurmak için durdura basın", countDownTimer, pendingIntent, alarm);
                    startAlarmSound();
                }
            }.start();
            alarm.setCountDownTimer(countDownTimer);
        }
    }
    private long calculateNextAlarmTime(AlarmInfo alarm) {
        List<String> selectedDays = alarm.getSelectedDays();

        if (selectedDays == null || selectedDays.isEmpty()) {
            // Eğer günler seçilmemişse, alarmı bulunduğumuz gün ve saatte ayarla
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, alarm.getHour());
            calendar.set(Calendar.MINUTE, alarm.getMinute());
            calendar.set(Calendar.SECOND, 0);

            // Eğer seçilen saat şu anki saatten önceyse, bir sonraki günü seç
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_WEEK, 1);
            }
            return calendar.getTimeInMillis();
        } else {
            // Eğer günler seçilmişse, belirtilen günlerdeki ilk alarm tarihini bul
            return findNextAlarmDate(alarm);
        }
    }
    private long findNextAlarmDate(AlarmInfo alarm) {
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_WEEK);

        for (String selectedDay : alarm.getSelectedDays()) {
            int selectedDayIndex = getDayIndex(selectedDay);

            if (selectedDayIndex > currentDay || (currentDay == Calendar.SATURDAY && selectedDayIndex == Calendar.SUNDAY)) {
                // Eğer seçilen gün bugünkü günden büyükse veya bugün Cumartesi ve seçilen gün Pazar ise
                calendar.set(Calendar.DAY_OF_WEEK, selectedDayIndex);
            } else if (selectedDayIndex == currentDay) {
                // Eğer seçilen gün bugünkü günle aynıysa ve şu anki saat geçmiş bir saatse bir sonraki haftaki o günü seç
                if (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE) < alarm.getHour() * 60 + alarm.getMinute()) {
                    calendar.set(Calendar.DAY_OF_WEEK, selectedDayIndex);
                } else {
                    // Aynı gün, ancak bu hafta alarmın zamanı geçmişse bir sonraki haftaki o günü seç
                    calendar.add(Calendar.DAY_OF_WEEK, 7);
                    calendar.set(Calendar.DAY_OF_WEEK, selectedDayIndex);
                }
            } else {
                // Eğer seçilen gün bugünkü günden küçükse veya bugün Cumartesi ve seçilen gün Pazar ise
                calendar.add(Calendar.DAY_OF_WEEK, 7);
                calendar.set(Calendar.DAY_OF_WEEK, selectedDayIndex);
            }

            calendar.set(Calendar.HOUR_OF_DAY, alarm.getHour());
            calendar.set(Calendar.MINUTE, alarm.getMinute());
            calendar.set(Calendar.SECOND, 0);

            // Eğer seçilen gün ve saat şu anki tarihten önceyse, bir sonraki haftaki o günü seç
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_WEEK, 7);
            }
            return calendar.getTimeInMillis();
        }
        return 0; // Hata durumu, bu durumda 0 döndürülebilir.
    }
    private int getDayIndex(String day) {
        switch (day) {
            case "Pazartesi":
                return Calendar.MONDAY;
            case "Salı":
                return Calendar.TUESDAY;
            case "Çarşamba":
                return Calendar.WEDNESDAY;
            case "Perşembe":
                return Calendar.THURSDAY;
            case "Cuma":
                return Calendar.FRIDAY;
            case "Cumartesi":
                return Calendar.SATURDAY;
            case "Pazar":
                return Calendar.SUNDAY;
            default:
                return -1;
        }
    }
    private void startAlarmSound() {
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // Medya dosyası tamamlandığında yapılacak işlemler buraya yazılır.
                    mediaPlayer.start(); // Medya dosyasını tekrar başlat
                }
            });
            mediaPlayer.start();
        }
    }
    private void stopCountdown(AlarmInfo alarm) {
        if (alarm.getCountDownTimer() != null) {
            alarm.getCountDownTimer().cancel();
            alarm.setCountDownTimer(null);
            updateCountdownTextView(0);
        }
    }
    private void updateCountdownTextView(long millisUntilFinished) {
        long secondsUntilFinished = millisUntilFinished / 1000;
        long minutesUntilFinished = secondsUntilFinished / 60;
        long hoursUntilFinished = minutesUntilFinished / 60;
        long daysUntilFinished = millisUntilFinished / (1000 * 60 * 60 * 24);

        String countDown = String.format(Locale.getDefault(), "Alarm in: %02d:%02d:%02d:%02d",
                daysUntilFinished, hoursUntilFinished % 24, minutesUntilFinished % 60, secondsUntilFinished % 60);
        countdownTextView.setText(countDown);
    }
}