package id.sejutasantri.alhikam;
import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.view.*;
import android.webkit.*;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdListener;
import android.widget.FrameLayout;
import com.android.billingclient.api.*;
import java.util.*;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
public class MainActivity extends Activity {
 private static final String REMOVE_ADS_ID="remove_ads_lifetime";
 private BillingClient billingClient;
 private volatile boolean adsFree=false;
 private boolean billingConnected=false;
 private ProductDetails adFreeProduct;
 private void billingNotice(String message){runOnUiThread(()->{
   if(web!=null) web.evaluateJavascript("window.alHikamBillingNotice&&window.alHikamBillingNotice("+org.json.JSONObject.quote(message)+")",null);
 });}
 private void premiumChanged(boolean value){
   adsFree=value;
   runOnUiThread(()->{
     if(value){if(topBanner!=null)topBanner.setVisibility(View.GONE);if(bottomBanner!=null)bottomBanner.setVisibility(View.GONE);}
     if(web!=null)web.evaluateJavascript("window.alHikamPremiumChanged&&window.alHikamPremiumChanged("+value+")",null);
   });
 }
 private void startBilling(){
   billingClient=BillingClient.newBuilder(this)
    .setListener((result,purchases)->{
      if(result.getResponseCode()==BillingClient.BillingResponseCode.OK && purchases!=null) processPurchases(purchases);
      else if(result.getResponseCode()!=BillingClient.BillingResponseCode.USER_CANCELED) billingNotice("Pembelian belum berhasil: "+result.getDebugMessage());
    })
    .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
    .enableAutoServiceReconnection().build();
   billingClient.startConnection(new BillingClientStateListener(){
     @Override public void onBillingSetupFinished(BillingResult result){
       billingConnected=result.getResponseCode()==BillingClient.BillingResponseCode.OK;
       if(billingConnected){refreshPurchases();queryAdFreeProduct();}
     }
     @Override public void onBillingServiceDisconnected(){billingConnected=false;}
   });
 }
 private void refreshPurchases(){
   if(billingClient==null||!billingClient.isReady())return;
   QueryPurchasesParams params=QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build();
   billingClient.queryPurchasesAsync(params,(result,purchases)->{
     if(result.getResponseCode()==BillingClient.BillingResponseCode.OK)processPurchases(purchases);
   });
 }
 private void processPurchases(List<Purchase> purchases){
   boolean owned=false;
   for(Purchase purchase:purchases){
     if(!purchase.getProducts().contains(REMOVE_ADS_ID))continue;
     if(purchase.getPurchaseState()==Purchase.PurchaseState.PURCHASED){
       owned=true;
       // Production should verify purchase tokens via Play Developer API on a trusted backend.
       if(!purchase.isAcknowledged()){
         AcknowledgePurchaseParams params=AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
         billingClient.acknowledgePurchase(params,result->{
            if(result.getResponseCode()!=BillingClient.BillingResponseCode.OK)billingNotice("Pembelian diterima; konfirmasi Google Play sedang diproses.");
         });
       }
     } else if(purchase.getPurchaseState()==Purchase.PurchaseState.PENDING){billingNotice("Pembayaran masih menunggu konfirmasi Google Play.");}
   }
   premiumChanged(owned);
 }
 private void queryAdFreeProduct(){
   if(billingClient==null||!billingClient.isReady())return;
   QueryProductDetailsParams.Product product=QueryProductDetailsParams.Product.newBuilder().setProductId(REMOVE_ADS_ID).setProductType(BillingClient.ProductType.INAPP).build();
   billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(product)).build(),(result,details)->{
     adFreeProduct=null;
     if(result.getResponseCode()==BillingClient.BillingResponseCode.OK && !details.getProductDetailsList().isEmpty())adFreeProduct=details.getProductDetailsList().get(0);
     if(adFreeProduct!=null && adFreeProduct.getOneTimePurchaseOfferDetailsList()!=null && !adFreeProduct.getOneTimePurchaseOfferDetailsList().isEmpty()){
       String price=adFreeProduct.getOneTimePurchaseOfferDetailsList().get(0).getFormattedPrice();
       runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.alHikamBillingPrice&&window.alHikamBillingPrice("+org.json.JSONObject.quote(price)+")",null);});
     }
   });
 }
 private void purchaseAdFree(){
   if(adsFree){billingNotice("Mode bebas iklan sudah aktif.");return;}
   if(billingClient==null||!billingClient.isReady()){billingNotice("Google Play belum terhubung. Coba lagi setelah aplikasi terpasang lewat Google Play.");return;}
   if(adFreeProduct==null){queryAdFreeProduct();billingNotice("Produk belum tersedia. Pastikan produk sekali beli sudah aktif di Play Console.");return;}
   List<ProductDetails.OneTimePurchaseOfferDetails> offers=adFreeProduct.getOneTimePurchaseOfferDetailsList();
   if(offers==null||offers.isEmpty()){billingNotice("Penawaran pembelian belum tersedia.");return;}
   String token=offers.get(0).getOfferToken();
   BillingFlowParams.ProductDetailsParams params=BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(adFreeProduct).setOfferToken(token).build();
   BillingResult result=billingClient.launchBillingFlow(this,BillingFlowParams.newBuilder().setProductDetailsParamsList(Collections.singletonList(params)).build());
   if(result.getResponseCode()!=BillingClient.BillingResponseCode.OK)billingNotice("Google Play belum dapat membuka pembayaran: "+result.getDebugMessage());
 }
 private WebView web;
 private FrameLayout adsOverlay;
 private AdView bottomBanner;
 private boolean bottomBannerLoaded=false;
 private boolean bottomBannerFailed=false;
 private static final String TEST_BANNER_ID="ca-app-pub-3940256099942544/9214589741";
 private int adaptiveBannerHeightDp=0;
 private int adaptiveBannerWidthDp=0;
 private int deviceBannerWidthDp(){
   android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
   return Math.max(1,Math.round(metrics.widthPixels/metrics.density)-16);
 }
 private void updateBannerPosition(float cssLeft,float cssTop,float cssWidth,float cssHeight){
   if(adsFree || !bottomBannerLoaded || bottomBanner==null || adsOverlay==null || web==null || isFinishing())return;
   float scale=web.getScale();
   float density=getResources().getDisplayMetrics().density;
   int actualWidth=Math.round(adaptiveBannerWidthDp*density);
   int actualHeight=Math.round(adaptiveBannerHeightDp*density);
   int left=Math.round((cssLeft+cssWidth/2f)*scale)-actualWidth/2+web.getLeft();
   int top=Math.round(cssTop*scale)+web.getTop();
   FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(actualWidth,actualHeight,Gravity.TOP|Gravity.LEFT);
   lp.leftMargin=Math.max(0,left);lp.topMargin=Math.max(0,top);
   bottomBanner.setLayoutParams(lp);
   bottomBanner.setVisibility(View.VISIBLE);
 }
 private void loadBottomBanner(){
   if(adsFree || bottomBanner!=null || web==null || adsOverlay==null || isFinishing())return;
   adaptiveBannerWidthDp=deviceBannerWidthDp();
   AdSize adaptive=AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this,adaptiveBannerWidthDp);
   adaptiveBannerHeightDp=adaptive.getHeight();
   bottomBanner=new AdView(this);
   bottomBanner.setAdUnitId(TEST_BANNER_ID);
   bottomBanner.setAdSize(adaptive);
   bottomBanner.setVisibility(View.GONE);
   adsOverlay.addView(bottomBanner,new FrameLayout.LayoutParams(-2,-2));
   bottomBanner.setAdListener(new AdListener(){
     @Override public void onAdLoaded(){
       bottomBannerLoaded=true;bottomBannerFailed=false;
       if(web!=null)web.evaluateJavascript("window.alHikamNativeBannerState&&window.alHikamNativeBannerState(true,"+adaptiveBannerHeightDp+")",null);
     }
     @Override public void onAdFailedToLoad(LoadAdError error){
       bottomBannerLoaded=false;bottomBannerFailed=true;
       bottomBanner.setVisibility(View.GONE);
       if(web!=null)web.evaluateJavascript("window.alHikamNativeBannerState&&window.alHikamNativeBannerState(false,0)",null);
     }
   });
   bottomBanner.loadAd(new AdRequest.Builder().build());
 }

 // v1.1.1: NO inline scrolling native ads; two anchored native banners only.
 private AdView topBanner;
 private boolean topBannerLoaded=false;
 private int topBannerHeightDp=0;
 private void updateTopBannerPosition(float cssLeft,float cssTop,float cssWidth,float cssHeight){
   if(adsFree || topBanner==null || adsOverlay==null || web==null || !topBannerLoaded || isFinishing())return;
   float scale=web.getScale();
   float density=getResources().getDisplayMetrics().density;
   int width=Math.round(adaptiveBannerWidthDp*density);
   int height=Math.round(topBannerHeightDp*density);
   int left=Math.round((cssLeft+cssWidth/2)*scale)-width/2+web.getLeft();
   int top=Math.round(cssTop*scale)+web.getTop();
   FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(width,height,Gravity.TOP|Gravity.LEFT);
   lp.leftMargin=Math.max(0,left);lp.topMargin=Math.max(0,top);
   topBanner.setLayoutParams(lp);topBanner.setVisibility(View.VISIBLE);
 }
 private void loadTopBanner(){
   if(adsFree || topBanner!=null || adsOverlay==null || web==null || isFinishing())return;
   if(adaptiveBannerWidthDp==0)adaptiveBannerWidthDp=deviceBannerWidthDp();
   AdSize size=AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this,adaptiveBannerWidthDp);
   topBannerHeightDp=size.getHeight();
   topBanner=new AdView(this);topBanner.setAdUnitId(TEST_BANNER_ID);topBanner.setAdSize(size);
   topBanner.setVisibility(View.GONE);adsOverlay.addView(topBanner,new FrameLayout.LayoutParams(-2,-2));
   topBanner.setAdListener(new AdListener(){
     @Override public void onAdLoaded(){topBannerLoaded=true;
       if(web!=null)web.evaluateJavascript("window.alHikamNativeTopBannerState&&window.alHikamNativeTopBannerState(true,"+topBannerHeightDp+")",null);
     }
     @Override public void onAdFailedToLoad(LoadAdError error){topBannerLoaded=false;topBanner.setVisibility(View.GONE);
       if(web!=null)web.evaluateJavascript("window.alHikamNativeTopBannerState&&window.alHikamNativeTopBannerState(false,0)",null);
     }
   });topBanner.loadAd(new AdRequest.Builder().build());
 }
 private InterstitialAd interstitialAd;
 private boolean interstitialShowing=false;
 private long lastInterstitialShownAt=System.currentTimeMillis();
 private static final long INTERSTITIAL_COOLDOWN_MS=180000L;
 // Official Google test unit; NEVER live monetization during QA.
 private static final String TEST_INTERSTITIAL_ID="ca-app-pub-3940256099942544/1033173712";
 private void loadInterstitial(){
   if(adsFree || isFinishing() || interstitialAd!=null || interstitialShowing)return;
   InterstitialAd.load(this, TEST_INTERSTITIAL_ID, new AdRequest.Builder().build(),new InterstitialAdLoadCallback(){
     @Override public void onAdLoaded(InterstitialAd ad){interstitialAd=ad;}
     @Override public void onAdFailedToLoad(LoadAdError error){interstitialAd=null;}
   });
 }
 private void resumeWebNavigation(){
   if(web!=null)web.evaluateJavascript("window.alHikamNativeInterstitialFinished&&window.alHikamNativeInterstitialFinished()",null);
 }
 private boolean isNativeInterstitialAvailable(){
   return !adsFree && interstitialAd!=null && !interstitialShowing &&
       System.currentTimeMillis()-lastInterstitialShownAt>=INTERSTITIAL_COOLDOWN_MS;
 }
 private void showNativeInterstitial(){
   InterstitialAd ad=interstitialAd;
   if(ad==null || adsFree){resumeWebNavigation();return;}
   interstitialAd=null;interstitialShowing=true;
   ad.setFullScreenContentCallback(new FullScreenContentCallback(){
     @Override public void onAdShowedFullScreenContent(){lastInterstitialShownAt=System.currentTimeMillis();}
     @Override public void onAdDismissedFullScreenContent(){interstitialShowing=false;resumeWebNavigation();loadInterstitial();}
     @Override public void onAdFailedToShowFullScreenContent(AdError error){interstitialShowing=false;resumeWebNavigation();loadInterstitial();}
   });
   ad.show(this);
 }

 private android.widget.FrameLayout frameForSystemBars;
 private View statusBarBacking;
 private View bottomBarBacking;
 private int activeStatusColor=Color.rgb(250,246,238);
 private int bottomInsetPx=0;
 private static final int BOTTOM_COLOR=Color.rgb(48,70,52);
 private static final int NOTIFICATION_REQUEST=19;
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  startBilling();
  // Inisialisasi SDK saja. TIDAK meminta iklan aktif pada tahap ini.
  // Consent pengguna harus disiapkan sebelum pemuatan iklan nyata.
  new Thread(() -> MobileAds.initialize(this, status -> runOnUiThread(()->{
    loadInterstitial();
    loadBottomBanner();
    loadTopBanner();
  }))).start();
  getWindow().setStatusBarColor(Color.rgb(250,246,238));
  getWindow().setNavigationBarColor(Color.TRANSPARENT);
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
      v.evaluateJavascript("document.documentElement.classList.add('android-native');document.documentElement.style.setProperty('--android-bottom-inset','"+bottomInsetPx+"px')",null);
    }
  });
  web.addJavascriptInterface(new Object(){
   // V1.1.1: read-only entitlement contract. Must be replaced ONLY after integrating
   // Google Play Billing verified purchases; no JS method is allowed to set it.
   @JavascriptInterface public boolean isAdsFree(){return adsFree;}
   @JavascriptInterface public void buyAdsFree(){runOnUiThread(()->purchaseAdFree());}
   @JavascriptInterface public void restoreAdsFree(){runOnUiThread(()->{refreshPurchases();billingNotice("Sedang memeriksa pembelian Google Play...");});}
   @JavascriptInterface public void positionBottomBanner(double l,double t,double w,double h){
     runOnUiThread(()->updateBannerPosition((float)l,(float)t,(float)w,(float)h));
   }
   @JavascriptInterface public void positionTopBanner(double l,double t,double w,double h){
     runOnUiThread(()->updateTopBannerPosition((float)l,(float)t,(float)w,(float)h));
   }
   @JavascriptInterface public void hideTopBanner(){
     runOnUiThread(()->{if(topBanner!=null)topBanner.setVisibility(View.GONE);});
   }
   @JavascriptInterface public void hideBottomBanner(){
     runOnUiThread(()->{if(bottomBanner!=null)bottomBanner.setVisibility(View.GONE);});
   }
   // Synchronous readiness gate; no ad means navigation stays immediate.
   @JavascriptInterface public boolean nativeInterstitialReady(){return isNativeInterstitialAvailable();}
   @JavascriptInterface public void showNativeInterstitial(){
      runOnUiThread(()->MainActivity.this.showNativeInterstitial());
   }
   @JavascriptInterface public void setBottomBarColor(String hex){
     runOnUiThread(()->{
       try{
         if(hex==null || !hex.matches("#[0-9a-fA-F]{6}"))return;
         int c=Color.parseColor(hex);
         if(bottomBarBacking!=null)bottomBarBacking.setBackgroundColor(c);
         getWindow().setNavigationBarColor(Color.TRANSPARENT);
       }catch(Exception ignored){}
     });
   }

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
          flags&=~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
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
  adsOverlay=new FrameLayout(this);
  adsOverlay.setClickable(false);
  frame.addView(adsOverlay,new FrameLayout.LayoutParams(-1,-1));
  statusBarBacking=new View(this);
  statusBarBacking.setBackgroundColor(activeStatusColor);
  frame.addView(statusBarBacking,new android.widget.FrameLayout.LayoutParams(-1,0,Gravity.TOP));
  bottomBarBacking=new View(this);
  bottomBarBacking.setBackgroundColor(BOTTOM_COLOR);
  frame.addView(bottomBarBacking,0,new android.widget.FrameLayout.LayoutParams(-1,0,Gravity.BOTTOM));
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
    bottomInsetPx=bottom;
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
      View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
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
 @Override protected void onPause(){if(bottomBanner!=null)bottomBanner.pause();if(topBanner!=null)topBanner.pause();super.onPause();}
 @Override protected void onResume(){super.onResume();refreshPurchases();if(bottomBanner!=null)bottomBanner.resume();if(topBanner!=null)topBanner.resume();}
 @Override protected void onDestroy(){if(billingClient!=null){billingClient.endConnection();billingClient=null;}if(topBanner!=null){topBanner.destroy();topBanner=null;}if(bottomBanner!=null){bottomBanner.destroy();bottomBanner=null;}if(web!=null){web.destroy();web=null;}super.onDestroy();}
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
