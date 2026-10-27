package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.response.CommonResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class I18nControllerTest
{
    @TempDir
    private Path temporary;

    @Test
    public void testBundledPacksWithoutConfigurationLocation()
    {
        I18nController controller = controller(new MockEnvironment());
        assertEquals("Home", pack(controller, "en").getProperty("common.home"));
        assertEquals("首页", pack(controller, "zh_cn").getProperty("common.home"));
        assertEquals("首页", pack(controller, "zh-CN").getProperty("common.home"));
    }

    @Test
    public void testCommandLineDirectoryOverridesBundledPack()
            throws Exception
    {
        writePack(temporary, "Custom home");
        I18nController controller = controller(new MockEnvironment(), "--spring.config.location=" + temporary.toUri());
        Properties pack = pack(controller, "en");
        assertEquals("Custom home", pack.getProperty("common.home"));
        assertEquals("Query", pack.getProperty("common.query"));
    }

    @Test
    public void testCommandLinePropertiesFileAndOptionalPrefix()
            throws Exception
    {
        writePack(temporary, "File configuration");
        Path config = temporary.resolve("application.properties");
        Files.writeString(config, "", StandardCharsets.UTF_8);
        I18nController controller = controller(new MockEnvironment(), "--spring.config.location=optional:" + config.toUri());
        assertEquals("File configuration", pack(controller, "en").getProperty("common.home"));
    }

    @Test
    public void testEnvironmentConfigurationDirectory()
            throws Exception
    {
        writePack(temporary, "Environment configuration");
        MockEnvironment environment = new MockEnvironment().withProperty("spring.config.location", temporary.toString());
        assertEquals("Environment configuration", pack(controller(environment), "en").getProperty("common.home"));
    }

    @Test
    public void testAdditionalLocationsAndLastLocationTakePrecedence()
            throws Exception
    {
        Path base = temporary.resolve("base");
        Path extra = temporary.resolve("extra");
        Path last = temporary.resolve("last");
        writePack(base, "Base");
        writePack(extra, "Extra");
        writePack(last, "Last");
        I18nController controller = controller(new MockEnvironment(),
                "--spring.config.location=" + base.toUri(),
                "--spring.config.additional-location=" + extra.toUri() + "," + last.toUri());
        assertEquals("Last", pack(controller, "en").getProperty("common.home"));
    }

    @Test
    public void testMissingExternalFileFallsBackToBundledPack()
    {
        I18nController controller = controller(new MockEnvironment(), "--spring.config.location=" + temporary.toUri());
        assertEquals("Home", pack(controller, "en").getProperty("common.home"));
    }

    @Test
    public void testUnsupportedLocaleReturnsFailure()
    {
        assertFalse(controller(new MockEnvironment()).getLanguagePack("missing").getStatus());
    }

    private I18nController controller(MockEnvironment environment, String... arguments)
    {
        return new I18nController(environment, new DefaultApplicationArguments(arguments), new DefaultResourceLoader());
    }

    private Properties pack(I18nController controller, String locale)
    {
        CommonResponse<Properties> response = controller.getLanguagePack(locale);
        assertTrue(response.getStatus(), String.valueOf(response.getMessage()));
        assertTrue(response.getData().size() > 100);
        return response.getData();
    }

    private void writePack(Path directory, String home)
            throws Exception
    {
        Path translations = Files.createDirectories(directory.resolve("i18n"));
        Files.writeString(translations.resolve("messages_en.properties"), "common.home=" + home + "\n", StandardCharsets.UTF_8);
    }
}
