package com.jetbrains.rider.plugins.ulearnintegration.infrastructure

import com.jetbrains.rider.plugins.ulearnintegration.domain.Exercise
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.zip.ZipInputStream

/** Скачивает zip, распаковывает, пишет .ulearn. Блокирующий: только из фона. */
object ProjectDownloader {

    private val http = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    fun download(exercise: Exercise, zipUrl: String, targetDir: Path) {
        Files.createDirectories(targetDir)
        val tmp = Files.createTempFile("ulearn-", ".zip")
        try {
            val request = HttpRequest.newBuilder(URI.create(zipUrl))
                .header("User-Agent", "Mozilla/5.0")
                .timeout(Duration.ofMinutes(2))
                .GET().build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofFile(tmp))
            if (response.statusCode() !in 200..299) {
                throw IOException("Не удалось скачать проект: код ${response.statusCode()}")
            }
            unzip(tmp, targetDir)
            UlearnProjectFile.write(targetDir, exercise)
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    private fun unzip(zip: Path, targetDir: Path) {
        val root = targetDir.toAbsolutePath().normalize()
        ZipInputStream(Files.newInputStream(zip)).use { zis ->
            generateSequence { zis.nextEntry }.forEach { entry ->
                val out = root.resolve(entry.name).normalize()
                // Запись не должна выходить за пределы папки
                if (!out.startsWith(root)) throw IOException("Недопустимый путь в архиве: ${entry.name}")
                if (entry.isDirectory) {
                    Files.createDirectories(out)
                } else {
                    Files.createDirectories(out.parent)
                    Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}