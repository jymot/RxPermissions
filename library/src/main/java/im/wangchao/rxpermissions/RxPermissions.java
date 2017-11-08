package im.wangchao.rxpermissions;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableTransformer;
import io.reactivex.subjects.PublishSubject;

/**
 * <p>Description  : RxPermissions.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2017/11/2.</p>
 * <p>Time         : 下午6:53.</p>
 */
public class RxPermissions {
    private static final String TAG = "RxPermissions";

    private final Object TRIGGER = new Object();
    private final RxPermissionsFragment mFragment;

    public static RxPermissions with(Activity activity){
        return new RxPermissions(activity);
    }

    public RxPermissions(Activity activity) {
        mFragment = RxPermissionsFragment.getRxPermissionsFragment(activity);
    }

    /**
     * 请求权限，其中处理 shouldShowRequestPermissionRationale 和用户主动勾选不在提示的情况
     * 如果授权了所有权限，那么继续发送数据 true，否则发送 false
     *
     * @param rationale 如果用户第一次拒绝请求，则会显示一条消息，解释为什么应用程序需要这组权限
     * @param neverAskAgainPrompt 如果用户主动勾选不在提示，那么提示该信息
     * @param permissions 请求的权限
     * @return Observable
     */
    public Observable<Boolean> requestWithRationale(String rationale,
                                                    String neverAskAgainPrompt,
                                                    String... permissions) {
        return requestWithRationale(rationale, neverAskAgainPrompt, (dialog, which) -> dialog.dismiss(), permissions);
    }

    /**
     * 请求权限，其中处理 shouldShowRequestPermissionRationale 和用户主动勾选不在提示的情况
     * 如果授权了所有权限，那么继续发送数据 true，否则发送 false
     *
     * @param rationale 如果用户第一次拒绝请求，则会显示一条消息，解释为什么应用程序需要这组权限
     * @param neverAskAgainPrompt 如果用户主动勾选不在提示，那么提示该信息
     * @param onClickListener 点击提示 neverAskAgainPrompt 对话框时触发的事件
     * @param permissions 请求的权限
     * @return Observable
     */
    public Observable<Boolean> requestWithRationale(String rationale,
                                                    String neverAskAgainPrompt,
                                                    DialogInterface.OnClickListener onClickListener,
                                                    String... permissions) {
        return Observable.just(TRIGGER)
                .compose(ensureWithRationaleResultBoolean(rationale, neverAskAgainPrompt, onClickListener, permissions));
    }

    /**
     * 检测权限，返回每个权限对应的 {@link PermissionResult} 对象
     */
    public <T> ObservableTransformer<T, PermissionResult> ensure(String... permissions) {
        return upstream -> requestPermissions(upstream, permissions);
    }

    /**
     * 请求权限，其中处理 shouldShowRequestPermissionRationale 和用户主动勾选不在提示的情况
     * 如果授权了所有的请求权限，那么继续发送开始的数据源
     *
     * @param rationale 如果用户第一次拒绝请求，则会显示一条消息，解释为什么应用程序需要这组权限
     * @param neverAskAgainPrompt 如果用户主动勾选不在提示，那么提示该信息
     * @param permissions 请求的权限
     */
    public <T> ObservableTransformer<T, T> ensureWithRationale(String rationale,
                                                               String neverAskAgainPrompt,
                                                               String... permissions) {
        return ensureWithRationale(rationale, neverAskAgainPrompt, (dialog, which) -> dialog.dismiss(), permissions);
    }

