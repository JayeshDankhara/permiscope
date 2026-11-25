package io.github.jayeshdankhara.permiscope.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitivityTest {

    @Test
    fun everyRuntimePermissionMapsToItsGroup() {
        Sensitivity.runtime.forEach { group ->
            assertTrue("${group.name} has no permissions", group.permissions.isNotEmpty())
            group.permissions.forEach { assertEquals(group, Sensitivity.byPermission[it]) }
        }
    }

    @Test
    fun noPermissionBelongsToTwoGroups() {
        val all = Sensitivity.runtime.flatMap { it.permissions }
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun specialAccessHasNoRuntimePermissions() {
        Sensitivity.specialAccess.forEach { assertTrue(it.permissions.isEmpty()) }
    }

    @Test
    fun weightsArePositive() {
        Sensitivity.entries.forEach { assertTrue(it.weight > 0) }
    }
}
