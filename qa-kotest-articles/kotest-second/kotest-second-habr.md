Kotlin. Автоматизация тестирования (часть 2). Kotest. Deep Diving + Spring
======
![Kotest](https://habrastorage.org/webt/v5/e5/bd/v5e5bdqn6wzbubebhh1lkni0iiw.png)

Продолжение цикла публикаций/руководства про автоматизацию функционального тестирования на **Kotlin** 
на основе фреймворка **Kotest** с использованием наиболее полезных дополнительных библиотек, 
существенно облегчающих и ускоряющих создание тестов. 
В этой части мы углубимся в возможности Kotest, рассмотрим Property Testing и создадим пользовательское расширение.

Для освежения знаний привожу ссылки на прошлые части руководства:
- [Часть 1. Kotest: Начало](https://habr.com/ru/post/520380/)

### Мотивация и цели
На основе первой части руководства можно вполне успешно создавать сценарии, расширять покрытие. 
Но с увеличением кол-ва тестов и функциональности целевого приложения неизбежно растет и сложность тестов.
Чтобы не изобретать "велосипеды" и не добавлять в проект тестов лишние зависимости хороший QA обязать детально знать функционал фреймворка для запуска тестов
и всех сторонних библиотек. 
Это позволит в дальнейшем проектировать минималистичную, масштабируемую и расширяемую архитектуру каркаса для тестов и самих тестов.
И успешно интегрировать популярные библиотеки в свой каркас.
Уже в следующей части я расскажу про интеграцию со сторонними библиотеками:
- Spring Core/Test
- TestContainers
- Allure
- Awaitility

Все что излагается в данной публикации — это авторские выдержки из официальной документации [kotest.io](https://kotest.io/) дополненные моими наблюдениями, пояснениями и примерами.
> Первая версия документации находилась, в [GitHub проекта](https://github.com/kotest/kotest), на 15.01.2021 уже третья версия расположена на сайте [kotest.io](https://kotest.io/).
> В предыдущей версии сайта был доступен поиск, сейчас почему-то документация осталась без поиска, что несколько усложняет навигацию.

### Группировка тестов
Рекомендую уже на самых ранних стадиях развития проекта с тестами продумать группировку тестов.
Самый базовые критерии группировки, которые первыми приходят на ум:
1. По уровню.
Модульные -> На один сервис (в контексте микро-сервисов) -> Интеграционные -> E2E

2. По платформе.
Windows\Linux\MacOS   
Desktop\Web\IOS\Android

3. По функциональности
Frontend\Backend
В контексте целевого приложения:Авторизация\Администрирование\Отчетность...
   
4. По релизам
Sprint-1\Sprint-2
1.0.0\2.0.0   

Ниже описание реализации группировки в **Kotest**
#### [Метки](https://kotest.io/docs/framework/tags.html)
Для группировки тестов используются тэги — объекты класса `io.kotest.core.Tag`.
Есть несколько вариантов декларирования меток:
- расширить класс `Tag`, тогда в качестве имени будет использовано `simpleName` расширяющего класса, либо переопределенное свойство `name`.
- использовать объект класса `io.kotest.core.NamedTag`, где имя передается в конструкторе.
- создать `String` константу и использовать в аннотации `@io.kotest.core.annotation.Tags`, однако в прошлых версиях **kotest** эта аннотация принимал тип класса тэгов.

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
class LinuxSpec: FreeSpec() {
    init {
        "1-Test for Linux" { }
        "2-Test for Linux and Regress only".config(tags = setOf(regressTag)) { }
    }
}

class WindowsSpec: FreeSpec() {
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
В коде спецификации можно задать динамические правила включения\выключения тестов двумя путями:
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
class OnCICondition: EnabledCondition {
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
По-умолчанию спецификации выполняются в порядке загрузки классов JVM. 
Это зависит от платформы, но скорее всего порядок будет алфавитный.
Есть возможность задать порядок через конфигурацию уровня проекта используя enum `io.kotest.core.spec.SpecExecutionOrder`
```kotlin
object ProjectConfig : AbstractProjectConfig() {
    override val specExecutionOrder = SpecExecutionOrder.Annotated
}
```
- SpecExecutionOrder.Undefined. Используется по-умолчанию и зависит от загрузки классов на платформе.
- SpecExecutionOrder.Lexicographic. В алфавитном порядке имен классов спецификаций
- SpecExecutionOrder.Random. В случайном порядке.
- SpecExecutionOrder.Annotated. На основе аннотаций `@Order` над классами спецификаций с номерным аргументом. 
  Меньше номер — раньше выполнение. 
  Не помеченные выполняются в конце по стратегии `Undefined`
- SpecExecutionOrder.FailureFirst. Новая стратегия. 
  Сначала выполняет упавшие в предыдущем прогоне тесты, а остальные спецификации по стратегии `Lexicographic`

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



### Параллельность
#### Уровень спецификации
#### Уровень тестов

### Таймауты

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