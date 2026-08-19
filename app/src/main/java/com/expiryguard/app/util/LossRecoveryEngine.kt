package com.expiryguard.app.util

import com.expiryguard.app.data.local.ProductEntity

data class LossRecoveryReport(
    val totalAtRisk: Int,
    val criticalCount: Int,
    val expiredCount: Int,
    val highPriorityItems: List<RecoveryAction>,
    val followUpItems: List<RecoveryAction>,
    val allActions: List<RecoveryAction>
)

data class RecoveryAction(
    val product: ProductEntity,
    val daysLeft: Long,
    val riskLevel: String,
    val bestAction: String,
    val whyThisAction: String,
    val doToday: String,
    val doNext: String,
    val notifySupplier: Boolean,
    val discount: Boolean,
    val frontShelf: Boolean,
    val bundle: Boolean,
    val transfer: Boolean,
    val quarantine: Boolean,
    val confidence: Int
)

object LossRecoveryEngine {

    fun analyze(products: List<ProductEntity>, language: String = "en"): LossRecoveryReport {
        val actions = products.map { generateAction(it, language) }.sortedBy { it.daysLeft }
        
        val totalAtRisk = actions.count { it.daysLeft in 0..60 }
        // We match Hindi or English words for "Critical"
        val criticalCount = actions.count { it.riskLevel == "Critical" || it.riskLevel == "गंभीर" || it.riskLevel == "बेहद गंभीर" }
        val expiredCount = actions.count { it.daysLeft < 0 }
        
        val highPriority = actions.filter { it.riskLevel == "Critical" || it.riskLevel == "High" || it.riskLevel == "गंभीर" || it.riskLevel == "बेहद गंभीर" || it.riskLevel == "अधिक" }.take(5)
        val followUps = actions.filter { it.notifySupplier }.take(5)

        return LossRecoveryReport(
            totalAtRisk = totalAtRisk,
            criticalCount = criticalCount,
            expiredCount = expiredCount,
            highPriorityItems = highPriority,
            followUpItems = followUps,
            allActions = actions
        )
    }

