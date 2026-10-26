package id.sejutasantri.alhikam;

import android.content.Context;
import android.app.ActivityManager;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.*;
import com.android.billingclient.api.*;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Best-effort 3h check while app is not visible. No permanent service / wake lock. */
public class BackgroundBillingWorker extends Worker {
 private static final String PRODUCT="remove_ads_lifetime";
 private static final String VERIFY="https://sejutasantri-billing-verifier-654659401534.asia-southeast2.run.app/verify";
 private static final String UNIQUE="alhikam-background-billing-3h";
 public BackgroundBillingWorker(@NonNull Context context,@NonNull WorkerParameters params){super(context,params);}
 public static void schedule(Context context){
   Constraints constraints=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
   PeriodicWorkRequest request=new PeriodicWorkRequest.Builder(BackgroundBillingWorker.class,3,TimeUnit.HOURS)
      .setConstraints(constraints).build();
   WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE,ExistingPeriodicWorkPolicy.KEEP,request);
 }
 @NonNull @Override public Result doWork(){
   Context context=getApplicationContext();
   SharedPreferences prefs=context.getSharedPreferences("billing_background",Context.MODE_PRIVATE);
   // Skip if app is visibly in foreground, even during long reading sessions.
   ActivityManager.RunningAppProcessInfo info=new ActivityManager.RunningAppProcessInfo();
   ActivityManager.getMyMemoryState(info);
   if(info.importance==ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) return Result.success();
   BillingClient client=BillingClient.newBuilder(context).setListener((r,p)->{}).enablePendingPurchases(
      PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).build();
   try{
     CountDownLatch connect=new CountDownLatch(1);
     final boolean[] connected={false};
     client.startConnection(new BillingClientStateListener(){
       @Override public void onBillingSetupFinished(@NonNull BillingResult r){connected[0]=r.getResponseCode()==BillingClient.BillingResponseCode.OK;connect.countDown();}
       @Override public void onBillingServiceDisconnected(){connect.countDown();}
     });
     if(!connect.await(20,TimeUnit.SECONDS)||!connected[0])return Result.retry();
     CountDownLatch query=new CountDownLatch(1);
     final Purchase[] owned={null};
     final boolean[] ok={false};
     client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),(r,purchases)->{
       ok[0]=r.getResponseCode()==BillingClient.BillingResponseCode.OK;
       if(purchases!=null)for(Purchase purchase:purchases)
         if(purchase.getProducts().contains(PRODUCT)&&purchase.getPurchaseState()==Purchase.PurchaseState.PURCHASED){owned[0]=purchase;break;}
       query.countDown();
     });
     if(!query.await(20,TimeUnit.SECONDS)||!ok[0])return Result.retry();
     if(owned[0]==null){prefs.edit().putLong("checkedAt",System.currentTimeMillis()).putString("serverState","NO_LOCAL_PURCHASE").apply();return Result.success();}
     if(FirebaseApp.getApps(context).isEmpty()){
       JSONObject cfg;
       try(InputStream in=context.getAssets().open("firebase-billing-config.json")){
         ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[512];int n;
         while((n=in.read(buf))!=-1)out.write(buf,0,n);
         cfg=new JSONObject(out.toString("UTF-8"));
       }
       FirebaseOptions opts=new FirebaseOptions.Builder().setApiKey(cfg.getString("apiKey"))
          .setApplicationId(cfg.getString("applicationId")).setProjectId(cfg.getString("projectId")).build();
       FirebaseApp.initializeApp(context,opts);
     }
     FirebaseAuth auth=FirebaseAuth.getInstance();
     FirebaseUser user=auth.getCurrentUser();
     if(user==null)user=Tasks.await(auth.signInAnonymously(),20,TimeUnit.SECONDS).getUser();
     if(user==null)return Result.retry();
     String idToken=Tasks.await(user.getIdToken(false),20,TimeUnit.SECONDS).getToken();
     if(idToken==null)return Result.retry();
     HttpURLConnection conn=null;
     try{
       conn=(HttpURLConnection)new URL(VERIFY).openConnection();
       conn.setRequestMethod("POST");conn.setDoOutput(true);conn.setConnectTimeout(10000);conn.setReadTimeout(15000);
       conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
       conn.setRequestProperty("Authorization","Bearer "+idToken);
       JSONObject body=new JSONObject();body.put("packageName",context.getPackageName());body.put("productId",PRODUCT);body.put("purchaseToken",owned[0].getPurchaseToken());
       try(OutputStream out=conn.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
       if(conn.getResponseCode()!=200)return Result.retry();
       JSONObject reply;
       try(InputStream in=conn.getInputStream()){
         ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[1024];int n;
         while((n=in.read(buf))!=-1){if(out.size()+n>16384)return Result.failure();out.write(buf,0,n);}
         reply=new JSONObject(out.toString("UTF-8"));
       }
       if(!"GOOGLE_PLAY_VERIFIED".equals(reply.optString("status")))return Result.retry();
       // Do not grant/revoke entitlement invisibly from a worker. Record only a result;
       // MainActivity refreshes against Google Play on next resume and updates native ads.
       prefs.edit().putLong("checkedAt",System.currentTimeMillis())
          .putString("serverState",reply.optString("purchaseState","UNKNOWN"))
          .putBoolean("serverEntitled",reply.optBoolean("entitled",false)).apply();
       return Result.success();
     }finally{if(conn!=null)conn.disconnect();}
   }catch(Exception e){return Result.retry();}
   finally{client.endConnection();}
 }
}
