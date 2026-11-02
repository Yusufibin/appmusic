package com.company.product

import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class TelegramProtocolTest {
    @Test fun `normal private messages preserve the complete authorization context`() {
        val updates = TelegramProtocol.updates(JSONArray("""[{"update_id":10,"message":{"chat":{"id":42,"type":"private"},"from":{"id":42,"is_bot":false},"date":1700000000,"text":"/ping"}}]"""))
        assertEquals(10L, updates.single().id)
        val message = updates.single().message!!
        assertTrue(CommandPolicy.authorized(message, 42, 1700000000))
        assertEquals("/ping", message.text)
    }
    @Test fun `callbacks and non-message updates are never routed`() {
        val updates = TelegramProtocol.updates(JSONArray("""[{"update_id":11,"callback_query":{"data":"select:device"}},{"update_id":12,"edited_message":{"text":"/ping"}}]"""))
        assertEquals(listOf(11L, 12L), updates.map { it.id })
        assertTrue(updates.all { it.message == null })
    }
    @Test fun `missing sender and incomplete authorization fields fail closed`() {
        val updates = TelegramProtocol.updates(JSONArray("""[{"update_id":1,"message":{"chat":{"id":42,"type":"private"}}},{"update_id":2,"message":{"chat":{"id":42,"type":"private"},"from":{"id":42},"date":1700000000,"text":"/ping"}}]"""))
        assertNull(updates[0].message)
        assertFalse(CommandPolicy.authorized(updates[1].message!!, 42, 1700000000))
    }
    @Test fun `documents do not turn captions into executable commands`() {
        val message = TelegramProtocol.updates(JSONArray("""[{"update_id":1,"message":{"chat":{"id":42,"type":"private"},"from":{"id":42,"is_bot":false},"date":1700000000,"document":{"file_id":"x"},"caption":"/ping"}}]""")).single().message!!
        assertNull(message.text)
    }
    @Test fun `empty and unrecognized items are ignored`() {
        assertTrue(TelegramProtocol.updates(JSONArray()).isEmpty())
        assertTrue(TelegramProtocol.updates(JSONArray("[null,1,{}]")).isEmpty())
    }
}
