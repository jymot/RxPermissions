package im.wangchao.rxpermissions;

/**
 * <p>Description  : PermissionResult.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2017/11/2.</p>
 * <p>Time         : 下午7:05.</p>
 */
public class PermissionResult {
    public final String name;
    public final boolean granted;
    public final boolean shouldShowRequestPermissionRationale;

    public static PermissionResult newInstance(String name, boolean granted) {
        return new PermissionResult(name, granted, false);
    }

    public static PermissionResult newInstance(String name, boolean granted, boolean shouldShowRequestPermissionRationale){
        return new PermissionResult(name, granted, shouldShowRequestPermissionRationale);
    }

    private PermissionResult(String name, boolean granted, boolean shouldShowRequestPermissionRationale){
        this.name = name;
        this.granted = granted;
        this.shouldShowRequestPermissionRationale = shouldShowRequestPermissionRationale;
    }
}
