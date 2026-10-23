package id.sejutasantri.alhikam;
import android.app.*;
import android.content.*;
import android.os.Build;
import java.util.Calendar;
public class ReminderScheduler {
 static void schedule(Context c){
   scheduleOne(c,5,0,501);
   scheduleOne(c,17,0,1701);
 }
 static void scheduleOne(Context c,int hour,int minute,int requestCode){
   Calendar when=Calendar.getInstance();
   when.set(Calendar.HOUR_OF_DAY,hour);when.set(Calendar.MINUTE,minute);when.set(Calendar.SECOND,0);when.set(Calendar.MILLISECOND,0);
   if(when.getTimeInMillis()<=System.currentTimeMillis())when.add(Calendar.DAY_OF_YEAR,1);
   Intent intent=new Intent(c,ReminderReceiver.class).putExtra("hour",hour).putExtra("code",requestCode);
   int flags=PendingIntent.FLAG_UPDATE_CURRENT;
   if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
   PendingIntent pending=PendingIntent.getBroadcast(c,requestCode,intent,flags);
   AlarmManager alarms=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
   if(alarms!=null)alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pending);
 }
}
