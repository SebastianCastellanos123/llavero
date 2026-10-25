package io.github.sebastiancastellanos123.llavero.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockPolicyTest {
    private var clock = 1_000L
    private fun policy(enabled: Boolean = true) = LockPolicy(enabled, graceMillis = 60_000, now = { clock })

    @Test
    fun startsLockedWhenEnabled() {
        assertTrue(policy().locked.value)
        assertFalse(policy(enabled = false).locked.value)
    }

    @Test
    fun staysUnlockedWithinGracePeriod() {
        val p = policy()
        p.unlock()
        p.onBackground()
        clock += 59_999
        p.onForeground(lockEnabled = true)
        assertFalse(p.locked.value)
    }

    @Test
    fun locksAfterGracePeriod() {
        val p = policy()
        p.unlock()
        p.onBackground()
        clock += 60_000
        p.onForeground(lockEnabled = true)
        assertTrue(p.locked.value)
    }

    @Test
    fun foregroundWithoutBackgroundDoesNotLock() {
        val p = policy()
        p.unlock()
        clock += 10 * 60_000
        p.onForeground(lockEnabled = true)
        assertFalse(p.locked.value)
    }

    @Test
    fun timeSpentLockedDoesNotCount() {
        // Mientras se muestra la pantalla del PIN la app sale de primer plano: eso no debe
        // dejar un "salió a las X" que luego la vuelva a bloquear apenas se desbloquea.
        val p = policy()
        p.onBackground()
        clock += 5 * 60_000
        p.unlock()
        p.onForeground(lockEnabled = true)
        assertFalse(p.locked.value)
    }

    @Test
    fun gracePeriodIsMeasuredFromLastBackground() {
        val p = policy()
        p.unlock()
        p.onBackground()
        clock += 30_000
        p.onForeground(lockEnabled = true)
        p.onBackground()
        clock += 59_000
        p.onForeground(lockEnabled = true)
        assertFalse(p.locked.value)
    }

    @Test
    fun disablingLockUnlocks() {
        val p = policy()
        p.onForeground(lockEnabled = false)
        assertFalse(p.locked.value)
    }

    @Test
    fun neverLocksWhenDisabled() {
        val p = policy(enabled = false)
        p.onBackground()
        clock += 10 * 60_000
        p.onForeground(lockEnabled = false)
        assertFalse(p.locked.value)
    }
}
