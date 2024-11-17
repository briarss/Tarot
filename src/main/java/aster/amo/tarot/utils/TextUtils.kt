package soul.software.tarot.utils

import aster.amo.tarot.Tarot
import net.minecraft.network.chat.Component

object TextUtils {
    fun toNative(text: String): Component {
        return Tarot.INSTANCE.adventure.toNative(Tarot.MINI_MESSAGE.deserialize(text))
    }

    fun toComponent(text: String): net.kyori.adventure.text.Component {
        return Tarot.MINI_MESSAGE.deserialize(text)
    }
}
