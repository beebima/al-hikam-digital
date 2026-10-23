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
 private View statusBarBacking;
 private View bottomBarBacking;
 private int activeStatusColor=Color.rgb(250,246,238);
 private static final int BOTTOM_COLOR=Color.rgb(234,247,240);
 private static final int NOTIFICATION_REQUEST=19;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  getWindow().setStatusBarColor(Color.rgb(250,246,238));
  getWindow().setNavigationBarColor(BOTTOM_COLOR);
  if(Build.VERSION.SDK_INT>=29){
   getWindow().setStatusBarContrastEnforced(false);
   getWindow().setNavigationBarContrastEnforced(false);
  }
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
          activeStatusColor=color;
          getWindow().setStatusBarColor(color);
          if(statusBarBacking!=null)statusBarBacking.setBackgroundColor(color);
          int flags=getWindow().getDecorView().getSystemUiVisibility();
          if(splash)flags|=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
          else flags&=~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
          flags|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
          getWindow().getDecorView().setSystemUiVisibility(flags);
          if(frameForSystemBars!=null)frameForSystemBars.setBackgroundColor(BOTTOM_COLOR);
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
  // Android 15 enforces edge-to-edge for targetSdk 35.
  // Paint separate inset surfaces; never leave transparent/white strips.
  android.widget.FrameLayout frame=new android.widget.FrameLayout(this);
  frame.setBackgroundColor(BOTTOM_COLOR);
  frameForSystemBars=frame;
  frame.addView(web,new android.widget.FrameLayout.LayoutParams(-1,-1));
  statusBarBacking=new View(this);
  statusBarBacking.setBackgroundColor(activeStatusColor);
  frame.addView(statusBarBacking,new android.widget.FrameLayout.LayoutParams(-1,0,Gravity.TOP));
  bottomBarBacking=new View(this);
  bottomBarBacking.setBackgroundColor(BOTTOM_COLOR);
  frame.addView(bottomBarBacking,new android.widget.FrameLayout.LayoutParams(-1,0,Gravity.BOTTOM));
  frame.setOnApplyWindowInsetsListener((view,insets)->{
    int top,left,right,bottom;
    if(Build.VERSION.SDK_INT>=30){
      android.graphics.Insets st=insets.getInsets(WindowInsets.Type.statusBars()|WindowInsets.Type.displayCutout());
      android.graphics.Insets nav=insets.getInsets(WindowInsets.Type.navigationBars());
      top=st.top;left=Math.max(st.left,nav.left);right=Math.max(st.right,nav.right);bottom=nav.bottom;
    }else{
      top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();
      left=insets.getSystemWindowInsetLeft();right=insets.getSystemWindowInsetRight();
    }
    android.widget.FrameLayout.LayoutParams page=(android.widget.FrameLayout.LayoutParams)web.getLayoutParams();
    // Let the WebView extend to the bottom edge, including the system navigation inset.
    // The HTML banner adds safe padding for gesture/buttons and the nav follows it.
    page.setMargins(left,top,right,0);
    final int bottomInsetPx=bottom;
    web.post(()->web.evaluateJavascript(
      "document.documentElement.style.setProperty('--android-bottom-inset', '"+bottomInsetPx+"px')",null));
    web.setLayoutParams(page);
    android.widget.FrameLayout.LayoutParams status=(android.widget.FrameLayout.LayoutParams)statusBarBacking.getLayoutParams();
    status.height=top;statusBarBacking.setLayoutParams(status);
    android.widget.FrameLayout.LayoutParams nav=(android.widget.FrameLayout.LayoutParams)bottomBarBacking.getLayoutParams();
    nav.height=bottom;bottomBarBacking.setLayoutParams(nav);
    return insets;
  });
  setContentView(frame);
  if(Build.VERSION.SDK_INT>=26){
    getWindow().getDecorView().setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  }
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
