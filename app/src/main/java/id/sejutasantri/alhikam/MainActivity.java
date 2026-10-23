package id.sejutasantri.alhikam;
import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.webkit.*;
public class MainActivity extends Activity {
  private WebView web;
  private static final int NOTIFICATION_REQUEST=19;
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    web=new WebView(this);
    web.getSettings().setJavaScriptEnabled(true);
    web.getSettings().setDomStorageEnabled(true);
    web.getSettings().setAllowFileAccess(true);
    web.getSettings().setAllowContentAccess(false);
    web.getSettings().setAllowFileAccessFromFileURLs(false);
    web.getSettings().setAllowUniversalAccessFromFileURLs(false);
    web.setWebViewClient(new WebViewClient());
    web.addJavascriptInterface(new Object(){
      @JavascriptInterface public void requestNotificationPermission(){
        runOnUiThread(()->{
          if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATION_REQUEST);
          else if(Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
            ReminderScheduler.schedule(MainActivity.this);
        });
      }
    },"AlHikamAndroid");
    setContentView(web);
    web.loadUrl("file:///android_asset/index.html");
    if(Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
      ReminderScheduler.schedule(this);
  }
  @Override public void onRequestPermissionsResult(int req,String[] p,int[] results){
    super.onRequestPermissionsResult(req,p,results);
    if(req==NOTIFICATION_REQUEST && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED)
      ReminderScheduler.schedule(this);
  }
  @Override public void onBackPressed(){
    if(web.canGoBack())web.goBack();else super.onBackPressed();
  }
}
