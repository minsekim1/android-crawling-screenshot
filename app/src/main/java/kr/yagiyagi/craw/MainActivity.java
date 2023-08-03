package kr.yagiyagi.craw;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import kr.yagiyagi.craw.databinding.ActivityMainBinding;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private boolean isTouchAreaShown = false;
    private boolean isRecording = false;
    private List<TouchEvent> recordedEvents = new ArrayList<>(); // 터치 이벤트 저장 리스트

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button playBtn = findViewById(R.id.play_btn);
        Button repeatBtn = findViewById(R.id.repeat_btn);
        Button recordBtn = findViewById(R.id.record_btn);
        Button toggleBtn = findViewById(R.id.toggle_btn);

        playBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MainActivity", "제스쳐 실행하기 버튼 클릭됨");
                String filename = "recorded_events.json"; // 파일 이름은 실제 저장된 파일에 맞춰 설정
                List<TouchEvent> recordedEvents = loadRecordedEvents(filename);
                if (recordedEvents != null) {
                    replayRecordedEvents(recordedEvents, v); // 여기에 View 인스턴스를 전달
                } else {
                    Log.e("MainActivity", "저장된 제스쳐를 찾을 수 없습니다.");
                }
            }
        });
        repeatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MainActivity", "제스쳐 무한반복하기 버튼 클릭됨");
            }
        });
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    // 녹화 중지
                    Log.d("MainActivity", "제스쳐 녹화 중지");
                    isRecording = false;
                    saveRecordedEvents(recordedEvents); // 저장 로직
                } else {
                    // 녹화 시작
                    Log.d("MainActivity", "제스쳐 녹화 시작");
                    isRecording = true;
                    recordedEvents.clear();
                }
            }
        });
        toggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (((ToggleButton) v).isChecked()) {
                    isTouchAreaShown = true;
                    startService(new Intent(MainActivity.this, TouchAreaService.class));
                } else {
                    isTouchAreaShown = false;
                    stopService(new Intent(MainActivity.this, TouchAreaService.class));
                }
            }
        });
    }
    private void replayRecordedEvents(List<TouchEvent> recordedEvents, View targetView) {
        // 파일에서 녹화된 터치 이벤트 불러오기를 이미 완료한 상태이므로, 이 라인은 필요 없습니다.
        // List<TouchEvent> recordedEvents = loadRecordedEvents(filename);

        // 루트 권한이 필요한 작업을 수행하는 별도의 스레드 생성
        new Thread(() -> {
            long lastTimestamp = 0; // 이전 이벤트의 타임스탬프를 추적
            try {
                // 녹화된 터치 이벤트 재생
                for (TouchEvent event : recordedEvents) {
                    // 이벤트의 발생 시간과 이전 이벤트 사이의 딜레이 계산
                    long delay = event.timestamp - lastTimestamp;

                    // 딜레이가 필요하면 대기
                    if (delay > 0) {
                        Thread.sleep(delay);
                    }

                    // 루트 권한으로 터치 이벤트 전달
                    sendTouchEvent(targetView, event.action, event.x, event.y);

                    // 현재 이벤트의 타임스탬프를 저장
                    lastTimestamp = event.timestamp;
                }
            } catch (InterruptedException e) {
                // 스레드가 중단된 경우 (예: 사용자가 재생을 중지하고 싶어하는 경우)
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (isRecording) {
            recordedEvents.add(new TouchEvent(ev.getX(), ev.getY(), ev.getAction(), System.currentTimeMillis())); // 이벤트 저장
        }
        return super.dispatchTouchEvent(ev);
    }

    private void saveRecordedEvents(List<TouchEvent> recordedEvents) {
        File file = new File(getFilesDir(), "recorded_gestures.txt");
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(recordedEvents);
        } catch (IOException e) {
            Log.e("MainActivity", "제스쳐 저장 실패", e);
        }
    }

    private static class TouchEvent {
        float x;
        float y;
        int action;
        long timestamp;

        TouchEvent(float x, float y, int action, long timestamp) {
            this.x = x;
            this.y = y;
            this.action = action;
            this.timestamp = timestamp;
        }

        public long getTime() {
            return timestamp;
        }

        // 필요한 경우 다른 getter 메서드도 추가 가능
    }



    //    private void saveRecordedEvents(List<TouchEvent> recordedEvents) {
//        Gson gson = new Gson();
//        String json = gson.toJson(recordedEvents);
//
//        // 파일에 JSON 문자열 저장
//        FileOutputStream fos = null;
//        try {
//            fos = openFileOutput("recorded_events.json", MODE_PRIVATE);
//            fos.write(json.getBytes());
//            Log.d("MainActivity", "터치 이벤트가 저장되었습니다.");
//        } catch (FileNotFoundException e) {
//            Log.e("MainActivity", "파일을 찾을 수 없습니다.", e);
//        } catch (IOException e) {
//            Log.e("MainActivity", "파일 저장 중 오류가 발생했습니다.", e);
//        } finally {
//            if (fos != null) {
//                try {
//                    fos.close();
//                } catch (IOException e) {
//                    Log.e("MainActivity", "파일을 닫는 중 오류가 발생했습니다.", e);
//                }
//            }
//        }
//    }
private List<TouchEvent> loadRecordedEvents(String filename) {
    FileInputStream fis = null;
    try {
        fis = openFileInput(filename); // 파일 이름을 파라미터에서 받음
        InputStreamReader isr = new InputStreamReader(fis);
        BufferedReader bufferedReader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line);
        }
        String json = sb.toString();

        Gson gson = new Gson();
        Type type = new TypeToken<List<TouchEvent>>() {}.getType();
        return gson.fromJson(json, type);
    } catch (FileNotFoundException e) {
        Log.e("MainActivity", "파일을 찾을 수 없습니다.", e);
    } catch (IOException e) {
        Log.e("MainActivity", "파일 읽기 중 오류가 발생했습니다.", e);
    } finally {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                Log.e("MainActivity", "파일을 닫는 중 오류가 발생했습니다.", e);
            }
        }
    }
    return null;
}

    private void sendTouchEvent(View view, int action, float x, float y) {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        MotionEvent motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                action,
                x,
                y,
                0 // MetaState
        );
        view.dispatchTouchEvent(motionEvent);
    }

}
