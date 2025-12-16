package no.nav.helse.bakrommet.behandling.validering

import org.junit.jupiter.api.Test
import java.io.File
import java.lang.reflect.Modifier
import java.net.JarURLConnection
import java.net.URLDecoder
import kotlin.reflect.KClass
import kotlin.test.assertTrue
import kotlin.test.fail

class ValideringSjekkTest {
    @Test
    fun `alle ValideringSjekk implementasjoner skal være med i alleSjekker lista`() {
        val alleKjenteImplementasjoner = findImplementations(ValideringSjekk::class.java, "no.nav.helse.bakrommet.behandling.validering.sjekker")

        // Hent alle sjekker som er registrert i alleSjekker
        val registrerteSjekker = alleSjekker.map { it::class }.toSet()

        // Sjekk at alle kjente implementasjoner er registrert
        val ikkeRegistrerte = alleKjenteImplementasjoner.filter { it !in registrerteSjekker }

        if (ikkeRegistrerte.isNotEmpty()) {
            fail(
                "Følgende ValideringSjekk implementasjoner mangler i alleSjekker:\n" +
                    ikkeRegistrerte.joinToString("\n") { "  - ${it.simpleName}" },
            )
        }

        // Sjekk at antallet stemmer
        assertTrue(
            alleKjenteImplementasjoner.size == alleSjekker.size,
            "Antall kjente implementasjoner (${alleKjenteImplementasjoner.size}) stemmer ikke med antall i alleSjekker (${alleSjekker.size}). " +
                "Enten mangler noen i alleSjekker, eller så finnes det duplikater/ekstra i alleSjekker.",
        )

        // Sjekk at alleSjekker ikke er tom
        assertTrue(
            registrerteSjekker.isNotEmpty(),
            "alleSjekker skal ikke være tom",
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> findImplementations(
        interfaceClass: Class<T>,
        packageName: String,
    ): List<KClass<out T>> {
        val classLoader = Thread.currentThread().contextClassLoader
        val path = packageName.replace('.', '/')
        val classes = mutableListOf<Class<*>>()

        // Prøv å lese fra filsystemet (fungerer i test-miljøet)
        val resources = classLoader.getResources(path)
        while (resources.hasMoreElements()) {
            val url = resources.nextElement()
            when {
                url.protocol == "file" -> {
                    val filePath = URLDecoder.decode(url.file, "UTF-8")
                    val dir = File(filePath)
                    if (dir.exists() && dir.isDirectory) {
                        dir
                            .walkTopDown()
                            .filter { it.isFile && it.name.endsWith(".class") }
                            .forEach { file ->
                                val relativePath = file.relativeTo(dir).path
                                val className =
                                    relativePath
                                        .removeSuffix(".class")
                                        .replace(File.separatorChar, '.')

                                val fqcn = "$packageName.$className"
                                runCatching {
                                    Class.forName(fqcn, false, classLoader)
                                }.getOrNull()?.let { classes.add(it) }
                            }
                    }
                }
                url.protocol == "jar" -> {
                    // For JAR-filer, må vi lese fra JAR-en
                    val entryPath = url.path.substringAfter("!/")

                    runCatching {
                        val connection = url.openConnection() as? JarURLConnection
                        connection?.jarFile?.use { jarFile ->
                            jarFile
                                .entries()
                                .asSequence()
                                .filter { entry -> entry.name.startsWith(entryPath) && entry.name.endsWith(".class") }
                                .forEach { entry ->
                                    val className =
                                        entry.name
                                            .removeSuffix(".class")
                                            .replace('/', '.')

                                    runCatching {
                                        Class.forName(className, false, classLoader)
                                    }.getOrNull()?.let { classes.add(it) }
                                }
                        }
                    }.getOrNull()
                }
            }
        }

        return classes
            .filter { interfaceClass.isAssignableFrom(it) }
            .filter { !it.isInterface }
            .filter { !Modifier.isAbstract(it.modifiers) }
            .map { it.kotlin as KClass<out T> }
    }
}
