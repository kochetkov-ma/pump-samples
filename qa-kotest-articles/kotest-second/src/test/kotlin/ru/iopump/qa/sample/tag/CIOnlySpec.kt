package ru.iopump.qa.sample.tag

import io.kotest.core.annotation.EnabledCondition
import io.kotest.core.annotation.EnabledIf
import io.kotest.core.spec.Spec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.TestCase
import kotlin.reflect.KClass

/** [io.kotest.core.annotation.EnabledIf] annotation with [io.kotest.core.annotation.EnabledCondition] */
@EnabledIf(OnCICondition::class)
class CIOnlySpec : FreeSpec() {
    init {

        "Test for Jenkins".config(enabledIf = jenkinsTestCase, enabled = System.getProperty("CI") == "true") { }
    }
}

/** typealias EnabledIf = (TestCase) -> Boolean */
val jenkinsTestCase: io.kotest.core.test.EnabledIf = { testCase: TestCase -> testCase.displayName.contains("Jenkins") }

/** Separate class implementation [io.kotest.core.annotation.EnabledCondition] */
class OnCICondition: EnabledCondition {
    override fun enabled(specKlass: KClass<out Spec>) = System.getProperty("CI") == "true"
}