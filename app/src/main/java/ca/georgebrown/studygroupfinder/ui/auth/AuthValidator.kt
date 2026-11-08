package ca.georgebrown.studygroupfinder.ui.auth

/**
 * Pure input rules for Login / Sign Up (FR-01). No Android imports, so it is unit-tested on the JVM
 * (see AuthValidatorTest). Each function returns an error message, or null when the input is fine.
 */
object AuthValidator {

    /**
     * Email domains accepted at sign-up and login. Empty = any email.
     *
     * The design proposal (FR-01) says college emails only. We allow any email for now so testers
     * without a GBC address can sign up; set this to listOf("georgebrown.ca") to enforce FR-01 again.
     */
    val allowedEmailDomains: List<String> = emptyList()

    /** True if [email]'s domain is allowed. Exact match, so "evilgeorgebrown.ca" doesn't pass for "georgebrown.ca". */
    fun isAllowedDomain(email: String, allowedDomains: List<String> = allowedEmailDomains): Boolean =
        allowedDomains.isEmpty() || email.trim().substringAfter('@').lowercase() in allowedDomains

    const val MIN_PASSWORD_LENGTH = 8

    private val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateFullName(name: String): String? = when {
        name.isBlank() -> "Enter your full name."
        name.trim().length < 2 -> "Name is too short."
        else -> null
    }

    fun validateEmail(email: String, allowedDomains: List<String> = allowedEmailDomains): String? {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return "Enter your email."
        if (!emailPattern.matches(trimmed)) return "That doesn't look like an email address."
        if (!isAllowedDomain(trimmed, allowedDomains)) return "Use your college email (@${allowedDomains.first()})."
        return null
    }

    /** Login only checks the field isn't empty; the length rule applies when creating a password. */
    fun validateLoginPassword(password: String): String? =
        if (password.isEmpty()) "Enter your password." else null

    fun validateNewPassword(password: String): String? = when {
        password.isEmpty() -> "Choose a password."
        password.length < MIN_PASSWORD_LENGTH -> "Use at least $MIN_PASSWORD_LENGTH characters."
        password.none { it.isDigit() } || password.none { it.isLetter() } -> "Use both letters and numbers."
        else -> null
    }

    fun validateConfirmPassword(password: String, confirm: String): String? = when {
        confirm.isEmpty() -> "Confirm your password."
        confirm != password -> "Passwords must match."
        else -> null
    }
}
