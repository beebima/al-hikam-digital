package id.sejutasantri.alhikam

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.android.billingclient.api.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private val paper=Color(0xFFFAF6EE)
private val cardPaper=Color(0xFFFFFDF8)
private val muted=Color(0xFF637184)
private val line=Color(0xFFE1E5E8)
private val ink=Color(0xFF172C45)
private val gold=Color(0xFFC6A36A)
private const val TEST_BANNER="ca-app-pub-3940256099942544/6300978111"
private const val TEST_INTER="ca-app-pub-3940256099942544/1033173712"
private const val PRODUCT_ID="ads_free_lifetime"
private const val COOLDOWN=180000L

class ComposeMainActivity:ComponentActivity(){
 private var interstitial:InterstitialAd?=null
 private var lastShown=System.currentTimeMillis()
 private lateinit var billing:BillingClient
 private val adsFree=mutableStateOf(false)
 private val billingReady=mutableStateOf(false)
 private val productAvailable=mutableStateOf(false)
 private var details:ProductDetails?=null
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  window.statusBarColor=android.graphics.Color.rgb(250,246,238)
  window.navigationBarColor=android.graphics.Color.rgb(250,246,238)
  window.decorView.systemUiVisibility=android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
  MobileAds.initialize(this){};loadInterstitial();connectBilling()
  val data=JSONArray(assets.open("hikmah.json").bufferedReader().use{it.readText()})
  val full=JSONObject(assets.open("fullHikam.json").bufferedReader().use{it.readText()})
  val english=JSONObject(assets.open("englishContent.json").bufferedReader().use{it.readText()})
  setContent { MaterialTheme { ReaderApp(this,data,full,english,adsFree.value,billingReady.value,productAvailable.value,::purchase,::restore,::showInterstitial) } }
 }
 private fun loadInterstitial(){if(adsFree.value)return;InterstitialAd.load(this,TEST_INTER,AdRequest.Builder().build(),object:InterstitialAdLoadCallback(){override fun onAdLoaded(ad:InterstitialAd){interstitial=ad};override fun onAdFailedToLoad(error:LoadAdError){interstitial=null}})}
 private fun showInterstitial(){if(adsFree.value||System.currentTimeMillis()-lastShown<COOLDOWN)return;val ad=interstitial?:run{loadInterstitial();return};interstitial=null;ad.fullScreenContentCallback=object:FullScreenContentCallback(){override fun onAdDismissedFullScreenContent(){loadInterstitial()};override fun onAdFailedToShowFullScreenContent(error:AdError){loadInterstitial()}};lastShown=System.currentTimeMillis();ad.show(this)}
 private fun connectBilling(){billing=BillingClient.newBuilder(this).setListener{result,purchases->if(result.responseCode==BillingClient.BillingResponseCode.OK)purchases?.forEach(::handlePurchase)}.enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()).build()
  billing.startConnection(object:BillingClientStateListener{
   override fun onBillingServiceDisconnected(){billingReady.value=false;productAvailable.value=false}
   override fun onBillingSetupFinished(result:BillingResult){if(result.responseCode!=BillingClient.BillingResponseCode.OK)return;billingReady.value=true;restore();val params=QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(PRODUCT_ID).setProductType(BillingClient.ProductType.INAPP).build())).build();billing.queryProductDetailsAsync(params){response,products->if(response.responseCode==BillingClient.BillingResponseCode.OK){details=products.firstOrNull();productAvailable.value=details!=null}}}
  })
 }
 private fun restore(){if(!::billing.isInitialized||!billing.isReady)return;billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()){result,purchases->if(result.responseCode==BillingClient.BillingResponseCode.OK){adsFree.value=purchases.any{it.products.contains(PRODUCT_ID)&&it.purchaseState==Purchase.PurchaseState.PURCHASED};purchases.forEach(::handlePurchase);if(adsFree.value)interstitial=null}}}
 private fun handlePurchase(p:Purchase){if(!p.products.contains(PRODUCT_ID)||p.purchaseState!=Purchase.PurchaseState.PURCHASED)return;adsFree.value=true;interstitial=null;if(!p.isAcknowledged)billing.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()){} }
 private fun purchase(){val d=details?:return;if(!billing.isReady)return;billing.launchBillingFlow(this,BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build())).build())}
 override fun onResume(){super.onResume();if(::billing.isInitialized&&billing.isReady)restore()}
 override fun onDestroy(){if(::billing.isInitialized)billing.endConnection();super.onDestroy()}
}

