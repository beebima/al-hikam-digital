package id.sejutasantri.alhikam;
import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.*;
import android.webkit.*;
public class MainActivity extends Activity {
 private WebView web;
 private static final int NOTIFICATION_REQUEST=19;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setStatusBarColor(Color.rgb(22,65,53));
  getWindow().setNavigationBarColor(Color.rgb(22,65,53));
  getWindow().getDecorView().setSystemUiVisibility(0);
  web=new WebView(this);
  web.setBackgroundColor(Color.rgb(250,246,238));
  web.setLayerType(View.LAYER_TYPE_HARDWARE,null);
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setAllowFileAccess(true);
  web.getSettings().setAllowContentAccess(false);
  web.getSettings().setAllowFileAccessFromFileURLs(false);
  web.getSettings().setAllowUniversalAccessFromFileURLs(false);
  web.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
  web.setWebViewClient(new WebViewClient(){
    @Override public void onPageFinished(WebView v,String url){
      v.evaluateJavascript("document.documentElement.classList.add('android-native')",null);
    }
  });
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
  // Explicitly respect Android status/navigation system bars, including Android 15 edge-to-edge.
  android.widget.FrameLayout frame=new android.widget.FrameLayout(this);
  frame.addView(web,new android.widget.FrameLayout.LayoutParams(-1,-1));
  frame.setOnApplyWindowInsetsListener((view,insets)->{
   android.graphics.Insets bars;
   if(Build.VERSION.SDK_INT>=30){
    bars=insets.getInsets(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars()|WindowInsets.Type.displayCutout());
    view.setPadding(bars.left,bars.top,bars.right,bars.bottom);
   }else{
    view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
   }
   return insets;
  });
  setContentView(frame);
  web.loadUrl("file:///android_asset/index.html");
  if(Build.VERSION.SDK_INT<33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
   ReminderScheduler.schedule(this);
 }
 @Override public void onRequestPermissionsResult(int req,String[] p,int[] results){
  super.onRequestPermissionsResult(req,p,results);
  if(req==NOTIFICATION_REQUEST && results.length>0 && results[0]==PackageManager.PERMISSION_GRANTED)
   ReminderScheduler.schedule(this);
 }
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
