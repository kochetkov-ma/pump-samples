Kotlin. Автоматизация тестирования (часть 1). Kotest: Начало 
======

Непрерывно нахожусь в поиске наиболее подходящего набора инструментов для организации автоматизации тестирования в больших командах.  
Основная цель поисков — найти и доработать под себя один Фреймворк или несколько отдельных технологий и объединить в единый подход, который обеспечит:
- быстрый старт для нового QA
- хорошую документацию
- минимальную дублируемость кода
- читаемость кода тестов
- понятные отчеты о результате
- масштабируемость и надежность самого инструмента
- удобство работы большой командой или множеством мелких
- раньше была острая необходимость в параллельности, но сейчас в эпоху облачных ресурсов, контейнерезации и оркестрации это качество отодвинулось на второй план

Очередной мой новый проект оказался на **Kotlin**. 
Проанализировав множество вариантов, решил описывать тесты в виде кода, с помощью довольно молодого Фреймворка, написанного на **Kotlin** - **Kotest** (в прошлом _Kotlin Test_).
Это платформа для создания / конфигурирования / запуска / контроля выполнения тестов - `Junit` для **Kotlin**.  
По итогу использования, сам **Kotest** и вариант стиля написания сценариев мне очень понравились по эффективности, лаконичности и простоте.

Обзоров и статей по автоматизации тестирования на **Kotlin** и про **Kotest** в частности — немного. 
Я считаю хорошей идеей — написать цикл про **Kotest**, а также про организацию проекта авто-тестов, сборку, запуск и сопутствующие технологии.
По итогу должен получится полноценный мануал — как создать систему или даже эко-систему автоматизации функционального тестирования на языке **Kotlin** и платформе **Kotest**.

Минусы
------
Сходу определимся по глобальным минусам и учтем, что проект быстро развивается. Те проблемы, что есть в актуальной на время написания статьи версии `4.2.5` уже могут быть исправлены в свежих версиях.

И первым очевидным минусом является очень быстрое изменение минорных и мажорных версий.
Еще в марте 2020 фреймворк назывался `KotlinTest`, и в 4 версии поменял название всех пакетов, потерял совместимость с плагином Idea и стал именоваться `Kotest`, а также стал мульти-платформенным.
При переходе с версии 4.0.7 на 4.1 перестали работать расширения, написанные для 4.0.7, а также сменились названия базовых модулей и много чего еще, то есть принцип семантического версионирования нарушился при переходе с `4.0` на `4.1`.
Это немыслимо для Java мира — это что-то из JS.  
У инструмента пока небольшое комьюнити.  
В пользу широкого функционала иногда приносится в жертву продуманный дизайн.   
Не все предоставляемые стили написания тестов одинаково работают и отображаются в отчете. На текущий момент не корректно отображаются в отчетах data-driven и property-based тесты.  
Шаблоны и фабрики тестов работают не как ожидается.  
Встроенное расширение allure совсем никуда не годится (к примеру, оно пытается обрабатывать аннотации, которыми в принципе невозможно аннотировать тестовые DSL методы).

Однако ни одного критического или блокирующего дефекта я не встречал.

Почему Kotest
------
Множество разработчиков на Kotlin не особо заморачиваются с выбором тестового фреймворка и продолжают использовать Junit4 или Junit5. 
Для них тесты — это, как правило, класс, помеченный аннотацией `@SpringBootTest`, набор методов с аннотацией `@Test`, возможно методы before и beforeClass с соответствующими аннотациями.
Для полноценных функциональных e2e тестов этого недостаточно. Цель — это написание понятных тестов на основе требований, удобная организация проверок, тестовых данных и отчетности.

