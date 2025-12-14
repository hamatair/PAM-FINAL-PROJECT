package com.example.pam_1.utils

import kotlin.random.Random

object InviteCodeGenerator {
    private const val CODE_LENGTH = 6
    private const val CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Exclude ambiguous chars: I, O, 0, 1

    /**
     * Generate a human-friendly invite code Format: 6 characters, uppercase alphanumeric Example:
     * ABC123, XYZ789
     */
    fun generateCode(): String {
        return (1..CODE_LENGTH).map { CHARS[Random.nextInt(CHARS.length)] }.joinToString("")
    }

    /** Generate a code with custom length */
    fun generateCode(length: Int): String {
        require(length > 0) { "Code length must be positive" }
        return (1..length).map { CHARS[Random.nextInt(CHARS.length)] }.joinToString("")
    }

    /** Validate if a code matches the expected format */
    fun isValidFormat(code: String): Boolean {
        if (code.length < 6) return false
        return code.all { it in CHARS }
    }
}