    private fun generateAction(product: ProductEntity, language: String): RecoveryAction {
        val daysLeft = DateUtils.daysUntilExpiry(product.expiryDate)
        val category = product.category.lowercase()
        
        // Default flags
        var notifySupplier = false
        var discount = false
        var frontShelf = false
        var bundle = false
        var transfer = false
        var quarantine = false
        var riskLevel = if (language == "hi") "कम" else "Low"
        var bestAction = ""
        var why = ""
        var doToday = ""
        var doNext = ""
        var confidence = 90

        if (daysLeft < 0) {
            riskLevel = if (language == "hi") "बेहद गंभीर" else "Critical"
            quarantine = true
            notifySupplier = true
            if (language == "hi") {
                bestAction = "तुरंत बिक्री से हटाएं और अलग करें (Quarantine)"
                why = "एक्सपायर हो चुकी चीज़ बेचना गैरकानूनी और असुरक्षित है। इससे ग्राहकों का भरोसा टूटता है।"
                doToday = "इसे तुरंत शेल्फ से हटाकर अलग डब्बे (quarantine zone) में रखें।"
                doNext = "अपने डिस्ट्रीब्यूटर से पूछें कि क्या इसे वापस किया जा सकता है। अगर नहीं, तो सुरक्षित रूप से नष्ट कर दें।"
            } else {
                bestAction = "Mark for non-sale & Quarantine immediately."
                why = "Selling expired items is illegal and unsafe. It damages customer trust."
                doToday = "Remove it from the shelf immediately and place it in a separate quarantine bin."
                doNext = "Ask your distributor about their damage/expired return policy. If not returnable, dispose of it safely."
            }
            confidence = 100
            
            return RecoveryAction(product, daysLeft, riskLevel, bestAction, why, doToday, doNext, notifySupplier, discount, frontShelf, bundle, transfer, quarantine, confidence)
        }

        when {
            daysLeft in 0..7 -> {
                riskLevel = if (language == "hi") "गंभीर" else "Critical"
                if (category.contains("dairy") || category.contains("food") || category.contains("grocery")) {
                    if (language == "hi") {
                        bestAction = "क्लीयरेंस सेल (1 पर 1 मुफ्त) + आगे रखें"
                        why = "खाने-पीने की चीजें एक्सपायरी के बाद खराब हो जाएंगी। मार्जिन से ज्यादा स्टॉक निकालना जरूरी है।"
                        doToday = "बिलिंग काउंटर के ठीक सामने क्लीयरेंस बास्केट में डालें।"
                        val nextDays = Math.max(1L, daysLeft / 2)
                        doNext = "अगर $nextDays दिन में न बिके, तो अपने स्टाफ को कहें कि ग्राहकों को सुझाव दें।"
                    } else {
                        bestAction = "Clearance Sale (Buy 1 Get 1 Free) + Front Shelf"
                        why = "Food items will go to waste after expiry. Clearing stock is more important than margin."
                        doToday = "Place it in a clearance basket right in front of the billing counter."
                        val nextDays = Math.max(1L, daysLeft / 2)
                        doNext = "If it doesn't sell in $nextDays days, ask your staff to recommend it to customers."
                    }
                    frontShelf = true
                    discount = true
                    bundle = true
                } else if (category.contains("medicine")) {
                    if (language == "hi") {
                        bestAction = "पुराना स्टॉक क्लियर करें + तुरंत रिटर्न चेक करें"
                        why = "दवाई बहुत कम दिन बचे होने पर नहीं बिकती। डिस्ट्रीब्यूटर से जल्दी बात करनी होगी।"
                        doToday = "अपने डिस्ट्रीब्यूटर को कॉल करके तुरंत रिटर्न या एक्सचेंज की बात करें।"
                        doNext = "अगर रिटर्न न हो, तो शेल्फ पर सबसे आगे रखें और नियमित ग्राहकों को सुझाव दें।"
                    } else {
                        bestAction = "Clear old stock + Urgent Return Check"
                        why = "Medicine doesn't sell well near expiry. You must talk to the distributor quickly."
                        doToday = "Call your distributor to discuss an urgent return or exchange."
                        doNext = "If return is not possible, place it at the front of the shelf and suggest to regular customers."
                    }
                    notifySupplier = true
                    frontShelf = true
                } else {
                    if (language == "hi") {
                        bestAction = "भारी डिस्काउंट + बंडल सेल"
                        why = "सामान जल्दी खराब होगा, पैसा ब्लॉक न हो इसलिए क्लीयरेंस जरूरी है।"
                        doToday = "30-50% ऑफ का टैग लगाकर सबसे आगे डिस्प्ले करें।"
                        doNext = "किसी तेजी से बिकने वाले आइटम के साथ बंडल बना दें।"
                    } else {
                        bestAction = "Heavy Discount + Bundle Sale"
                        why = "Item will perish soon, clearance is necessary so money isn't blocked."
                        doToday = "Display at the front with a 30-50% off tag."
                        doNext = "Bundle it with a fast-selling item."
                    }
                    frontShelf = true
                    discount = true
                    bundle = true
                }
                confidence = 95
            }
            daysLeft in 8..30 -> {
                riskLevel = if (language == "hi") "अधिक" else "High"
                if (category.contains("cosmetic") || category.contains("beauty")) {
                    if (language == "hi") {
                        bestAction = "डिस्ट्रीब्यूटर रिटर्न पॉलिसी चेक करें + बंडल सेल"
                        why = "कॉस्मेटिक्स महंगे होते हैं। एक्सपायरी करीब हो तो लोग चेक करके वापस रख देते हैं।"
                        doToday = "डिस्ट्रीब्यूटर से कन्फर्म करें कि क्रेडिट नोट मिलेगा या नहीं।"
                        doNext = "अगर रिटर्न नहीं हो रहा है, तो इसे तेजी से बिकने वाले आइटम के साथ बंडल कर दें।"
                    } else {
                        bestAction = "Check distributor return policy + Bundle sale"
                        why = "Cosmetics are expensive. Customers check expiry dates and put them back if close."
                        doToday = "Confirm with the distributor if a credit note is possible."
                        doNext = "If not returning, bundle it with a fast-selling item."
                    }
                    notifySupplier = true
                    bundle = true
                    discount = true
                    confidence = 85
                } else {
                    val waitDays = Math.max(2L, daysLeft / 3)
                    if (language == "hi") {
                        bestAction = "पुराना स्टॉक सबसे आगे रखें (क्विक सेल)"
                        why = "$daysLeft दिनों में आइटम आराम से निकल सकते हैं अगर डिस्प्ले अच्छा हो।"
                        doToday = "नया स्टॉक पीछे रखें और इसे शेल्फ में सबसे आगे (front-row) रखें।"
                        doNext = "अगर अगले $waitDays दिनों में सेल नहीं बढ़ती, तो छोटा डिस्काउंट लगाएं।"
                    } else {
                        bestAction = "Place old stock at the front (Quick Sale)"
                        why = "Items can easily clear in $daysLeft days if the display is good."
                        doToday = "Put new stock in the back and place this in the front row of the shelf."
                        doNext = "If sales don't increase in the next $waitDays days, apply a small discount."
                    }
                    frontShelf = true
                }
            }
            daysLeft in 31..60 -> {
                riskLevel = if (language == "hi") "मध्यम" else "Medium"
                if (language == "hi") {
                    bestAction = "स्टाफ की सिफारिश के जरिए बेचें + इंटर-स्टोर ट्रांसफर चेक करें"
                    why = "$daysLeft दिन निकालने के लिए काफी हैं। भारी डिस्काउंट की जरूरत नहीं है।"
                    doToday = "बिलिंग काउंटर पर स्टाफ को कहें कि वे इसकी सिफारिश करें।"
                    doNext = "अगर आपकी कोई दूसरी दुकान है जहां यह जल्दी बिकता है, तो वहां ट्रांसफर कर दें।"
                } else {
                    bestAction = "Push via staff recommendation + Check inter-store transfer"
                    why = "$daysLeft days are enough to clear. Heavy discount is not needed."
                    doToday = "Ask staff at the billing counter to recommend it."
                    doNext = "If you have another store where it sells faster, transfer it there."
                }
                transfer = true
                frontShelf = true
            }
            else -> {
                riskLevel = if (language == "hi") "कम" else "Low"
                if (language == "hi") {
                    bestAction = "होल्ड करें और मॉनिटर करें"
                    why = "अभी काफी दिन बचे हैं। सामान्य सेल होने दें।"
                    doToday = "कोई तत्काल कार्रवाई की जरूरत नहीं।"
                    doNext = "अगले महीने इन्वेंटरी रिव्यू में चेक करेंगे।"
                } else {
                    bestAction = "Hold and monitor"
                    why = "Plenty of days left. Let normal sales continue."
                    doToday = "No urgent action required."
                    doNext = "Check again in next month's inventory review."
                }
            }
        }

        return RecoveryAction(
            product = product,
            daysLeft = daysLeft,
            riskLevel = riskLevel,
            bestAction = bestAction,
            whyThisAction = why,
            doToday = doToday,
            doNext = doNext,
            notifySupplier = notifySupplier,
            discount = discount,
            frontShelf = frontShelf,
            bundle = bundle,
            transfer = transfer,
            quarantine = quarantine,
            confidence = confidence
        )
    }
}
