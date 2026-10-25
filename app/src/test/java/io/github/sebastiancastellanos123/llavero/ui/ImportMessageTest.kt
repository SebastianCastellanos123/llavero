package io.github.sebastiancastellanos123.llavero.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportMessageTest {
    @Test
    fun singularAndPlural() {
        assertEquals("Se importó 1 cuenta", importMessage(1, 0))
        assertEquals("Se importaron 3 cuentas", importMessage(3, 0))
        assertEquals("Se importó 1 cuenta (1 ya estaba)", importMessage(1, 1))
        assertEquals("Se importaron 2 cuentas (4 ya estaban)", importMessage(2, 4))
    }
}
