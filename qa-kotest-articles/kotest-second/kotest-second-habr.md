Kotlin. Автоматизация тестирования (часть 2). Kotest. Deep Diving
======
![Kotest](https://habrastorage.org/webt/v5/e5/bd/v5e5bdqn6wzbubebhh1lkni0iiw.png)

Продолжение цикла публикаций/руководства про автоматизацию функционального тестирования на **Kotlin**
на основе фреймворка **Kotest** с использованием наиболее полезных дополнительных библиотек, существенно облегчающих и ускоряющих создание
тестов. В этой части мы углубимся в возможности Kotest, рассмотрим Property Testing и создадим пользовательское расширение.

Все части руководства:

- [Часть 1. Kotest: Начало](https://habr.com/ru/post/520380/)

### Мотивация и цели

На основе первой части руководства можно вполне успешно создавать сценарии, расширять покрытие. Но с увеличением кол-ва тестов и
функциональности целевого приложения неизбежно растет и сложность тестов. Чтобы не изобретать "велосипеды" и не добавлять в проект тестов
лишние зависимости хороший QA обязать детально знать функционал фреймворка для запуска тестов и всех сторонних библиотек. Это позволит в
дальнейшем проектировать минималистичную, масштабируемую и расширяемую архитектуру каркаса для тестов и самих тестов. И успешно
интегрировать популярные библиотеки в свой каркас. Уже в следующей части я расскажу про интеграцию со сторонними библиотеками:

- Spring Core/Test
- TestContainers
- Allure
- Awaitility

Все что излагается в данной публикации — это авторские выдержки из официальной документации [kotest.io](https://kotest.io/) дополненные
моими наблюдениями, пояснениями и примерами.
> Первая версия документации находилась, в [GitHub проекта](https://github.com/kotest/kotest), на 15.01.2021 уже третья версия расположена на сайте [kotest.io](https://kotest.io/).
> В предыдущей версии сайта был доступен поиск, сейчас почему-то документация осталась без поиска, что несколько усложняет навигацию.

### Группировка тестов

Рекомендую уже на самых ранних стадиях развития проекта с тестами продумать группировку тестов. Самый базовые критерии группировки, которые
первыми приходят на ум:

1. По уровню. Модульные -> На один сервис (в контексте микро-сервисов) -> Интеграционные -> E2E

2. По платформе. Windows\Linux\MacOS   
   Desktop\Web\IOS\Android

3. По функциональности Frontend\Backend В контексте целевого приложения:Авторизация\Администрирование\Отчетность...

4. По релизам Sprint-1\Sprint-2 1.0.0\2.0.0

Ниже описание реализации группировки в **Kotest**

#### [Метки](https://kotest.io/docs/framework/tags.html)

Для группировки тестов используются тэги — объекты класса `io.kotest.core.Tag`. Есть несколько вариантов декларирования меток:

- расширить класс `Tag`, тогда в качестве имени будет использовано `simpleName` расширяющего класса, либо переопределенное свойство `name`.
- использовать объект класса `io.kotest.core.NamedTag`, где имя передается в конструкторе.
- создать `String` константу и использовать в аннотации `@io.kotest.core.annotation.Tags`, однако в прошлых версиях **
  kotest** эта аннотация принимал тип класса тэгов.

Ниже представлены все варианты декларирования тэгов:

```kotlin
/**
 * TAG for annotation @Tag only.
 */
const val LINUX_TAG = "Linux"

/**
 * Name will be class simple name=Windows
 */
object Windows : Tag()

/**
 * Override name to Linux.
 */
object LinuxTag : Tag() {
    override val name: String = LINUX_TAG
}

/**
 * Create [NamedTag] object with name by constructor.
 * Substitute deprecated [io.kotest.core.StringTag]
 */
val regressTag = NamedTag("regress")
```

Применить тэг к тесту можно несколькими путями:

- через аннотацию `@Tags` на классе спецификации
- extension функцию `String.config` в тесте или контейнере теста
- расширив метод `tags(): Set<Tag>` у спецификации

> Однако, последний вариант я не рекомендую, т.к. Фреймворку для получения тэга и проверки
> придется создать экземпляр класса `Spec` и выполнить `init` блок,
> и выполнить все инъекции зависимостей, если используется DI в конструкторе.

Пример с 2-мя классами спецификаций:

```kotlin
@Tags(LINUX_TAG)
class LinuxSpec : FreeSpec() {
    init {
        "1-Test for Linux" { }
        "2-Test for Linux and Regress only".config(tags = setOf(regressTag)) { }
    }
}

class WindowsSpec : FreeSpec() {
    /** Override tags method */
    override fun tags(): Set<Tag> = setOf(Windows)

    init {
        "Test for Windows" { }
    }
}
```

Остается только задать выражение тэгов для запуска ожидаемого набора тестов через системную переменную `-Dkotest.tags=`
Поддерживаемые символы: `(`, `)`, `|`, `&`
> Тэги чувствительны к регистру

Привожу несколько вариантов запуска в виде отдельных `gradle-task`

```groovy
// Используется встроенный в Gradle фильтр тестов по пакетам без фильтрации тэгов
task gradleBuildInFilterTest(type: Test) {
    group "test"
    useJUnitPlatform()
    systemProperties = System.properties
    filter { includeTestsMatching("ru.iopump.qa.sample.tag.*") }
}
// Запустить только тест с тэгами regress (в тесте) и Linux (в аннотации спецификации) в LinuxSpec
task linuxWithRegressOnlyTest(type: Test) {
    group "test"
    useJUnitPlatform()
    systemProperties = System.properties + ["kotest.tags": "Linux & regress"]
}
// Запустить 2 теста с тэгом Linux (в аннотации спецификации) в LinuxSpec
task linuxAllTest(type: Test) {
    group "test"
    useJUnitPlatform()
    systemProperties = System.properties + ["kotest.tags": "Linux"]
}
// Исключить из запуска тесты с тэгом Linux, а также Windows. То есть запуститься 0 тестов 
task noTest(type: Test) {
    group "test"
    useJUnitPlatform()
    systemProperties = System.properties + ["kotest.tags": "!Linux & !Windows"]
}
// Запустить тесты, у которых имеется тэг Linux либо Windows. То есть все тесты
task linuxAndWindowsTest(type: Test) {
    group "test"
    useJUnitPlatform()
    systemProperties = System.properties + ["kotest.tags": "Linux | Windows"]
}
```

Обращаю внимание на 2 момента:

- kotest запускается через junit5 runner, поэтому декларирован `useJUnitPlatform()`
- gradle не копирует системные переменные в тест, поэтому необходимо явно указать `systemProperties = System.properties`

#### [Условный запуск](https://kotest.io/docs/framework/conditional-evaluation.html)

В коде спецификации можно задать динамические правила включения/выключения тестов двумя путями:

- на уровне спецификации через аннотацию `@io.kotest.core.annotation.EnabledIf` и реализацию интерфейса `EnabledCondition`
- на уровне теста через расширение `String.config(enabledIf = (TestCase) -> Boolean)`
  Вот пример:

```kotlin
/** [io.kotest.core.annotation.EnabledIf] annotation with [io.kotest.core.annotation.EnabledCondition] */
@EnabledIf(OnCICondition::class)
class CIOnlySpec : FreeSpec() {
    init {
        "Test for Jenkins".config(enabledIf = jenkinsTestCase) { }
    }
}

/** typealias EnabledIf = (TestCase) -> Boolean */
val jenkinsTestCase: io.kotest.core.test.EnabledIf = { testCase: TestCase -> testCase.displayName.contains("Jenkins") }

/** Separate class implementation [io.kotest.core.annotation.EnabledCondition] */
class OnCICondition : EnabledCondition {
    override fun enabled(specKlass: KClass<out Spec>) = System.getProperty("CI") == "true"
}
```

Тест запустится, если:

1. Среди системных переменных будет переменная `CI=true`
2. В отображаемом имени `TestCase` встретится строка `Jenkins`

> Функционал не самый используемый.  
> Есть более простой, но менее гибкий вариант через параметр `enabled`.  
> Например: `"My test".config(enabled = System.getProperty("CI") == "true") { }`

### Последовательность выполнения

#### [Уровень спецификации](https://kotest.io/docs/framework/spec-ordering.html)

По-умолчанию спецификации выполняются в порядке загрузки классов JVM. Это зависит от платформы, но скорее всего порядок будет алфавитный.
Есть возможность задать порядок через конфигурацию уровня проекта используя enum `io.kotest.core.spec.SpecExecutionOrder`

```kotlin
object ProjectConfig : AbstractProjectConfig() {
    override val specExecutionOrder = SpecExecutionOrder.Annotated
}
```

- SpecExecutionOrder.Undefined. Используется по-умолчанию и зависит от загрузки классов на платформе.
- SpecExecutionOrder.Lexicographic. В алфавитном порядке имен классов спецификаций
- SpecExecutionOrder.Random. В случайном порядке.
- SpecExecutionOrder.Annotated. На основе аннотаций `@Order` над классами спецификаций с номерным аргументом. Меньше номер — раньше
  выполнение. Не помеченные выполняются в конце по стратегии `Undefined`
- SpecExecutionOrder.FailureFirst. Новая стратегия. Сначала выполняет упавшие в предыдущем прогоне тесты, а остальные спецификации по
  стратегии `Lexicographic`

Для использования `FailureFirst` необходимо включить сохранение результатов прогона в конфигурации проекта.

```kotlin
object ProjectConfig : AbstractProjectConfig() {
    override val specExecutionOrder = SpecExecutionOrder.FailureFirst

    /**
     * Save execution results to file for [SpecExecutionOrder.FailureFirst] strategy.
     * File location: [io.kotest.core.config.Configuration.specFailureFilePath] = "./.kotest/spec_failures"
     */
    override val writeSpecFailureFile = true
}
```

Привожу пример использования стратегии `Annotated`:

```kotlin
object ProjectConfig : AbstractProjectConfig() {
    override val specExecutionOrder = SpecExecutionOrder.Annotated
}

@Order(Int.MIN_VALUE)
class FirstSpec : FreeSpec() {
    init {
        "FirstSpec-Test" { }
    }
}

@Order(Int.MIN_VALUE + 1)
class SecondSpec : FreeSpec() {
    init {
        "SecondSpec-Test" { }
    }
}

@Order(Int.MAX_VALUE)
class LastSpec : FreeSpec() {
    init {
        "LastSpec-Test" { }
    }
}
```

Порядок выполнения будет такой: `FirstSpec` -> `SecondSpec` -> `LastSpec`

#### [Уровень тестов](https://kotest.io/docs/framework/test-ordering.html)

По-умолчанию внутри класса спецификации тесты выполняются в порядке следования `TestCaseOrder.Sequential` и это наиболее удачный вариант.
Однако и этот порядок можно переопределить, если есть уверенность, что каждый тест абсолютно независим.

В конфигурации проекта:

```kotlin
object ProjectConfig : AbstractProjectConfig() {
    override val testCaseOrder: TestCaseOrder = TestCaseOrder.Random
}
```

В спецификации через метод `testCaseOrder`:

```kotlin
class TestOrderingSpec : FreeSpec() {
    override fun testCaseOrder(): TestCaseOrder = TestCaseOrder.Lexicographic
}
```

### Параллельность

Очень интересная и сложная тема как в контексте разработки, так и в контексте тестирования. В будущем хотелось бы рассмотреть это в рамках
отдельной статьи.  
Рекомендую ознакомиться с книгой `Java Concurrency in Practice` - она как раз недавно появилась на русском языке.  
Основой для принятия решения запуска тестов параллельно служит свойство теста быть независимым в контексте Фреймворка тестов и тестируемой
системы.  
Мало обеспечить очистку состояния тестируемой системы, в которое она перешла после выполнения теста,- необходимо обеспечить неизменное
состояние во время выполнения теста, либо гарантию, что изменение состояния не имеет инвариантов с другими воздействиями на систему.

Для гарантии изолированности необходимо:

- отдельное окружение на каждый поток выполнения тестов
- возврат системы в исходное состояние после теста (либо отсутствие состояния у системы, либо пересоздание всего окружения на каждый тест)
- возврат Фреймворка тестов в исходное состояние (либо отсутствие состояния у тестов)
- отсутствие зависимости между тестами

Есть системы, которые потенциально независимо обрабатывают запросы или сессии, можно положиться на это, однако 100% гарантии никто не даст.
Также наш тест может воздействовать на систему в обход публичного API (например что-то записать в БД напрямую), что также может нарушить
изоляцию.

**Kotest** не решает все проблемы, но предоставляет инструменты для организации параллельного выполнения тестов/спецификаций на уровне
тестов и спецификаций

#### Уровень тестов

Можно задать кол-во потоков для тестов в рамках одного класса спецификации переопределив метод `fun threads()`

Пример спецификации с 2-мя тестами и 2-мя потоками:

```kotlin
class OneParallelOnTestLevelSpec : FreeSpec() {
    private companion object {
        private val log = LoggerFactory.getLogger(OneParallelOnTestLevelSpec::class.java)
    }

    override fun threads(): Int = 2

    init {
        "parallel on test level 1" {
            log.info("test 1 started")
            delay(500)
            log.info("test 1 finished")
        }

        "parallel on test level 2" {
            log.info("test 2 started")
            delay(1000)
            log.info("test 2 finished")
        }
    }
}
```

Запустим с помощью gradle задачи:

```groovy
task parallelismTest(type: Test) {
    group "test"
    useJUnitPlatform()
    filter { includeTestsMatching("ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec") }
}
```

И лог:

```
21:15:44:979 OneParallelOnTestLevelSpec - test 2 started
21:15:44:979 OneParallelOnTestLevelSpec - test 1 started
21:15:45:490 OneParallelOnTestLevelSpec - test 1 finished
21:15:45:990 OneParallelOnTestLevelSpec - test 2 finished
```

По логу видно, что `test 1` и `test 2` запустились одновременно в `21:15:44:979` и завершились первый в `21:15:45:490` и второй через 500мс

- все по-плану.

#### Уровень спецификации

Чтобы запустить классы спецификаций в несколько потов нужно задать настройки параллельности до из запуска, то есть на уровне конфигурации
проекта:

```kotlin
object ProjectConfig : AbstractProjectConfig() {
    override val parallelism: Int = 3
}
```

Либо через системную переменную `-Dkotest.framework.parallelism=3` или прямо в задаче gradle:

```groovy
task parallelismTest(type: Test) {
    group "test"
    useJUnitPlatform()
    doFirst { systemProperties = System.properties + ["kotest.framework.parallelism": 3] }
    filter { includeTestsMatching("ru.iopump.qa.sample.parallelism.*") }
}
```

Используем одну спецификацию с прошлого раздела `OneParallelOnTestLevelSpec` и 2 новые, где сами тесты внутри будут выполняться
последовательно:

```kotlin
class TwoParallelSpec : FreeSpec() {
    private companion object {
        private val log = LoggerFactory.getLogger(TwoParallelSpec::class.java)
    }

    init {
        "sequential test 1" {
            log.info("test 1 started")
            delay(1000)
            log.info("test 1 finished")
        }

        "sequential test 2" {
            log.info("test 2 started")
            delay(1000)
            log.info("test 2 finished")
        }
    }
}

class ThreeParallelSpec : FreeSpec() {
    private companion object {
        private val log = LoggerFactory.getLogger(ThreeParallelSpec::class.java)
    }

    init {
        "sequential test 1" {
            log.info("test 1 started")
            delay(1000)
            log.info("test 1 finished")
        }

        "sequential test 2" {
            log.info("test 2 started")
            delay(1000)
            log.info("test 2 finished")
        }
    }
}
```

Все 3 спецификации должны запуститься параллельно, а также тесты в `OneParallelOnTestLevelSpec` выполняться в своих 2-ух потоках:

```
21:44:16:216 [kotest-engine-1] CustomKotestExtension - [BEFORE] prepareSpec class ru.iopump.qa.sample.parallelism.ThreeParallelSpec
21:44:16:216 [kotest-engine-2] CustomKotestExtension - [BEFORE] prepareSpec class ru.iopump.qa.sample.parallelism.TwoParallelSpec
21:44:16:216 [kotest-engine-0] CustomKotestExtension - [BEFORE] prepareSpec class ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec
21:44:18:448 [SpecRunner-3] ThreeParallelSpec - test 2 started
21:44:18:448 [SpecRunner-6] OneParallelOnTestLevelSpec - test 1 started
21:44:18:448 [SpecRunner-5] TwoParallelSpec - test 1 started
21:44:18:448 [SpecRunner-4] OneParallelOnTestLevelSpec - test 2 started
21:44:18:959 [SpecRunner-6] OneParallelOnTestLevelSpec - test 1 finished
21:44:19:465 [SpecRunner-5] TwoParallelSpec - test 1 finished
21:44:19:465 [SpecRunner-3] ThreeParallelSpec - test 2 finished
21:44:19:465 [SpecRunner-4] OneParallelOnTestLevelSpec - test 2 finished
21:44:19:471 [SpecRunner-5] TwoParallelSpec - test 2 started
21:44:19:472 [SpecRunner-3] ThreeParallelSpec - test 1 started
21:44:20:484 [SpecRunner-3] ThreeParallelSpec - test 1 finished
21:44:20:484 [SpecRunner-5] TwoParallelSpec - test 2 finished
```

По логу видно, что все 3 спецификации прошли за 2 сек — это сумма последовательных тестов одной их спецификаций выше, тесты
из `OneParallelOnTestLevelSpec` выполнились еще быстрее. В выводе я оставил первые три строки, чтобы продемонстрировать имена потоков:

- пул потоков для спецификаций `NamedThreadFactory("kotest-engine-%d")`
- пул дочерних потоков для тестов `NamedThreadFactory("SpecRunner-%d")`

Все это запускается с помощью `Executors.newFixedThreadPool` и особенности работы с корутинами конкретно в этой конфигурации не
используются, т.к. `suspend` функции выполняются через `runBlocking`:

```kotlin
executor.submit {
    runBlocking {
        run(testCase)
    }
}
```

> Подробности реализации в методах `io.kotest.engine.KotestEngine.submitBatch` и `io.kotest.engine.spec.SpecRunner.runParallel`

#### Исключение из параллельного запуска

Если спецификацию по каким-то причинам следует выполнять однопоточно, то есть аннотация `@DoNotParallelize`.  
Для отдельного теста на текущий момент подобного не предусмотрено.

### Таймауты

Имеется большой набор вариантов для установки таймаутов на время выполнения теста.

Привожу пример спецификации со всеми вариантами таймаута:

> Для пояснений в коде я буду использовать метки формата `/*номер*/` и после примера рассказывать про каждую в виде: `номер` -

```kotlin
@ExperimentalTime
class TimeoutSpec : FreeSpec() {
    private companion object {
        private val log = LoggerFactory.getLogger(TimeoutSpec::class.java)
    }

    /*1*/override fun timeout(): Long = 2000 // Spec Level timeout for each TestCase not total
    /*2*/override fun invocationTimeout(): Long =
        2000 // Spec Level invocation timeout for each TestCase invocation not total

    init {
        /*3*/timeout = 1500 // Spec Level timeout for each TestCase not total
        /*4*/invocationTimeout = 1500 // Spec Level invocation timeout for each TestCase invocation not total
        "should be invoked 2 times and will be successful at all".config(
            /*5*/invocations = 2,
            /*6*/invocationTimeout = 550.milliseconds,
            /*7*/timeout = 1100.milliseconds
        ) {
            log.info("test 1")
            delay(500)
        }

        "should be invoked 2 times and every will fail by invocationTimeout".config(
            invocations = 2,
            invocationTimeout = 400.milliseconds,
            timeout = 1050.milliseconds
        ) {
            log.info("test 2")
            delay(500)
        }

        "should be invoked 2 times and last will fail by total timeout".config(
            invocations = 2,
            invocationTimeout = 525.milliseconds,
            timeout = 1000.milliseconds
        ) {
            log.info("test 3")
            delay(500)
        }
    }
}
```

> Агрегирующего таймаута на всю спецификацию нет. Все вариант устанавливают таймаут только на тест.

`5` - В **Kotest** есть возможность задать кол-во успешных выполнений одного теста, чтобы считать его успешным.  
Например, чтобы проверить требование отсутствия состояния в системы, можно выполнить тест несколько раз, если хотя бы одно выполнение будет
неуспешным, то тест будет считаться неуспешным.  
Не путайте с fluky - это как раз обратная ситуация.

`1` - Через переопределение метода. Установить таймаут на тест в мс.

`2` - Через переопределение метода. Установить таймаут на один вызов теста в мс (см `5`).

`3` - Через свойство. Установить таймаут на тест в мс и переписать `1`

`4` - Через свойство. Установить таймаут на один вызов теста в мс и переписать `2` (см `5`).

`6` - Через метод конфигурации теста. Установить таймаут на тест в `kotlin.time.Duration` и переписать `3`.

`7` - Через метод конфигурации теста. Установить таймаут на один вызов теста в `kotlin.time.Duration` и переписать `4` (см `5`).

> `kotlin.time.Duration` имеет статус `Experimental` и при использовании требует установки `@ExperimentalTime` над классом спецификации

Разберем лог запуска `TimeoutSpec`:

```
08:23:35:183 TimeoutSpec - test 1
08:23:35:698 TimeoutSpec - test 1
08:23:36:212 CustomKotestExtension - [AFTER] afterTest. Test case duration: 1047 ms

08:23:36:217 TimeoutSpec - test 2
Test did not complete within 400ms
TimeoutException(duration=400)

08:23:36:625 TimeoutSpec - test 3
08:23:37:141 TimeoutSpec - test 3
Test did not complete within 1000ms
TimeoutException(duration=1000)
```

Первый тест прошел успешно 2 раза, каждое выполнение уместилось `invocationTimeout` и весь тест в `timeout`  
Второй тест упал на первом выполнении и не стал далее выполняться по `invocationTimeout` - `TimeoutException(duration=400)`  
Третий тест упал на втором выполнении по своему `timeout` - `TimeoutException(duration=1000)`

Все таймауты можно задать в глобальном конфиге и через системные переменные:

```kotlin
@ExperimentalTime
object ProjectConfig : AbstractProjectConfig() {
    /** -Dkotest.framework.timeout in ms */
    override val timeout = 60.seconds

    /** -Dkotest.framework.invocation.timeout in ms */
    override val invocationTimeout: Long = 10_000
}
```

### Встроенные ожидания

Во время тестов система реагирует на наши действия не мгновенно, особенно в E2E. Поэтому для **всех** проверок результат взаимодействия
необходимо использовать ожидание. Приведу неполный список основных:

- ожидание HTTP ответа (уже есть на уровне HTTP клиента, достаточно только указать таймаут)
- ожидание появления запроса на заглушке
- ожидание появления/изменения записи в БД
- ожидание сообщения в очереди
- ожидание реакции UI (для Web реализовано в Selenide)
- ожидание письма на SMTP сервере

> Если не понятно, почему нужно ждать в заглушке или в БД, - отвечу в комментариях или сделаю отдельную публикацию

Есть отличная утилита для свех-тонкой настройки ожиданий: [awaitility](https://github.com/awaitility/awaitility) - о ней пойдет речь в
будущих частях. Но **Kotest** из коробки предоставляет простой функционал ожиданий. В документации движка этот раздел
называется `Non-deterministic Testing`.

#### Eventually

Подождать пока блок кода пройдет без Исключений, то есть успешно, в случае неуспеха повторять блок еще раз.  
Функция из
пакета `io.kotest.assertions.timing`: `suspend fun <T, E : Throwable> eventually(duration: Duration, poll: Duration, exceptionClass: KClass<E>, f: suspend () -> T): T`

`f` - это блок кода, который может выбросить Исключение и этот метод будет его перезапускать, пока не выполнит успешно либо не пройдет
таймаут `duration`.  
`poll` - это промежуток бездействия между попытками выполнения  
`exceptionClass` - класс исключения по которому нужно попытаться еще раз. Если вы знаете, что если блок кода
выбросил `IllegalStateException`, то можно пробовать еще раз, но если, выбросил что-то другое, то успешно точно уже никогда не выполниться и
даже пытаться не стоит. Тогда указываем в этом параметры именно `IllegalStateException::class`.

#### Continually

Выполнять переданный блок в цикле, пока не закончится выделенное время либо пока код не выбросит Исключение.  
Функция из пакета `io.kotest.assertions.timing`: `suspend fun <T> continually(duration: Duration, poll: Duration, f: suspend () -> T): T?`

`f` - это блок кода, который будет запускаться в цикле пока не пройдет таймаут `duration` либо не будет выброшено Исключение.
`poll` - это промежуток бездействия между попытками выполнения

#### Рассмотрим пример из четырех тестов (2 eventually + 2 continually)

```kotlin
@ExperimentalTime
class WaitSpec : FreeSpec() {
    private companion object {
        private val log = LoggerFactory.getLogger(WaitSpec::class.java)
    }

    /*1*/
    private lateinit var tries: Iterator<Boolean>
    private lateinit var counter: AtomicInteger
    private val num: Int get() = counter.incrementAndGet()

    init {
        /*2*/
        beforeTest {
            tries = listOf(false, false, true).iterator()
            counter = AtomicInteger()
        }

        "eventually waiting should be success" {
            /*3*/eventually(200.milliseconds, 50.milliseconds, IllegalStateException::class) {
            log.info("Try $num")
            if (tries.next().not()) /*4*/ throw IllegalStateException("Try $counter")
        }
        }

        "eventually waiting should be failed on second try" {
            /*5*/shouldThrow<AssertionError> {
            eventually(/*6*/100.milliseconds, 50.milliseconds, IllegalStateException::class) {
                log.info("Try $num")
                if (tries.next().not()) throw IllegalStateException("Try $counter")
            }
        }.toString().also(log::error)
        }

        "continually waiting should be success" - {
            /*7*/continually(200.milliseconds, 50.milliseconds) {
            log.info("Try #$num")
        }
        }

        "continually waiting should be failed on third try" {
            /*8*/shouldThrow<IllegalStateException> {
            continually(200.milliseconds, 50.milliseconds) {
                log.info("Try #$num")
                if (tries.next()) throw IllegalStateException("Try $counter")
            }
        }.toString().also(log::error)
        }
    }
}

```

`1` - Блок с полями для подсчета попыток и описания итератора из 3-ех элементов

`2` - Перед каждым тестовым контейнером сбрасывать счетчики

`3` - Вызывается функция `eventually`, который закончится успешно. Общий таймаут 200 мс, перерыв между попытками 50 мс, игнорировать
исключение `IllegalStateException`

`4` - Первые две итерации выбрасывают `IllegalStateException`, а 3-я завершается успешно.

`5` - Тут ожидается, что `eventually` закончится неуспешно и выполняется проверка выброшенного исключения. При неудаче `eventually`
выбрасывает `AssertionError` с информацией о причине неудачи и настройках.

`6` - таймаута 100 мс и перерыв между попытками 50 мс, позволяют выполнить только 2 неудачный попытки, в итоге ожидание завершается
неуспешно

`7` - `continually` выполнит 4 попытки, все из которых будут успешные и само ожидание завершится успехом

`8` - Тут ожидается, что `continually` закончится неуспешно и выполняется проверка выброшенного исключения. При неудаче `continually`
перебрасывает последнее Исключение от кода внутри, то есть `IllegalStateException`, чем отличается от `eventually`

Лог выполнения с пояснениями:

```
/////////////////////////////////////////////////////////////////// 1 ////////////////////////////////////////////////////////////////////////
21:12:14:796 INFO CustomKotestExtension - [BEFORE] test eventually waiting should be success
21:12:14:812 INFO WaitSpec - Try #1
21:12:14:875 INFO WaitSpec - Try #2
21:12:14:940 INFO WaitSpec - Try #3
/////////////////////////////////////////////////////////////////// 2 ////////////////////////////////////////////////////////////////////////
21:12:14:940 INFO CustomKotestExtension - [BEFORE] test eventually waiting should be failed on second try
21:12:14:956 INFO WaitSpec - Try #1
21:12:15:018 INFO WaitSpec - Try #2

/* Сообщение в ошибке содержит информацию о настройках ожидания */
21:12:15:081 ERROR WaitSpec - java.lang.AssertionError: Eventually block failed after 100ms; attempted 2 time(s); 50.0ms delay between attempts

/* Сообщение в ошибке содержит первое выброшенное кодом Исключение и последнее */ 
The first error was caused by: Try #1
java.lang.IllegalStateException: Try #1
The last error was caused by: Try #2
java.lang.IllegalStateException: Try #2
//////////////////////////////////////////////////////////////////// 3 ///////////////////////////////////////////////////////////////////////
21:12:15:081 INFO CustomKotestExtension - [BEFORE] test continually waiting should be success
21:12:15:081 INFO WaitSpec - Try #1
21:12:15:159 INFO WaitSpec - Try #2
21:12:15:221 INFO WaitSpec - Try #3
21:12:15:284 INFO WaitSpec - Try #4
///////////////////////////////////////////////////////////////////// 4 //////////////////////////////////////////////////////////////////////
21:12:15:346 INFO CustomKotestExtension - [BEFORE] test continually waiting should be failed on third try
21:12:15:346 INFO WaitSpec - Try #1
21:12:15:409 INFO WaitSpec - Try #2
21:12:15:469 INFO WaitSpec - Try #3

/* Здесь выбрасывается Исключение из выполняемого блока в continually */
21:12:15:469 ERROR WaitSpec - java.lang.IllegalStateException: Try #3
```

### Flaky тесты

Flaky тесты — это нестабильные тесты, которые могут случайным образом проходить неудачно.  
Причин этому крайне много и разбирать мы их конечно же не будем в рамках этой статьи, обсудим варианты перезапуска подобных тестов.  
Можно выделить несколько уровней для перезапуска:

- на уровне CI перезапускать всю Задачу прогона тестов, если она неуспешна
- на уровне Gradle сборки, используя плагин [`test-retry-gradle-plugin`](https://github.com/gradle/test-retry-gradle-plugin) или аналог
- на уровне Gradle сборки, с помощью своей реализации, например сохранять упавшие тесты и запускать их в другой задаче

> Другие инструменты для сборки Java/Kotlin/Groovy кроме Gradle не рассматриваю и другим не советую

- на уровне тестового движка
- на уровне блоков кода в тесте (см. раздел Встроенные ожидания и retry)

На текущей момент в версии **Kotest** `4.3.2` нет функционала для перезапуска нестабильных тестов.  
И не работает интеграция с Gradle плагином [`test-retry-gradle-plugin`](https://github.com/gradle/test-retry-gradle-plugin).

Есть возможность перезапустить отдельный блок кода. Это особенно актуально для UI тестирования, когда клик на некоторые элементы иногда не
срабатывает.  
Представьте ситуацию с `Dropdown` внизу страницы, после скролла происходит клик, далее ожидание раскрытия элементов и клик на элемент
списка, так вот независимо от Фронтового фреймворка иногда клик для раскрытия списка может не сработать и список не появляется, нужно еще
раз попробовать начиная с клика.

Можно сделать повтор через `try/catch`, но лучше использовать
функцию `suspend fun <T, E : Throwable> retry(maxRetry: Int, timeout: Duration, delay: Duration = 1.seconds, multiplier: Int = 1, exceptionClass: KClass<E>, f: suspend () -> T):`:

```kotlin
// using Selenide
dropdown.scrollTo()
retry(2, 20.seconds, exceptionClass = Throwable::class) {
    dropdown.click()
    dropdownItems.find(Condition.text("1")).apply {
        click()
        should(Condition.disappear)
    }
}
```

Действие `scrollTo` повторять не нужно. После перехода к элементу блок с раскрытием списка и выбором повторить 2 раза, но не дольше 20 сек
игнорируя все исключения (или более конкретные).

В **Kotest** возможно создать свое расширение для перезапуска упавших `спецификаций/контейнеров теста/тестов`.  
В *3 части* руководства я покажу как создать самый простой вариант для тестов.  
Остальные варианты я планирую реализовать в своем GitHub репозиторие и выпустить в MavenCentral, если к тому моменту разработчик Kotest не
добавит поддержку 'из коробки'.

### Фабрики тестов

Представим, что в каждом тесте нужно выполнять одни и те же действия для подготовки окружения: наполнять БД, очереди, конфиги на файловой
системе. Для этих действий хочется создать шаблон теста с адекватным именем, возможно с описанием в виде `javadoc`/`kdoc` и несколькими
аргументами, например именем и паролем тестового пользователя. В **Kotest** такой подход называется `Test Factories` и позволяет вставлять
куски тестов в корневой тест.  
Это ни функции, ни методы, ни абстракции — это параметризованные части теста с той же структурой, что основной тест, но используемые в
нескольких местах кода.

> Я придерживаюсь правила, что **спецификация** теста должен быть понятен любому человеку, имеющему экспертизу в предметной бизнес-области теста.  
> По **спецификацией** теста я понимаю не реализацию, а описания шагов в виде строк в BDD стиле. В [1-ой части руководства]() я раскрывал тему форматирования теста более подробно.  
> Даже если требуется много `copy/paste` для сохранения формата теста, **нужно** это делать.  
> А вот тестовый движок/фреймворк позволяет **спецификацию** теста запустить, провести взаимодействия с системой автоматически, как если бы все что написано в тесте делал человек.

Очень важно не нарушать читабельность теста использованием шаблонов:

- имя шаблона должно быть понятное и сопровождаться описанием, а так же описанием всех параметров.
- шаблон должен выполнять одну функцию, например настройка БД (принцип единственной ответственности)
- шаблон должен быть описан в BDD стиле
- шаблон не должен быть слишком абстрактным (субъективно)

Теперь к реализации в **Kotest**. Она очень ограничена, поэтому я приведу пример того как рекомендует делать шаблоны официальная
документация и как это удобнее делать своими силами через `scoped` функции и функции-расширения. Пример кода с пояснениями:

```kotlin
class FactorySpec : FreeSpec() {
    init {
        /*1.1*/include(containerFactory(1, 2, log))
        "root container" - {
            /*2.1*/containerTemplate()
        }
    }
}

/** Kotest factory */
fun containerFactory(argument1: Any, argument2: Any, logger: Logger) =
    /*1.2*/freeSpec {
    beforeContainer { logger.info("This 'beforeContainer' callback located in the test factory") }

    "factory container" - {
        "factory test with argument1 = $argument1" { }
        "factory test with argument2 = $argument2" { }
    }
}

/** Add [TestType.Container] by scoped function extension */
/*2.2*/suspend inline fun FreeScope.containerTemplate(): Unit {
    "template container with FreeScope context" - {
        /*2.3*/testCaseTemplate()
    }
}

/** Add [TestType.Test] by scoped function extension */
/*2.4*/suspend inline fun FreeScope.testCaseTemplate(): Unit {
    "nested template testcase with FreeScope context" { }
}

private val log = LoggerFactory.getLogger(FactorySpec::class.java)
```

##### TestFactory

`1.1` - С помощью метода `fun include(factory: TestFactory)` ме можем включить шаблон теста в спецификацию в качестве самостоятельного
теста.

`1.2` - Определена функция, которая возвращает экземпляр `TestFactory` и принимает несколько параметров. Для создания инстанса `TestFactory`
используем функцию `fun freeSpec(block: FreeSpecTestFactoryConfiguration.() -> Unit): TestFactory`, которая принимает блок тестов и
предоставляет контекст `FreeSpecTestFactoryConfiguration`. Внутри этого блока пишем тест, как обычно, в BDD силе. Далее с помощью `include`
шаблон вставляется как обычный контейнер теста.

> Для `TestFactory` выполняются все обратные вызовы, что и определены для спецификации, куда встраивается шаблон.   
> А также доступны все методы для создания обратных вызовов внутри фабрики, например `beforeContainer`.  
> У `TestFactory` есть большие ограничения:
> - нельзя вставлять друг в друга
> - встраивать можно только на уровень спецификации, то есть фабрика должна быть полным тестом, а не частью теста.

##### Шаблоны через `scoped`-функции и функции-расширения.

`2.1` - Внутри контейнера вызывается пользовательская функция `containerTemplate` и добавляет к контексту этого контейнера новые шаги теста.

`2.2` - Функция `suspend inline fun FreeScope.containerTemplate(): Unit` выполняет свой код в контексте внешнего теста `FreeScope` и
просто **изменяет** этот контекст, добавляя новый вложенный контейнер.  
Функция ничего не возвращает, а именно изменяет (`side-effect`) переданный контекст. Тесты пишем, так как будто они не в отдельной функции,
а в спецификации.  
`suspend` - обязателен, т.к. все тесты запускаются в корутине.  
`inline` - для скорости, не обязателен. Указывает компилятору на то, что код этой функции нужно просто скопировать в место вызова, то есть в
байт-коде, не будет `containerTemplate`, а будет вставленный код в спецификации.  
`FreeScope.` - внутренности вызываются в контексте этого объекта, в данном случае контейнера

`2.3` - Внутри нашего шаблона вызываем другой шаблон, который добавлен новые шаги. В реальных тестах так делать **не следует**

`2.4` - Функция `suspend inline fun FreeScope.testCaseTemplate(): Unit` добавляет вложенные шаги теста в место вызова. Все тоже самое, что
и `2.2`

В итоге мы имеем следующую структуру теста после выполнения:

![](https://habrastorage.org/webt/p-/ie/zr/p-iezrb-ceh51xhmesr5fvmlico.png)

### Property тестирование

#### Генераторы данных

#### Написание и конфигурирование Property тестов

#### Использование генераторов для Data-Driver тестов

Заключение
------
Во-первых, привожу ссылку на все
примеры [qa-kotest-articles/kotest-second](https://github.com/kochetkov-ma/pump-samples/tree/master/qa-kotest-articles/kotest-second).

Был рассмотрен практически весь функционал **Kotest**

Осталось рассказать про создание расширений и доступные встроенные расширения, а далее перейдем к интеграции с другими библиотеками
помогающими в автоматизации тестирования

Ресурсы
------
[Kotlin. Автоматизация тестирования (часть 1). Kotest: Начало](https://habr.com/ru/post/520380/)

[Примеры](https://github.com/kochetkov-ma/pump-samples/tree/master/qa-kotest-articles/kotest-second)

[Официальная документация Kotest](https://kotest.io/docs/quickstart)

[Kotest GitHub](https://github.com/kotest/kotest)

[Kotlinlang](https://kotlinlang.org/docs/reference/)

[Coroutines tutorial](https://kotlinlang.org/docs/tutorials/coroutines/coroutines-basic-jvm.html)

[Gradle testing](https://docs.gradle.org/current/userguide/java_testing.html)
