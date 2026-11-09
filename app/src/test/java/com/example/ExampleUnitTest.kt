package com.example

import com.example.ai.GeminiAiService
import com.example.data.model.ActionType
import com.example.data.model.AssistantType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val geminiService = GeminiAiService()

    @Test
    fun assistantSwitch_targetCorrect() {
        assertEquals(AssistantType.MAYA, AssistantType.KING.switchTarget())
        assertEquals(AssistantType.KING, AssistantType.MAYA.switchTarget())
    }

    @Test
    fun voiceSwitching_kingToMaya_detected() {
        val switch1 = geminiService.checkAssistantSwitchCommand("KING, call MAYA", AssistantType.KING)
        assertEquals(AssistantType.MAYA, switch1)

        val switch2 = geminiService.checkAssistantSwitchCommand("Maya ko bulao", AssistantType.KING)
        assertEquals(AssistantType.MAYA, switch2)

        val switch3 = geminiService.checkAssistantSwitchCommand("Maya ne bolav", AssistantType.KING)
        assertEquals(AssistantType.MAYA, switch3)
    }

    @Test
    fun voiceSwitching_mayaToKing_detected() {
        val switch1 = geminiService.checkAssistantSwitchCommand("MAYA, call KING", AssistantType.MAYA)
        assertEquals(AssistantType.KING, switch1)

        val switch2 = geminiService.checkAssistantSwitchCommand("King ko bulao", AssistantType.MAYA)
        assertEquals(AssistantType.KING, switch2)

        val switch3 = geminiService.checkAssistantSwitchCommand("King ne bolav", AssistantType.MAYA)
        assertEquals(AssistantType.KING, switch3)
    }

    @Test
    fun voiceSwitching_sameAssistant_doesNotTrigger() {
        val noSwitch = geminiService.checkAssistantSwitchCommand("KING, call MAYA", AssistantType.MAYA)
        assertNull(noSwitch)
    }

    @Test
    fun mobileControl_whatsAppOpen_detected() {
        val res = geminiService.checkLocalMobileIntent("KING, WhatsApp open karo", AssistantType.KING)
        assertNotNull(res)
        assertEquals(ActionType.OPEN_WHATSAPP, res?.actionType)
    }

    @Test
    fun mobileControl_whatsAppMessageConfirmation_required() {
        val res = geminiService.checkLocalMobileIntent("MAYA, Rahul ko message likho meeting starts at 5", AssistantType.MAYA)
        assertNotNull(res)
        assertEquals(ActionType.SEND_WHATSAPP, res?.actionType)
        assertEquals("Rahul", res?.actionTarget)
        assertTrue(res?.requiresConfirmation == true)
        assertTrue(res?.actionPayload?.contains("meeting starts at 5") == true)
    }

    @Test
    fun mobileControl_youTubeSearch_detected() {
        val res = geminiService.checkLocalMobileIntent("play Arijit Singh songs on youtube", AssistantType.KING)
        assertNotNull(res)
        assertEquals(ActionType.SEARCH_YOUTUBE, res?.actionType)
    }

    @Test
    fun mobileControl_cameraAndPhone_detected() {
        val cameraRes = geminiService.checkLocalMobileIntent("camera open karo", AssistantType.KING)
        assertEquals(ActionType.OPEN_CAMERA, cameraRes?.actionType)

        val phoneRes = geminiService.checkLocalMobileIntent("phone dialer kholo", AssistantType.MAYA)
        assertEquals(ActionType.OPEN_PHONE, phoneRes?.actionType)
    }
}
