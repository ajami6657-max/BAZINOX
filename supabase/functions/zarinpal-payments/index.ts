// BAZARINO ZarinPal payment Edge Function.
// Secrets required in Supabase: ZARINPAL_MERCHANT_ID.
// Never put merchant credentials or Supabase secret keys in Android/GitHub.
import { createClient } from "npm:@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, GET, OPTIONS",
};

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { ...cors, "Content-Type": "application/json; charset=utf-8" },
  });

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey =
  Deno.env.get("SUPABASE_SECRET_KEY") ??
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ??
  "";
const merchantId = Deno.env.get("ZARINPAL_MERCHANT_ID") ?? "";
const admin = createClient(supabaseUrl, serviceKey, {
  auth: { persistSession: false, autoRefreshToken: false },
});

const plans: Record<number, number> = {
  30: 750000,
  60: 1500000,
  90: 2250000,
};

async function createPayment(req: Request) {
  const authHeader = req.headers.get("Authorization") ?? "";
  const token = authHeader.replace(/^Bearer\s+/i, "");
  if (!token) return json({ error: "ورود به حساب الزامی است." }, 401);

  const { data: userData, error: userError } = await admin.auth.getUser(token);
  if (userError || !userData.user) return json({ error: "نشست کاربر معتبر نیست." }, 401);

  const body = await req.json().catch(() => ({}));
  const listingId = String(body.listing_id ?? "");
  if (!/^[0-9a-f-]{36}$/i.test(listingId)) {
    return json({ error: "شناسه آگهی معتبر نیست." }, 400);
  }

  const { data: listing, error: listingError } = await admin
    .from("listings")
    .select("id,seller_id,status,plan_days,listing_fee_irr,title")
    .eq("id", listingId)
    .eq("seller_id", userData.user.id)
    .maybeSingle();

  if (listingError || !listing) return json({ error: "آگهی پیدا نشد." }, 404);
  if (listing.status !== "pending_fee") {
    return json({ error: "این آگهی در انتظار پرداخت هزینه نیست." }, 409);
  }

  const amount = plans[Number(listing.plan_days)];
  if (!amount || Number(listing.listing_fee_irr) !== amount) {
    return json({ error: "تعرفه آگهی معتبر نیست." }, 400);
  }
  if (!merchantId || !serviceKey) {
    return json({ error: "تنظیمات امن درگاه هنوز تکمیل نشده است." }, 503);
  }

  const paymentId = crypto.randomUUID();
  const callback = new URL(`${supabaseUrl}/functions/v1/zarinpal-payments`);
  callback.searchParams.set("action", "callback");
  callback.searchParams.set("payment_id", paymentId);

  const gatewayResponse = await fetch("https://payment.zarinpal.com/pg/v4/payment/request.json", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({
      merchant_id: merchantId,
      amount,
      callback_url: callback.toString(),
      description: `هزینه انتشار آگهی بازارینو - ${listing.title}`,
      metadata: { email: userData.user.email ?? undefined },
    }),
  });

  const gateway = await gatewayResponse.json().catch(() => ({}));
  const authority = gateway?.data?.authority;
  if (!gatewayResponse.ok || gateway?.data?.code !== 100 || !authority) {
    return json({ error: "دریافت لینک پرداخت از زرین‌پال ناموفق بود.", details: gateway?.errors ?? [] }, 502);
  }

  const { error: insertError } = await admin.from("listing_fee_payments").insert({
    id: paymentId,
    listing_id: listing.id,
    amount_irr: amount,
    provider: "zarinpal",
    provider_reference: authority,
    status: "pending",
  });
  if (insertError) {
    return json({ error: "ثبت درخواست پرداخت در بازارینو ناموفق بود." }, 500);
  }

  return json({
    payment_id: paymentId,
    authority,
    payment_url: `https://www.zarinpal.com/pg/StartPay/${encodeURIComponent(authority)}`,
  });
}

async function verifyCallback(url: URL) {
  const paymentId = url.searchParams.get("payment_id") ?? "";
  const status = url.searchParams.get("Status") ?? url.searchParams.get("status") ?? "";
  const authority = url.searchParams.get("Authority") ?? url.searchParams.get("authority") ?? "";

  if (!/^[0-9a-f-]{36}$/i.test(paymentId) || !authority) {
    return new Response("درخواست بازگشت پرداخت نامعتبر است.", { status: 400, headers: cors });
  }

  const { data: payment, error } = await admin
    .from("listing_fee_payments")
    .select("id,listing_id,amount_irr,provider,provider_reference,status")
    .eq("id", paymentId)
    .maybeSingle();

  if (error || !payment || payment.provider !== "zarinpal" ||
      payment.provider_reference !== authority) {
    return new Response("تراکنش در بازارینو پیدا نشد.", { status: 404, headers: cors });
  }
  if (payment.status === "paid") {
    return Response.redirect("https://bazarino.ir/payment/success", 303);
  }
  if (status.toUpperCase() !== "OK") {
    await admin.from("listing_fee_payments").update({ status: "failed" })
      .eq("id", paymentId).eq("status", "pending");
    return Response.redirect("https://bazarino.ir/payment/failed", 303);
  }

  const verifyResponse = await fetch("https://payment.zarinpal.com/pg/v4/payment/verify.json", {
    method: "POST",
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    body: JSON.stringify({
      merchant_id: merchantId,
      amount: Number(payment.amount_irr),
      authority,
    }),
  });
  const verified = await verifyResponse.json().catch(() => ({}));
  const code = Number(verified?.data?.code);
  if (!verifyResponse.ok || ![100, 101].includes(code)) {
    return new Response("تأیید پرداخت از زرین‌پال ناموفق بود.", { status: 402, headers: cors });
  }

  const refId = String(verified?.data?.ref_id ?? authority);
  const digestBytes = new TextEncoder().encode(JSON.stringify({
    authority, ref_id: refId, amount: payment.amount_irr, code,
  }));
  const digest = Array.from(new Uint8Array(await crypto.subtle.digest("SHA-256", digestBytes)))
    .map((b) => b.toString(16).padStart(2, "0")).join("");

  const { error: rpcError } = await admin.rpc("confirm_listing_fee_payment", {
    p_payment_id: paymentId,
    p_provider_event_id: refId,
    p_payload_digest: digest,
  });
  if (rpcError) {
    console.error("Listing payment confirmation failed:", rpcError.message);
    return new Response("پرداخت بررسی شد اما ثبت نهایی در بازارینو ناموفق بود؛ پشتیبانی پیگیری می‌کند.", {
      status: 500, headers: cors,
    });
  }
  return Response.redirect("https://bazarino.ir/payment/success", 303);
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  if (req.method === "GET") {
    if (new URL(req.url).searchParams.get("action") === "callback") {
      try { return await verifyCallback(new URL(req.url)); }
      catch (error) {
        console.error("ZarinPal callback error:", error);
        return new Response("خطای داخلی در بررسی پرداخت.", { status: 500, headers: cors });
      }
    }
    return json({ error: "مسیر نامعتبر است." }, 404);
  }
  if (req.method === "POST") {
    try { return await createPayment(req); }
    catch (error) {
      console.error("ZarinPal create payment error:", error);
      return json({ error: "خطای داخلی در ایجاد پرداخت." }, 500);
    }
  }
  return json({ error: "Method not allowed" }, 405);
});
