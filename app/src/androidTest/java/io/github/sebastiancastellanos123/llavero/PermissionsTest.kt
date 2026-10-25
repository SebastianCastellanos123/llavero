package io.github.sebastiancastellanos123.llavero

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Revisa el manifiesto final, ya mezclado con el de todas las librerías:
 * si alguna dependencia agregara INTERNET u otro permiso, esta prueba falla.
 */
@RunWith(AndroidJUnit4::class)
class PermissionsTest {
    @Test
    fun onlyCameraPermissionIsRequested() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions.orEmpty().toSet()
            // Permiso interno que AndroidX agrega para sus propios receptores; no da acceso a nada externo.
            .filterNot { it.endsWith(".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION") }
            .toSet()
        assertEquals(setOf(Manifest.permission.CAMERA), requested)
    }
}
