package ca.georgebrown.studygroupfinder

import ca.georgebrown.studygroupfinder.ui.auth.AuthValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Normal, edge and invalid inputs for every Login / Sign Up rule. Run: gradlew testDebugUnitTest */
class AuthValidatorTest {

    // --- email ---
    @Test fun `college email is accepted`() = assertNull(AuthValidator.validateEmail("aisha.patel@georgebrown.ca"))
    @Test fun `email is trimmed and case-insensitive`() = assertNull(AuthValidator.validateEmail("  Aisha@GeorgeBrown.CA "))
    @Test fun `empty email is rejected`() = assertEquals("Enter your college email.", AuthValidator.validateEmail(""))
    @Test fun `malformed email is rejected`() = assertNotNull(AuthValidator.validateEmail("aisha@"))
    @Test fun `non college domain is rejected`() = assertNotNull(AuthValidator.validateEmail("aisha@gmail.com"))
    @Test fun `lookalike domain is rejected`() = assertNotNull(AuthValidator.validateEmail("a@evilgeorgebrown.ca"))

    // --- full name ---
    @Test fun `normal name is accepted`() = assertNull(AuthValidator.validateFullName("Aisha Patel"))
    @Test fun `blank name is rejected`() = assertNotNull(AuthValidator.validateFullName("   "))
    @Test fun `one letter name is rejected`() = assertNotNull(AuthValidator.validateFullName("A"))

    // --- passwords ---
    @Test fun `login only needs a non-empty password`() = assertNull(AuthValidator.validateLoginPassword("x"))
    @Test fun `login rejects empty password`() = assertNotNull(AuthValidator.validateLoginPassword(""))
    @Test fun `strong new password is accepted`() = assertNull(AuthValidator.validateNewPassword("password123"))
    @Test fun `exactly minimum length is accepted`() = assertNull(AuthValidator.validateNewPassword("abcdefg1"))
    @Test fun `short new password is rejected`() = assertNotNull(AuthValidator.validateNewPassword("abc1234"))
    @Test fun `letters only password is rejected`() = assertNotNull(AuthValidator.validateNewPassword("abcdefghij"))
    @Test fun `digits only password is rejected`() = assertNotNull(AuthValidator.validateNewPassword("1234567890"))

    // --- confirm ---
    @Test fun `matching confirm is accepted`() = assertNull(AuthValidator.validateConfirmPassword("password123", "password123"))
    @Test fun `mismatched confirm is rejected`() = assertEquals("Passwords must match.", AuthValidator.validateConfirmPassword("password123", "password124"))
    @Test fun `empty confirm is rejected`() = assertNotNull(AuthValidator.validateConfirmPassword("password123", ""))
}
