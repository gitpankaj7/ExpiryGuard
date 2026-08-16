const functions = require("firebase-functions");
const admin = require("firebase-admin");
const Razorpay = require("razorpay");
const crypto = require("crypto");

admin.initializeApp();

// Hardcoded Test Keys for implementation phase
// In a real app, use functions.config() or Secret Manager
const RAZORPAY_KEY_ID = "rzp_test_1234567890";
const RAZORPAY_KEY_SECRET = "test_secret_1234567890";

const razorpay = new Razorpay({
  key_id: RAZORPAY_KEY_ID,
  key_secret: RAZORPAY_KEY_SECRET,
});

exports.createRazorpayOrder = functions.https.onCall(async (data, context) => {
  // Ensure user is authenticated
  if (!context.auth) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "User must be logged in to create an order."
    );
  }

  // Define subscription price: ₹49.00 -> 4900 paise
  const options = {
    amount: 4900,
    currency: "INR",
    receipt: `receipt_${context.auth.uid}_${Date.now()}`,
  };

  try {
    const order = await razorpay.orders.create(options);
    return {
      id: order.id,
      amount: order.amount,
      currency: order.currency,
      keyId: RAZORPAY_KEY_ID // Return the test key to the client for checkout init
    };
  } catch (error) {
    console.error("Error creating order:", error);
    throw new functions.https.HttpsError("internal", "Failed to create order");
  }
});

exports.verifyRazorpayPayment = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
  }

  const { razorpay_order_id, razorpay_payment_id, razorpay_signature } = data;

  if (!razorpay_order_id || !razorpay_payment_id || !razorpay_signature) {
    throw new functions.https.HttpsError("invalid-argument", "Missing payment parameters.");
  }

  // Verify the signature cryptographically
  const generatedSignature = crypto
    .createHmac("sha256", RAZORPAY_KEY_SECRET)
    .update(razorpay_order_id + "|" + razorpay_payment_id)
    .digest("hex");

  if (generatedSignature !== razorpay_signature) {
    throw new functions.https.HttpsError("permission-denied", "Payment signature verification failed.");
  }

  // If verified, update the user's profile securely
  const uid = context.auth.uid;
  try {
    await admin.firestore().collection("users").doc(uid).update({
      isSubscribed: true,
      subscriptionEnd: admin.firestore.FieldValue.serverTimestamp() // Could be +1 month logic
    });
    return { success: true };
  } catch (error) {
    console.error("Error updating user document:", error);
    throw new functions.https.HttpsError("internal", "Payment verified, but failed to update profile.");
  }
});
