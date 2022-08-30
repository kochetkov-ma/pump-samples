Kotlin. Автоматизация тестирования (Часть 3). Расширения Kotest и Spring Test
======
![Kotest](https://raw.githubusercontent.com/kochetkov-ma/pump-samples/master/qa-kotest-articles/kotest-first/kotest.png)

Продолжаем автоматизировать функциональные тесты на **Kotlin** и знакомиться с возможностями фреймворка **Kotest**

Расскажу про расширения `Kotest`:

- что это такое
- как расширения помогают писать тесты
- реализацию запуска расширений в `Kotest`  
- некоторые встроенные расширения
- про расширение для **Spring**
- углублюсь в интеграцию **Kotest** и **Spring Boot Test**
- сравню с **Junit5**
- и на закуску добавлю отчеты **Allure**

> ⚠️Будет много кода, внутренностей и примеров.

Все части руководства:

- [Kotlin. Автоматизация тестирования (часть 1). Kotest: Начало](https://habr.com/ru/post/520380/)
- [Kotlin. Автоматизация тестирования (Часть 2). Kotest. Deep Diving](https://habr.com/ru/company/nspk/blog/542754/)
  <cut />

## О себе
Являюсь **QA Лидом Автоматизации** на большом проекте в [Мир Plat.Form (НСПК)](https://habr.com/ru/company/nspk/).

Проект зародился 3 года назад и вырос до четырех команд, где трудится в общей сложности более **10 разработчиков** в тестировании (**SDET**), без учета остальных участников в лице аналитиков, разработчиков и технологов.
Наша задача — автоматизировать функциональные тесты на уровне отдельных сервисов, интеграций между ними и **E2E** до попадания функционала в **релиз** - всего порядка 40 микро-сервисов.
От 1 до 5 микро-релизов в неделю.
Взаимодействие между сервисами - `Kafka`, внешний API - REST, а также 3 фронтовых Web приложения.
Разработка самой системы и тестов ведется на языке **Kotlin**, а движок для тестов был выбран **Kotest**.

В данной статье и в остальных публикациях серии я максимально подробно рассказываю о тестовом Движке и вспомогательных технологиях в формате **Руководства/Tutorial**.

## Парадигма расширений

Что такое расширение для фреймворка тестирования?

#### Это класс, который реализует определенный интерфейс-маркер для фреймворка тестирования или его производные интерфейсы.

#### Интерфейс расширения предоставляет методы по перехвату событий жизненного цикла или даже для изменения этого цикла.

Например, интерфейс расширения, которое, может отключить тест, то есть изменить его жизненный цикл.

```kotlin
interface EnabledExtension : Extension {

    suspend fun isEnabled(descriptor: Descriptor): Enabled
}
```

#### Расширение подключается к проекту / классу / тесту с помощью аннотаций или программно добавлением в реестр расширений.

Например, расширения `Junit5` обычно подключаются на класс с помощью аннотации `@ExtendWith`:

```kotlin
@ExtendWith(SpringExtension::class)
internal class Junit5Test 
```

Также этой аннотацией можно пометить другие аннотации:

```java

@ExtendWith(SpringExtension.class)
public @interface SpringBootTest {
}
```

Система расширений - это стандартный функционал для любого тестового фреймворка.

- В Junit4 - это интерфейсы `TestRule` и `MethodRule`
- В Junit5 - это `Extension`
- В Kotest - тоже интерфейс `Extension`

Я бы выделил два вида расширений в `Kotest`: `Listener` и `Interceptor`

### Listener

- На вход принимает неизменяемый объект, например `TestCase` или `TestResult`, возможно еще какой-то контекст.
- Что-то делает и результат транслирует в сторонней сущности, например репорте.
- Ничего не возвращает, либо отдает какой-то неизменяемый результат для журналирования.

Например, недавно появившийся в `Kotest` интерфейс `InstantiationErrorListener` - он позволяет перехватить ошибку при создании сущности класса теста.  
Он решает проблему, когда в результате неверного контекста или ошибок в инициализации класс с тестами просто не удалось создать.  
Тогда в отчете может вообще отсутствовать этот не созданный тест, отчет будет успешным, а сборка проваленной.

```kotlin
interface InstantiationErrorListener : Extension {
    suspend fun instantiationError(kclass: KClass<*>, t: Throwable)
}
```
Все слушатели могут выполняться асинхронно, так как не влияют на жизненный цикл теста и друг друга, а результат их выполнений собирается в коллекцию. 
Первое перехватываемое событие перед началом выполнения теста реализуется в слушателях `BeforeInvocationListener`:   
![](https://habrastorage.org/webt/_z/ze/fv/_zzefvos7v0i4lqcq0qrxvwy_7s.png)

### Interceptor

- На вход принимает неизменяемый объект или функцию
- Что-то делает с объектом, запускает функцию, обрабатывает результат функции, а может и не запускает функцию
- На выходе возвращает новую сущность или результат функции, возможно измененный
- Выходные данные влияют на дальнейшее выполнение жизненного цикла теста
- Результат (внутри фреймворка) передается следующему перехватчику или расширению как в паттерне `Chain of Responsibility`

> Название `Interceptor` абстрактное - не факт, что оно фигурирует в имени интерфейса, который подходит под описание

В качестве примера приведу фундаментальное расширение: `ConstructorExtension`.  
Перехватывает момент создания объекта класса теста, на выходе ожидает внутренний объект фреймворка - `Spec`.  
Тут можно также обработать ошибки создания, как в `InstantiationErrorListener`, но также придется взять на себя ответственность за подготовку всего тестового класса для дальнейших действий в цепочке.

> Сразу отвечаю на вопрос: `Что будет если несколько таких расширений?`  
> Будет использован результат первого добавленного в реестр, если оно вернет не `null`. Либо вызывается логика создания `Spec` по умолчанию.

На самом деле внутри `Kotest` большая часть функционала также запускается по модели перехватчиков. 
Специальным внутренним перехватчикам делегируется поиск и запуск пользовательских расширений:
![](https://habrastorage.org/webt/tt/pv/dk/ttpvdksb5cpvg7pbdqer7knuj2e.png)

В ходе этого этапа создается [функция высшего порядка с помощью свертывания fold](https://en.wikipedia.org/wiki/Fold_(higher-order_function)), с порядком равным кол-ву перехватчиков.
То есть каждый перехватчик превращается в функцию, которая вызывается внутри другого перехватчика и результат последнего выполнения рекурсивно передается в вышестоящую функцию.
- `1` Запускается первый перехватчик - в ядре Kotest `5.4.1` это `TestPathContextInterceptor`, он выполняет подготовку контекста `coroutine` и вызывает следующий перехватчик.
- `2` Где-то в середине списка выполняется `TestCaseExtensionInterceptor`, который внутри ищет пользовательские перехватчики и выполняет уже их.
- `3` В случае этой стать последовательно выполняются два пользовательских внешних перехватчика: `KotestAllureListener` и `SpringTestExtension`
- `4` Последним выполняется `CoroutineDebugProbeInterceptor` и передается свой результат обратно в вызвавший его перехватчик и обратно до верхнего вызова

## Как расширения помогают писать тесты?

Тот же `Spring Test` можно нормально использовать совместно с тестовым Фреймворком только через расширение, так как необходимо контролировать создание сущностей тестов и контекст к ним.

Расширения для всех основных фреймворков тестирования работают примерно одинаково.
Поэтому на единой кодовой базе можно очень быстро создать набор адаптеров-расширений для всех Фреймворков, а все вызовы делегировать единой внутренней логике.
Очень популярны расширения для создания заглушек зависимостей.
Например, с помощью расширения `MockitoExtension` для `Junit5` можно легко создать заглушки всех репозиториев и не поднимать реальную базу просто добавив аннотацию `@Mock` на поле.
> Опуская момент с настройкой ответов ...

```kotlin
@ExtendWith(MockitoExtension::class)
internal class Junit5Test {

    @Mock UserRepository userRepository
}
```

- Расширение берет на себя всю работу по созданию ресурсов для тестирования
- Контролирует жизненный цикл ресурса и привязывает его к циклу тестов, чтобы сохранить изоляцию между сценариями
- Освобождает ресурсы
- Обрабатывает ошибки
- И много чего еще, в зависимости от конкретного расширения

> Расширения позволяют сократить время и строки кода при создании тестов.   
> А также ошибки в тестах, так как расширения сами оттестированы, а ваш код обвязки тестов скорее всего нет.

## Немного встроенных расширений Kotest

Вся документация по расширениям есть в разделе [Extensions](https://kotest.io/docs/extensions/extensions.html).
Но мы здесь собрались, чтобы попробовать самое интересное, а не парсить документацию.

> Однако документация у них супер классная! Есть вообще все!
>
> - версии под каждый релиз
> - удобная навигация
> - приятный для чтения дизайн
> - подробный `Changelog`
> - ссылки на статьи / `StackOverflow` / `GitHub` / стабильные и snapshot версии
>
> Все это заслуга разработчиков `Kotest` и Фреймворка `Docusaurus 2.0` - мой лайк [Docusaurus 2.0](https://github.com/facebook/docusaurus)

В `Kotest` есть набор встроенных расширений. Все они в артефакте `io.kotest:kotest-extensions-jvm`,
который транзитивно приходит вместе с основным `io.kotest:kotest-runner-junit5`.
Находятся в пакете `io.kotest.extensions` - просто имейте это ввиду, там их много и я расскажу про несколько.

#### Возьмем расширение `SystemEnvironmentTestListener`

Все просто - в конструкторе `SystemEnvironmentTestListener` мы задаем набор переменных окружения, которые подменяем на время работы теста.
Внутри теста эти переменные будут иметь значения, указанные пользователем. После теста переменные возвращают свои значения.
Расширение не потокобезопасно, поэтому нужно помечать тест `@DoNotParallelize` - даже если не пускаете тесты параллельно, нужно пометить!
Либо применять расширение сразу ко всему проекту.

```kotlin
@DoNotParallelize
internal class KotestSystemEnvironmentTest : StringSpec() {

    /* 1 */
    override fun extensions() =
        listOf(SystemEnvironmentTestListener(/* 2 */mapOf("USERNAME" to "TEST", "OS" to "Astra Linux"), /* 3 */OverrideMode.SetOrOverride))

    init {
        /* 4 */
        println("Before use listener: " + System.getenv("USERNAME"))
        println("Before use listener: " + System.getenv("OS"))

        "Scenario: environment variables should be mocked" {
            /* 5 */
            System.getenv("USERNAME") shouldBe "TEST"
            System.getenv("OS") shouldBe "Astra Linux"
        }
    }
}
```

- `1` Расширение применяется на уровне спецификации
- `2` Словарь с переменными и новыми значениями
- `3` Режим переопределения. Данный режим в любом случае перезаписывает переменную. А `OverrideMode.SetOrError` например только добавляет, но падает с ошибкой если пытаемся переписать существующую переменную.
- `4` В этом месте выводятся все еще реальные значения
- `5` А в тесте уже переопределенные

> Кстати класс `OverrideMode` - это `sealed class`  
> И в `Kotest` очень много примеров использования вместо `enum`-ов `sealed class`-ов

#### Посмотрим очень интересное расширение `SpecSystemExitListener`

Иногда в тестируемом приложении может вызываться `System.exit()`, чтобы завершить процесс, например выполнить `graceful shutdown` или завершить проложение из-за недостатка ОП с кодом `137`.
Эту ситуацию можно перехватить и проверить, что:

- `System.exit()` действительно был вызван
- Код выхода действительно соответствует ситуации

```kotlin
@DoNotParallelize /* 1 */
internal class KotestSystemExitTest : StringSpec() {
    /* 2 */
    override fun extensions() = listOf(SpecSystemExitListener)

    init {
        "Scenario: testing application try use System.exit" {
            /* 3 */ shouldThrow<SystemExitException> {
            runApplicationWithOutOfMemoryExitCode() /* 4 */
        }.exitCode shouldBe 137 /* 5 */
        }
    }
}

private fun runApplicationWithOutOfMemoryExitCode(): Nothing = exitProcess(137)
```

- `1` Здесь тоже нет гарантий на работу в параллельном режиме, поэтому `@DoNotParallelize`
- `2` Добавляем расширение
- `3` Выполняем код и ожидаем специальное исключение от Kotest - `SystemExitException`
- `4` Внутри выполняется приложение, которое вызывает `Sysytem.exit()` == `exitProcess()`
- `5` Ожидаемый код окончания `137`

#### Последнее на сегодня и, наверное, самое популярное расширение `ConstantNowTestListener`

Часто в приложении используется текущая дата, обычно в UTC. И часто очень сложно предсказать какая получится текущая дата, чтобы ее проверить на выходе.
Также часто необходимо подменить текущую дату на определенную, чтобы проверить логику и не очень удобно делать это через системное время.
Рассматриваемое расширение решает эту проблему и позволяет переопределить метод `now()` у любой реализации `Temporal`, например `LocalDate` / `ZonedDateTime`.

```kotlin
@DoNotParallelize
internal class KotestNowTest : StringSpec() {

    override fun extensions() = listOf(
        /* 1 */ ConstantNowTestListener<LocalDate>(LocalDate.EPOCH),
        /* 2 */ ConstantNowTestListener<LocalTime>(LocalTime.NOON)
    )

    init {
        "Scenario: date and time will be mocked, but dateTime not" {
            /* 3 */
            LocalDate.now() shouldBe LocalDate.EPOCH
            LocalTime.now() shouldBe LocalTime.NOON

            /* 4 */
            val localDateTimeNow = LocalDateTime.now()
            delay(100)
            LocalDateTime.now() shouldBeAfter localDateTimeNow
        }
    }
}
```

- `1` Заменяем `now()` для класса `LocalDate` - будет возвращать `LocalDate.EPOCH` (`01.01.1970`)
- `2` Заменяем `now()` для класса `LocalTime` - будет возвращать `LocalTime.NOON` (`12:00`)
- `3` Проверяем, что теперь `now()` действительно возвращает статичное значение
- `4` Но класс `LocalDateTime` все еще работает по-старому

## Интеграция со Spring

Вот мы и добрались до кульминации рассказа. На самом деле все это затевалось, чтобы показать как `Kotest` работает со `Spring Test` и `Spring Boot Test` в частности.
Что использовать `Kotest` для написания `unit` / `интеграционных` / `e2e` / `функциональных` тестов для вашего `Spring Boot` приложения не сложнее, чем `Junit`.
А в части читаемости и поддерживаемости функциональных тестов даже предпочтительнее, по мнению автора.

#### Добавляем необходимые зависимости в наш `Gradle` проект

Начинаем с плагинов:

```groovy
plugins {
    id 'org.jetbrains.kotlin.jvm' version '1.7.10'
    id 'org.jetbrains.kotlin.plugin.spring' version '1.7.10'
    id 'org.springframework.boot' version '2.7.2'
}
```

Плагин для поддержки `kotlin`, плагин для корректной работы Spring AOP с неизменяемыми по умолчанию классами котлина `kotlin.plugin.spring` и `spring boot` плагин.

Далее активируем удобный `spring.dependency-management` из состава `spring boot` плагина, чтобы использовать заранее подготовленный `BOM` с версиями большинства библиотек
и не заботится о совместимости.
После подключения нет необходимости указывать версии библиотек в блоке `dependencies`.
Версии можно проверить на сайте [docs.spring.io](https://docs.spring.io/spring-boot/docs/current/reference/html/dependency-versions.html)

```groovy
apply plugin: 'io.spring.dependency-management'
```

Подключаем зависимости:

```groovy
dependencies {
    // Для примера приложения
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'com.fasterxml.jackson.module:jackson-module-kotlin'

    // Sprint Boot Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    // HTTP клиент для e2e тестирования
    testImplementation 'io.rest-assured:rest-assured'

    // JUnit5
    testImplementation('org.junit.jupiter:junit-jupiter')
    testRuntimeOnly('org.junit.jupiter:junit-jupiter-engine')
    testImplementation('org.junit.jupiter:junit-jupiter-params')

    // Kotest
    testImplementation platform('io.kotest:kotest-bom:5.4.1')
    testImplementation 'io.kotest:kotest-runner-junit5'
    // Spring + Kotest
    testImplementation('io.kotest.extensions:kotest-extensions-spring:1.1.2') { exclude group: 'io.kotest' }
}
```

> Обращаю внимание на несколько вещей:
>
> - без `jackson-module-kotlin` не будет работать десериализация в `data` классы `Kotlin`. Часто забывается при создании нового проекта.
> - движок для тестирования `junit-jupiter-engine` не нужен на этапе компиляции, поэтому `testRuntimeOnly`
> - `testImplementation platform('io.kotest:kotest-bom:5.4.1')` kotest нет среди библиотек `Spring Boot`, поэтому подключаем `BOM`
> - `kotest-extensions-spring` это целевое расширение для интеграции `Kotest` и `Spring`, находится в отдельном проекте и ведет свое версионирование

Включаем `junit5` - он в любом случае нужен как для запуска собственных тестов, так и для запуска тестов `kotest`

```groovy
test {
    useJUnitPlatform()
    systemProperty "kotest.framework.dump.config", "true" // Хотим напечатать конфигурацию запуска для Kotest
}
```

#### Готовим конфигурацию проекта

Добавляем объект `SpringExtension` для всего проекта.

```kotlin
object KotestProjectConfig : AbstractProjectConfig() {

    override fun extensions() = listOf(SpringExtension)
}
```

То есть контекст будет подниматься и переиспользоваться для любой `Kotest` спецификации.
Если хочется включить `Spring` только для некоторых классов, то добавляем расширения на уровне спецификации:

```kotlin
class MyTestSpec : FunSpec() {
    override fun extensions() = listOf(SpringExtension)
}
```

Так или иначе после включения расширения создается спринговый `TestContextManager`, которому делегируется инициализация контекста и класса спецификации.
По умолчанию подключается возможность внедрять все аргументы из конструктора теста, даже без дополнительных аннотаций типа `@Autowired`.
Эта возможность доступна через `Kotest` перехватчик `ConstructorExtension` - `Spring` расширение реализует его и берет на себя создание объекта класса спецификации.

```kotlin
internal class MyTestSpec(propertyResolverInConstructor: PropertyResolver) : FunSpec()
```

Тут аргумент `propertyResolverInConstructor`внедряется без дополнительных телодвижений, как полагается, через конструктор, а не через `setter`!

#### Что будем тестировать?

Тестируем `Spring` контроллер с одним методом `POST`, который проверяет текст в запросе.

Входящий запрос:

```kotlin
data class RequestDto(
    val text: String?
)
```

Ответ:

```kotlin
data class ResponseDto(
    val code: Int,
    val message: String
)
```

И сам контроллер:

```kotlin
@RestController
class ValidationController {

    @PostMapping("/validation", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun sampleValidateEndpoint(@RequestBody request: RequestDto): ResponseDto =
        when {
            request.text == null -> ResponseDto(1, "Null text") /* 1 */
            request.text.isBlank() -> ResponseDto(2, "Blank text") /* 2 */
            else -> ResponseDto(0, "Ok") /* 3 */
        }
}
```

- `1` Если в запросе текст отсутствует, то отвечаем `{ "code": 1, "message": "Null text" }`
- `2` Если в запросе текст пустой или из пробелов, то отвечаем `{ "code": 2, "message": "Blank text" }`
- `3` Если в текст есть, то `{ "code": 0, "message": "Ok" }`

#### Создаем E2E сценарий

В тесте мы поднимаем полноценный сервер на случайном порту, подключаемся к нему `HTTP` клиентом, отправляем реальные запросы и проверяем реальные ответы, — поэтому **End 2 End**.
Будем писать, как полагается, Data Driven Test на 3 набора входных данных в BDD стиле.
Теоретически такой тест может быть использован в BDD подходе к разработке. И написание этого теста не требует наличия рабочей функциональности - только четкие требования.

Для сценария `Kotest` предоставляет множество стилей - я всегда выбираю `FreeSpec`.
Во всех предыдущих частях использовал либо `StringSpec` для плоских тестов без вложенности, либо `FreeSpec` для тестов с вложенностью и логическими блоками.

```kotlin
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/* 1 */
internal class ValidationControllerKotestTest(
    @Value("\${local.server.port}") private val localServerPort: String /* 2 */
) : FreeSpec() 
```

- `1` Подключаем `SpringBootTest`, то есть говорим тесту, что нужно поднимать весь контекст и выполнять все настроенные стартеры, но на случайном доступном порту.
- `2` Для теста нужно знать номер случайного порта, на котором поднимается приложение, получаем его в конструкторе.

Я хочу показать, что в конструкторе можно внедрить любой `Bean` из контекста без использования аннотаций:

```kotlin
internal class ValidationControllerKotestTest(
  @Value("\${local.server.port}") private val localServerPort: String,
  propertyResolverInConstructor: PropertyResolver /* 1 */
) : FreeSpec() 
```

- `1` Допустим я предпочитаю получать `property` через методы `PropertyResolver` - внедря этот `Bean` и в тесте чуть позже обязательно его проверю!

> 👉 На проекте в `Mir.Platform` у нас разрешены внедрения только через конструктор, что проверяется на уровне `code review`

В блоке `init` оформляю структуру сценария прямо как в `Cucumber` без реализации, которую уже можно запустить и отдать на ревью коллегам:

```kotlin
init {
    table(
        headers("Text field", "Expected Code", "Expected Message"), /* 1 */
        row("Hello", 0, "Ok"), /* 2 */
        row(null, 1, "Null text"),
        row("    ", 2, "Blank text")
    ).forAll { text, expectedCode, expectedMessage ->

        "Scenario: Validation for text '$text'" - { /* 3 */

            "Given spring context injected successfully" { } /* 4 */

            "Given POST request prepared with text '$text'" { } /* 5 */

            "When request sent" { }

            "Then response with body code $expectedCode and body message '$expectedMessage'" { }
        }
    }
}
```

- `1` Наборы тестовых данных я буду создавать в виде таблицы. Заголовки нужны для читабельности кода сценария.
- `2` Три набора данных - три запуска теста.
- `3` Блок сценария, который будет выполнен 3 раза и в отчете будет отображаться как 3 отдельных сценария.
- `4` Проверю корректность создания контекста и работу `PropertyResolver`
- `5` Подготовка запроса > отправка запроса > проверка ответа

Добавляем реализацию шагов:

```kotlin
"Scenario: Validation for text '$text'" - {

    "Given spring context injected successfully" {
        val appContextExample = testContextManager().testContext.applicationContext /* 1 */

        appContextExample.environment.getProperty("local.server.port") shouldBe localServerPort /* 2 */
        propertyResolverInConstructor.getProperty("local.server.port") shouldBe localServerPort /* 3 */
    }

    "Given POST request prepared with text '$text'" {
        body = RestAssured /* 4 */
            .with()
            .log()
            .all()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .body(RequestDto(text))
    }

    "When request sent" {
        post = body.post("http://localhost:$localServerPort/validation") /* 5 */
    }

    "Then response with body code $expectedCode and body message '$expectedMessage'" {
        post
            .then()
            .log()
            .all()
            .statusCode(200) /* 6 */
            .body("code", Matchers.equalTo(expectedCode))
            .body("message", Matchers.equalTo(expectedMessage))
    }
}
```

- `1` `testContextManager()` метод доступный в контексте теста и возвращающий текущий Spring `ContextManager`, а в нем информация о тестовом классе и `applicationContext`
- `2` Убеждаемся, что это именно тот контекст, который внедрил нам аргументы конструктора тест-класса.
- `3` Как я упоминал выше проверяем `propertyResolver` внедренный в конструкторе.
- `4` Готовим запрос с помощью `RestAssured`
- `5` Отправляем запрос
- `6` Проверяем ответ

Отлично тест готов!

Если запустить его в Idea, то вывод хода выполнения будет таким:

![](https://habrastorage.org/webt/vq/a_/is/vqa_isvvard51rqsiuxvbjauk3w.png)

Контекст стартует один раз на все тестовые классы и переиспользуется, если нет аннотации `@DirtiesContext`. У меня старт данного приложения занимает примерно **2.297** секунды

## Сравним с Junit5

В одном проекте и даже в одной `source-папке` успешно уживаются разные тестовые движки, интегрируемые через `JunitPlatform`.
Старый добрый `Junit5` успешно справляется с _E2E DataDriven_ тестом нашего `Spring Boot` приложения.

```kotlin
@ExtendWith(SpringExtension::class)
/* 1 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
/* 2 */
internal class ValidationControllerJunitTest {

    @Value("\${local.server.port}")
    private val localServerPort = 0 /* 3 */

    @ParameterizedTest(name = "Validation for {0}")
    /* 4 */
    @CsvSource( /* 5 */
        textBlock = """
      Text field | Expected Code | Expected Message 
      Hello      | 0             | Ok
      null       | 1             | Null text
      '   '      | 2             | Blank text""",

        delimiter = '|', useHeadersInDisplayName = true, nullValues = ["null"]
    )
    fun testSampleGetEndpointTextNull(text: String?, expectedCode: Int, expectedMessage: String) {
        RestAssured /* 6 */
            .with()
            .log()
            .all()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .accept(MediaType.APPLICATION_JSON_VALUE)
            .body(RequestDto(text))
            .post("http://localhost:$localServerPort/validation")
            .then()
            .log()
            .all()
            .statusCode(200)
            .body("code", Matchers.equalTo(expectedCode))
            .body("message", Matchers.equalTo(expectedMessage))
    }
}
```

- `1` Подключаем расширение `SpringTest` к тесту. В `Kotest` мы сделали это на уровне проекта, но можно и к конкретному тесту.
- `2` Стартуем приложение на случайном порту
- `3` Внедряем значение порта в поле. Но через сеттер! В конструкторе `Junit` теста внедрить не получится!

> ❔ А как это работает с `val`?   
> Помогает gradle-плагин `kotlin.plugin.spring` и делает это поле не финальным.

- `4` Естественно тест будет параметризованным
- `5` А `@CsvSource` позволяет нарисовать такую DDT табличку аналогично, той, что мы делали методами в `Kotest` с тремя наборами
- `6` И с помощью удобного DSL RestAssured отправляем запрос и проверяем ответ.

А вот какой вывод имеем в окне с ходом выполнения теста:

![](https://habrastorage.org/webt/kw/e0/yf/kwe0yfwqjjrzmkmu0hp_sjzug-w.png)

Тест `JUnit5` выглядит компактнее, так почему же мы используем `Kotest` для **функционального** тестирования.   
Подчеркну, что именно для него - в модульных тестах успешно работает `JUnit5`.  
А в функциональных тестах нам важна очень четкая и гибкая структура сценария и тестовых данных, разбитая на логические блоки - `Kotest` подходит идеально в нашем тех. стеке.

> _И еще интересное наблюдение:_  `Spring Context`, который поднялся для `Kotest` переиспользовался и для `Junit5`!

## Остается отчетность
Естественно для успешного анализа результатов сложной логики функциональных тестов необходима отчетность. 
И чтобы вся структура теста и тестовых данных корректно легла в эту отчетность...

Добавляем `Allure` отчеты в тесты и заодно в `RestAssured`, чтобы подхватить отправленные запросы и принятые ответы. 

Само собой, для добавления используем расширение.
Документация рекомендует `io.kotest.extensions:kotest-extensions-allure`, но оно нам к сожалению не подходит по нескольким причинам:
- с июля 2022 года по как минимум конец августа 2022 года **не** совместимо с `Kotest > 5.4.0`, то есть не работает и не компилируется 
- некорректно отображает вложенную структуру теста для `Data Driven` сценариев
- не поддерживает все аннотации `Allure`

Но есть решение! 
Альтернативное расширение **[`ru.iopump.kotest:kotest-allure`](https://github.com/kochetkov-ma/kotest-allure)** - оно работает и все корректно отображает, а также отмечено звездочкой от создателя `Kotest`, поэтому можно доверять.

Подключаем расширение в `Gradle`, а заодно и добавим и для `RestAssured`
```groovy
dependencies {
  //Allure
  testImplementation 'io.qameta.allure:allure-rest-assured:2.18.1'
  testImplementation 'ru.iopump.kotest:kotest-allure:5.4.1'
}
```

Далее в коде... А в коде для `Kotest` добавлять не нужно, так как расширение помечено аннотацией `@AutoScan` и подключается автоматически.

Тогда для `RestAssured` - сделаем это на уровне проекта:
```kotlin
object KotestProjectConfig : AbstractProjectConfig() {

    override suspend fun beforeProject() = RestAssured.filters(AllureRestAssured())
}
```

И набросаем минимальный набор аннотаций, который мы обязательно добавляем на каждый тест-класс в `Mir.Platform`
```kotlin
@Epic("Habr") /* 1 */
@Feature("Kotest") /* 2 */
@Story("Validation") /* 3 */
@Link(name = "Requirements", url = "https://habr.com/ru/company/nspk/blog/") /* 4 */
@KJira("KT-1") /* 5 */
@KDescription(
  """
Kotest integration with Spring Boot.
Also using Allure Listener for test reporting.
    """
) /* 6 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class ValidationControllerKotestTest
```
- `1` Верхний уровень группировки тестов
- `2` Группировка по функциональности
- `3` Группировка по конкретной истории
- `4` Ссылка на требования
- `5` Ссылка на номер задачи
- `6` Многострочное человеко читаемое описание, поясняющее особенности подхода в тесте / логику / проверки

Чтобы ссылки на задачи корректно работали, необходимо оформить шаблоны для ссылок в `allure.properties` и положит в папку `resources/`.
Вот пример, который будет подставлять вместо `{}` номер задачи и вести в `youtrack.jetbrains.com`:
```properties
allure.link.jira.pattern=https://youtrack.jetbrains.com/issue/{}
```
Сюда можно добавлять свои шаблоны, давать им имена и создавать собственные аннотации для определенного вида ссылок.

Для добавления задач генерации отчета и включения перехвата Allure аннотаций через `AspectJ` желательно подключить Gradle-плагин:
```groovy
plugins {
    id "io.qameta.allure" version "2.10.0"
}
```

У нас используется несколько Фреймворков для тестирования, а также `Kotest` **пока** не поддерживается Allure плагином, то полностью автоматическая конфигурация нам не подойдет:
```groovy
allureReport {
  clean = true /* 1 */
}

allure {
  adapter {
    autoconfigureListeners = false /* 2 */
    version = '2.18.1' /* 3 */
    frameworks {
      junit5 {
        enabled = false /* 4 */
      }
    }
  }
}
```
- `1` Для задачи `allureReport` включаем автоматическую очистку перед генерацией - иначе он выбрасывает исключение, если отчет уже сгенерирован
- `2` Отключаем автоматическую конфигурацию расширений под Фреймворки
- `3` Указываем версию библиотек Allure для генератора
- `4` Отключаем расширение для `junit5` иначе будет дублирование записей, так как `Kotest` воспринимается плагином как `Junit5`

Запускаем тесты и генерацию Allure-отчета:
```
gradle test allureReport
```

И остается разобрать сгенерированный Allure отчет, хотя там все довольно очевидно
![](https://habrastorage.org/webt/tb/mg/jm/tbmgjmhtsvbwppcqqiavzlb5oxi.png)

Заключение
------
По традиции, ссылка на все примеры [qa-kotest-articles/kotest-third](https://github.com/kochetkov-ma/pump-samples/tree/master/qa-kotest-articles/kotest-third).

Наконец-таки закончился цикл статей про `Kotest` - мы разобрали все основные аспекты данного фреймворка и было продемонстрировано успешное применение для тестирования `Spring` приложения на любом уровне.  

С помощью системы расширений можно добавить любую, не упомянутую мной функциональность в `Kotest`:
- управлять контейнерами с помощью `kotest-extensions-testcontainers`
- контролировать **HTTP** заглушки в `kotest-extensions-wiremock`
- работать с `Kafka` через `kotest-extensions-embedded-kafka`

Ресурсы
------
⚡ [Блог Mir.Platform](https://habr.com/ru/company/nspk/blog/)

⚡ [Kotlin. Автоматизация тестирования (часть 1). Kotest: Начало](https://habr.com/ru/post/520380/)

⚡ [Kotlin. Автоматизация тестирования (Часть 2). Kotest. Deep Diving](https://habr.com/ru/company/nspk/blog/542754/)

⚡ [Примеры кода](https://github.com/kochetkov-ma/pump-samples/tree/master/qa-kotest-articles/kotest-third/src)

⚡ [Kotest GitHub](https://github.com/kotest/kotest)

⚡ [Kotest Spring GitHub](https://github.com/kotest/kotest-extensions-spring)

⚡ [Kotest Allure GitHub](https://github.com/kochetkov-ma/kotest-allure)

📔 [Официальная документация Kotest](https://kotest.io/docs/quickstart)

📔 [Kotlin Lang](https://kotlinlang.org/docs/home.html)

📔 [JUnit5](https://junit.org/junit5/docs/current/user-guide/)

📔 [Gradle Testing](https://docs.gradle.org/current/userguide/java_testing.html)