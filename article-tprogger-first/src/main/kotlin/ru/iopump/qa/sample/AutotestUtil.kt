package ru.iopump.qa.sample

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import sun.misc.Unsafe
import java.lang.reflect.Field

object AutotestUtil {
    inline fun <reified T : Any> newLogger(): Logger = LoggerFactory.getLogger(T::class.java)

    init {
        /* Suppress 'Illegal reflective access WARNING' */
        val theUnsafe: Field = Unsafe::class.java.getDeclaredField("theUnsafe")
        theUnsafe.isAccessible = true
        val u: Unsafe = theUnsafe.get(null) as Unsafe
        val cls = Class.forName("jdk.internal.module.IllegalAccessLogger")
        val logger: Field = cls.getDeclaredField("logger")
        u.putObjectVolatile(cls, u.staticFieldOffset(logger), null)
    }
}