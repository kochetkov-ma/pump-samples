Kotlin. Автоматизация тестирования (часть 2). Kotest. Deep Diving
======
![Kotest](https://habrastorage.org/webt/v5/e5/bd/v5e5bdqn6wzbubebhh1lkni0iiw.png)

Продолжение цикла публикаций/руководства про автоматизацию функционального тестирования на **Kotlin**
на основе фреймворка **Kotest** с использованием наиболее полезных дополнительных библиотек, существенно облегчающих и ускоряющих создание
тестов. В этой части мы углубимся в возможности Kotest, рассмотрим Property Testing и создадим пользовательское расширение.

Для освежения знаний привожу ссылки на прошлые части руководства:

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
2021-01-20 21:15:44:979 [SpecRunner-2] INFO ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec - test 2 started
2021-01-20 21:15:44:979 [SpecRunner-1] INFO ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec - test 1 started
2021-01-20 21:15:45:490 [SpecRunner-1] INFO ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec - test 1 finished
2021-01-20 21:15:45:990 [SpecRunner-2] INFO ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec - test 2 finished
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
2021-01-20 21:44:16:216 [kotest-engine-1] INFO ru.iopump.qa.sample.extension.CustomKotestExtension - [BEFORE] prepareSpec class ru.iopump.qa.sample.parallelism.ThreeParallelSpec
2021-01-20 21:44:16:216 [kotest-engine-2] INFO ru.iopump.qa.sample.extension.CustomKotestExtension - [BEFORE] prepareSpec class ru.iopump.qa.sample.parallelism.TwoParallelSpec
2021-01-20 21:44:16:216 [kotest-engine-0] INFO ru.iopump.qa.sample.extension.CustomKotestExtension - [BEFORE] prepareSpec class ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec
2021-01-20 21:44:18:448 [SpecRunner-3] INFO ru.iopump.qa.sample.parallelism.ThreeParallelSpec - test 2 started
2021-01-20 21:44:18:448 [SpecRunner-6] INFO ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec - test 1 started
2021-01-20 21:44:18:448 [SpecRunner-5] INFO ru.iopump.qa.sample.parallelism.TwoParallelSpec - test 1 started
2021-01-20 21:44:18:448 [SpecRunner-4] INFO ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec - test 2 started
2021-01-20 21:44:18:959 [SpecRunner-6] INFO ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec - test 1 finished
2021-01-20 21:44:19:465 [SpecRunner-5] INFO ru.iopump.qa.sample.parallelism.TwoParallelSpec - test 1 finished
2021-01-20 21:44:19:465 [SpecRunner-3] INFO ru.iopump.qa.sample.parallelism.ThreeParallelSpec - test 2 finished
2021-01-20 21:44:19:465 [SpecRunner-4] INFO ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec - test 2 finished
2021-01-20 21:44:19:471 [SpecRunner-5] INFO ru.iopump.qa.sample.parallelism.TwoParallelSpec - test 2 started
2021-01-20 21:44:19:472 [SpecRunner-3] INFO ru.iopump.qa.sample.parallelism.ThreeParallelSpec - test 1 started
2021-01-20 21:44:20:484 [SpecRunner-3] INFO ru.iopump.qa.sample.parallelism.ThreeParallelSpec - test 1 finished
2021-01-20 21:44:20:484 [SpecRunner-5] INFO ru.iopump.qa.sample.parallelism.TwoParallelSpec - test 2 finished
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

**5** - В **Kotest** есть возможность задать кол-во успешных выполнений одного теста, чтобы считать его успешным.  
Например, чтобы проверить требование отсутствия состояния в системы, можно выполнить тест несколько раз, если хотя бы одно выполнение будет
неуспешным, то тест будет считаться неуспешным.  
Не путайте с fluky - это как раз обратная ситуация.

**1** - Через переопределение метода. Установить таймаут на тест в мс.

**2** - Через переопределение метода. Установить таймаут на один вызов теста в мс (см **5**).

**3** - Через свойство. Установить таймаут на тест в мс и переписать **1**

**4** - Через свойство. Установить таймаут на один вызов теста в мс и переписать **2** (см **5**).

**6** - Через метод конфигурации теста. Установить таймаут на тест в `kotlin.time.Duration` и переписать **3**.

**7** - Через метод конфигурации теста. Установить таймаут на один вызов теста в `kotlin.time.Duration` и переписать **
4** (см **5**).

> `kotlin.time.Duration` имеет статус `Experimental` и при использовании требует установки `@ExperimentalTime` над классом спецификации

Разберем лог запуска `TimeoutSpec`:

```
2021-01-21 08:23:35:183 [SpecRunner-1 @coroutine#6] INFO ru.iopump.qa.sample.timeout.TimeoutSpec - test 1
2021-01-21 08:23:35:698 [SpecRunner-1 @coroutine#7] INFO ru.iopump.qa.sample.timeout.TimeoutSpec - test 1
2021-01-21 08:23:36:212 [SpecRunner-1 @coroutine#4] INFO ru.iopump.qa.sample.extension.CustomKotestExtension - [AFTER] afterTest. Test case duration: 1047 ms

2021-01-21 08:23:36:217 [SpecRunner-1 @coroutine#10] INFO ru.iopump.qa.sample.timeout.TimeoutSpec - test 2
Test did not complete within 400ms
TimeoutException(duration=400)

2021-01-21 08:23:36:625 [SpecRunner-1 @coroutine#13] INFO ru.iopump.qa.sample.timeout.TimeoutSpec - test 3
2021-01-21 08:23:37:141 [SpecRunner-1 @coroutine#14] INFO ru.iopump.qa.sample.timeout.TimeoutSpec - test 3
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

### Fluky блоки

### Расширенные Matcher-ы для списков

### Фабрики тестов

### Встроенные расширения и слушатели

### Пишем свое расширение

### Property тестирование

#### Генераторы данных

#### Написание и конфигурирование Property тестов

#### Использование генераторов для Data-Driver тестов

### Глобальный конфиг 