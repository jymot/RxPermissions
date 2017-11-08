package im.wangchao.rxpermissions;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.subjects.PublishSubject;

/**
 * <p>Description  : RxPermissionsFragment.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2017/11/2.</p>
 * <p>Time         : 下午6:46.</p>
 */
public class RxPermissionsFragment extends Fragment {
    private static final String FRAGMENT_TAG = "RxPermissionsFragmentTag";
    private static final int REQUEST_PERMISSIONS_CODE = 20;

    private Map<String, PublishSubject<PermissionResult>> mSubjectMap = new HashMap<>();

    static RxPermissionsFragment getRxPermissionsFragment(Activity activity){
        RxPermissionsFragment fragment = (RxPermissionsFragment) activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment == null){
            fragment = new RxPermissionsFragment();
            FragmentManager fragmentManager = activity.getFragmentManager();
            fragmentManager.beginTransaction()
                    .add(fragment, FRAGMENT_TAG)
                    .commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }

        return fragment;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSIONS_CODE){
            return;
        }

        PublishSubject<PermissionResult> subject;
        for (int i = 0, len = permissions.length; i < len; i++){
            subject = getSubjectWithPermission(permissions[i]);
            if (subject == null){
                continue;
            }
            mSubjectMap.remove(permissions[i]);
            boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
            subject.onNext(PermissionResult.newInstance(permissions[i], granted, shouldShowRequestPermissionRationale(permissions[i])));
            subject.onComplete();
        }
    }

    @TargetApi(Build.VERSION_CODES.M) void requestPermissions(String ...permissions){
        requestPermissions(permissions, REQUEST_PERMISSIONS_CODE);
    }

    PublishSubject<PermissionResult> putSubjectWithPermission(String permission, PublishSubject<PermissionResult> subject){
        mSubjectMap.put(permission, subject);
        return subject;
    }

    PublishSubject<PermissionResult> getSubjectWithPermission(String permission){
        return mSubjectMap.get(permission);
    }


}
