package cc.unitmesh.devti.settings

import org.junit.Assert.*
import org.junit.Test

class LLMSettingComponentKtTest {

    @Test
    fun testJBTextField() {
        val param by LLMParam.creating {
            Editable("test")
        }
        val textField = ReactiveTextField(param) {}
        assertEquals("test", textField.text)

        textField.text = "test2"
        assertEquals("test2", param.value)
    }

    @Test
    fun testJBPassword() {
        val param by LLMParam.creating {
            Password("test")
        }
        val passwordField = ReactivePasswordField(param) {}
        assertEquals("test", passwordField.password.joinToString(""))

        passwordField.text = "test2"
        assertEquals("test2", param.value)
    }

    @Test
    fun testJBCombo() {
        val param by LLMParam.creating {
            ComboBox("test2", listOf("test", "test2"))
        }
        val comboBox = ReactiveComboBox(param)
        assertEquals("test2", comboBox.selectedItem)

        comboBox.selectedItem = "test"
        assertEquals("test", param.value)
    }

    @Test
    fun testLLMParamDelegate() {
        var count = 0
        val onChange: LLMParam.(String) -> Unit = {
            println("changed $it")
            count++
        }
        val param by LLMParam.creating(onChange) {
            Editable("test")
        }

        val textField = ReactiveTextField(param)
        textField.text = "abcdefg"

        assertEquals("value should be changed","abcdefg", param.value)

        // TODO why count is 2? doucment.addUndoableEditListener is called at initialization
        assertEquals("onChange should be called",2, count)
    }

    @Test
    fun testLLMParamOnChange() {
        var count = 0
        val param by LLMParam.creating(onChange = {
            println("changed $it")
            count ++
        }){
            ComboBox("test", listOf("test", "test2"))
        }
        val comboBox = ReactiveComboBox(param)
        comboBox.selectedItem = "test2"

        assertEquals(count, 1)
    }


    @Test
    fun testReactive() {
        var count = 0
        val callback: (Int) -> Unit = {
            println("changed $it")
            count++
        }
        var s by Reactive(1, callback)
        s = 2
        assertEquals("callback should be called", 1, count)
        assertEquals("s should be changed to 2", 2, s)

    }
}
