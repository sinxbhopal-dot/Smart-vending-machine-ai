package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.VendingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    assertEquals("Smart Vending Kiosk", appName)
  }

  @Test
  fun `admin password verification with master password`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = VendingViewModel(app)

    viewModel.requestAdminAccess()
    assertTrue(viewModel.isPasswordPromptOpen.value)
    assertFalse(viewModel.isConsoleOpen.value)

    // Test incorrect password
    val failed = viewModel.verifyAndUnlockAdmin("invalid123")
    assertFalse(failed)
    assertTrue(viewModel.isPasswordPromptOpen.value)
    assertFalse(viewModel.isConsoleOpen.value)

    // Test correct master password 8103551677
    val success = viewModel.verifyAndUnlockAdmin("8103551677")
    assertTrue(success)
    assertFalse(viewModel.isPasswordPromptOpen.value)
    assertTrue(viewModel.isConsoleOpen.value)
  }

  @Test
  fun `automatic inventory deduction and out of stock detection`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = VendingViewModel(app)

    // Set slot 1 stock to 1
    viewModel.restockSlot(slotId = 1, newQty = 1)
    val slot1Before = viewModel.slots.value.first { it.slotId == 1 }
    assertEquals(1, slot1Before.quantity)
    assertTrue(slot1Before.isAvailable)

    // Deduct stock (e.g. after successful purchase/dispense)
    viewModel.decrementSlotQuantity(1)
    val slot1After = viewModel.slots.value.first { it.slotId == 1 }
    assertEquals(0, slot1After.quantity)
    assertFalse(slot1After.isAvailable)

    // Ensure it doesn't go below 0
    viewModel.decrementSlotQuantity(1)
    val slot1Zero = viewModel.slots.value.first { it.slotId == 1 }
    assertEquals(0, slot1Zero.quantity)
    assertFalse(slot1Zero.isAvailable)
  }

  @Test
  fun `manual product and inventory edit persists across instances`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = VendingViewModel(app)

    val originalSlot = viewModel.slots.value.first { it.slotId == 2 }
    val updatedSlot = originalSlot.copy(
      title = "Custom Cyber Energy Drink",
      priceInr = 99.0,
      quantity = 15,
      subDescription = "Zero Sugar Focus Formulation"
    )

    viewModel.updateSlot(updatedSlot)

    // Create a new ViewModel instance to simulate app restart
    val newViewModel = VendingViewModel(app)
    val persistedSlot = newViewModel.slots.value.first { it.slotId == 2 }

    assertEquals("Custom Cyber Energy Drink", persistedSlot.title)
    assertEquals(99.0, persistedSlot.priceInr, 0.001)
    assertEquals(15, persistedSlot.quantity)
    assertEquals("Zero Sugar Focus Formulation", persistedSlot.subDescription)
    assertTrue(persistedSlot.isAvailable)
  }
}

