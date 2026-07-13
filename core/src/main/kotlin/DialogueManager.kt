package ru.triplethall.rpgturnbased

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue

data class DialogueNode(
    var id: Int = -1,
    var speaker: String = "",
    var text: String = "",
    var nextId: Int = -1,
    var choices: com.badlogic.gdx.utils.Array<DialogueChoice>? = null
) : Json.Serializable {
    override fun write(json: Json?) {}

    override fun read(json: Json, jsonData: JsonValue) {
        id = jsonData.getInt("id")
        speaker = jsonData.getString("speaker")
        text = jsonData.getString("text")
        nextId = jsonData.getInt("next_id", -1)

        val choicesValue = jsonData.get("choices")
        if (choicesValue != null && choicesValue.isArray) {
            choices = json.readValue(com.badlogic.gdx.utils.Array::class.java, DialogueChoice::class.java, choicesValue) as? com.badlogic.gdx.utils.Array<DialogueChoice>
        } else {
            choices = null
        }
    }
}

data class DialogueChoice(
    var text: String = "",
    var nextId: Int = -1
) : Json.Serializable {
    override fun write(json: Json?) {}
    override fun read(json: Json, jsonData: JsonValue)
    {
        text = jsonData.getString("text")
        nextId = jsonData.getInt("next_id",-1)
    }
}

class DialogueManager {
    private var currentDialogue: Map<Int, DialogueNode> = emptyMap()
    var currentNode: DialogueNode? = null
        private set
    fun startDialogue(nodes: List<DialogueNode>, startId: Int = 1)
    {
        currentDialogue = nodes.associateBy {it.id}
        currentNode = currentDialogue[startId]
        if (currentNode == null)
        {
            Gdx.app.log("DIALOGUE_DEBUG", "Dialogue with id $startId is not found")
        }
    }
    fun selectNode(id:Int)
    {
        if (id == -1)
        {
            endDialogue()
        }
        else
        {
            currentNode = currentDialogue[id]
        }
    }
    private fun endDialogue()
    {
        currentNode = null
    }
}
