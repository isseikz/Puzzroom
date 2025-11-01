package tokyo.isseikuzumaki.nolotracker.domain.parser

/**
 * Result of payment information parsing.
 * 
 * @property amount Extracted amount with currency (e.g., "1500 JPY")
 * @property merchant Extracted merchant/store name (e.g., "Starbucks")
 */
data class PaymentInfo(
    val amount: String,
    val merchant: String
)

/**
 * Parser for extracting payment information from notification text.
 * 
 * This parser uses regular expressions to extract payment amounts and merchant names
 * from payment application notifications. It supports common Japanese payment apps
 * and notification formats.
 */
object PaymentParser {
    
    // Regex patterns for common payment notification formats
    private val amountPatterns = listOf(
        // Matches: "1,500円", "1500円", "¥1,500", etc.
        Regex("""¥?([0-9,]+)\s*円"""),
        Regex("""¥\s?([0-9,]+)"""),
        // Matches: "1500 JPY", "JPY 1500"
        Regex("""([0-9,]+)\s*JPY"""),
        Regex("""JPY\s*([0-9,]+)"""),
        // Matches: "$15.00", "USD 15.00"
        Regex("""\$\s?([0-9,]+\.?\d{0,2})"""),
        Regex("""([0-9,]+\.?\d{0,2})\s*USD""")
    )
    
    private val merchantPatterns = listOf(
        // Matches: "at Starbucks", "でスターバックス", etc.
        Regex("""(?:at|で|にて)\s+([^\s]+(?:\s+[^\s]+)?)"""),
        // Matches merchant names in quotes
        Regex("""「([^」]+)」"""),
        Regex(""""([^"]+)""""),
        // Matches store/shop patterns
        Regex("""([^\s]+(?:店|ストア|ショップ))""")
    )
    
    /**
     * Parses payment information from notification text.
     * 
     * Attempts to extract both amount and merchant name from the provided text.
     * Returns null if either amount or merchant cannot be extracted.
     * 
     * @param text Raw notification text to parse
     * @return PaymentInfo if parsing succeeds, null otherwise
     */
    fun parse(text: String): PaymentInfo? {
        val amount = extractAmount(text) ?: return null
        val merchant = extractMerchant(text) ?: return null
        
        return PaymentInfo(amount = amount, merchant = merchant)
    }
    
    /**
     * Extracts payment amount from text.
     * 
     * @param text Text to extract amount from
     * @return Formatted amount string (e.g., "1500 JPY") or null if not found
     */
    private fun extractAmount(text: String): String? {
        for (pattern in amountPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val rawAmount = match.groupValues[1].replace(",", "")
                
                // Determine currency based on which pattern matched
                val currency = when {
                    text.contains("円") || text.contains("¥") -> "JPY"
                    text.contains("USD") || text.contains("$") -> "USD"
                    else -> "JPY" // Default to JPY for Japanese app
                }
                
                return "$rawAmount $currency"
            }
        }
        return null
    }
    
    /**
     * Extracts merchant name from text.
     * 
     * @param text Text to extract merchant from
     * @return Merchant name or null if not found
     */
    private fun extractMerchant(text: String): String? {
        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }
        return null
    }
}
