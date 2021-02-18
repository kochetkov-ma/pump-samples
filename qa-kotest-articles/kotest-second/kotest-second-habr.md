Kotlin. Автоматизация тестирования (Часть 2). Kotest. Deep Diving
======
![Kotest](https://habrastorage.org/webt/v5/e5/bd/v5e5bdqn6wzbubebhh1lkni0iiw.png)

Продолжение цикла публикаций про автоматизацию функционального тестирования на **Kotlin**
с использованием фреймворка **Kotest** совместно с наиболее полезными дополнительными библиотеками, существенно облегчающими и ускоряющими создание тестов.

В этой части мы углубимся в возможности Kotest:

- покажу все варианты группировки тесты
- расскажу про последовательность выполнения тестов и спецификаций
- изучим возможности параллельного запуска
- настроим таймауты на выполнение тестов
- проговорим про ожидания и **Flaky**-тесты
- рассмотрим использование **Фабрик тестов**
- и напоследок исследуем тему **Property Testing**

Все части руководства:

- [Часть 1. Kotest: Начало](https://habr.com/ru/post/520380/)
  <cut />

### О себе
Я являюсь QA Лидом на одном из проектов [Мир Plat.Form (НСПК)](https://habr.com/ru/company/nspk/). Проект зародился около года назад и уже вырос до четырех команд, где трудится в общей сложности около 10 разработчиков в тестировании (SDET), без учета остальных участников в лице аналитиков, разработчиков и технологов. 
Наша задача — автоматизировать функциональные тесты на уровне отдельных сервисов, интеграций между ними и E2E до попадания функционала в `master` - всего порядка 30 микро-сервисов. Взаимодействие между сервисами - Kafka, внешний API - REST, а также 2 фронтовых Web приложения. 
Разработка самой системы и тестов ведется на языке **Kotlin**, а движок для тестов был выбран **Kotest**.

В данной статье и в остальных публикациях серии я максимально подробно рассказываю о тестовом Движке и вспомогательных технологиях в формате **Руководства/Tutorial**. 

### Мотивация и цели

На основе первой части руководства можно вполне успешно создавать сценарии, расширять покрытие. Но с увеличением кол-ва тестов и функциональности целевого приложения неизбежно растет и сложность тестов. 
Чтобы не изобретать "велосипеды" и не добавлять в проект тестов лишние зависимости хороший QA обязан детально знать функционал тестового движка и всех сторонних библиотек. 
Это позволит в дальнейшем проектировать простую, масштабируемую и расширяемую архитектуру каркаса и тестов. А также успешно интегрировать популярные библиотеки.

Уже в следующей части я расскажу про интеграцию со сторонними библиотеками:
- Spring Core/Test. Dependencies Injection и конфигурирование профилей для тестов 
- Spring Data JPA. Работа с БД
- TestContainers. Управление Docker контейнерами
- Allure. Отчетность
- Awaitility. Ожидания

### Группировка тестов

> В публикации много примеров с пояснениями двух видов: комментарии в коде либо объяснения вне блока кода со ссылками на конкретные места примера.
> Куски кода больше половины стандартного экрана скрыты спойлером.  
> Рассматриваемая версия **Kotest 4.3.2**

Рекомендую на самых ранних стадиях развития проекта с тестами продумать группировку тестов. Самые базовые критерии группировки, которые первыми приходят на ум:

1. **По уровню**. Модульные -> На один сервис (в контексте микро-сервисов) -> Интеграционные -> E2E

2. **По платформе**. Windows\Linux\MacOS | Desktop\Web\IOS\Android

3. **По функциональности**. Frontend\Backend В контексте целевого приложения:Авторизация\Администрирование\Отчетность...

4. **По релизам**. Sprint-1\Sprint-2 1.0.0\2.0.0

Далее описание реализации группировки в **Kotest**

#### Теги

Для группировки тестов используются теги — объекты класса `abstract class io.kotest.core.Tag`. Есть несколько вариантов декларирования меток:

- расширить класс `Tag`, тогда в качестве имени метки будет использовано `simpleName` расширяющего класса, либо переопределенное свойство `name`.
- использовать объект класса `io.kotest.core.NamedTag`, где имя метки передается в конструкторе.
- создать `String` константу и использовать в аннотации `@io.kotest.core.annotation.Tags`, однако в прошлых версиях **Kotest** эта аннотация принимала тип `KClass<T>` класса тегов, а сейчас `String` имя тега.

Ниже представлены все варианты декларирования тегов:

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

Применить тег к тесту можно несколькими путями:

- через аннотацию `@Tags` на классе спецификации
- extension функцию `String.config` в тесте или контейнере теста
- переопределив метод `tags(): Set<Tag>` у спецификации

> Однако, последний вариант я не рекомендую, т.к. тестовому движку для получения тега и проверки придется создать экземпляр класса `Spec` и выполнить `init` блок, а еще выполнить все инъекции зависимостей, если используется *Dependency injection* в конструкторе.

Пример с 2-мя классами спецификаций:

```kotlin
@Tags(LINUX_TAG) // Аннотация
class LinuxSpec : FreeSpec() {
    init {
        "1-Test for Linux" { }
                                        /* конфигурация теста String.config */
        "2-Test for Linux and Regress only".config(tags = setOf(regressTag)) { }
    }
}

class WindowsSpec : FreeSpec() {
    /** Override tags method */
    override fun tags(): Set<Tag> = setOf(Windows) // переопределение метода

    init {
        "Test for Windows" { }
    }
}
```

Остается задать выражение тегов для запуска ожидаемого набора тестов через системную переменную `-Dkotest.tags=<выражение для выборки тестов по тегам>`.  
В выражении можно использовать ограниченный набор операторов: `(`, `)`, `|`, `&`
> Теги чувствительны к регистру

Привожу несколько вариантов запуска в виде `Gradle task`

<spoiler title="Набор Gradle Tasks для запуска групп тестов">

```java
// Используется встроенный в Gradle фильтр тестов по пакетам без фильтрации тегов
task gradleBuildInFilterTest(type: Test) {
    group "test"
    useJUnitPlatform()
    systemProperties = System.properties
    filter { includeTestsMatching("ru.iopump.qa.sample.tag.*") }
}

// Запустить только тест с тегами regress (в тесте) и Linux (в аннотации спецификации) в LinuxSpec
task linuxWithRegressOnlyTest(type: Test) {
    group "test"
    useJUnitPlatform()
    systemProperties = System.properties + ["kotest.tags": "Linux & regress"]
}

// Запустить 2 теста с тегом Linux (в аннотации спецификации) в LinuxSpec
task linuxAllTest(type: Test) {
    group "test"
    useJUnitPlatform()
    systemProperties = System.properties + ["kotest.tags": "Linux"]
}

// Исключить из запуска тесты с тегом Linux, а также Windows. То есть запуститься 0 тестов 
task noTest(type: Test) {
    group "test"
    useJUnitPlatform()
    systemProperties = System.properties + ["kotest.tags": "!Linux & !Windows"]
}

// Запустить тесты, у которых имеется тег Linux либо Windows. То есть все тесты
task linuxAndWindowsTest(type: Test) {
    group "test"
    useJUnitPlatform()
    systemProperties = System.properties + ["kotest.tags": "Linux | Windows"]
}
```

</spoiler>

Обращаю внимание на 2 момента:

- Kotest запускается через Junit5 Runner, поэтому декларирован **`useJUnitPlatform()`**
- Gradle **не** копирует системные переменные в тест, поэтому необходимо явно указать `systemProperties = System.properties`

#### Условный запуск

В коде спецификации можно задать динамические правила включения/выключения тестов двумя путями:

- на уровне спецификации через аннотацию `@io.kotest.core.annotation.EnabledIf` и реализацию интерфейса `EnabledCondition`
- на уровне теста через расширение `String.config(enabledIf = (TestCase) -> Boolean)`
  Вот пример:

```kotlin
/** [io.kotest.core.annotation.EnabledIf] annotation with [io.kotest.core.annotation.EnabledCondition] */
@EnabledIf(OnCICondition::class) // Аннотация принимает класс с логикой включение
class CIOnlySpec : FreeSpec() {
    init {
                            /* Логика включения передается в конфигурацию теста */
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

#### Уровень спецификации

По-умолчанию спецификации выполняются в порядке загрузки классов JVM. Это зависит от платформы, но скорее всего порядок будет алфавитный. Есть возможность задать порядок через конфигурацию уровня проекта используя enum `io.kotest.core.spec.SpecExecutionOrder`

```kotlin
object ProjectConfig : AbstractProjectConfig() {
    override val specExecutionOrder = SpecExecutionOrder.Annotated
}
```

- `SpecExecutionOrder.Undefined`. Используется по-умолчанию и зависит от загрузки классов на платформе.
- `SpecExecutionOrder.Lexicographic`. В алфавитном порядке имен классов спецификаций
- `SpecExecutionOrder.Random`. В случайном порядке.
- `SpecExecutionOrder.Annotated`. На основе аннотаций `@Order` над классами спецификаций с номерным аргументом. Меньше номер — раньше выполнение. Не помеченные выполняются в конце по стратегии `Undefined`
- `SpecExecutionOrder.FailureFirst`. Новая стратегия. Сначала выполняет упавшие в предыдущем прогоне тесты, а остальные спецификации по стратегии `Lexicographic`

Для использования `FailureFirst` необходимо включить сохранение результатов прогона в конфигурации проекта. По-умолчанию результаты сохраняются по пути на файловой системе `./.kotest/spec_failures` в директории проекта.

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

@Order(Int.MIN_VALUE) // Аннотация на классе
class FirstSpec : FreeSpec() {
    init {
        "FirstSpec-Test" { }
    }
}

@Order(Int.MIN_VALUE + 1) // Аннотация на классе
class SecondSpec : FreeSpec() {
    init {
        "SecondSpec-Test" { }
    }
}

@Order(Int.MAX_VALUE) // Аннотация на классе
class LastSpec : FreeSpec() {
    init {
        "LastSpec-Test" { }
    }
}
```

Порядок выполнения будет такой: `FirstSpec` -> `SecondSpec` -> `LastSpec`. Чем больше число в `@Order` тем **позже** выполнение.

#### Уровень тестов
Выше была рассмотрена последовательность запуска классов спецификаций (тестовых классов). Но также доступна настройка последовательности запуска тест-кейсов в рамках одного класса. 
По-умолчанию внутри класса спецификации тесты запускаются в порядке следования в коде `TestCaseOrder.Sequential` и это наиболее удачный вариант. Однако и этот порядок можно переопределить, если есть уверенность, что каждый тест абсолютно независим.

Для всего проекта можно настроить последовательность в конфигурации проекта:

```kotlin
object ProjectConfig : AbstractProjectConfig() {
    override val testCaseOrder: TestCaseOrder = TestCaseOrder.Random
}
```

В рамках одного класса через переопределение метода `testCaseOrder`:

```kotlin
class TestOrderingSpec : FreeSpec() {
    override fun testCaseOrder(): TestCaseOrder = TestCaseOrder.Lexicographic
}
```

### Параллельность

Очень интересная и сложная тема, как в контексте разработки, так и в контексте тестирования.  
Рекомендую ознакомиться с книгой `Java Concurrency in Practice` - она как раз недавно появилась на русском языке.  
Основой для принятия решения запуска тестов параллельно служит свойство теста быть независимым в контексте Фреймворка тестов и тестируемой системы.  
Недостаточно обеспечить очистку состояния тестируемой системы, в которое она перешла после выполнения теста,- необходимо обеспечить неизменное состояние во время выполнения теста, либо гарантию, что изменение состояния не имеет инвариантов с другими воздействиями на систему в других тестах.

Для гарантии изолированности необходимо:

- отдельное окружение на каждый поток выполнения тестов
- отсутствие состояния у системы, либо пересоздание всего окружения на каждый тест
- отсутствие состояния у самих тестов
- отсутствие зависимости между тестами
- отсутствие инвариантов состояния между тестами
- синхронизация в случае наличия зависимости между состояниями системы или тестов

Есть системы, которые потенциально независимо обрабатывают запросы или сессии, можно положиться на это, однако 100% гарантии никто не даст. 
Тест может воздействовать на систему в обход публичного API (например что-то записать в БД напрямую), что также может нарушить изоляцию.

**Kotest** не решает проблемы обеспечения условий для параллельного запуска, но предоставляет инструменты для организации параллельного выполнения на уровне тестов и спецификаций

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

```java
task parallelismTest(type: Test) {
    group "test"
    useJUnitPlatform()
    filter { includeTestsMatching("ru.iopump.qa.sample.parallelism.OneParallelOnTestLevelSpec") }
}
```

Имеем вывод:

```
21:15:44:979 OneParallelOnTestLevelSpec - test 2 started
21:15:44:979 OneParallelOnTestLevelSpec - test 1 started
21:15:45:490 OneParallelOnTestLevelSpec - test 1 finished
21:15:45:990 OneParallelOnTestLevelSpec - test 2 finished
```

По логу видно, что `test 1` и `test 2` запустились одновременно в `21:15:44:979` и завершились:
первый в `21:15:45:490`, второй через 500мс, - все по-плану.

#### Уровень спецификации

Чтобы запустить классы спецификаций в несколько потоков нужно задать настройки параллельности до их запуска, то есть на уровне конфигурации проекта:

```kotlin
object ProjectConfig : AbstractProjectConfig() {
    override val parallelism: Int = 3
}
```

Либо через системную переменную `-Dkotest.framework.parallelism=3` или прямо в задаче gradle:

```java
task parallelismTest(type: Test) {
    group "test"
    useJUnitPlatform()
    doFirst { systemProperties = System.properties + ["kotest.framework.parallelism": 3] }
    filter { includeTestsMatching("ru.iopump.qa.sample.parallelism.*") }
}
```

Используем одну спецификацию с прошлого раздела `OneParallelOnTestLevelSpec` и 2 новые, где сами тесты внутри будут выполняться последовательно:

<spoiler title="Еще 2 спецификации для параллельного запуска">

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

</spoiler>

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

По логу видно, что все 3 спецификации прошли за 2 сек — это сумма последовательных тестов одной их спецификаций выше, тесты из `OneParallelOnTestLevelSpec` выполнились еще быстрее. 
В выводе я оставил первые три строки, чтобы продемонстрировать имена потоков:

- пул потоков для спецификаций `NamedThreadFactory("kotest-engine-%d")`
- пул дочерних потоков для тестов `NamedThreadFactory("SpecRunner-%d")`

Все это запускается с помощью `Executors.newFixedThreadPool` и особенности работы с корутинами конкретно в этой конфигурации не используются, т.к. `suspend` функции выполняются через `runBlocking`:

```kotlin
executor.submit {
    runBlocking {
        run(testCase)
    }
}
```

> Подробности реализации в методах `io.kotest.engine.KotestEngine.submitBatch` и `io.kotest.engine.spec.SpecRunner.runParallel`

#### Исключение из параллельного запуска

Если спецификацию по каким-то причинам следует выполнять в единственном потоке, то есть аннотация `@DoNotParallelize`.  
Для отдельного теста на текущий момент подобного не предусмотрено.

### Таймауты

Имеется большой набор вариантов для установки таймаутов на время выполнения теста.

Привожу пример спецификации со всеми вариантами таймаута:

> Для пояснений в коде я буду использовать метки формата `/*номер*/` и после примера рассказывать про каждую в виде: `номер` -

<spoiler title="Варианты описания таймаутов">

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

</spoiler>

> Агрегирующего таймаута на всю спецификацию нет. Все варианты устанавливают таймаут только на тест.

`5` - `invocations = 2` В **Kotest** есть возможность задать ожидаемое кол-во успешных выполнений одного теста, чтобы считать его успешным.  
Например, чтобы проверить требование отсутствия состояния в системы, можно выполнить тест несколько раз, если хотя бы одно выполнение будет неуспешным, то тест будет считаться неуспешным. Не путайте с *fluky* - это обратная ситуация.

`1` - `override fun timeout(): Long = 2000` Через переопределение метода. Установить таймаут на тест в мс.

`2` - `override fun invocationTimeout(): Long = 2000` Через переопределение метода. Установить таймаут на один вызов теста в мс (см `5`).

`3` - `timeout = 1500` Через свойство. Установить таймаут на тест в мс и переписать `1`

`4` - `invocationTimeout = 1500` Через свойство. Установить таймаут на один вызов теста в мс и переписать `2` (см `5`).

`6` - `invocationTimeout = 550.milliseconds` Через метод конфигурации теста. Установить таймаут на тест в `kotlin.time.Duration` и переписать `3`.

`7` - `timeout = 1100.milliseconds` Через метод конфигурации теста. Установить таймаут на один вызов теста в `kotlin.time.Duration` и переписать `4` (см `5`).

> `kotlin.time.Duration` имеет статус `Experimental` и при использовании требует установки `@ExperimentalTime` над классом спецификации

Разберем вывод запуска `TimeoutSpec`:

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

Все таймауты можно задать в глобальной конфигурации и через системные переменные:

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

Во время тестов система реагирует на наши действия не мгновенно, особенно в E2E. Поэтому для **всех** проверок результатов взаимодействия с системой необходимо использовать ожидание. Приведу неполный список основных:

- ожидание HTTP ответа (уже есть на уровне HTTP клиента, достаточно только указать таймаут)
- ожидание появления запроса на заглушке
- ожидание появления/изменения записи в БД
- ожидание сообщения в очереди
- ожидание реакции UI (для Web реализовано в Selenide)
- ожидание письма на SMTP сервере

> Если не понятно, почему нужно ждать в заглушке или в БД, - отвечу в комментариях или сделаю отдельную публикацию

Есть отличная утилита для тонкой настройки ожиданий: [Awaitility](https://github.com/awaitility/awaitility) - о ней пойдет речь в будущих частях. Но **Kotest** из коробки предоставляет простой функционал ожиданий. 
В документации движка этот раздел называется `Non-deterministic Testing`.

#### Eventually

Подождать пока блок кода пройдет без Исключений, то есть успешно, в случае неуспеха повторять блок еще раз.  
Функция из пакета `io.kotest.assertions.timing`: `suspend fun <T, E : Throwable> eventually(duration: Duration, poll: Duration, exceptionClass: KClass<E>, f: suspend () -> T): T`

`f` - это блок кода, который может выбросить Исключение и этот метод будет его перезапускать, пока не выполнит успешно либо не пройдет таймаут `duration`.  
`poll` - это промежуток бездействия между попытками выполнения.  
`exceptionClass` - класс исключения по которому нужно попытаться еще раз.  
Если есть уверенность, что когда код выбрасывает `IllegalStateException`, то нужно пробовать еще раз, но если, класс исключения другой, то успешно код точно никогда не выполниться и пытаться еще раз выполнить код не нужно, тогда указываем конкретный класс `IllegalStateException::class`.

#### Continually

Выполнять переданный блок в цикле, пока не закончится выделенное время либо пока код не выбросит Исключение.  
Функция из пакета `io.kotest.assertions.timing`: `suspend fun <T> continually(duration: Duration, poll: Duration, f: suspend () -> T): T?`

`f` - блок кода, который будет запускаться в цикле пока не пройдет таймаут `duration` либо не будет выброшено Исключение.  
`poll` - промежуток бездействия между попытками выполнения

#### Рассмотрим пример из четырех тестов (2 eventually + 2 continually)

<spoiler title="Варианты использования ожидания">

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
            tries = listOf(true, true, false).iterator()
            counter = AtomicInteger()
        }

        "eventually waiting should be success" {
            /*3*/eventually(200.milliseconds, 50.milliseconds, IllegalStateException::class) {
            log.info("Try #$num")
            if (tries.next()) /*4*/ throw IllegalStateException("Try #$counter")
        }
        }

        "eventually waiting should be failed on second try" {
            /*5*/shouldThrow<AssertionError> {
            eventually(/*6*/100.milliseconds, 50.milliseconds, IllegalStateException::class) {
                log.info("Try #$num")
                if (tries.next()) throw IllegalStateException("Try #$counter")
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
                if (tries.next()) throw IllegalStateException("Try #$counter")
            }
        }.toString().also(log::error)
        }
    }
}
```

</spoiler>

`1` - Блок с полями для подсчета попыток и описания итератора из трех элементов

`2` - Перед каждым тестовым контейнером сбрасывать счетчики

`3` - Вызывается функция `eventually`, которая завершится успешно. Общий таймаут 200 мс, перерыв между попытками 50 мс, игнорировать исключение `IllegalStateException`

`4` - Первые две итерации выбрасывают `IllegalStateException`, а 3-я завершается успешно.

`5` - Ожидается, что `eventually` закончится неуспешно и выполняется проверка выброшенного исключения. При неудаче `eventually`
выбрасывает `AssertionError` с информацией о причине неудачи и настройках.

`6` - таймаута 100 мс и перерыв между попытками 50 мс, позволяют выполнить только 2 неудачный попытки, в итоге ожидание завершается неуспешно

`7` - `continually` выполнит 4 попытки, все из которых будут успешные и само ожидание завершится успехом

`8` - Ожидается, что `continually` закончится неуспешно и выполняется проверка выброшенного исключения. При неудаче `continually`
перебрасывает последнее Исключение от кода внутри, то есть `IllegalStateException`, чем отличается от `eventually`

<spoiler title="Лог выполнения с пояснениями">

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

</spoiler>

### Flaky тесты

Flaky тесты — это нестабильные тесты, которые могут случайным образом проходить неудачно. Причин этому крайне много и разбирать мы их конечно же не будем в рамках этой статьи, обсудим варианты перезапуска подобных тестов. Можно выделить несколько уровней для перезапуска:

- на уровне CI перезапускать всю Задачу прогона тестов, если она неуспешна
- на уровне Gradle сборки, используя плагин [`test-retry-gradle-plugin`](https://github.com/gradle/test-retry-gradle-plugin) или аналог
- на уровне Gradle сборки, с помощью своей реализации, например сохранять упавшие тесты и запускать их в другой задаче

> Другие инструменты для сборки Java/Kotlin/Groovy кроме Gradle не рассматриваю

- на уровне тестового движка
- на уровне блоков кода в тесте (см. раздел Встроенные ожидания и retry)

На текущей момент в версии **Kotest** `4.3.2` нет функционала для перезапуска нестабильных тестов. И не работает интеграция с Gradle плагином [`test-retry-gradle-plugin`](https://github.com/gradle/test-retry-gradle-plugin).

Есть возможность перезапустить отдельный блок кода. Это особенно актуально для UI тестирования, когда клик на некоторые элементы иногда не срабатывает.  
Представьте ситуацию с `Dropdown` внизу страницы, после скролла происходит клик, далее ожидание раскрытия элементов и клик на элемент списка. 
Так вот, независимо от Фронтового фреймворка, иногда клик для раскрытия списка может не сработать и список не появляется, нужно еще раз попробовать начиная с клика.

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

Действие `scrollTo` повторять не нужно. После перехода к элементу блок с раскрытием списка и выбором повторить 2 раза, но не дольше 20 сек игнорируя все исключения (или более конкретные).

В **Kotest** возможно создать свое расширение для перезапуска упавших `спецификаций/контейнеров теста/тестов`.  
В *3 части* руководства я покажу как создать самый простой вариант для уровня тестов.  
Остальные варианты я планирую реализовать в своем GitHub репозиторие и выпустить в MavenCentral, если к тому моменту разработчик Kotest не добавит поддержку 'из коробки'.

### Фабрики тестов

Представим, что в каждом тесте нужно выполнять одни и те же действия для подготовки окружения: наполнять БД, очереди, конфиги на файловой системе. 
Для этих действий хочется создать шаблон теста с адекватным именем, возможно с описанием в виде `javadoc`/`kdoc` и несколькими аргументами, например именем и паролем тестового пользователя. 
В **Kotest** такой подход называется `Test Factories` и позволяет вставлять куски тестов в корневой тест. 
Это ни функции, ни методы, ни абстракции — это параметризованные части теста с той же структурой, что основной тест, но используемые в нескольких местах кода.

>Я придерживаюсь правила, что **спецификация** теста должна быть понятна любому человеку, имеющему экспертизу в предметной области теста.  
>Под **спецификацией** теста я понимаю не реализацию, а описания шагов в виде строк в BDD стиле. В [1-ой части руководства](https://habr.com/ru/post/520380/) я раскрывал тему форматирования теста более подробно.  
>Даже если требуется много `copy/paste` для сохранения формата теста, **нужно** это делать.  
>А вот тестовый движок/фреймворк позволяет **спецификацию** теста запустить, провести взаимодействия с системой автоматически, как если бы все что написано в тесте делал человек.

Очень важно не нарушать читабельность теста использованием шаблонов:

- имя шаблона должно быть понятное и сопровождаться описанием, а так же описанием всех параметров.
- шаблон должен выполнять одну функцию, например настройка БД (принцип единственной ответственности)
- шаблон должен быть описан в BDD стиле
- шаблон не должен быть слишком абстрактным (субъективно)

Теперь к реализации в **Kotest**. Она очень ограничена, поэтому я приведу пример того, как рекомендует делать шаблоны официальная документация и как это удобнее делать своими силами через `scope`-функции и функции-расширения. 
Пример кода с пояснениями:

<spoiler title="2 вида фабрик тестов. TestFactory и scope-функции">

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

/** Add [TestType.Container] by scope function extension */
/*2.2*/suspend inline fun FreeScope.containerTemplate(): Unit {
    "template container with FreeScope context" - {
        /*2.3*/testCaseTemplate()
    }
}

/** Add [TestType.Test] by scope function extension */
/*2.4*/suspend inline fun FreeScope.testCaseTemplate() {
    "nested template testcase with FreeScope context" { }
}

private val log = LoggerFactory.getLogger(FactorySpec::class.java)
```

</spoiler>

##### TestFactory

`1.1` - С помощью метода `fun include(factory: TestFactory)` включаем шаблон теста в спецификацию в качестве самостоятельного теста.

`1.2` - Определена функция `containerFactory`, которая возвращает экземпляр `TestFactory` и принимает несколько параметров. Для создания объекта `TestFactory` используем функцию `fun freeSpec(block: FreeSpecTestFactoryConfiguration.() -> Unit): TestFactory`, которая принимает блок тестов и предоставляет контекст `FreeSpecTestFactoryConfiguration`. 
Внутри этого блока пишем тест, как обычно, в BDD силе. Далее с помощью `include`шаблон вставляется как обычный контейнер теста.

>Для `TestFactory` выполняются все обратные вызовы, что определены для спецификации, куда встраивается шаблон.  
>А также доступны все методы для создания обратных вызовов внутри фабрики, например `beforeContainer`.  
>У `TestFactory` есть большие ограничения:
>- нельзя вставлять `include` друг в друга
>- встраивать можно только на уровень спецификации, то есть фабрика должна быть полным тестом, а не частью теста.

##### Шаблоны через `scope`-функции и функции-расширения.

`2.1` - Внутри контейнера вызывается пользовательская функция `containerTemplate` и добавляет к контексту этого контейнера новые шаги теста.

`2.2` - Функция `suspend inline fun FreeScope.containerTemplate(): Unit` выполняет свой код в контексте внешнего теста `FreeScope` и просто **изменяет** этот контекст, добавляя новый вложенный контейнер.  
Функция ничего не возвращает, а именно изменяет (`side-effect`) переданный контекст. Тесты пишем так, как будто они не в отдельной функции, а в спецификации.  
`suspend` - обязателен, т.к. все тесты запускаются в корутине.  
`inline` - для скорости, не обязателен. Указывает компилятору на то, что код этой функции нужно просто скопировать вместо вызова, то есть в байт-коде, не будет `containerTemplate`, а будет вставленный код в спецификации.  
`FreeScope.` - внутренности вызываются в контексте этого объекта, в данном случае контейнера

`2.3` - Внутри шаблона вызывается другой шаблон, который добавлен новые шаги. В реальных тестах так делать **не следует**

`2.4` - Функция `suspend inline fun FreeScope.testCaseTemplate(): Unit` добавляет вложенные шаги теста вместо вызова. Все то же самое, что и `2.2`

В итоге имеем следующую структуру теста после выполнения:

![](https://habrastorage.org/webt/p-/ie/zr/p-iezrb-ceh51xhmesr5fvmlico.png)

### Property тестирование

Представим, что мы в 90-х годах на большом и сложном проекте, где критично качество и есть ресурсы на всеобъемлющее тестирование, на всех вариантах тестовых данных и окружений. 
В этом случае невозможно использовать ограниченный тестовых набор, если только тип входных данных не ограничен парой вариантов - Data-Driver не подходит. 
В таких случаях необходимо генерировать тестовые данные случайным образом, либо вообще покрывать абсолютно все возможные значения. 
Необходимо не просто уметь генерировать данные, но и легко менять параметры генерации:функции распределения значений, граничные значения и т.п. 
Не менее важна возможность повторить последовательность с прошлого прогона, чтобы воспроизвести ошибку. А также требуются инструменты для создания своих генераторов.

#### Генераторы данных

В Kotest существует 2 типа генераторов данных:

- `Arb` (Arbitrary - Случайный) генерирует бесконечные последовательности из которых по-умолчанию в тесте будет используется 1000 значений
- `Exhaustive` (Исчерпывающий) служит для полного перебора ограниченного набора значений

Полный список генерируемых типов довольно большой и хорошо описан в [официальной документации](https://kotest.io/docs/proptest/property-test-generators-list.html)

Основная цель генераторов — возможность запускать Property-тесты на их основе. Второстепенная цель — генерация единичных значений. Также генераторы предоставляют широкий набор методов по настройке и модификации генерируемой последовательности.

Приведу несколько примеров генераторов для генерации единичных значений или ограниченного набора значений, которые можно с успехом использовать в Data-Driven тестах:

<spoiler title="Применение Arb генераторов для генерации случайных данных">

```kotlin
/** For string generator with leading zero */
/*1*/val numberCodepoint: Arb<Codepoint> = Arb.int(0x0030..0x0039)
        .map { Codepoint(it) }

/** For english string generator */
/*2*/val engCodepoint: Arb<Codepoint> = Arb.int('a'.toInt()..'z'.toInt())
    .merge(Arb.int('A'.toInt()..'Z'.toInt()))
    .map { Codepoint(it) }

class GeneratorSpec : FreeSpec() {
    init {
        /*3*/"random number supported leading zero" {
            Arb.string(10, numberCodepoint).next()
                .also(::println)
        }

        /*4*/"random english string" {
            Arb.string(10, engCodepoint).orNull(0.5).next()
                .also(::println)
        }

        /*5*/"random russian mobile number" {
            Arb.stringPattern("+7\\(\\d{3}\\)\\d{3}-\\d{2}-\\d{2}").next()
                .also(::println)
        }

        /*6*/"exhaustive collection and enum multiply" {
            Exhaustive.ints(1..5).times(Exhaustive.enum<Level>()).values
                .also(::println)
        }

        /*7*/"exhaustive collection and enum merge" {
            Exhaustive.ints(1..5).merge(Exhaustive.enum<Level>()).values
                .also(::println)
        }
    }
}
```

</spoiler>

`1` - Класс `Codepoint` задает набор символов в виде кодов Unicode. Этот класс используется в `Arb` для генерации строк.  
В **Kotest** есть встроенные наборы символов в файле `io.kotest.property.arbitrary.codepoints`.  
В примере определен набор Unicode цифр (такого набор нет среди встроенных). В дальнейших шагах этот набор будет использован для генерации номеров, которые могут начинаться на `0`

`2` - Определен набор букв английского алфавита. Т.к. в Unicode они идут не по порядку, то сначала создается набор малого регистра, потом `merge` с набором большого регистра.

`3` - Использование набора символов цифр `numberCodepoint` для генерации номеров, где возможен `0` в начале типа `String`, длина 10 символов.

`4` - Использование набора символов английского алфавит `engCodepoint` для генерации слов длиной 10 символов. А также с вероятностью 50% вместо строки сгенерируется `null` - метод `orNull(0.5)`

`5` - Метод `Arb.stringPattern` позволяет генерировать строки на основе RegEx паттерна — очень полезная функциональность. Реализация на основе [Generex](https://github.com/mifmif/Generex). Производительность оставляет желать лучшего и зависит от сложности регулярного выражения. 
Для номера телефона генерация происходит в 10 раз медленнее, чем при использовании `Arb.string(10,numberCodepoint)` с форматированием. У меня **1000** генераций телефона по паттерну -> **294 мс** и 1000 генераций телефона из строки цифр с последующим форматированием -> **32 мс**. 
Замерять удобно методом `measureTimeMillis`.

`6` - `Exhaustive` фактически просто обертка для перечислимых типов и актуальна только в интеграции с Property-тестами, однако также имеет интересные возможности. Здесь происходит умножение набора `Int` на объекты `enum Level` и на выходы получается 25 `Pair`. Если интегрировать в тест, то будет 25 запусков для каждой пары.

`7` - `Exhaustive` объединение в одну коллекцию из 10 разнородных элементов вида: `[1, ERROR, 2, WARN ...]`

> Вся функциональность генераторов доступна не только для Property-тестов, но и для простых тестов, где нужны случайные значения тестовых данных.  
> Для генерации одного значения используется терминальный метод `Arb.next`

Результаты запуска:

```
// Test 1
2198463900
// Test 2
tcMPeaTeXG
// Test 3
+7(670)792-05-16
// Test 4
[(1, ERROR), (1, WARN), (1, INFO), (1, DEBUG), (1, TRACE), (2, ERROR), (2, WARN), (2, INFO), (2, DEBUG), (2, TRACE), (3, ERROR), (3, WARN), (3, INFO), (3, DEBUG), (3, TRACE), (4, ERROR), (4, WARN), (4, INFO), (4, DEBUG), (4, TRACE), (5, ERROR), (5, WARN), (5, INFO), (5, DEBUG), (5, TRACE)]
// Test 5
[1, ERROR, 2, WARN, 3, INFO, 4, DEBUG, 5, TRACE]
```

#### Написание и конфигурирование Property тестов

**Kotest** предоставляет две функции для запуска Property-тестов, а также их перегруженные вариации:

- `suspend inline fun <reified A> forAll(crossinline property: PropertyContext.(A) -> Boolean)`
- `suspend inline fun <reified A> checkAll(noinline property: suspend PropertyContext.(A) -> Unit)`

Отличие между ними заключается в том, что переданный блок в `forAll` должен возвращать `Boolean`, а в `checkAll` все проверки должны быть внутри, что более привычно.

Выполнить 1000 итераций кода со случайным входным числом типа `Long`:

```kotlin
checkAll<Long> { long: Long ->
    val attempt = this.attempts()
    println("#$attempt - $long")
    long.shouldNotBeNull()
}
```

Обращаю внимание на то, что функциональное выражение в аргументе `forAll` и `checkAll` не простое, а с дополнительным контекстом!

`suspend PropertyContext.(A) -> Unit` - имеем аргумент generic типа (в примере это `long: Long`), но также имеем доступ к контексту `PropertyContext`, через ключевое слово `this` (для красоты его можно опустить).

`val attempt = this.attempts()` - здесь возвращается кол-во пройденных попыток из `PropertyContext`, но там есть и другие полезные методы.

##### Пользовательские генераторы

У обоих видов генераторов (`Arb` и `Exhaustive`) есть терминальные методы `forAll` и `checkAll`, которые запускают блок теста, принимающий сгенерированные значения от пользовательских генераторов в качестве аргумента.

###### Arb

**Задача**: Рассмотрим написание Property-теста на примере основной теоремы арифметики: каждое натуральное число можно представить в виде произведения простых чисел. Также это называется факторизация. 
Допустим имеется реализованный на Kotlin алгоритм разложения и его необходимо протестировать на достаточном наборе данных. 
Код алгоритма можно найти [в примерах](https://github.com/kochetkov-ma/pump-samples/blob/master/qa-kotest-articles/kotest-second/src/test/kotlin/ru/iopump/qa/sample/property/PropertySpec.kt#L49)

**Тестовый сценарий**: сгенерированное число, разложить с помощью алгоритма на простые множители и проверить, что полученные множители простые и их произведение равно исходному числу — тем самым мы проверим реализацию алгоритма.  
Очевидно, что одного прогона недостаточно и 10 прогонов недостаточно, как минимум нужно прогнать тест на 1000 случайных величин, а может и больше.

**Параметры генератора данных**: Кол-во = 1000. Сгенерированное число должно быть больше 1. В сгенерированной последовательности первые два числа должны быть граничные: `2` и `2147483647`.

```kotlin
"Basic theorem of arithmetic. Any number can be factorized to list of prime" {
    /*1*/Arb.int(2..Int.MAX_VALUE).withEdgecases(2, Int.MAX_VALUE).forAll(1000) { number ->
    val primeFactors = number.primeFactors
    println("#${attempts()} Source number '$number' = $primeFactors")
    /*2*/primeFactors.all(Int::isPrime) && primeFactors.reduce(Int::times) == number
}

    /*3*/Arb.int(2..Int.MAX_VALUE).checkAll(1000) { number ->
    val primeFactors = number.primeFactors
    println("#${attempts()} Source number '$number' = $primeFactors")
    /*4*/primeFactors.onEach { it.isPrime.shouldBeTrue() }.reduce(Int::times) shouldBe number
}
}
```

`1` - создается генератор на 1000 итераций, множество значений от 2 до Int.MAX_VALUE включительно, отдельным методом `withEdgecases`необходимо явно указать 2 и Int.MAX_VALUE граничными значениями. 
Методом `forAll` запускаем тестирование, выходной результат блока теста `Boolean`. `AssertionError` Движок выбросит за нас, если будет результат `false`.

`2` - метод `all` принимает ссылку на метод и проверяет, что каждый множитель простой, метод `reduce` выполняет умножение, а также выполняется конъюнкцию двух проверок.

`3` - все абсолютно аналогично `1`. Отличие только в том, что блок кода не должен возвращать `Boolean` и используются стандартные `Assertions`, как в обычном тесте.

`4` - вместо логических операций используем проверки **Kotest**

###### Exhaustive

Для исчерпывающего тестирования все аналогично `Arb` подходу.

Допустим, реализован метод, который в зависимости от enum `UUIDVersion` генерирует указанный тип `UUID`. А также же метод должен принимать `null` и генерировать `UUID` типа `UUIDVersion.ANY`. 
Сигнатура: `fun UUIDVersion?.generateUuid(): UUID`

Чтобы проверить этот функционал, нужно перебрать абсолютно **все** значения `UUIDVersion` + `null`. 
Не имея знаний о возможностях `Exhaustive` можно просто перебрать данные в цикле. Однако `Exhaustive` упрощает и без того несложную задачу:

```kotlin
"UUIDVersion should be matched with regexp" {
    /*1*/Exhaustive.enum<UUIDVersion>().andNull().checkAll { uuidVersion ->
    /*2*/uuidVersion.generateUuid().toString()
    /*3*/.shouldBeUUID(uuidVersion ?: UUIDVersion.ANY)
    .also { println("${attempts()} $uuidVersion: $it") }
}
}
```

`1` - `reified` функция `Exhaustive.enum` создаст последовательность всех значений `UUIDVersion` и добавит `null`, а далее будет вызван Property-тест

`2` - вызов тестируемой функции-расширения для генерации `UUID`

`3` - встроенная в **Kotest** проверка на соответствие `UUID` регулярному выражению

##### Генераторы по-умолчанию

Если использовать функции `forAll` и `checkAll` без явного указания генератора типа `Arb` или `Exhaustive`, то будет использоваться генератор по-умолчанию в зависимости от `Generic-типа`. 
Например для `forAll<String>{ }` будет использован генератор `Arb.string()`. Вот полный набор из внутренностей **Kotest**:

```kotlin
fun <A> defaultForClass(kClass: KClass<*>): Arb<A>? {
    return when (kClass.bestName()) {
        "java.lang.String", "kotlin.String", "String" -> Arb.string() as Arb<A>
        "java.lang.Character", "kotlin.Char", "Char" -> Arb.char() as Arb<A>
        "java.lang.Long", "kotlin.Long", "Long" -> Arb.long() as Arb<A>
        "java.lang.Integer", "kotlin.Int", "Int" -> Arb.int() as Arb<A>
        "java.lang.Short", "kotlin.Short", "Short" -> Arb.short() as Arb<A>
        "java.lang.Byte", "kotlin.Byte", "Byte" -> Arb.byte() as Arb<A>
        "java.lang.Double", "kotlin.Double", "Double" -> Arb.double() as Arb<A>
        "java.lang.Float", "kotlin.Float", "Float" -> Arb.float() as Arb<A>
        "java.lang.Boolean", "kotlin.Boolean", "Boolean" -> Arb.bool() as Arb<A>
        else -> null
    }
}
```

Примеры кода:

```kotlin
"check 1000 Long numbers" {
    checkAll<Long> { long ->
        long.shouldNotBeNull()
    }
}
```

##### Seed

Последнее, что хотелось бы добавить про Property-тестирование и генераторы, это воспроизведение ошибки. Для воспроизведения необходимо повторить тестовые данные. Чтобы повторить последовательность, существует такое понятие, как **seed**.
Используемый алгоритм псевдо случайных чисел для генерации последовательности использует некий порождающий элемент (seed), и для равных seed получаются равные последовательности. 
Cемя изначально генерируется случайно, используя внешний порождающий элемент, например, время либо физический процесс. При возникновении ошибки, **Kotest** печатает используемое семя, которое можно указать в конфигурации теста и воспроизвести результат.

```kotlin
"print seed on fail" {
    /*1*/shouldThrow<AssertionError> {
    checkAll<Int> { number ->
        println("#${attempts()} $number")
        /*2*/number.shouldBeGreaterThanOrEqual(0)
    }
}./*3*/message.shouldContain("Repeat this test by using seed -?\\d+".toRegex())
}
"test with seed will generate the same sequence" {
    Arb.int().checkAll(/*4*/ PropTestConfig(1234567890)) { number ->
        /*5*/if (attempts() == 24) number shouldBe 196548668
        if (attempts() == 428) number shouldBe -601350461
        if (attempts() == 866) number shouldBe 1742824805
    }
}
```

`1` - Ожидаем исключение `AssertionError` с семенем.

`2` - Из 1000 чисел последовательности примерно половина будет точно меньше 0 и проверка должна сработать почти сразу

`3` - Демонстрация, что сообщение исключения содержит информацию о числе **seed**

`4` - Для теста явно указано семя в конструкторе `PropTestConfig`. Ожидаем, что для этого теста будет генерироваться всегда одна последовательность, независимо от платформы

`5` - `attempts()` возвращает номер итерации. Для итераций 24, 428, 866 будут всегда сгенерированы одинаковые числа

Заключение
------
Во-первых, привожу ссылку на все примеры [qa-kotest-articles/kotest-second](https://github.com/kochetkov-ma/pump-samples/tree/master/qa-kotest-articles/kotest-second/src).

Был рассмотрен практически весь функционал **Kotest**

Осталось рассказать про создание расширений и доступные встроенные расширения, а далее перейдем к интеграции с другими библиотеками помогающими в автоматизации тестирования

Ресурсы
------
[Kotlin. Автоматизация тестирования (часть 1). Kotest: Начало](https://habr.com/ru/post/520380/)

[Примеры кода](https://github.com/kochetkov-ma/pump-samples/tree/master/qa-kotest-articles/kotest-second/src)

[Официальная документация Kotest](https://kotest.io/docs/quickstart)

[Kotest GitHub](https://github.com/kotest/kotest)

[Kotlin Lang](https://kotlinlang.org/docs/home.html)

[Coroutines Tutorial](https://kotlinlang.org/docs/coroutines-basic-jvm.html)

[Gradle Testing](https://docs.gradle.org/current/userguide/java_testing.html)
