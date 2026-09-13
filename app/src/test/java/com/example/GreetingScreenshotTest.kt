package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.domain.model.ActionMacro
import com.example.domain.model.Macro
import com.example.domain.model.Trigger
import com.example.presentation.theme.CtrlTheme
import com.example.presentation.theme.components.CtrlMacroCard
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleMacro = Macro(
      id = "test_macro",
      nom = "Mode Nuit Automatique",
      description = "Coupe le Bluetooth et baisse le volume",
      active = true,
      trigger = Trigger.HeureFixe(heure = 22, minute = 30),
      conditions = emptyList(),
      actions = listOf(ActionMacro.EnvoyerNotification("Ctrl", "Nuit")),
      dateCreation = 1000L
    )

    composeTestRule.setContent {
      CtrlTheme {
        CtrlMacroCard(
          macro = sampleMacro,
          onToggleActive = {},
          onTestClick = {},
          onDetailsClick = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

