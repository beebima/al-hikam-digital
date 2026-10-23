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
 private android.widget.FrameLayout frameForSystemBars;
 private static final int NOTIFICATION_REQUEST=19;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setStatusBarColor(Color.rgb(250,246,238));
  getWindow().setNavigationBarColor(Color.rgb(22,65,53));
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
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
   @JavascriptInterface public void setSystemBarColor(String hex,boolean splash){
      runOnUiThread(()->{
        try{
          if(hex==null || !hex.matches("#[0-9a-fA-F]{6}"))return;
          int color=Color.parseColor(hex);
          getWindow().setStatusBarColor(color);
          int flags=getWindow().getDecorView().getSystemUiVisibility();
          if(splash)flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
          else flags&=~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
          getWindow().getDecorView().setSystemUiVisibility(flags);
          if(frameForSystemBars!=null)frameForSystemBars.setBackgroundColor(color);
        }catch(Exception ignored){}
      });
    }
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
  frame.setBackgroundColor(Color.rgb(250,246,238));
  frameForSystemBars=frame;
  frame.addView(web,new android.widget.FrameLayout.LayoutParams(-1,-1));
  frame.setOnApplyWindowInsetsListener((view,insets)->{
   android.graphics.Insets bars;
   if(Build.VERSION.SDK_INT>=30){
    bars=insets.getInsets(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars()|WindowInsets.Type.displayCutout());
    view.setPadding(bars.left,bars.top,bars.right,bars.bottom);
    // Keep the view backing synchronized with the active system-bar color.
    view.setBackgroundColor(getWindow().getStatusBarColor());
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
