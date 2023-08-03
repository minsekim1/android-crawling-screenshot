package kr.yagiyagi.craw;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

public class TouchAreaService extends Service {

    private WindowManager windowManager;
    private View touchAreaView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        touchAreaView = new View(this);
        touchAreaView.setBackgroundColor(Color.parseColor("#6464644D"));
        touchAreaView.setLayoutParams(new ViewGroup.LayoutParams(32, 32));

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                32,
                32,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);

        windowManager.addView(touchAreaView, params);

        touchAreaView.setVisibility(View.INVISIBLE);
    }

    public void showTouchArea(int x, int y) {
        WindowManager.LayoutParams params = (WindowManager.LayoutParams) touchAreaView.getLayoutParams();
        params.x = x - 16;
        params.y = y - 16;
        windowManager.updateViewLayout(touchAreaView, params);
        touchAreaView.setVisibility(View.VISIBLE);
    }

    public void hideTouchArea() {
        touchAreaView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (touchAreaView != null) windowManager.removeView(touchAreaView);
    }
}
