package id.sejutasantri.alhikam;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class ReminderReceiver extends android.content.BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){
   int hour=i.getIntExtra("hour",5);
   int code=i.getIntExtra("code",hour==5?501:1701);
   // Always schedule tomorrow, even if the user has disabled notification display.
   ReminderScheduler.scheduleOne(c,hour,0,code);
   if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
   try{
     String slot=hour==5?"morning":"evening";
     JSONArray messages=new JSONArray(readAsset(c,"notifications.json"));
     ArrayList<JSONObject> pool=new ArrayList<>();
     for(int n=0;n<messages.length();n++){JSONObject o=messages.getJSONObject(n);if(slot.equals(o.optString("slot")))pool.add(o);}
     if(pool.isEmpty())return;
     android.content.SharedPreferences prefs=c.getSharedPreferences("hikam_reminders",Context.MODE_PRIVATE);
     String key="queue_"+slot;
     JSONArray queue;
     try{queue=new JSONArray(prefs.getString(key,"[]"));}catch(Exception ex){queue=new JSONArray();}
     if(queue.length()==0){
       ArrayList<Integer> ids=new ArrayList<>();
       for(JSONObject o:pool)ids.add(o.getInt("id"));
       Collections.shuffle(ids);
       for(Integer id:ids)queue.put(id);
     }
     int chosen=queue.getInt(0);
     JSONArray remaining=new JSONArray();
     for(int n=1;n<queue.length();n++)remaining.put(queue.getInt(n));
     prefs.edit().putString(key,remaining.toString()).apply();
     JSONObject msg=null;
     for(JSONObject o:pool)if(o.getInt("id")==chosen){msg=o;break;}
     if(msg==null)return;
     NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
     String channel="hikam_learning_reminders";
     if(Build.VERSION.SDK_INT>=26){
       NotificationChannel nc=new NotificationChannel(channel,"Pengingat Belajar Al-Hikam",NotificationManager.IMPORTANCE_DEFAULT);
       nm.createNotificationChannel(nc);
     }
     Intent launch=new Intent(c,ComposeMainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
     int flags=PendingIntent.FLAG_UPDATE_CURRENT;
     if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
     PendingIntent tap=PendingIntent.getActivity(c,code,launch,flags);
     Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,channel):new Notification.Builder(c);
     builder.setSmallIcon(R.drawable.ic_notification)
       .setContentTitle(msg.optString("title"))
       .setContentText(msg.optString("body"))
       .setStyle(new Notification.BigTextStyle().bigText(msg.optString("body")))
       .setContentIntent(tap).setAutoCancel(true);
     nm.notify(code,builder.build());
   }catch(Exception ignored){}
 }
 private String readAsset(Context c,String path)throws IOException{
   try(InputStream stream=c.getAssets().open(path);ByteArrayOutputStream out=new ByteArrayOutputStream()){
     byte[] buf=new byte[4096];int n;
     while((n=stream.read(buf))!=-1)out.write(buf,0,n);
     return new String(out.toByteArray(),StandardCharsets.UTF_8);
   }
 }
}