    /**
     * 请求权限，其中处理 shouldShowRequestPermissionRationale 和用户主动勾选不在提示的情况
     * 如果授权了所有的请求权限，那么继续发送开始的数据源
     *
     * @param rationale 如果用户第一次拒绝请求，则会显示一条消息，解释为什么应用程序需要这组权限
     * @param neverAskAgainPrompt 如果用户主动勾选不在提示，那么提示该信息
     * @param onClickListener 点击提示 neverAskAgainPrompt 对话框时触发的事件
     * @param permissions 请求的权限
     */
    public <T> ObservableTransformer<T, T> ensureWithRationale(String rationale,
                                                               String neverAskAgainPrompt,
                                                               DialogInterface.OnClickListener onClickListener,
                                                               String ...permissions){
        return upstream -> Observable.just(TRIGGER)
                .compose(shouldShowRequestPermissionRationale(mFragment.getActivity(), rationale, permissions))
                .compose(ensure(permissions))
                .buffer(permissions.length)
                .take(1)
                .flatMap(PermissionResult -> {
                    PermissionResult permission;

                    boolean refuse = false;
                    for (int i = 0, len = PermissionResult.size(); i < len; i++) {
                        permission = PermissionResult.get(i);
                        // 有权限，继续遍历
                        if (permission.granted) {
                            continue;
                        } else {
                            refuse = true;
                        }

                        // 用户已经主动勾选不在提示，并且拒绝权限，所以需要提示用户在系统设置页面打开
                        if (!permission.shouldShowRequestPermissionRationale && !isEmpty(rationale)){
                            Activity context = mFragment.getActivity();

                            new AlertDialog.Builder(context)
                                    .setTitle("提示")
                                    .setMessage(neverAskAgainPrompt)
                                    .setPositiveButton("确定", onClickListener)
                                    .show();

                            return Observable.empty();
                        }

                    }

                    // 存在拒绝权限
                    if (refuse){
                        return Observable.empty();
                    }

                    return upstream;
                });
    }

    /**
     * 请求权限，其中处理 shouldShowRequestPermissionRationale 和用户主动勾选不在提示的情况
     * 如果授权了所有权限，那么继续发送数据 true，否则发送 false
     *
     * @param rationale 如果用户第一次拒绝请求，则会显示一条消息，解释为什么应用程序需要这组权限
     * @param neverAskAgainPrompt 如果用户主动勾选不在提示，那么提示该信息
     * @param permissions 请求的权限
     */
    public <T> ObservableTransformer<T, Boolean> ensureWithRationaleResultBoolean(String rationale,
                                                                                  String neverAskAgainPrompt,
                                                                                  String ...permissions){
        return ensureWithRationaleResultBoolean(rationale, neverAskAgainPrompt, (dialog, which) -> dialog.dismiss(), permissions);
    }

    /**
     * 请求权限，其中处理 shouldShowRequestPermissionRationale 和用户主动勾选不在提示的情况
     * 如果授权了所有权限，那么继续发送数据 true，否则发送 false
     *
     * @param rationale 如果用户第一次拒绝请求，则会显示一条消息，解释为什么应用程序需要这组权限
     * @param neverAskAgainPrompt 如果用户主动勾选不在提示，那么提示该信息
     * @param onClickListener 点击提示 neverAskAgainPrompt 对话框时触发的事件
     * @param permissions 请求的权限
     */
    public <T> ObservableTransformer<T, Boolean> ensureWithRationaleResultBoolean(String rationale,
                                                                                  String neverAskAgainPrompt,
                                                                                  DialogInterface.OnClickListener onClickListener,
                                                                                  String ...permissions){
        return upstream -> Observable.just(TRIGGER)
                .compose(shouldShowRequestPermissionRationale(mFragment.getActivity(), rationale, permissions))
                .compose(ensure(permissions))
                .buffer(permissions.length)
                .take(1)
                .flatMap(PermissionResult -> {
                    PermissionResult permission;

                    boolean refuse = false;
                    for (int i = 0, len = PermissionResult.size(); i < len; i++) {
                        permission = PermissionResult.get(i);
                        // 有权限，继续遍历
                        if (permission.granted) {
                            continue;
                        } else {
                            refuse = true;
                        }

                        // 用户已经主动勾选不在提示，并且拒绝权限，所以需要提示用户在系统设置页面打开
                        if (!permission.shouldShowRequestPermissionRationale && !isEmpty(rationale)){
                            Activity context = mFragment.getActivity();

                            new AlertDialog.Builder(context)
                                    .setTitle("提示")
                                    .setMessage(neverAskAgainPrompt)
                                    .setPositiveButton("确定", onClickListener)
                                    .show();

                            return Observable.just(false);
                        }

                    }

                    // 存在拒绝权限
                    if (refuse){
                        return Observable.just(false);
                    }

                    return Observable.just(true);
                });
    }

