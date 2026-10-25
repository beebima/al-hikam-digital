import { http } from '@google-cloud/functions-framework';
import { GoogleAuth } from 'google-auth-library';

const APP = 'id.sejutasantri.alhikam';
const PRODUCT = 'remove_ads_lifetime';
const auth = new GoogleAuth({scopes: ['https://www.googleapis.com/auth/androidpublisher']});

http('helloHttp', async (req, res) => {
  res.set('Cache-Control', 'no-store');
  res.set('X-Content-Type-Options', 'nosniff');
  if (req.method === 'GET' && req.path === '/') {
    try {
      const client = await auth.getClient();
      const params = new URLSearchParams({type:'0', startTime:String(Date.now()-29*86400000), endTime:String(Date.now())});
      const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${APP}/purchases/voidedpurchases?${params}`;
      const result = await client.request({url, method:'GET', timeout:12000});
      return res.status(200).json({service:'SejutaSantri Billing Verifier',mode:'PRIVATE_DIAGNOSTIC_ONLY',status:'GOOGLE_PLAY_API_SUCCESS',voidedPurchaseCount:(result.data?.voidedPurchases||[]).length,hasMore:!!result.data?.tokenPagination?.nextPageToken,premiumAccessChanged:false});
    } catch(err) {
      console.error('Voided purchases diagnostics failed',{httpStatus:err.response?.status||null});
      return res.status(503).json({status:'VERIFICATION_UNAVAILABLE'});
    }
  }
  if (req.method !== 'POST' || req.path !== '/verify') {
    return res.status(404).json({error: 'Not found'});
  }
  // One-time purchase token is sensitive. Never log the body or query string.
  if (Number(req.get('content-length') || 0) > 8192) {
    return res.status(413).json({error: 'Request too large'});
  }
  const {packageName, productId, purchaseToken} = req.body || {};
  if (packageName !== APP || productId !== PRODUCT ||
      typeof purchaseToken !== 'string' || !/^[A-Za-z0-9._:-]{8,2048}$/.test(purchaseToken)) {
    return res.status(400).json({error: 'Invalid request'});
  }
  try {
    const client = await auth.getClient();
    const url = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(APP)}/purchases/productsv2/tokens/${encodeURIComponent(purchaseToken)}`;
    const response = await client.request({url, method: 'GET', timeout: 12000});
    const purchase = response.data ?? {};
    const matching = (purchase.productLineItem || []).filter(x => x.productId === PRODUCT);
    const purchaseState = purchase.purchaseStateContext?.purchaseState || 'UNKNOWN';
    const acknowledgementState = purchase.acknowledgementState || 'UNKNOWN';
    // refundableQuantity is documented as the count that has NOT been refunded.
    // When absent, treat a PURCHASED state as provisionally entitled, not proof of no refund.
    const quantities = matching.map(x => x.productOfferDetails?.refundableQuantity)
      .filter(x => Number.isInteger(x));
    const allRefunded = quantities.length > 0 && quantities.every(q => q === 0);
    const entitled = matching.length > 0 && purchaseState === 'PURCHASED' && !allRefunded;
    return res.status(200).json({
      status: 'GOOGLE_PLAY_VERIFIED',
      entitled,
      purchaseState,
      acknowledgementState,
      fullyRefundedAccordingToQuantity: allRefunded,
      refundQuantityAvailable: quantities.length > 0,
      testPurchase: purchase.testPurchaseContext?.fopType === 'TEST',
      policy: 'PURCHASED_MATCHING_PRODUCT_AND_NOT_FULLY_REFUNDED',
      premiumAccessChanged: false
    });
  } catch (error) {
    const status = error.response?.status || null;
    // A token not found is explicitly invalid; Play API 401/403/5xx are infrastructure errors.
    if (status === 404 || status === 410) {
      return res.status(200).json({
        status: 'GOOGLE_PLAY_VERIFIED',
        entitled: false,
        purchaseState: 'NOT_FOUND',
        acknowledgementState: 'UNKNOWN',
        premiumAccessChanged: false
      });
    }
    console.error('Google Play verification failed', {httpStatus: status, errorType: error.name});
    return res.status(503).json({status: 'VERIFICATION_UNAVAILABLE'});
  }
});
