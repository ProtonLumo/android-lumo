package me.proton.android.lumo.config

import me.proton.android.lumo.BuildConfig

/**
 * Configuration object that centralizes all environment-specific settings
 */
object LumoConfig {

    /**
     * Environment name - can be set via build config or defaults to empty string for production
     */
    private val ENV_NAME: String = BuildConfig.ENV_NAME.ifEmpty { "" }

    /**
     * Base domain suffix - defaults to "proton.me" for production, can be "proton.black" for staging
     */
    private val BASE_DOMAIN: String = BuildConfig.BASE_DOMAIN.ifEmpty { "proton.me" }

    /**
     * Lumo domain URL
     */
    val LUMO_DOMAIN: String = buildDomain("lumo")

    /**
     * Account domain URL
     */
    val ACCOUNT_DOMAIN: String = buildDomain("account")

    /**
     * Full Lumo URL with HTTPS
     */
    val LUMO_URL: String = "https://$LUMO_DOMAIN"

    /**
     * Full Account URL with HTTPS
     */
    val ACCOUNT_URL: String = "https://$ACCOUNT_DOMAIN"

    /**
     * Builds domain name based on environment configuration
     * Format: {service}.{ENV_NAME}{BASE_DOMAIN}
     * Examples:
     * - Production: lumo.proton.me, account.proton.me
     * - Palladium: lumo.palladium.proton.black, account.palladium.proton.black
     */
    private fun buildDomain(service: String): String {
        return if (ENV_NAME.isNotEmpty()) {
            "$service.$ENV_NAME$BASE_DOMAIN"
        } else {
            "$service.$BASE_DOMAIN"
        }
    }

    /**
     * Checks if a URL belongs to the Lumo domain
     */
    fun isLumoDomain(url: String?): Boolean = url?.contains(LUMO_DOMAIN) == true

    /**
     * Checks if a URL belongs to the Account domain
     */
    fun isAccountDomain(url: String?): Boolean = url?.contains(ACCOUNT_DOMAIN) == true

    /**
     * Checks if a URL belongs to any of our configured domains
     */
    fun isKnownDomain(url: String?): Boolean = isLumoDomain(url) || isAccountDomain(url)

    /**
     * Get all configured domains for logging/debugging
     */
    fun getConfigInfo(): String {
        return """
            LumoConfig:
            - ENV_NAME: '$ENV_NAME'
            - BASE_DOMAIN: '$BASE_DOMAIN'
            - LUMO_DOMAIN: '$LUMO_DOMAIN'
            - ACCOUNT_DOMAIN: '$ACCOUNT_DOMAIN'
            - LUMO_URL: '$LUMO_URL'
            - ACCOUNT_URL: '$ACCOUNT_URL'
        """.trimIndent()
    }
} 