Так вот `Kotest` позволяет:
- создавать крайне понятные тесты в BDD стиле с помощью Kotlin DSL и функций расширения,
- легко создавать data driven тесты в функциональном стиле
- также с помощью DSL определять действия перед тестом и тестовым классом и после них.
- определить действия на уровне всего прогона (фича, которой нет явно в junit)
- использовать встроенный крайне удобные assertions
- удобное конфигурирование тестовых классов и тестового проекта из кода
и много чего еще, см. [полную документацию](https://kotest.io/) и [сам проект в GitHub](https://github.com/kotest/kotest)  

Какой стиль выбрать? 
------
Kotest дает возможность выбора между несколькими вариантами DSL для формирования структуры тестов.
Самый базовый и простой [String Spec](https://kotest.io/styles/#string-spec) - идеально подойдет для написания unit-тестов с одним уровнем вложенности шагов.
Для полноценных функциональных авто-тестов нужно что-то посложнее: по структуре схожее с `Gherkin`, но менее формализованное, особенно по ключевым словам. 

После долгих экспериментов я остановился на стиле [FreeSpec](https://kotest.io/styles/#free-spec).

#### Начинаем создавать тест
Используя `Kotest` я рекомендую продолжать писать тесты в BDD стиле, как в языке `Gherkin` (CucumberJVM). 
`FreeStyle` не накладывает ограничений на именование тестов, ключевые слова и вложенность, поэтому есть вещи нужно контролировать на уровне гайдов, обучения и Merge-Request`ов.

##### Иерархия тестовых сущностей
В нашем подходе будет 5 базовых тестовых сущностей (или уровней) в рамках Kotest.  
Важно определить это сейчас, потому что в дальнейшем оперировать я буду этими уровнями, от которых зависят различные области видимости.
1. **Тестовый прогон** - Execution (или Project)
Запуск определенного набора тестов

2. **Спецификация** - Spec
Тестовый класс. В cucumber - это Feature

3. **Контейнер теста** - Top Level Test
Сценарий верхнего уровня в Спецификации. В cucumber - это Scenario

4. **Шаг теста** - Nested Test
Шаг в сценарии, который начинается с ключевого слова. 
Ключевое слово обозначает этап: подготовка (Дано), воздействие (Когда), проверка ожидаемой реакции (Тогда).
В cucumber - это Step

5. **Вложенные Шаги** - Nested Step
Это любая дополнительная информация о произведенных действиях, например аннотация `@Step` в `Allure`. 
В рамках описания сценария эти шаги не несут нагрузки — они нужны для отчета, для отладки, для выяснения причин ошибки. 
Kotest позволяет создавать любую вложенность, но в данном подходе ограничиваемся `4 - Шаг теста - Nested Test` - дальнейшая вложенность воспринимается как шаги для отчета. 

С точки зрения Форматирования теста и Review интерес представляют уровни **1** - **4**.

В `Gherkin` есть сущность Структура Сценария (Scenario Template) - это реализация Data Driven. 
В Kotest уровень `3. Контейнер теста - Top Level Test`, также может являться Структурой Сценария — то есть помножиться на наборы тестовых данных.    

##### Превращаем требования в сценарий
Допустим мы тестируем REST API сервиса и имеются требования. 
Не известно, как будем отправлять запросы, как получать, десериализовать и проверять, но сейчас это не нужно. 

Пишем скелет сценария:
```kotlin
open class KotestFirstAutomatedTesting : FreeSpec() {

    private companion object {
        private val log = LoggerFactory.getLogger(KotestFirstAutomatedTesting::class.java)
    }
    
    init {
        "Scenario. Single case" - {
            val expectedCode = 200

            "Given server is up" { }

            "When request prepared and sent" { }

            "Then response received and has $expectedCode code" { }
        }
    }
}
```
_Очень похоже на Сценарии в Cucumber_

Во-первых, стоит обратить внимание, что здесь нет понятия `тестовый класс`, а есть `спецификация` (`FreeSpec`). И это не спроста.
Вспоминаем, что Kotlin DSL - это type-safe builder, а значит при запуске тесты сначала формируют дерево тестов / тестовых контейнеров / pre и after функций / вложенных шагов умноженных на наборы тестовых данных.

Отмечу использование интерполяции строк в имени шага `"Then response received and has $expectedCode code"`

##### Принцип работы DSL
###### Контейнер теста.
> _**Используется минус после названия!** Важно его не пропускать!_

Тест наследуется от класса `FreeSpec`, в свою очередь он реализует `FreeSpecRootScope`: 
```kotlin 
abstract class FreeSpec(body: FreeSpec.() -> Unit = {}) : DslDrivenSpec(), FreeSpecRootScope
```

В `FreeSpecRootScope` для класса `String` переопределяется оператор `-`:
```kotlin
infix operator fun String.minus(test: suspend FreeScope.() -> Unit) { }
```
Соответсвенно запись `"string" - { }` вызывает этот метод, передает внутрь функциональный тип с контекстом `FreeScope` и добавляет в дерево тестов Тест контейнер.

###### Шаги теста
В том же интерфейсе `FreeSpecRootScope` для класса `String` переопределяется оператор вызова `invoke`
```kotlin
infix operator fun String.invoke(test: suspend TestContext.() -> Unit) { }
```
Запись `"string" { }` является вызовом функции расширения с аргументом функционального типа с контекстом `TestContext`, а так как для лямбд круглые скобки можно опускать, то получается такая конструкция.
В базовом классе `FreeSpec` имеется переопределенный `infix` оператор 'минус' в виде функции расширения для String.
То есть запись `"test" - { }` вызывает функцию расширения `String.minus(test: suspend FreeScope.() -> Unit)` куда в качестве единственного аргумента передается лямбда нашего теста.

##### Реализация теста и проверок
Вот как будет выглядеть тест с реализацией:
```
init {
        "Scenario. Single case" - {

            //region Variables
            val expectedCode = 200
            val testEnvironment = Server()
            val tester = Client()
            //endregion

            "Given server is up" {
                testEnvironment.start()
            }

            "When request prepared and sent" {
                val request = Request()
                tester.send(request)
            }
            
            lateinit var response: Response
            "Then response received" {
                response = tester.receive()
            }

            "And has $expectedCode code" {
                response.code shouldBe expectedCode
            }
        }
    }
```
Поясню некоторые момент и мотивацию
1. Константы для конкретного сценария определены прямо в блоке и окружены конструкцией Idea для сворачивания
2. Для обмена информацией между шагами приходится использовать переменные типа `lateinit var response: Response`, определенные непосредственно перед блоком, в котором они инициализируются

#### Kotest Assertions и Matchers
В **Kotest** уже есть довольно обширная библиотека `Assertions and Matchers`.

Зависимость `testImplementation "io.kotest:kotest-assertions-core:$kotestVersion"` предоставляет набор [Matcher-ов](https://kotest.io/matchers/core/), [SoftAssertion](https://kotest.io/assertions/#soft-assertions) и [Assertion для проверки Исключений](https://kotest.io/assertions/#exceptions).  
Есть возможность расширять ее и добавлять свои комплексные Matcher-ы, а также использовать уже готовые расширения.  
Вот общее описание [**kotest-assertions**](https://kotest.io/assertions/) и список доступных [проверок из коробки](https://kotest.io/matchers/core/)  

Немного расширим последний шаг и добавим в него побольше проверок:
```kotlin
"And has $expectedCode code" {
    assertSoftly {
        response.asClue {
            it.code shouldBe expectedCode
            it.body.shouldNotBeBlank()
        }
    }
    val assertion = assertThrows<AssertionError> {
        assertSoftly {
            response.asClue {
                it.code shouldBe expectedCode + 10
                it.body.shouldBeBlank()
            }
        }
    }
    assertion.message shouldContain "The following 2 assertions failed"
    log.error("Expected assertion", assertion)
}
```

1. **`assertSoftly { code }`**  
Soft Assert из библиотеки assertions `Kotest` - выполнит блок кода полностью и сформирует сообщение со всеми ошибками.
2. **`response.asClue { }`**   
_MUST HAVE_ для проверок в тестах. Scope функция kotlin `asClue` - при возникновении ошибки добавит в сообщение строковое представление **всего** объекта `response`
3. [`Matchers`](https://kotest.io/matchers/core/)  
Matchers от `Kotest` - отличная расширяемая библиотека проверок, полностью покрывает базовые потребности.
`shouldBe` - infix версия проверки на равенство.
`shouldBeBlank` - не infix (т.к. нет аргумента) проверка на пустоту строки.  
4. `assertThrows<AssertionError>`  
Статическая функция расширенной для Котлина библиотеки Junit5  
`inline fun <reified T : Throwable> assertThrows(noinline executable: () -> Unit)` - выполняет блок, проверяет тип ожидаемого Исключения и возвращает его для дальнейших проверок

#### Добавляем pre / after обратные вызовы
Существует большое количество вариантов обратных вызовов на события тестов. 
На текущий момент (`4.3.5`) все встроенные события представлены в файле `io.kotest.core.spec.CallbackAliasesKt` в артефакте `kotest-framework-api-jvm` в виде `typealias`:  
```kotlin
typealias BeforeTest = suspend (TestCase) -> Unit
typealias AfterTest = suspend (Tuple2<TestCase, TestResult>) -> Unit
typealias BeforeEach = suspend (TestCase) -> Unit
typealias AfterEach = suspend (Tuple2<TestCase, TestResult>) -> Unit
typealias BeforeContainer = suspend (TestCase) -> Unit
typealias AfterContainer = suspend (Tuple2<TestCase, TestResult>) -> Unit
typealias BeforeAny = suspend (TestCase) -> Unit
typealias AfterAny = suspend (Tuple2<TestCase, TestResult>) -> Unit
typealias BeforeSpec = suspend (Spec) -> Unit
typealias AfterSpec = suspend (Spec) -> Unit
typealias AfterProject = () -> Unit
typealias PrepareSpec = suspend (KClass<out Spec>) -> Unit
typealias FinalizeSpec = suspend (Tuple2<KClass<out Spec>, Map<TestCase, TestResult>>) -> Unit
typealias TestCaseExtensionFn = suspend (Tuple2<TestCase, suspend (TestCase) -> TestResult>) -> TestResult
typealias AroundTestFn = suspend (Tuple2<TestCase, suspend (TestCase) -> TestResult>) -> TestResult
typealias AroundSpecFn = suspend (Tuple2<KClass<out Spec>, suspend () -> Unit>) -> Unit
``` 
Существует 2 вида интерфейсов, которые реализуют обратные вызовы:
- Listener
- Extension

Первый позволяет создать обратные вызов на событие теста и предоставляет immutable описание теста или результат (для `after`).  
Второй позволяет вмешиваться в выполнение теста, что-то изменять, получать информацию о внутреннем состоянии движка, то есть небезопасен и подходит для разработки расширений, меняющих функционал Фреймворка.

Сейчас ограничимся событиями `Listener` - поверьте этого более чем достаточно. 
Этот интерфейс в свою очередь делится еще на 2 главных:
- TestListener
- ProjectListener

Добавить свой `callback` можно множеством способов:
- переопределить метод в спецификации или проекте
- реализовать свой `Listener` и добавить его в список Слушателей явно
- реализовать свой `Listener` и аннотировать его `@AutoScan`
- вызвать метод экземпляра спецификации или проекта — наиболее удобный способ, который будем рассматривать

##### Обратные вызовы уровня спецификации
Самый простой способ добавить `callback` для одного из типов доступных событий — это вызвать одноименный метод из `FreeSpec`, каждый из которых принимает функциональный тип соответствующего события:
```kotlin
 init {
        ///// ALL IN INVOCATION ORDER /////

        //// BEFORE ////
        beforeSpec { spec ->
            log.info("[BEFORE][1] beforeSpec '$spec'")
        }
        beforeContainer { onlyContainerTestType ->
            log.info("[BEFORE][2] beforeContainer onlyContainerTestType '$onlyContainerTestType'")
        }
        beforeEach { onlyTestCaseType ->
            log.info("[BEFORE][3] beforeEach onlyTestCaseType '$onlyTestCaseType'")
        }
        beforeAny { containerOrTestCaseType ->
            log.info("[BEFORE][4] beforeAny containerOrTestCaseType '$containerOrTestCaseType'")
        }
        beforeTest { anyTestCaseType ->
            log.info("[BEFORE][5] beforeTest anyTestCaseType '$anyTestCaseType'")
        }

        //// AFTER ////
        afterTest { anyTestCaseTypeWithResult ->
            log.info("[AFTER][1] afterTest anyTestCaseTypeWithResult '$anyTestCaseTypeWithResult'")
        }
        afterAny { containerOrTestCaseTypeAndResult ->
            log.info("[AFTER][2] afterAny containerOrTestCaseTypeAndResult '$containerOrTestCaseTypeAndResult'")
        }
        afterEach { onlyTestCaseTypeAndResult ->
            log.info("[AFTER][3] afterEach onlyTestCaseTypeAndResult '$onlyTestCaseTypeAndResult'")
        }
        afterContainer { onlyContainerTestTypeAndResult ->
            log.info("[AFTER][4] afterContainer onlyContainerTestTypeAndResult '$onlyContainerTestTypeAndResult'")
        }
        afterSpec { specWithoutResult ->
            log.info("[AFTER][5] afterSpec specWithoutResult '$specWithoutResult'")
        }

        //// AT THE END ////
        finalizeSpec {specWithAllResults ->
            log.info("[FINALIZE][LAST] finalizeSpec specWithAllResults '$specWithAllResults'")
        }

        "Scenario" - { }
}
```
В коде выше все обратные вызовы определены в порядке выполнения до и после теста.
##### before
1. `beforeSpec`
Выполняется сразу после создания экземпляра класс `FreeSpec` и перед выполнением первого теста, имеет один аргумент — `Spec` описание спецификации

2. `beforeContainer`
Выполняется только перед контейнером теста `TestType.Container`, имеет один аргумент — `TestCase` описание контейнера теста 

3. `beforeEach`
Выполняется только перед шагами (тестами) в контейнере теста `TestType.Test`, имеет один аргумент — `TestCase` описание шага теста (вложенный сценарий)

4. `beforeAny`
Выполняется перед контейнером теста `TestType.Container` и перед шагами `TestType.Test`, имеет один аргумент — `TestCase` описание шага или контейнера

5. `beforeTest`
Выполняется перед любой сущностью `TestCase` будь то контейнер или шаг, или новый `TestType` которого пока не существует. 
Фактически сейчас это `beforeAny`. Нужен для сохранения совместимости с прошлыми версиями (когда не было `TestType`) и с будущими (когда будут новые `TestType`)

##### after
1. `afterTest`
Аналогично `beforeTest` только после. 
Имеет аргумент пару — `TestCase` + `TestResult`

2. `afterAny`
Аналогично `beforeAny` только после. 
Имеет аргумент пару — `TestCase` + `TestResult`

3. `afterEach`
Аналогично `beforeEach` только после. 
Имеет аргумент пару — `TestCase` + `TestResult`

4. `afterContainer`
Аналогично `beforeContainer` только после. 
Имеет аргумент пару — `TestCase` + `TestResult`

5. `afterSpec`
Аналогично `beforeSpec` только после. 
Имеет аргумент пару — `Spec`

##### finalizeSpec
Выполняется сразу после окончания работы всех тестов в Спецификации.
Имеет аргумент пару — класс спецификации `KClass<out Spec>` + отображение всех тестов и результатов `Map<TestCase, TestResult>`

##### Обратные вызовы уровня проекта
**Kotest** предоставляет возможность определить callback на события всего запуска тестов.
Их два:
1. `beforeAll`
Выполняется перед первым тестом в запуске

2. `afterAll`
Выполняется после окончания всех тестов

Определить можно с помощью реализации своих [`ProjectListener`](https://kotest.io/listeners/#projectlistener) и добавления в список слушателей проекта в конфигурации проекта, в `AbstractProjectConfig` либо одиночке `Project`.  
Также можно переопределить одноименные методы в [`AbstractProjectConfig`](https://kotest.io/project_config/) - в большинстве случаев предпочтительный способ:
```kotlin
object ProjectConfig : AbstractProjectConfig() {
    private val log = LoggerFactory.getLogger(ProjectConfig::class.java)

    override fun beforeAll() {
        log.info("[BEFORE PROJECT] beforeAll")
    }

    override fun afterAll() {
        log.info("[AFTER PROJECT] afterAll")
    }
}
``` 

#### Делаем тест Data Driven
В пакете `io.kotest.data` предоставлен набор классов и функций для организации [Data Driven Testing](https://kotest.io/data_driven_testing/)

Создадим простейший тест c Data Provider-ом:
```kotlin
init {
        "Scenario. Single case" - {

            //region Variables
            val testEnvironment = Server()
            val tester = Client()
            //endregion

            "Given server is up. Will execute only one time" {
                testEnvironment.start()
            }

            forAll(
                    row(1, UUID.randomUUID().toString()),
                    row(2, UUID.randomUUID().toString())
            ) { index, uuid ->

                "When request prepared and sent [$index]" {
                    tester.send(Request(uuid))
                }

                "Then response received [$index]" {
                    tester.receive().code shouldBe 200
                }
            }
        }
    }
```
_Выглядит довольно просто, а главное понятно (наверное)_

1. Начало как в обычном тесте — определяем контейнер для тестов. 
2. Следующий шаг `Given server is up` выполнится также как в обычном тесте — единожды.
3. Далее следует функция `forAll`. Она принимает наборы `Row` и функциональный блок, в котором мы продолжаем декларировать шаги теста.
4. Функция `row` определяет один набор тестовых данных для одной итерации. 
В пакете файле `io.kotest.data.rows.kt` определено 22 функции для разного кол-ва данных в одном наборе. 
Если этого не хватает, то есть возможность определить свою последовательность в подходе [Property Based Testing](https://kotest.io/property_testing/) (это выходит за рамки этой статьи)      
5. В итоге имеем:
```kotlin
forAll(
    row(1, UUID.randomUUID().toString()),
    row(2, UUID.randomUUID().toString())
) { index, uuid -> block }
```
2 итерации со своим набором тестовых данных.  
В каждом наборе 2 значения. Функциональный блок, который выполнится 2 раза.

Существует важное ограничение на имена тестов в рамках контейнера — все **имена шагов** должны быть **уникальными**.
Поэтому в шагах добавлены уникальные индексы `[$index]`. 
Можно обойтись без индекса и печатать `uuid` в каждом шаге — индекс используется только для удобства упорядоченности. 

#### Выводы после первого знакомства
Во-первых, привожу ссылку на все примеры [qa-kotest-articles/kotest-first](https://github.com/kochetkov-ma/pump-samples/tree/master/qa-kotest-articles/kotest-first).

По итогу имеем полноценный фреймворк для запуска тестов. 

Широкий контроль жизненного цикла Всего запуска, Спецификации, каждого сценария и его шагов. 

Результаты запуска трансформируются в стандартные junit отчеты и все события публикуются в слушатели junit для корректного отображения в консоли Idea.

Также имеется Idea плагин.

Data Driven без всяких аннотаций или дополнительных классов.

Все это дело похоже на Groovy Spoke, но только для Kotlin.

Отмечу удобную документацию в виде сайта [kotest.io](https://kotest.io/) - еще в версии `4.2.0` документация была в множестве readme.md проекта на github.

#### Планы
В планах написание следующих частей, которые покроют тему 'Kotlin. Автоматизация тестирования':
- Kotest. Расширения, конфигурирование проекта, конфигурирование спецификации и конфигурирование тестов, тэги и фабрики тестов, Property Based Testing
- Spring Test. Интеграция с Kotest. Конфигурирование тестового контекста и контроль жизненного цикла бинов.
- Ожидания Awaitility. Retrofit для тестирования API. Работа c БД через Spring Data Jpa. 
- Gradle. Масштабируемая и распределенная структура множества проектов авто-тестов.
- Управление окружением. TestContainers, gradle compose plugin, kubernetes java api + helm