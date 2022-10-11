package io.agora.examples.lyrics_view;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import io.agora.lrcview.DownloadManager;
import io.agora.lrcview.LrcLoadUtils;
import io.agora.lrcview.LrcView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, LrcView.OnActionListener {

    private static final String LRC_SAMPLE_1 = "https://webdemo.agora.io/ktv/chocolateice.xml";
    private static final String LRC_SAMPLE_2 = "https://webdemo.agora.io/ktv/001.xml";

    private LrcView lrcView;
    private boolean switched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.clear_cache).setOnClickListener(this);
        lrcView = findViewById(R.id.lrcView);
        lrcView.setActionListener(this);
        loadLrcFromUrl(LRC_SAMPLE_1);
    }

    private void loadLrcFromUrl(String lrcSample) {
        lrcView.reset();
        DownloadManager.getInstance().download(this, lrcSample
                , file -> lrcView.setLrcData(LrcLoadUtils.parse(file))
                , Throwable::printStackTrace);
    }

    @Override
    public void onClick(View view) {
        DownloadManager.getInstance().clearCache(this);
        if (switched) {
            loadLrcFromUrl(LRC_SAMPLE_1);
        } else {
            loadLrcFromUrl(LRC_SAMPLE_2);
        }
        switched = !switched;
    }

    @Override
    public void onProgressChanged(long time) {
        lrcView.updateTime(time + 1000);
    }

    @Override
    public void onStartTrackingTouch() {

    }

    @Override
    public void onStopTrackingTouch() {

    }
}