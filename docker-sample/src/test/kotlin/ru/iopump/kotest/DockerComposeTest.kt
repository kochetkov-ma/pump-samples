package ru.iopump.kotest

import com.codeborne.selenide.*
import com.codeborne.selenide.CollectionCondition.size
import com.codeborne.selenide.Condition.exist
import io.kotest.core.spec.style.FreeSpec
import io.kotest.extensions.testcontainers.perSpec
import org.openqa.selenium.support.FindBy
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File

open class DockerComposeTest : FreeSpec() {

    /**
     * Read compose file from classpath resources
     */
    private val dockerComposeFile: File = File(DockerComposeTest::class.java.getResource("/docker-compose.yml").toURI())

    /**
     * Run via native 'docker-compose'.
     * You must install docker and docker-compose (for Windows contained in Docker Desktop, but not for Linux)
     */
    private val dockerComposeEnvironment: DockerComposeContainer<*> =
        DockerComposeContainer<Nothing>(dockerComposeFile).apply {
            System.getenv().put("PATH", "путь до bin")
            withExposedService("browser", 4444, Wait.forListeningPort())
            withExposedService("testing-application", 80, Wait.forListeningPort())
            withLocalCompose(true)
        }


    init {
        /**
         * Add TestContainer listener to manage container lifecycle.
         */
        listener(dockerComposeEnvironment.perSpec())

        "Browser with selenium server in container should started and web application 'getting-started' is accessible through this browser" - {

            lateinit var seleniumHost: String
            lateinit var seleniumPort: String

            "Given Selenium host and port for RemoteWebDriver" {
                seleniumHost = dockerComposeEnvironment.getServiceHost("browser", 4444) // Get container hostname.
                seleniumPort =
                    dockerComposeEnvironment.getServicePort("browser", 4444).toString() // Get container external port.
            }

            "Given RemoteWebDriver is created" {
                Configuration.remote = "http://$seleniumHost:$seleniumPort/wd/hub" // Make selenium server hub url
                Selenide.open() // Create web driver
            }

            "When main page of 'getting-started' app is opened" {
                Selenide.open("http://testing-application:80") // Using service name as host name inside the docker network
            }

            "Then docker logo exists and navigation items amount is 10" {
                val page = Selenide.page(GettingStartedPage::class.java) // Create and init page object
                page.dockerLogo.shouldBe(exist) // Check Docker Logo
                page.leftNavigationLinks.should(size(10)) // Check Navigation Bar Elements

                val itemsText = page.leftNavigationLinks.texts().joinToString("\n")
                println("\n\n\nNAVIGATION PANEL ITEMS: $itemsText \n\n\n")
            }
        }
    }
}

/**
 * Page Object
 */
open class GettingStartedPage {

    @FindBy(css = "a.md-header-nav__button")
    lateinit var dockerLogo: SelenideElement

    @FindBy(css = "div.md-sidebar--primary nav.md-nav--primary > ul > li")
    lateinit var leftNavigationLinks: ElementsCollection
}