    /**
     * 返回是否需要解释请求权限原因的数据源
     */
    public Observable<Boolean> shouldShowRequestPermissionRationale(Activity context, String ...permissions){
        if (!isM()){
            return Observable.just(false);
        }

        for (String permission: permissions){
            if (!isGranted(permission) && !context.shouldShowRequestPermissionRationale(permission)){
                return Observable.just(false);
            }
        }

        return Observable.just(true);
    }

    /**
     * 处理是否解释请求权限原因数据源，如果允许了提示内容，那么继续发送原数据源
     */
    public <T> ObservableTransformer<T, T> shouldShowRequestPermissionRationale(Activity context, String rationale, String ...permissions){

        return upstream -> {
            if (hasPermissions(context, permissions)){
                return upstream;
            }
            return shouldShowRequestPermissionRationale(context, permissions)
                    .flatMap(shouldShowRequestPermissionRationale -> {
                        if (shouldShowRequestPermissionRationale){
                            PublishSubject<Boolean> publishSubject = PublishSubject.create();
                            new AlertDialog.Builder(context)
                                    .setCancelable(false)
                                    .setTitle("提示")
                                    .setMessage(rationale)
                                    .setPositiveButton("允许", (dialog, which) -> {
                                        publishSubject.onNext(true);
                                        publishSubject.onComplete();
                                    })
                                    .setNegativeButton("拒绝", (dialog, which) -> {
                                        publishSubject.onNext(false);
                                        publishSubject.onComplete();
                                    })
                                    .show();
                            return publishSubject;
                        }
                        return Observable.just(true);
                    })
                    .flatMap(result -> {
                        if (result){
                            return upstream;
                        }
                        return Observable.empty();
                    });
        };
    }

    private <T> Observable<PermissionResult> requestPermissions(Observable<T> upstream, String ...permissions){
        if (permissions == null || permissions.length == 0){
            throw new RuntimeException("you must request permissions at least one");
        }

        return upstream.flatMap(t -> requestPermissionsImpl(permissions));
    }

    private Observable<PermissionResult> requestPermissionsImpl(String ...permissions){
        final int length = permissions.length;
        List<Observable<PermissionResult>> permissionResultList = new ArrayList<>(length);
        List<String> notGrantedList = new ArrayList<>();

        for (String permission: permissions){
            if (isGranted(permission)){
                permissionResultList.add(Observable.just(PermissionResult.newInstance(permission, true)));
                continue;
            }

            if (isRevoked(permission)){
                permissionResultList.add(Observable.just(PermissionResult.newInstance(permission, false)));
                continue;
            }

            PublishSubject<PermissionResult> subject = mFragment.getSubjectWithPermission(permission);
            if (subject == null){
                notGrantedList.add(permission);
                subject = PublishSubject.create();
            }

            permissionResultList.add(mFragment.putSubjectWithPermission(permission, subject));
        }

        // request the permissions, if permissions are not granted
        if (!notGrantedList.isEmpty()){
            mFragment.requestPermissions(notGrantedList.toArray(new String[notGrantedList.size()]));
        }

        return Observable.concat(permissionResultList);
    }

    /**
     * 是否已经授权权限
     */
    public boolean isGranted(String permission){
        return !isM() || ContextCompat.checkSelfPermission(mFragment.getContext(), permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 是否已经取消权限
     */
    public boolean isRevoked(String permission){
        return isM() && mFragment.getActivity().getPackageManager().isPermissionRevokedByPolicy(permission, mFragment.getActivity().getPackageName());
    }

    /**
     * Check if the calling context has a set of permissions.
     *
     * @param context the calling context.
     * @param perms   one ore more permissions, such as {@code android.Manifest.permission.CAMERA}.
     * @return true if all permissions are already granted, false if at least one permission
     * is not yet granted.
     */
    public static boolean hasPermissions(Context context, String... perms) {
        // Always return true for SDK < M, let the system deal with the permissions
        if (!isM()) {
            Log.w(TAG, "hasPermissions: API version < M, returning true by default");
            return true;
        }

        for (String perm : perms) {
            boolean hasPerm = (ContextCompat.checkSelfPermission(context, perm) ==
                    PackageManager.PERMISSION_GRANTED);
            if (!hasPerm) {
                return false;
            }
        }

        return true;
    }

    private static boolean isM(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
}
