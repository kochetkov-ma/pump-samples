package ru.iopump.kotest

import com.jayway.jsonpath.matchers.JsonPathMatchers
import com.jayway.jsonpath.matchers.JsonPathMatchers.isJson
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.StringSpec
import net.javacrumbs.jsonunit.assertj.JsonAssert.ConfigurableJsonAssert
import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.HamcrestCondition
import org.assertj.core.api.SoftAssertions
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.json.JSONObject
import org.junit.jupiter.api.function.Executable

class AssertionTest : StringSpec() {

    private val jsonObject = JSONObject("{\"a\": 1, \"ids\": [100, 200, 300]}")
    private val jsonString = jsonObject.toString(4)

    init {

        // https://github.com/lukas-krecan/JsonUnit/issues/145
        "Сценарий: Библиотека JsonUnit не поддерживает Soft Assertions для AssertJ 1" {

            assertJSoftly {

                assertThatJson(jsonObject) {

                    inPath("$.a").isIntegralNumber.isEqualTo(3)
                    inPath("$.ids[1]").isIntegralNumber.isEqualTo(300)
                    inPath("$.ids").isArray.isEqualTo(arrayOf(100, 200, 300))
                }
            }
        }

        "Сценарий: Библиотека JsonUnit не поддерживает Soft Assertions для AssertJ 2" {

            assertJSoftly {

                assertThatJson(jsonObject) { inPath("$.a").isIntegralNumber.isEqualTo(1) }

                assertThatJson(jsonObject) { inPath("$.ids[1]").isIntegralNumber.isEqualTo(300) }

                assertThatJson(jsonObject) { inPath("$.ids").isArray.isEqualTo(arrayOf(100, 200, 300)) }

            }
        }

        // Неудобный вывод. Не работает сравнение сложных объектов по JsonPtah
        "Сценарий: Библиотека Kotest Assertions Json" {

            assertSoftly {

                jsonString.apply {

                    shouldContainJsonKeyValue("$.a", 1)

                    shouldContainJsonKeyValue("$.ids[0]", 300)

                    shouldContainJsonKeyValue("$.ids", arrayOf(100, 200, 300)) // Ошибка Kotest
                }
            }
        }

        // https://github.com/json-path/JsonPath/tree/master/json-path-assert
        // Работает некорректно. Ошибка
        "Сценарий: Библиотека из JayWay Jsonpath и Hamcrest Matchers + assertJ" {

            assertJSoftly {

                assertThat(jsonString)
                    .`is`(HamcrestCondition.matching(JsonPathMatchers.withJsonPath("$.a", Matchers.equalTo(1))))

                assertThat(jsonString)
                    .`is`(HamcrestCondition.matching(JsonPathMatchers.withJsonPath("$.ids[1]", Matchers.equalTo(300))))

                assertThat(jsonString)
                    .`is`(
                        HamcrestCondition.matching(
                            JsonPathMatchers.withJsonPath(
                                "$.ids",
                                Matchers.equalTo(arrayOf(100, 200, 300))
                            )
                        )
                    )
            }


        }

        "Сценарий: Junit5 Assertions.assertAll + JsonUnit - [единственный рабочий вариант 1]" {
            org.junit.jupiter.api.Assertions.assertAll(

                { assertThatJson(jsonObject) { inPath("$.a").isIntegralNumber.isEqualTo(3) } },

                { assertThatJson(jsonObject) { inPath("$.ids[1]").isIntegralNumber.isEqualTo(300) } },

                { assertThatJson(jsonObject) { inPath("$.ids").isArray.isEqualTo(arrayOf(100, 200, 300)) } }
            )

        }

        "Сценарий: Junit5 Assertions.assertAll + JsonUnit + расширение - [единственный рабочий вариант 2]" {
            assertThatJsonSoftly(jsonObject) {

                add { inPath("$.a").isIntegralNumber.isEqualTo(3) }

                and { inPath("$.ids[1]").isIntegralNumber.isEqualTo(300) }

                and { inPath("$.ids").isArray.isEqualTo(arrayOf(100, 200, 300)) }
            }
        }

        // Нет Soft Assertions
        "Сценарий: Библиотека из JayWay Jsonpath и Hamcrest Matchers.allOf" {
            assertThat(
                jsonString,
                isJson(
                    Matchers.allOf(
                        JsonPathMatchers.withJsonPath("$.a", Matchers.equalTo(3)),
                        JsonPathMatchers.withJsonPath("$.ids[1]", Matchers.equalTo(300)),
                        JsonPathMatchers.withJsonPath("$.ids", Matchers.equalTo(arrayOf(100, 200, 300))),
                    )
                )
            )
        }
    }

    companion object {

        fun assertJSoftly(assertionsFunction: SoftAssertions.() -> Unit) =
            SoftAssertions.assertSoftly(assertionsFunction)

        fun assertThatJsonSoftly(json: Any, assertions: AssertionBuilder.() -> Unit) =
            org.junit.jupiter.api.Assertions
                .assertAll(AssertionBuilder().apply(assertions).asserts.map { Executable { assertThatJson(json, it) } }
                    .toList())

        class AssertionBuilder {
            val asserts: MutableCollection<ConfigurableJsonAssert.() -> Unit> = mutableListOf()
            infix fun AssertionBuilder.add(next: ConfigurableJsonAssert.() -> Unit) = asserts.add(next)
            infix fun AssertionBuilder.and(next: ConfigurableJsonAssert.() -> Unit) = asserts.add(next)
        }
    }

}
