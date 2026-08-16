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

    fun analyze(products: List<ProductEntity>): LossRecoveryReport {
        val actions = products.map { generateAction(it) }.sortedBy { it.daysLeft }
        
        val totalAtRisk = actions.count { it.daysLeft in 0..60 }
        val criticalCount = actions.count { it.riskLevel == "Critical" }
        val expiredCount = actions.count { it.daysLeft < 0 }
        
        val highPriority = actions.filter { it.riskLevel == "Critical" || it.riskLevel == "High" }.take(5)
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

    private fun generateAction(product: ProductEntity): RecoveryAction {
        val daysLeft = DateUtils.daysUntilExpiry(product.expiryDate)
        val category = product.category.lowercase()
        
        // Default flags
        var notifySupplier = false
        var discount = false
        var frontShelf = false
        var bundle = false
        var transfer = false
        var quarantine = false
        var riskLevel = "Low"
        var bestAction = ""
        var why = ""
        var doToday = ""
        var doNext = ""
        var confidence = 90

        if (daysLeft < 0) {
            riskLevel = "Critical"
            quarantine = true
            notifySupplier = true
            bestAction = "Mark for non-sale & Quarantine immediately."
            why = "Expired item bechna illegal aur unsafe hai. Customer trust kharab hoga."
            doToday = "Turant shelf se hata kar alag dabbe (quarantine zone) me rakho."
            doNext = "Distributor se pucho damage/expired replacement policy kya hai. Agar wapas nahi hota toh dispose kar do."
            confidence = 100
            
            return RecoveryAction(product, daysLeft, riskLevel, bestAction, why, doToday, doNext, notifySupplier, discount, frontShelf, bundle, transfer, quarantine, confidence)
        }

        when {
            daysLeft in 0..7 -> {
                riskLevel = "Critical"
                if (category.contains("dairy") || category.contains("food") || category.contains("grocery")) {
                    bestAction = "Clearance Sale (Buy 1 Get 1 Free) + Front Shelf"
                    why = "Khane peene ki cheez expiry ke baad waste ho jayegi. Margin se zyada stock nikalna zaroori hai."
                    doToday = "Billing counter ke theek samne clearance basket me daalo."
                    val nextDays = Math.max(1L, daysLeft / 2)
                    doNext = "Agar $nextDays din me na bike toh apne staff ko bolo customers ko suggest karein."
                    frontShelf = true
                    discount = true
                    bundle = true
                } else if (category.contains("medicine")) {
                    bestAction = "Purana stock clear karo + Urgent Return Check"
                    why = "Dawai bahut kam din bache hone par nahi biktī. Distributor se jaldi baat karni hogi."
                    doToday = "Apne distributor ko call karke urgent return ya exchange ki baat karo."
                    doNext = "Agar return na ho, toh shelf par sabse aage rakho aur regular customers ko suggest karo."
                    notifySupplier = true
                    frontShelf = true
                } else {
                    bestAction = "Heavy Discount + Bundle Sale"
                    why = "Samaan jaldi kharab hoga, paisa block na ho isliye clearance zaroori hai."
                    doToday = "30-50% off ka tag lagakar sabse aage display karo."
                    doNext = "Kisi fast-selling item ke sath bundle bana do."
                    frontShelf = true
                    discount = true
                    bundle = true
                }
                confidence = 95
            }
            daysLeft in 8..30 -> {
                riskLevel = "High"
                if (category.contains("cosmetic") || category.contains("beauty")) {
                    bestAction = "Distributor return policy check + Bundle sale"
                    why = "Cosmetics expensive hote hain. Expiry kareeb ho toh log check karke wapas rakh dete hain."
                    doToday = "Distributor se confirm karo ki credit note milega ya nahi."
                    doNext = "Agar return nahi ho raha, toh isko fast-selling item ke sath bundle kardo."
                    notifySupplier = true
                    bundle = true
                    discount = true
                    confidence = 85
                } else {
                    bestAction = "Purana stock sabse aage rakho (Quick Sale)"
                    val waitDays = Math.max(2L, daysLeft / 3)
                    why = "$daysLeft din me items aaram se nikal sakte hain agar display accha ho."
                    doToday = "Naya stock piche rakho aur ise shelf me sabse aage (front-row) me rakho."
                    doNext = "Agar agle $waitDays din me sale nahi badhti, toh chhota discount lagao."
                    frontShelf = true
                }
            }
            daysLeft in 31..60 -> {
                riskLevel = "Medium"
                bestAction = "Push via staff recommendation + Check inter-store transfer"
                why = "$daysLeft din kaafi hain nikalne ke liye. Heavy discount ki zaroorat nahi hai."
                doToday = "Billing counter par staff ko bolo ki ise recommend karein."
                doNext = "Agar aapke paas dusri dukan hai jahan ye jaldi bikta hai, wahan transfer kar do."
                transfer = true
                frontShelf = true
            }
            else -> {
                riskLevel = "Low"
                bestAction = "Hold and monitor"
                why = "Abhi kaafi din bache hain. Normal sale hone do."
                doToday = "Koi urgent action ki zaroorat nahi."
                doNext = "Next month inventory review me check karenge."
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
