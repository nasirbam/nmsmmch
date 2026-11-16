package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.NmsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NMS Nurses", appName)
  }

  @Test
  fun `viewModel instantiates and restores nurse password`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm = NmsViewModel(app)
    assertTrue(vm.state.value.nurses.isNotEmpty())

    // Test instant restore with sample nurse_2 (phone 01711000002, hris 1002, dob 1994-08-20)
    val err = vm.repository.restoreNursePasswordInstant(
      phone = "01711000002",
      hris = "1002",
      dobOrReg = "1994-08-20",
      newPass = "MyNewPass99"
    )
    assertNull(err)
    assertNull(vm.repository.loginNurse("01711000002", "MyNewPass99"))
  }
}