@Composable private fun AdBanner(){val context=LocalContext.current;val view=remember(context){AdView(context).apply{setAdSize(AdSize.BANNER);adUnitId=TEST_BANNER;loadAd(AdRequest.Builder().build())}};DisposableEffect(view){onDispose{view.destroy()}};Box(Modifier.fillMaxWidth().height(52.dp),contentAlignment=Alignment.Center){AndroidView(factory={view})}}
private fun paragraphs(a:JSONArray?):String=if(a==null)"Konten tidak tersedia" else (0 until a.length()).joinToString("\n\n"){i->val v=a.opt(i);when(v){is String->v;is JSONArray->(0 until v.length()).joinToString(" — "){v.optString(it)};else->v.toString()}}
private fun textFor(o:JSONObject,f:JSONObject?,e:JSONObject?,section:String,lang:String):String=when(section){
 "Hikmah"->if(lang=="ID")o.optString("reflection") else o.optString("reflectionEn",o.optString("reflection"))
 "Per Frasa"->paragraphs(if(lang=="ID")o.optJSONArray("phrases") else e?.optJSONArray("phrasesEn"))
 "Per Kata"->paragraphs(if(lang=="ID")o.optJSONArray("words") else e?.optJSONArray("wordsEn"))
 else->{val key=when(section){"Syarah"->"syarah";"Kultum"->"short";else->"talk"}+(if(lang=="ID")"Id" else "En");paragraphs(f?.optJSONArray(key)?:e?.optJSONArray(key))}
}
@Composable private fun ReaderApp(activity:Activity,data:JSONArray,full:JSONObject,english:JSONObject,adsFree:Boolean,billingReady:Boolean,productAvailable:Boolean,buy:()->Unit,restore:()->Unit,onNavigate:()->Unit){
 val prefs=remember{activity.getSharedPreferences("alhikam_native",Context.MODE_PRIVATE)}
 var page by remember{mutableStateOf("Beranda")};var selected by remember{mutableIntStateOf(prefs.getInt("last_hikmah",1).coerceIn(1,127))}
 var language by remember{mutableStateOf(prefs.getString("language","ID")?:"ID")};var section by remember{mutableStateOf("Hikmah")}
 var query by remember{mutableStateOf("")};var favorites by remember{mutableStateOf(prefs.getStringSet("favorites",emptySet())!!.toSet())};var bookmarks by remember{mutableStateOf(prefs.getStringSet("bookmarks",emptySet())!!.toSet())}
 var fontSize by remember{mutableFloatStateOf(prefs.getFloat("fontSize",18f))};var dark by remember{mutableStateOf(prefs.getBoolean("dark",false))}
 var prompter by remember{mutableStateOf(false)};var running by remember{mutableStateOf(false)};var speed by remember{mutableFloatStateOf(prefs.getFloat("prompterSpeed",2f))};var mirrored by remember{mutableStateOf(false)}
 val bg=if(dark)Color(0xFF17202A) else paper;val fg=if(dark)Color(0xFFF5EAD9) else ink
 fun navigate(p:String){if(page!=p){onNavigate();page=p}}
 fun select(n:Int){selected=n.coerceIn(1,127);prefs.edit().putInt("last_hikmah",selected).apply();navigate("Baca")}
 BackHandler(prompter||page!="Beranda"){if(prompter){prompter=false;running=false}else navigate("Beranda")}
 val item=data.getJSONObject(selected-1);val f=full.optJSONObject("$selected");val e=english.optJSONObject("$selected")
 val content=textFor(item,f,e,section,language)
 val scroll=rememberScrollState()
 LaunchedEffect(selected,section,language,prompter){scroll.scrollTo(0);running=false}
 LaunchedEffect(prompter,running,speed,scroll.maxValue){while(prompter&&running&&scroll.value<scroll.maxValue){delay(32);scroll.scrollTo((scroll.value+(speed*2).toInt().coerceAtLeast(1)).coerceAtMost(scroll.maxValue))};if(scroll.value>=scroll.maxValue)running=false}
 Column(Modifier.fillMaxSize().background(bg)){
  if(!prompter){
   Row(Modifier.fillMaxWidth().background(bg).padding(horizontal=20.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){
    Column(Modifier.weight(1f)){Text("الْحِكَمُ",fontSize=27.sp,color=gold,fontFamily=FontFamily.Serif,lineHeight=32.sp);Text("AL-HIKAM  •  DIGITAL",fontSize=11.sp,color=fg,letterSpacing=2.sp,fontFamily=FontFamily.Serif)}
    Surface(color=cardPaper,shape=RoundedCornerShape(22.dp),border=BorderStroke(1.dp,line)){Text(if(language=="ID")"ID  ▾" else "EN  ▾",modifier=Modifier.clickable{language=if(language=="ID")"EN" else "ID";prefs.edit().putString("language",language).apply()}.padding(horizontal=13.dp,vertical=9.dp),fontSize=12.sp,color=ink)}
   }
   HorizontalDivider(color=gold.copy(alpha=.48f),thickness=1.dp)
  }
  Box(Modifier.weight(1f)){
   when{
    prompter->Column(Modifier.fillMaxSize()){
     Row(Modifier.fillMaxWidth().padding(6.dp),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={prompter=false;running=false}){Text("✕ Tutup")};TextButton(onClick={running=!running}){Text(if(running)"⏸ Jeda" else "▶ Mulai")};TextButton(onClick={mirrored=!mirrored}){Text(if(mirrored)"Cermin ✓" else "Cermin")}}
     Row(Modifier.padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically){Text("Kecepatan",color=fg);Slider(value=speed,onValueChange={speed=it;prefs.edit().putFloat("prompterSpeed",it).apply()},valueRange=1f..8f,modifier=Modifier.weight(1f));Text("${speed.toInt()}×",color=fg);TextButton(onClick={fontSize=(fontSize-2).coerceAtLeast(14f)}){Text("A−")};TextButton(onClick={fontSize=(fontSize+2).coerceAtMost(42f)}){Text("A+")}}
     Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal=24.dp,vertical=16.dp)){Text("Hikmah $selected — $section",fontSize=18.sp,color=gold);Spacer(Modifier.height(20.dp));Text(content,fontSize=fontSize.sp,lineHeight=(fontSize*1.65f).sp,color=fg,modifier=if(mirrored)Modifier.graphicsLayerMirror() else Modifier);Spacer(Modifier.height(500.dp))}
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.Center){TextButton(onClick={running=false}){Text("Jeda")};TextButton(onClick={running=false;prompter=false}){Text("Kembali ke bacaan")}}
    }
    page=="Beranda"->Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal=19.dp,vertical=19.dp),horizontalAlignment=Alignment.CenterHorizontally){
     Text("✦  بسم الله الرحمن الرحيم  ✦",fontSize=20.sp,color=gold,textAlign=TextAlign.Center)
     Spacer(Modifier.height(16.dp))
     Text("Kitab Al-Hikam",fontSize=35.sp,fontFamily=FontFamily.Serif,color=fg,textAlign=TextAlign.Center)
     Text("IBNU ‘ATHA’ILLAH AS-SAKANDARI",fontSize=10.sp,letterSpacing=2.sp,color=muted,textAlign=TextAlign.Center)
     Spacer(Modifier.height(17.dp))
     Text("❖ ─────────── ❖",color=gold,fontSize=18.sp)
     Spacer(Modifier.height(17.dp))
     Surface(color=cardPaper,shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,line),modifier=Modifier.fillMaxWidth()){
      Column(Modifier.padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
       Text("PERJALANAN HATI",fontSize=11.sp,letterSpacing=2.sp,color=gold,fontWeight=FontWeight.Bold)
       Spacer(Modifier.height(10.dp))
       Text("127 Hikmah Menuju Kedekatan dengan Allah",fontSize=21.sp,lineHeight=29.sp,fontFamily=FontFamily.Serif,color=ink,textAlign=TextAlign.Center)
       Spacer(Modifier.height(12.dp))
       Text("Teks Arab • Terjemahan • Syarah • Kultum • Tausiah",fontSize=12.sp,lineHeight=19.sp,color=muted,textAlign=TextAlign.Center)
       Spacer(Modifier.height(20.dp))
       Button(onClick={select(prefs.getInt("last_hikmah",1))},colors=ButtonDefaults.buttonColors(containerColor=ink),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(12.dp)){Text("Lanjutkan membaca  →",color=Color.White)}
       Spacer(Modifier.height(9.dp))
       Text("Terakhir dibaca: Hikmah ${prefs.getInt("last_hikmah",1)} dari 127",fontSize=12.sp,color=muted)
      }
     }
     Spacer(Modifier.height(18.dp))
     Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()){
      listOf(Triple("☷","Daftar","127 hikmah"),Triple("♡","Favorit","${favorites.size} disimpan")).forEach{(symbol,target,subtitle)->
       Surface(modifier=Modifier.weight(1f).clickable{navigate(target)},shape=RoundedCornerShape(14.dp),color=cardPaper,border=BorderStroke(1.dp,line)){
        Column(Modifier.padding(16.dp)){Text(symbol,fontSize=26.sp,color=gold);Text(target,fontFamily=FontFamily.Serif,fontSize=19.sp,color=ink);Text(subtitle,fontSize=11.sp,color=muted)}
       }
      }
     }
     Spacer(Modifier.height(10.dp))
     Surface(modifier=Modifier.fillMaxWidth().clickable{navigate("Bookmark")},shape=RoundedCornerShape(14.dp),color=cardPaper,border=BorderStroke(1.dp,line)){
      Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Text("♧",fontSize=26.sp,color=gold);Spacer(Modifier.width(14.dp));Column(Modifier.weight(1f)){Text("Penanda Bacaan",fontFamily=FontFamily.Serif,fontSize=19.sp,color=ink);Text("${bookmarks.size} hikmah ditandai",fontSize=11.sp,color=muted)};Text("→",color=gold)}
     }
     Spacer(Modifier.height(20.dp))
     Text("SEJUTA SANTRI  •  MENEBAR HIKMAH",fontSize=10.sp,letterSpacing=1.5.sp,color=muted)
    }
    page in listOf("Daftar","Favorit","Bookmark")->Column(Modifier.padding(horizontal=16.dp)) {
     Spacer(Modifier.height(16.dp))
     Text(when(page){"Daftar"->"Daftar Hikmah";"Favorit"->"Hikmah Favorit";else->"Penanda Bacaan"},fontSize=28.sp,fontFamily=FontFamily.Serif,color=fg)
     Text("Kumpulan mutiara hikmah Ibnu ‘Atha’illah",fontSize=12.sp,color=muted)
     Spacer(Modifier.height(12.dp))
     OutlinedTextField(query,{query=it},placeholder={Text("Cari hikmah, judul, atau makna…",fontSize=13.sp)},singleLine=true,shape=RoundedCornerShape(12.dp),modifier=Modifier.fillMaxWidth(),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=gold,unfocusedBorderColor=line))
     val matches=remember(page,query,favorites,bookmarks,language){(0 until data.length()).filter{idx->val o=data.getJSONObject(idx);val id="${idx+1}";(page!="Favorit"||id in favorites)&&(page!="Bookmark"||id in bookmarks)&&(query.isBlank()||listOf(id,o.optString("title"),o.optString("arab"),o.optString("translation"),o.optString("english")).any{it.contains(query,true)})}}
     Spacer(Modifier.height(12.dp))
     LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(bottom=18.dp)){
      items(matches){idx->val o=data.getJSONObject(idx)
       Surface(onClick={select(idx+1)},shape=RoundedCornerShape(13.dp),color=cardPaper,border=BorderStroke(1.dp,line),modifier=Modifier.fillMaxWidth()){
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.Top){
         Text("%02d".format(idx+1),fontFamily=FontFamily.Serif,fontSize=24.sp,color=gold,modifier=Modifier.width(44.dp))
         Column(Modifier.weight(1f)){Text(o.optString("title"),fontSize=17.sp,fontFamily=FontFamily.Serif,color=ink);Spacer(Modifier.height(5.dp));Text(if(language=="ID")o.optString("translation") else o.optString("english"),fontSize=12.sp,lineHeight=18.sp,color=muted,maxLines=3,overflow=TextOverflow.Ellipsis)}
         Text("›",fontSize=24.sp,color=gold)
        }
       }
      }
     }
    }
    page=="Baca"->Column(Modifier.verticalScroll(scroll).padding(horizontal=17.dp)){
     Spacer(Modifier.height(16.dp))
     Row(verticalAlignment=Alignment.CenterVertically){Text("HIKMAH  %02d / 127".format(selected),fontSize=11.sp,letterSpacing=1.5.sp,color=gold,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));TextButton(onClick={favorites=if("$selected" in favorites)favorites-"$selected" else favorites+"$selected";prefs.edit().putStringSet("favorites",favorites).apply()}){Text(if("$selected" in favorites)"♥" else "♡",fontSize=25.sp,color=gold)};TextButton(onClick={bookmarks=if("$selected" in bookmarks)bookmarks-"$selected" else bookmarks+"$selected";prefs.edit().putStringSet("bookmarks",bookmarks).apply()}){Text(if("$selected" in bookmarks)"◆" else "◇",fontSize=21.sp,color=gold)}}
     Text(item.optString("title"),fontFamily=FontFamily.Serif,fontSize=27.sp,color=fg)
     Spacer(Modifier.height(12.dp))
     Surface(color=cardPaper,shape=RoundedCornerShape(15.dp),border=BorderStroke(1.dp,line),modifier=Modifier.fillMaxWidth()){
      Column(Modifier.padding(19.dp),horizontalAlignment=Alignment.CenterHorizontally){
       Text("۞",fontSize=26.sp,color=gold)
       Text(item.optString("arab"),fontSize=28.sp,lineHeight=49.sp,textAlign=TextAlign.Center,color=ink,modifier=Modifier.fillMaxWidth().padding(vertical=12.dp))
       HorizontalDivider(color=line)
       Spacer(Modifier.height(16.dp))
       Text(if(language=="ID")item.optString("translation") else e?.optString("translation",item.optString("english"))?:item.optString("english"),fontFamily=FontFamily.Serif,fontSize=18.sp,lineHeight=29.sp,textAlign=TextAlign.Center,color=ink)
       Spacer(Modifier.height(12.dp))
      }
     }
     Spacer(Modifier.height(17.dp))
     Text("JELAJAHI HIKMAH",fontSize=11.sp,letterSpacing=1.5.sp,color=gold,fontWeight=FontWeight.Bold)
     Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("Hikmah","Per Frasa","Per Kata","Syarah","Kultum","Tausiah").forEach{s->Surface(onClick={section=s},color=if(section==s)ink else cardPaper,shape=RoundedCornerShape(20.dp),border=BorderStroke(1.dp,if(section==s)ink else line)){Text(s,modifier=Modifier.padding(horizontal=13.dp,vertical=9.dp),fontSize=12.sp,color=if(section==s)Color.White else ink)}}}
     Surface(color=cardPaper,shape=RoundedCornerShape(14.dp),border=BorderStroke(1.dp,line),modifier=Modifier.fillMaxWidth()){
      Column(Modifier.padding(18.dp)){Text(section.uppercase(),fontSize=11.sp,letterSpacing=1.4.sp,color=gold,fontWeight=FontWeight.Bold);Spacer(Modifier.height(12.dp));Text(content,fontSize=fontSize.sp,lineHeight=(fontSize*1.65f).sp,color=ink)}
     }
     Spacer(Modifier.height(12.dp))
     Row(Modifier.horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically){TextButton(onClick={fontSize=(fontSize-1).coerceAtLeast(13f);prefs.edit().putFloat("fontSize",fontSize).apply()}){Text("A−",color=fg)};TextButton(onClick={fontSize=(fontSize+1).coerceAtMost(36f);prefs.edit().putFloat("fontSize",fontSize).apply()}){Text("A+",color=fg)};TextButton(onClick={prompter=true}){Text("▷ Teleprompter",color=fg)};TextButton(onClick={val clip=activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager;clip.setPrimaryClip(ClipData.newPlainText("Al-Hikam #$selected", "${item.optString("arab")}\n${item.optString("translation")}\n\n$content"))}){Text("Salin",color=fg)};TextButton(onClick={val share=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,"Al-Hikam #$selected\n${item.optString("arab")}\n${item.optString("translation")}\n\n$content")};activity.startActivity(Intent.createChooser(share,"Bagikan hikmah"))}){Text("Bagikan",color=fg)}}
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){TextButton(onClick={select(selected-1)},enabled=selected>1){Text("← Sebelumnya",color=gold)};TextButton(onClick={select(selected+1)},enabled=selected<127){Text("Berikutnya →",color=gold)}}
    }
    page=="Pengaturan"->Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)){Text("Pengaturan",fontSize=26.sp,color=fg);Row(verticalAlignment=Alignment.CenterVertically){Text("Mode gelap",color=fg,modifier=Modifier.weight(1f));Switch(checked=dark,onCheckedChange={dark=it;prefs.edit().putBoolean("dark",it).apply()})};Text("Bahasa: $language",color=fg);Text("Google Play Ads-Free selamanya",fontSize=19.sp,color=fg);Text(if(adsFree)"Aktif — banner dan interstitial dimatikan" else if(!billingReady)"Menghubungkan Google Play…" else if(!productAvailable)"Produk belum tersedia. Periksa aktivasi produk di Play Console dan jalur pengujian." else "Produk tersedia untuk pembelian",color=fg);Button(onClick=buy,enabled=!adsFree&&billingReady&&productAvailable){Text(if(adsFree)"Sudah dimiliki" else "Beli Ads-Free")};TextButton(onClick=restore,enabled=billingReady){Text("Pulihkan pembelian")};Text("ID produk: $PRODUCT_ID · Iklan: ID uji",fontSize=12.sp,color=fg);Text("Konten offline · 127 hikmah · Penyimpanan lokal",color=fg)}
   }
  }
  if(!prompter){
   HorizontalDivider(color=line)
   val menus=listOf(Triple("⌂","Beranda","Beranda"),Triple("☷","Daftar","Daftar"),Triple("▤","Baca","Baca"),Triple("♡","Favorit","Favorit"),Triple("◇","Bookmark","Bookmark"),Triple("⚙","Pengaturan","Atur"))
   Row(Modifier.fillMaxWidth().background(cardPaper).padding(horizontal=3.dp,vertical=4.dp),horizontalArrangement=Arrangement.SpaceEvenly){menus.forEach{(symbol,target,label)->
    Column(Modifier.weight(1f).clickable{navigate(target)}.background(if(page==target)Color(0xFFE9EDF2) else Color.Transparent,RoundedCornerShape(9.dp)).padding(vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(symbol,fontSize=21.sp,color=if(page==target)ink else muted,lineHeight=25.sp);Text(label,fontSize=9.sp,color=if(page==target)ink else muted,maxLines=1)}
   }}
  }
  // Satu slot banner permanen; tidak dibuat pada tiap halaman/komposisi.
  if(!adsFree&&!prompter)AdBanner()
 }
}
private fun Modifier.graphicsLayerMirror():Modifier=this.graphicsLayer(scaleX=-1f)
