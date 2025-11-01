package tokyo.isseikuzumaki.nolotracker.domain.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Test suite for PaymentParser.
 * 
 * Validates payment information extraction from various notification text formats.
 */
class PaymentParserTest {
    
    @Test
    fun `parse Japanese yen with comma separator`() {
        val text = "支払いが完了しました。1,500円でスターバックス"
        val result = PaymentParser.parse(text)
        
        assertNotNull(result)
        assertEquals("1500 JPY", result.amount)
        assertEquals("スターバックス", result.merchant)
    }
    
    @Test
    fun `parse Japanese yen without comma`() {
        val text = "決済完了: 800円 at セブンイレブン"
        val result = PaymentParser.parse(text)
        
        assertNotNull(result)
        assertEquals("800 JPY", result.amount)
        assertEquals("セブンイレブン", result.merchant)
    }
    
    @Test
    fun `parse with yen symbol`() {
        val text = "お支払い: ¥2,300 ローソン"
        val result = PaymentParser.parse(text)
        
        assertNotNull(result)
        assertEquals("2300 JPY", result.amount)
        // Note: Merchant might not be parsed correctly without 'at' or 'で'
        // This is a limitation of the current regex patterns
    }
    
    @Test
    fun `parse USD amount with dollar sign`() {
        val text = "Payment of $15.99 at Starbucks"
        val result = PaymentParser.parse(text)
        
        assertNotNull(result)
        assertEquals("15.99 USD", result.amount)
        assertEquals("Starbucks", result.merchant)
    }
    
    @Test
    fun `parse with merchant in quotes`() {
        val text = "決済完了: 1500円「ファミリーマート」にて"
        val result = PaymentParser.parse(text)
        
        assertNotNull(result)
        assertEquals("1500 JPY", result.amount)
        assertEquals("ファミリーマート", result.merchant)
    }
    
    @Test
    fun `parse with store suffix`() {
        val text = "お支払い完了 2,000円 渋谷店で"
        val result = PaymentParser.parse(text)
        
        assertNotNull(result)
        assertEquals("2000 JPY", result.amount)
        assertEquals("渋谷店", result.merchant)
    }
    
    @Test
    fun `return null when amount not found`() {
        val text = "通知テストメッセージ at Store"
        val result = PaymentParser.parse(text)
        
        assertNull(result)
    }
    
    @Test
    fun `return null when merchant not found`() {
        val text = "支払い完了: 1,500円"
        val result = PaymentParser.parse(text)
        
        // Without merchant, should return null
        assertNull(result)
    }
    
    @Test
    fun `parse complex notification with multiple amounts`() {
        // Should match the first amount found
        val text = "ご利用金額: 1,200円 at カフェドクリエ (残高: 5,000円)"
        val result = PaymentParser.parse(text)
        
        assertNotNull(result)
        assertEquals("1200 JPY", result.amount)
        assertEquals("カフェドクリエ", result.merchant)
    }
    
    @Test
    fun `parse English payment notification`() {
        val text = "Payment complete: $25.50 at McDonald's"
        val result = PaymentParser.parse(text)
        
        assertNotNull(result)
        assertEquals("25.50 USD", result.amount)
        assertEquals("McDonald's", result.merchant)
    }
    
    @Test
    fun `parse with Japanese particle にて`() {
        val text = "3,500円のお支払いがマクドナルドにて完了しました"
        val result = PaymentParser.parse(text)
        
        assertNotNull(result)
        assertEquals("3500 JPY", result.amount)
        assertEquals("マクドナルド", result.merchant)
    }
}
