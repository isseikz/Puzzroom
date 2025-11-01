package tokyo.isseikuzumaki.nlt.domain.parser

/**
 * Parser for extracting payment information from notification text
 */
object PaymentParser {
    
    private val amountPatterns = listOf(
        // JPY format: ¥1,234 or ¥1234
        Regex("""¥\s*([\d,]+)"""),
        // Japanese yen: 1,234円 or 1234円
        Regex("""([\d,]+)\s*円"""),
        // English: $1,234.56 or $1234
        Regex("""\$\s*([\d,]+(?:\.\d{2})?)"""),
        // Generic: 1,234 JPY or 1234 USD
        Regex("""([\d,]+(?:\.\d{2})?)\s*(JPY|USD|EUR|yen|円)""", RegexOption.IGNORE_CASE)
    )
    
    private val merchantPatterns = listOf(
        // After "at" or "で"
        Regex("""(?:at|で)\s+([A-Za-z0-9\s\p{L}]+)""", RegexOption.IGNORE_CASE),
        // Between quotes
        Regex("""["""']([^"""']+)["""']"""),
        // Store name pattern (capitalized words)
        Regex("""([A-Z][a-z]+(?:\s+[A-Z][a-z]+)*)""")
    )
    
    /**
     * Parse payment information from notification text
     * 
     * @param title Notification title
     * @param text Notification body text
     * @return Pair of (amount, merchant) or nulls if not found
     */
    fun parse(title: String?, text: String): PaymentInfo {
        val fullText = "${title ?: ""} $text"
        
        val amount = parseAmount(fullText)
        val merchant = parseMerchant(fullText)
        
        return PaymentInfo(
            amount = amount,
            merchant = merchant,
            isParsed = amount != null || merchant != null
        )
    }
    
    /**
     * Extract amount from text
     */
    private fun parseAmount(text: String): String? {
        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val value = match.groupValues[1].replace(",", "")
                val currency = if (match.groupValues.size > 2) {
                    match.groupValues[2].uppercase()
                } else {
                    if (text.contains("¥") || text.contains("円")) "JPY"
                    else if (text.contains("$")) "USD"
                    else "JPY" // Default to JPY
                }
                return "$value $currency"
            }
        }
        return null
    }
    
    /**
     * Extract merchant name from text
     */
    private fun parseMerchant(text: String): String? {
        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            if (match != null && match.groupValues.size > 1) {
                val merchant = match.groupValues[1].trim()
                // Filter out common non-merchant words
                if (merchant.length > 2 && !isCommonWord(merchant)) {
                    return merchant
                }
            }
        }
        return null
    }
    
    private fun isCommonWord(word: String): Boolean {
        val commonWords = setOf(
            "payment", "paid", "transaction", "purchase", "buy", "bought",
            "支払い", "決済", "購入", "利用"
        )
        return commonWords.any { word.contains(it, ignoreCase = true) }
    }
}

/**
 * Parsed payment information
 */
data class PaymentInfo(
    val amount: String?,
    val merchant: String?,
    val isParsed: Boolean
)
