package ru.iopump.kotest

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.string.shouldStartWith
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.MountableFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readText

@Suppress("BlockingMethodInNonBlockingContext")
@ExperimentalPathApi
open class DockerTest : FreeSpec() {

    private val govnoContainer: GenericContainer<*> =
        GenericContainer<Nothing>("nginx:alpine").apply {
            withCopyFileToContainer(MountableFile.forClasspathResource("govno.txt"), "/govno.txt")
        }


    init {
        beforeSpec { govnoContainer.start() }
        afterSpec { govnoContainer.close() }
        "Scenario: start container, copy file, execute command and otain new file" {
            val newGovnoPath = Paths.get("./tmp/new-govno.txt")
            Files.createDirectories(newGovnoPath.parent)
            Files.deleteIfExists(newGovnoPath)
            Files.createFile(newGovnoPath)

            govnoContainer.execInContainer("ls", "-l").also(::println)
            govnoContainer.execInContainer("sh", "-c", "echo ' Sam ti govno' >> /govno.txt").also(::println)
            govnoContainer.execInContainer("cat", "/govno.txt").also(::println)
            govnoContainer.copyFileFromContainer("/govno.txt", newGovnoPath.toAbsolutePath().toString())
            newGovnoPath.readText(StandardCharsets.UTF_8) shouldStartWith "HI Govno. Sam ti govno"
        }
    }
}