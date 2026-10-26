package id.sejutasantri.alhikam;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
public class MainActivity extends Activity {
 private static final String REMOVE_ADS_ID="remove_ads_lifetime";
 private BillingClient billingClient;
 // Firebase-authenticated HTTPS endpoint. Server validates Firebase ID token and Google Play purchase.
 // Network errors do not revoke an existing purchase.
 private static final String VERIFY_URL="https://sejutasantri-billing-verifier-654659401534.asia-southeast2.run.app/verify";
 private FirebaseAuth firebaseAuth=null;
 private volatile String firebaseSetup="Firebase belum dikonfigurasi";
 private void initFirebaseBilling(){
   try{
     org.json.JSONObject cfg;
     try(InputStream in=getAssets().open("firebase-billing-config.json")){
       ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] b=new byte[512];int n;
       while((n=in.read(b))!=-1){if(out.size()+n>4096)throw new Exception("config too large");out.write(b,0,n);}
       cfg=new org.json.JSONObject(out.toString("UTF-8"));
     }
     String apiKey=cfg.optString("apiKey","");
     String appId=cfg.optString("applicationId","");
     String projectId=cfg.optString("projectId","");
     if(apiKey.isEmpty()||appId.isEmpty()||projectId.isEmpty()||apiKey.startsWith("REPLACE_"))return;
     if(FirebaseApp.getApps(this).isEmpty()){
       FirebaseOptions opts=new FirebaseOptions.Builder().setApiKey(apiKey).setApplicationId(appId).setProjectId(projectId).build();
       FirebaseApp.initializeApp(this,opts);
     }
     firebaseAuth=FirebaseAuth.getInstance();
     firebaseSetup="Firebase siap";
   }catch(Exception e){firebaseSetup="Firebase belum dikonfigurasi";}
 }
 private void sendFirebaseAuthenticatedVerification(String purchaseToken){
   if(firebaseAuth==null){purchaseDiagnostic="Google Play lokal aktif; verifikasi server menunggu konfigurasi Firebase";return;}
   FirebaseUser current=firebaseAuth.getCurrentUser();
   if(current==null){
     firebaseAuth.signInAnonymously().addOnCompleteListener(task->{
       if(task.isSuccessful())sendFirebaseAuthenticatedVerification(purchaseToken);
       else purchaseDiagnostic="Google Play lokal aktif; autentikasi Firebase belum tersedia";
     });
     return;
   }
   current.getIdToken(false).addOnCompleteListener(task->{
     if(task.isSuccessful() && task.getResult()!=null && task.getResult().getToken()!=null)
       verifyPurchaseWithIdToken(purchaseToken,task.getResult().getToken());
     else purchaseDiagnostic="Google Play lokal aktif; token Firebase tidak tersedia";
   });
 }
 // Foreground reconciliation every 9 minutes while open.
 // Background reconciliation handled separately by PeriodicWorkRequest every 3 hours (best effort).
 private static final long PURCHASE_CHECK_INTERVAL_MS=9L*60L*1000L;
 private final Handler billingRefreshHandler=new Handler(Looper.getMainLooper());
 private volatile long lastAutoPurchaseCheckAt=0L;
 private boolean billingForeground=false;
 private final Runnable foregroundBillingRefresh=new Runnable(){
   @Override public void run(){
     if(!billingForeground)return;
     long now=System.currentTimeMillis();
     if(billingClient!=null && billingClient.isReady() && now-lastAutoPurchaseCheckAt>=PURCHASE_CHECK_INTERVAL_MS)
       refreshPurchases(false);
     scheduleForegroundBillingRefresh();
   }
 };
 private void scheduleForegroundBillingRefresh(){
   billingRefreshHandler.removeCallbacks(foregroundBillingRefresh);
   if(!billingForeground)return;
   long elapsed=System.currentTimeMillis()-lastAutoPurchaseCheckAt;
   long delay=lastAutoPurchaseCheckAt==0L?PURCHASE_CHECK_INTERVAL_MS:Math.max(1000L,PURCHASE_CHECK_INTERVAL_MS-elapsed);
   billingRefreshHandler.postDelayed(foregroundBillingRefresh,delay);
 }
 private volatile String deniedPurchaseToken=null;
 private volatile String lastVerificationToken=null;
 private volatile boolean verificationInFlight=false;
 private volatile long lastSuccessfulVerificationAt=0L;
 private volatile int verificationSequence=0;
 private volatile boolean adsFree=false;
 private boolean adsSdkReady=false;
 private volatile String purchaseDiagnostic="Pemeriksaan belum selesai";
 private boolean billingConnected=false;
 private boolean purchaseLookupInProgress=false;
 private ProductDetails adFreeProduct;
 private volatile String lastBillingMessage="Memeriksa layanan Google Play…";
 private void billingNotice(String message){
   lastBillingMessage=message;
   runOnUiThread(()->{
     if(web!=null) web.evaluateJavascript("window.alHikamBillingNotice&&window.alHikamBillingNotice("+org.json.JSONObject.quote(message)+")",null);
     // Billing notices are shown only in the Ads-Free card, never as disruptive Toasts.
   });
 }
 private void premiumChanged(boolean value){
   boolean changed=adsFree!=value;
   adsFree=value;
   runOnUiThread(()->{
     if(value){
       if(topBanner!=null)topBanner.setVisibility(View.GONE);
       if(bottomBanner!=null)bottomBanner.setVisibility(View.GONE);
       if(interstitialAd!=null)interstitialAd=null;
     }else if(adsSdkReady && (changed || topBanner==null || bottomBanner==null)){
       // Also recover banners when free state is unchanged but AdViews were never created.
       if(topBanner!=null){topBanner.destroy();topBanner=null;topBannerLoaded=false;}
       if(bottomBanner!=null){bottomBanner.destroy();bottomBanner=null;bottomBannerLoaded=false;}
       loadTopBanner();loadBottomBanner();loadInterstitial();
     }
     if(web!=null)web.evaluateJavascript("window.alHikamPremiumChanged&&window.alHikamPremiumChanged("+value+")",null);
   });
 }
 private void startBilling(){
   billingClient=BillingClient.newBuilder(this)
    .setListener((result,purchases)->{
      if(result.getResponseCode()==BillingClient.BillingResponseCode.OK && purchases!=null) {
        processPurchases(purchases);
        // Reconcile against all currently owned products, not only the update event.
        refreshPurchases(false);
      }
      else if(result.getResponseCode()!=BillingClient.BillingResponseCode.USER_CANCELED) billingNotice("Pembelian belum berhasil: "+result.getDebugMessage());
    })
    .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
    .enableAutoServiceReconnection().build();
   billingClient.startConnection(new BillingClientStateListener(){
     @Override public void onBillingSetupFinished(BillingResult result){
       billingConnected=result.getResponseCode()==BillingClient.BillingResponseCode.OK;
       if(billingConnected){billingNotice("Google Play Billing terhubung. Memeriksa produk…");refreshPurchases(false);queryAdFreeProduct();}
       else billingNotice("Google Play Billing belum terhubung: "+result.getDebugMessage()+" (kode "+result.getResponseCode()+").");
     }
     @Override public void onBillingServiceDisconnected(){billingConnected=false;}
   });
 }
 private void refreshPurchases(boolean userRequested){
   if(billingClient==null||!billingClient.isReady()){
     if(userRequested)billingNotice("Layanan Google Play belum siap. Pastikan Play Store aktif, internet tersambung, lalu buka ulang aplikasi dan coba lagi.");
     return;
   }
   if(purchaseLookupInProgress){if(userRequested)billingNotice("Pemeriksaan pembelian sedang berlangsung. Coba sebentar lagi.");return;}
   purchaseLookupInProgress=true;
   // On each real Play Billing query, advance the foreground-only refresh clock.
   lastAutoPurchaseCheckAt=System.currentTimeMillis();
   if(billingForeground)scheduleForegroundBillingRefresh();
   if(userRequested)lastSuccessfulVerificationAt=0L;
   QueryPurchasesParams params=QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build();
   billingClient.queryPurchasesAsync(params,(result,purchases)->{
     purchaseLookupInProgress=false;
     if(result.getResponseCode()==BillingClient.BillingResponseCode.OK){
       int count=0; if(purchases!=null)for(Purchase p:purchases)if(p.getProducts().contains(REMOVE_ADS_ID) && p.getPurchaseState()==Purchase.PurchaseState.PURCHASED)count++;
       purchaseDiagnostic="Google Play: " + count + " pembelian aktif; sinkronisasi berhasil";
       processPurchases(purchases == null ? Collections.emptyList() : purchases);
       if(userRequested){
         boolean found=false;
         if(purchases!=null)for(Purchase purchase:purchases){
           if(purchase.getProducts().contains(REMOVE_ADS_ID) && purchase.getPurchaseState()==Purchase.PurchaseState.PURCHASED){found=true;break;}
         }
         billingNotice(found ? "Pembelian Ads-Free ditemukan dan dipulihkan." : "Belum ditemukan pembelian Ads-Free pada akun Google Play ini. Jika sudah membeli, pastikan memakai akun yang sama.");
       }
     }else{
       purchaseDiagnostic="Pemeriksaan Google Play gagal: " + result.getResponseCode();
       if(userRequested) billingNotice("Gagal memeriksa riwayat pembelian Google Play: "+result.getDebugMessage()+" (kode "+result.getResponseCode()+").");
     }
   });
 }
 private void processPurchases(List<Purchase> purchases){
   boolean owned=false;
   for(Purchase purchase:purchases){
     if(!purchase.getProducts().contains(REMOVE_ADS_ID))continue;
     if(purchase.getPurchaseState()==Purchase.PurchaseState.PURCHASED){
       if(!purchase.getPurchaseToken().equals(deniedPurchaseToken)) owned=true;
       sendFirebaseAuthenticatedVerification(purchase.getPurchaseToken());
       // Locally owned purchase is only provisional until server verification completes.
       if(!purchase.isAcknowledged()){
         AcknowledgePurchaseParams params=AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
         billingClient.acknowledgePurchase(params,result->{
            if(result.getResponseCode()!=BillingClient.BillingResponseCode.OK)
              billingNotice("Pembelian diterima, tetapi acknowledgement gagal (kode "+result.getResponseCode()+"): "+result.getDebugMessage());
            else billingNotice("Google Play: acknowledgement pembelian berhasil.");
         });
       }
     } else if(purchase.getPurchaseState()==Purchase.PurchaseState.PENDING){billingNotice("Pembayaran masih menunggu konfirmasi Google Play.");}
   }
   if(!owned && (purchases==null || purchases.isEmpty())){
     deniedPurchaseToken=null;
     lastVerificationToken=null;
   }
   premiumChanged(owned);
 }
 /**
  * Server-assisted reconciliation. No secret embedded in the APK. A signed Google Play
  * purchase token is obtained only from BillingClient and sent to the fixed HTTPS host.
  * Only an explicit verified denial can remove locally reported entitlement.
  * Network errors, 401/403 (private Cloud Run), and 5xx preserve local entitlement.
  */
 private void verifyPurchaseWithIdToken(String token,String idToken){
   if(token==null || token.length()<8) return;
   // A previously denied token MUST be rechecked: refunds may be reversed or entitlement restored.
   // Avoid repeated API calls between foreground checks, but never cache a denial forever.
   if(token.equals(lastVerificationToken) && lastSuccessfulVerificationAt>0L &&
       System.currentTimeMillis()-lastSuccessfulVerificationAt<PURCHASE_CHECK_INTERVAL_MS)return;
   if(verificationInFlight && token.equals(lastVerificationToken)) return;
   verificationInFlight=true;
   lastVerificationToken=token;
   final int sequence=++verificationSequence;
   new Thread(()->{
     HttpURLConnection conn=null;
     try {
       URL url=new URL(VERIFY_URL);
       conn=(HttpURLConnection)url.openConnection();
       conn.setRequestMethod("POST");
       conn.setConnectTimeout(7000);
       conn.setReadTimeout(12000);
       conn.setDoOutput(true);
       conn.setRequestProperty("Content-Type","application/json; charset=utf-8");
       conn.setRequestProperty("Accept","application/json");
       conn.setRequestProperty("Authorization","Bearer "+idToken);
       org.json.JSONObject body=new org.json.JSONObject();
       body.put("packageName",getPackageName());
       body.put("productId",REMOVE_ADS_ID);
       body.put("purchaseToken",token);
       byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
       try(OutputStream out=conn.getOutputStream()){out.write(bytes);}
       int status=conn.getResponseCode();
       if(status==200){
         InputStream stream=conn.getInputStream();
         ByteArrayOutputStream buffer=new ByteArrayOutputStream();
         byte[] chunk=new byte[2048];int n;
         while((n=stream.read(chunk))!=-1){if(buffer.size()+n>16384)throw new Exception("Oversized response");buffer.write(chunk,0,n);}
         stream.close();
         org.json.JSONObject reply=new org.json.JSONObject(buffer.toString("UTF-8"));
         if(!"GOOGLE_PLAY_VERIFIED".equals(reply.optString("status")))throw new Exception("Unexpected verification status");
         boolean entitled=reply.optBoolean("entitled",false);
         String state=reply.optString("purchaseState","UNKNOWN");
         synchronized(this){
           if(sequence!=verificationSequence)return;
           deniedPurchaseToken=entitled?null:token;
           lastSuccessfulVerificationAt=System.currentTimeMillis();
           verificationInFlight=false;
         }
         purchaseDiagnostic="Server Play API: "+state+(entitled?"; PREMIUM terverifikasi":"; akses Ads-Free tidak valid");
         premiumChanged(entitled);
       }else{
         // 401/403 are expected while the Cloud Run service remains IAM-private.
         synchronized(this){if(sequence==verificationSequence)verificationInFlight=false;}
         purchaseDiagnostic="Google Play lokal aktif; verifikasi server belum tersedia (HTTP "+status+")";
       }
     } catch(Exception e){
       synchronized(this){if(sequence==verificationSequence)verificationInFlight=false;}
       purchaseDiagnostic="Google Play lokal aktif; verifikasi server tertunda";
     } finally{if(conn!=null)conn.disconnect();}
   },"play-purchase-verifier").start();
 }
 private void queryAdFreeProduct(){
   if(billingClient==null||!billingClient.isReady())return;
   QueryProductDetailsParams.Product product=QueryProductDetailsParams.Product.newBuilder().setProductId(REMOVE_ADS_ID).setProductType(BillingClient.ProductType.INAPP).build();
   billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(Collections.singletonList(product)).build(),(result,details)->{
     adFreeProduct=null;
     if(result.getResponseCode()==BillingClient.BillingResponseCode.OK && !details.getProductDetailsList().isEmpty())adFreeProduct=details.getProductDetailsList().get(0);
     if(adFreeProduct==null){billingNotice("Produk remove_ads_lifetime belum tersedia dari Google Play (kode "+result.getResponseCode()+"). Pastikan produk aktif dan instal aplikasi dari track pengujian Play Store.");}else{billingNotice("Produk Ads-Free tersedia di Google Play. Pembelian siap diuji.");}
     if(adFreeProduct!=null && adFreeProduct.getOneTimePurchaseOfferDetailsList()!=null && !adFreeProduct.getOneTimePurchaseOfferDetailsList().isEmpty()){
       String price=adFreeProduct.getOneTimePurchaseOfferDetailsList().get(0).getFormattedPrice();
       runOnUiThread(()->{if(web!=null)web.evaluateJavascript("window.alHikamBillingPrice&&window.alHikamBillingPrice("+org.json.JSONObject.quote(price)+")",null);});
     }
   });
 }
 private void purchaseAdFree(){
   if(adsFree){billingNotice("Mode bebas iklan sudah aktif.");return;}
   if(billingClient==null||!billingClient.isReady()){billingNotice("Google Play belum terhubung. Coba lagi setelah aplikasi terpasang lewat Google Play.");return;}
   if(adFreeProduct==null){queryAdFreeProduct();billingNotice("Produk remove_ads_lifetime belum tersedia. Buat dan aktifkan produk satu kali di Play Console, lalu instal aplikasi dari track pengujian Play Store.");return;}
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
  initFirebaseBilling();
  BackgroundBillingWorker.schedule(this);
  startBilling();
  // Inisialisasi SDK saja. TIDAK meminta iklan aktif pada tahap ini.
  // Consent pengguna harus disiapkan sebelum pemuatan iklan nyata.
  new Thread(() -> MobileAds.initialize(this, status -> runOnUiThread(()->{
    adsSdkReady=true;
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
      v.evaluateJavascript("window.alHikamPremiumChanged&&window.alHikamPremiumChanged("+adsFree+")",null);
      v.evaluateJavascript("window.alHikamBillingNotice&&window.alHikamBillingNotice("+org.json.JSONObject.quote(lastBillingMessage)+")",null);
      if(!adsFree){
        if(topBannerLoaded)v.evaluateJavascript("window.alHikamNativeTopBannerState&&window.alHikamNativeTopBannerState(true,"+topBannerHeightDp+")",null);
        if(bottomBannerLoaded)v.evaluateJavascript("window.alHikamNativeBannerState&&window.alHikamNativeBannerState(true,"+adaptiveBannerHeightDp+")",null);
      }
    }
  });
  web.addJavascriptInterface(new Object(){
   // V1.1.1: read-only entitlement contract. Must be replaced ONLY after integrating
   // Google Play Billing verified purchases; no JS method is allowed to set it.
   @JavascriptInterface public boolean isAdsFree(){return adsFree;}
   @JavascriptInterface public String billingStatus(){return lastBillingMessage;}
   @JavascriptInterface public String monetizationStatus(){return (adsFree?"PREMIUM":"GRATIS")+" | "+purchaseDiagnostic+" | Banner atas: "+(topBannerLoaded?"siap":"belum")+" | Banner bawah: "+(bottomBannerLoaded?"siap":"belum");}
   @JavascriptInterface public void buyAdsFree(){runOnUiThread(()->{billingNotice("Tombol beli diterima Android. Menghubungkan ke Google Play…");purchaseAdFree();});}
   @JavascriptInterface public void restoreAdsFree(){runOnUiThread(()->{billingNotice("Tombol pemulihan diterima Android. Memeriksa pembelian…");refreshPurchases(true);});}
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
 @Override protected void onPause(){
   billingForeground=false;
   getSharedPreferences("billing_background",MODE_PRIVATE).edit().putBoolean("visible",false).apply();
   billingRefreshHandler.removeCallbacks(foregroundBillingRefresh);
   if(bottomBanner!=null)bottomBanner.pause();if(topBanner!=null)topBanner.pause();super.onPause();
 }
 @Override protected void onResume(){
   super.onResume();
   billingForeground=true;
   getSharedPreferences("billing_background",MODE_PRIVATE).edit().putBoolean("visible",true).putLong("visibleAt",System.currentTimeMillis()).apply();
   // A fresh launch triggers the normal startBilling() check; returning after 3 hours rechecks.
   if(billingClient!=null && billingClient.isReady() &&
      (lastAutoPurchaseCheckAt==0L || System.currentTimeMillis()-lastAutoPurchaseCheckAt>=PURCHASE_CHECK_INTERVAL_MS ||
       getSharedPreferences("billing_background",MODE_PRIVATE).getLong("checkedAt",0L)>lastAutoPurchaseCheckAt))
     refreshPurchases(false);
   scheduleForegroundBillingRefresh();
   if(!adsFree && adsSdkReady){loadTopBanner();loadBottomBanner();}
   if(bottomBanner!=null)bottomBanner.resume();if(topBanner!=null)topBanner.resume();
 }
 @Override protected void onDestroy(){
   billingForeground=false;
   getSharedPreferences("billing_background",MODE_PRIVATE).edit().putBoolean("visible",false).apply();
   billingRefreshHandler.removeCallbacks(foregroundBillingRefresh);
   if(billingClient!=null){billingClient.endConnection();billingClient=null;}
   if(topBanner!=null){topBanner.destroy();topBanner=null;}
   if(bottomBanner!=null){bottomBanner.destroy();bottomBanner=null;}
   if(web!=null){web.destroy();web=null;}super.onDestroy();
 }
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
