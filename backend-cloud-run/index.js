import {http} from '@google-cloud/functions-framework';
import {GoogleAuth} from 'google-auth-library';
import {initializeApp, applicationDefault} from 'firebase-admin/app';
import {getAuth} from 'firebase-admin/auth';

const APP='id.sejutasantri.alhikam';
const PRODUCT='remove_ads_lifetime';
const PROJECT='sejutasantri-play-billing';
const playAuth=new GoogleAuth({scopes:['https://www.googleapis.com/auth/androidpublisher']});
initializeApp({credential:applicationDefault(),projectId:PROJECT});

http('helloHttp',async(req,res)=>{
  res.set('Cache-Control','no-store');
  res.set('X-Content-Type-Options','nosniff');
  // When exposed as HTTPS Cloud Run, all endpoints require a signed Firebase ID token.
  if(req.method!=='POST'||req.path!=='/verify')return res.status(404).json({status:'NOT_FOUND'});
  const authorization=req.get('Authorization')||'';
  const match=/^Bearer ([A-Za-z0-9._-]{100,4096})$/.exec(authorization);
  if(!match)return res.status(401).json({status:'AUTH_REQUIRED'});
  try {await getAuth().verifyIdToken(match[1],true);} catch(e){
    return res.status(401).json({status:'INVALID_FIREBASE_TOKEN'});
  }
  if(Number(req.get('content-length')||0)>8192)return res.status(413).json({status:'TOO_LARGE'});
  const {packageName,productId,purchaseToken}=req.body||{};
  if(packageName!==APP||productId!==PRODUCT||typeof purchaseToken!=='string'||
     !/^[A-Za-z0-9._:-]{8,2048}$/.test(purchaseToken))
    return res.status(400).json({status:'INVALID_INPUT'});
  try {
    const client=await playAuth.getClient();
    const url=`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${APP}/purchases/productsv2/tokens/${encodeURIComponent(purchaseToken)}`;
    const result=await client.request({url,method:'GET',timeout:12000});
    const purchase=result.data??{};
    const items=(purchase.productLineItem??[]).filter(x=>x.productId===PRODUCT);
    const state=purchase.purchaseStateContext?.purchaseState??'UNKNOWN';
    const quantities=items.map(x=>x.productOfferDetails?.refundableQuantity)
      .filter(x=>Number.isInteger(x));
    const allRefunded=quantities.length>0&&quantities.every(q=>q===0);
    const entitled=items.length>0&&state==='PURCHASED'&&!allRefunded;
    return res.status(200).json({status:'GOOGLE_PLAY_VERIFIED',entitled,
      purchaseState:state,acknowledgementState:purchase.acknowledgementState??'UNKNOWN',
      refundQuantityAvailable:quantities.length>0,fullyRefundedAccordingToQuantity:allRefunded,
      testPurchase:purchase.testPurchaseContext?.fopType==='TEST',premiumAccessChanged:false});
  }catch(error){
    const status=error.response?.status??null;
    // Invalid input can yield Play API 400; avoid treating all 400s as proof of refund.
    if(status===404||status===410)return res.status(200).json({status:'GOOGLE_PLAY_VERIFIED',entitled:false,purchaseState:'NOT_FOUND',premiumAccessChanged:false});
    console.error('Play verification failed',{httpStatus:status,errorType:error.name??'Error'});
    return res.status(503).json({status:'VERIFICATION_UNAVAILABLE'});
  }
});
