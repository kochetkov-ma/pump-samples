Kotest + Spring + Allure Reports
=================================

It is subproject of [pump-samples](/) 

All subprojects define in [settings.gradle](./../settings.gradle)

Base gradle settings get from [root build.gradle](./../build.gradle)

Kotest + Spring + Allure Reports is extremely powerful approach to make pretty auto-tests in BDD style.
You may get acquainted with the Kotest framework in detail by the [official documentation](https://github.com/kotest/kotest/blob/master/doc/reference.md).
I want to pay you attention on using [Allure Report Listener](https://github.com/kochetkov-ma/kotest-allure).

1. See [quick-start](https://github.com/kochetkov-ma/kotest-allure#quick-start)
2. I recommend you consider using Spring Framework for Dependency Injection and Configuration
3. Adjust build script like this [gradle.build](build.gradle) - this a minimal suite for pretty tests with Kotest, Spring and Allure Reports
4. Create you beans like [AllureStep](src/test/kotlin/ru/iopump/kotest/AllureStep.kt)
5. Adjust project configuration singleton [ProjectConfig](src/test/kotlin/ru/iopump/kotest/ProjectConfig.kt)
6. Create you test in Free Style or another [ReportingFreeSpec](src/test/kotlin/ru/iopump/kotest/ReportingFreeSpec.kt)