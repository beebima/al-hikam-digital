package id.sejutasantri.alhikam;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
public class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
   if(Build.VERSION.SDK_INT<33 || c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
     ReminderScheduler.schedule(c);
 }
}
