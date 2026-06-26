package jp.sakuramochi.kaisatsupatch.client

import net.minecraft.util.StatCollector

internal fun t(key: String): String = StatCollector.translateToLocal(key)
internal fun tf(key: String, vararg args: Any): String = String.format(t(key), *args)
