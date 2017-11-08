package im.wangchao.rxpermissionssample;

import android.Manifest;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import im.wangchao.rxpermissions.RxPermissions;
import io.reactivex.Observable;

public class MainActivity extends AppCompatActivity {

    private RxPermissions mPermissions;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.one).setOnClickListener(this.click());
        findViewById(R.id.two).setOnClickListener(this.click());
        mPermissions = RxPermissions.with(this);
    }

    private View.OnClickListener click(){
        return v -> {
            switch (v.getId()){
                case R.id.one:
                    requestOne();
                    break;
                case R.id.two:
                    requestTwo();
                    break;
            }
        };
    }

    private void requestOne(){
        mPermissions.requestWithRationale(
                "this is rationale.",
                "this is never ask again prompt.",
                Manifest.permission.CAMERA)
                .subscribe(result -> {
                   Log.e("wcwcwc", "request result: " + result);
                });
    }

    private void requestTwo(){
        String event = "some event.";
        Observable.just(event)
                .compose(mPermissions.ensureWithRationale(
                        "this is rationale.",
                        "this is never ask again prompt.",
                        Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .subscribe(result -> {
                   // result is event
                    Log.e("wcwcwc", "request success: " + result);
                });
    }
}
