package tokyo.isseikuzumaki.vibeterminal.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

private const val SERVICE = "tokyo.isseikuzumaki.vibeterminal"

@OptIn(ExperimentalForeignApi::class)
object KeychainHelper {

    /**
     * Store a secret string in the Keychain under the given key.
     */
    fun save(key: String, value: String): Boolean {
        val data = NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding)
            ?: return false
        // Delete any existing item first
        delete(key)

        // Convert NSData to CFData via raw bytes to avoid ObjC/CF bridging issues
        val cfData = CFDataCreate(
            kCFAllocatorDefault,
            data.bytes?.reinterpret(),
            data.length.toLong()
        ) ?: return false

        val serviceStr = cfString(SERVICE) ?: run { CFRelease(cfData); return false }
        val accountStr = cfString(key) ?: run { CFRelease(cfData); return false }
        // Explicitly retain CF strings — Kotlin/Native ARC may release them after the last
        // lexical reference (i.e. after the CFDictionarySetValue calls below), but
        // SecItemAdd internally calls CFDictionaryCopyMutable which still needs the values.
        CFRetain(serviceStr)
        CFRetain(accountStr)

        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)!!
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, serviceStr)
        CFDictionarySetValue(query, kSecAttrAccount, accountStr)
        CFDictionarySetValue(query, kSecValueData, cfData)

        val status = SecItemAdd(query, null)
        CFRelease(serviceStr)
        CFRelease(accountStr)
        CFRelease(cfData)
        CFRelease(query)
        return status == errSecSuccess
    }

    /**
     * Retrieve a secret string from the Keychain for the given key.
     */
    fun load(key: String): String? {
        val serviceStr = cfString(SERVICE) ?: return null
        val accountStr = cfString(key) ?: return null
        // Explicitly retain CF strings — Kotlin/Native ARC may release them after the last
        // lexical reference (i.e. after the CFDictionarySetValue calls below), but
        // SecItemCopyMatching internally calls CFDictionaryCopyMutable which still needs
        // the values alive.
        CFRetain(serviceStr)
        CFRetain(accountStr)

        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)!!
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, serviceStr)
        CFDictionarySetValue(query, kSecAttrAccount, accountStr)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)

        var result: String? = null
        memScoped {
            val dataRef = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, dataRef.ptr)
            if (status == errSecSuccess) {
                val nsData = dataRef.value as? NSData
                if (nsData != null) {
                    result = NSString.create(data = nsData, encoding = NSUTF8StringEncoding) as? String
                }
            }
        }

        CFRelease(serviceStr)
        CFRelease(accountStr)
        CFRelease(query)
        return result
    }

    /**
     * Delete a stored item from the Keychain.
     */
    fun delete(key: String): Boolean {
        val serviceStr = cfString(SERVICE) ?: return false
        val accountStr = cfString(key) ?: return false
        CFRetain(serviceStr)
        CFRetain(accountStr)

        val query = CFDictionaryCreateMutable(kCFAllocatorDefault, 0, null, null)!!
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, serviceStr)
        CFDictionarySetValue(query, kSecAttrAccount, accountStr)

        val status = SecItemDelete(query)
        CFRelease(serviceStr)
        CFRelease(accountStr)
        CFRelease(query)
        return status == errSecSuccess
    }

    private fun cfString(value: String): CFStringRef? =
        NSString.create(string = value) as? CFStringRef
}
