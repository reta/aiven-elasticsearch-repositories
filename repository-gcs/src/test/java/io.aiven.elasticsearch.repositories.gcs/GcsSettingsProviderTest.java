/*
 * Copyright 2020 Aiven Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.aiven.elasticsearch.repositories.gcs;

import java.io.IOException;
import java.nio.file.Files;

import io.aiven.elasticsearch.repositories.CommonSettings;
import io.aiven.elasticsearch.repositories.DummySecureSettings;
import io.aiven.elasticsearch.repositories.RepositoryStorageIOProvider;
import io.aiven.elasticsearch.repositories.RsaKeyAwareTest;
import io.aiven.elasticsearch.repositories.security.EncryptionKeyProvider;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.storage.Storage;
import org.elasticsearch.common.settings.Settings;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GcsSettingsProviderTest extends RsaKeyAwareTest {

    @Test
    void providerInitialization() throws Exception {
        final var gcsSettingsProvider = new GcsSettingsProvider();
        final var settings = Settings.builder()
                .put(CommonSettings.RepositorySettings.BASE_PATH.getKey(), "base_path/")
                .put(GcsStorageSettings.CONNECTION_TIMEOUT.getKey(), 1)
                .put(GcsStorageSettings.READ_TIMEOUT.getKey(), 2)
                .put(GcsStorageSettings.PROJECT_ID.getKey(), "some_project")
                .setSecureSettings(createFullSecureSettings()).build();
        gcsSettingsProvider.reload(GcsRepositoryPlugin.REPOSITORY_TYPE, settings);

        final var repoIOProvider = gcsSettingsProvider.repositoryStorageIOProvider();
        assertNotNull(repoIOProvider);


        final var client = extractClient(repoIOProvider);

        assertTrue(client.getOptions().getTransportOptions() instanceof HttpTransportOptions);

        final var httpTransportOptions = (HttpTransportOptions) client.getOptions().getTransportOptions();
        assertEquals(1, httpTransportOptions.getConnectTimeout());
        assertEquals(2, httpTransportOptions.getReadTimeout());
        assertEquals(GcsSettingsProvider.HTTP_USER_AGENT, client.getOptions().getUserAgent());
        assertEquals("some_project", client.getOptions().getProjectId());

        assertEquals(loadCredentials(), client.getOptions().getCredentials());
    }

    @Test
    void providerInitializationWithDefaultValues() throws Exception {
        final var gcsSettingsProvider = new GcsSettingsProvider();
        final var settings = Settings.builder()
                .setSecureSettings(createFullSecureSettings()).build();
        gcsSettingsProvider.reload(GcsRepositoryPlugin.REPOSITORY_TYPE, settings);

        final var client = extractClient(gcsSettingsProvider.repositoryStorageIOProvider());
        assertNotNull(client);
        assertTrue(client.getOptions().getTransportOptions() instanceof HttpTransportOptions);

        final var httpTransportOptions = (HttpTransportOptions) client.getOptions().getTransportOptions();
        assertEquals(-1, httpTransportOptions.getConnectTimeout());
        assertEquals(-1, httpTransportOptions.getReadTimeout());
        assertEquals(GcsSettingsProvider.HTTP_USER_AGENT, client.getOptions().getUserAgent());
        //skip project id since GCS client returns default one

        assertEquals(loadCredentials(), client.getOptions().getCredentials());

    }

    @Test
    void providerReloadClient() throws Exception {
        final var gcsSettingsProvider = new GcsSettingsProvider();
        final var settings = Settings.builder()
                .setSecureSettings(createFullSecureSettings()).build();
        gcsSettingsProvider.reload(GcsRepositoryPlugin.REPOSITORY_TYPE, settings);

        var client = extractClient(gcsSettingsProvider.repositoryStorageIOProvider());
        assertNotNull(client);
        assertTrue(client.getOptions().getTransportOptions() instanceof HttpTransportOptions);

        var httpTransportOptions = (HttpTransportOptions) client.getOptions().getTransportOptions();
        assertEquals(-1, httpTransportOptions.getConnectTimeout());
        assertEquals(-1, httpTransportOptions.getReadTimeout());
        assertEquals(GcsSettingsProvider.HTTP_USER_AGENT, client.getOptions().getUserAgent());
        //skip project id since GCS client returns default one

        assertEquals(loadCredentials(), client.getOptions().getCredentials());

        final var newSettings = Settings.builder()
                .put(GcsStorageSettings.CONNECTION_TIMEOUT.getKey(), 10)
                .put(GcsStorageSettings.READ_TIMEOUT.getKey(), 20)
                .put(GcsStorageSettings.PROJECT_ID.getKey(), "super_project")
                .setSecureSettings(createFullSecureSettings())
                .build();
        gcsSettingsProvider.reload(GcsRepositoryPlugin.REPOSITORY_TYPE, newSettings);

        client = extractClient(gcsSettingsProvider.repositoryStorageIOProvider());
        assertNotNull(client);
        assertTrue(client.getOptions().getTransportOptions() instanceof HttpTransportOptions);

        httpTransportOptions = (HttpTransportOptions) client.getOptions().getTransportOptions();
        assertEquals(10, httpTransportOptions.getConnectTimeout());
        assertEquals(20, httpTransportOptions.getReadTimeout());
        assertEquals(GcsSettingsProvider.HTTP_USER_AGENT, client.getOptions().getUserAgent());
        assertEquals("super_project", client.getOptions().getProjectId());

        assertEquals(loadCredentials(), client.getOptions().getCredentials());
    }

    @Test
    void throwsIllegalArgumentExceptionForEmptySettings() throws IOException {
        final var gcsSettingsProvider = new GcsSettingsProvider();

        gcsSettingsProvider.reload(GcsRepositoryPlugin.REPOSITORY_TYPE, Settings.EMPTY);
        var e = assertThrows(IOException.class, gcsSettingsProvider::repositoryStorageIOProvider);
        assertEquals(
                "Cloud storage client haven't been configured",
                e.getMessage());

        final var anySettings = Settings.builder().put("foo", "bar").build();
        final var publicRsaKeyOnlySettings =
                Settings.builder()
                        .setSecureSettings(createPublicRsaKeyOnlySecureSettings())
                        .build();
        final var privateRsaKeyOnlySettings =
                Settings.builder()
                        .setSecureSettings(createPrivateRsaKeyOnlySecureSettings())
                        .build();

        e = assertThrows(IOException.class, () ->
                gcsSettingsProvider.reload(GcsRepositoryPlugin.REPOSITORY_TYPE, anySettings));
        assertEquals(
                "Settings with name aiven.public_key_file hasn't been set",
                e.getMessage());

        e = assertThrows(IOException.class, () ->
                gcsSettingsProvider.reload(GcsRepositoryPlugin.REPOSITORY_TYPE, privateRsaKeyOnlySettings));
        assertEquals(
                "Settings with name aiven.public_key_file hasn't been set",
                e.getMessage());

        e = assertThrows(IOException.class, () ->
                gcsSettingsProvider.reload(GcsRepositoryPlugin.REPOSITORY_TYPE, publicRsaKeyOnlySettings));
        assertEquals(
                "Settings with name aiven.private_key_file hasn't been set",
                e.getMessage());

    }

    private Storage extractClient(final RepositoryStorageIOProvider<Storage> storageIOProvider) throws Exception {
        final var field = ReflectionSupport.findFields(RepositoryStorageIOProvider.class, f -> f
                .getName().equals("client"),
                HierarchyTraversalMode.TOP_DOWN).get(0);
        field.setAccessible(true);
        return (Storage) field.get(storageIOProvider);
    }

    private DummySecureSettings createFullSecureSettings() throws IOException {
        return new DummySecureSettings()
                .setFile(
                        GcsStorageSettings.CREDENTIALS_FILE_SETTING.getKey(),
                        getClass().getClassLoader().getResourceAsStream("test_gcs_creds.json"))
                .setFile(EncryptionKeyProvider.PRIVATE_KEY_FILE.getKey(), Files.newInputStream(privateKeyPem))
                .setFile(EncryptionKeyProvider.PUBLIC_KEY_FILE.getKey(), Files.newInputStream(publicKeyPem));
    }

    private DummySecureSettings createPublicRsaKeyOnlySecureSettings() throws IOException {
        return new DummySecureSettings()
                .setFile(
                        GcsStorageSettings.CREDENTIALS_FILE_SETTING.getKey(),
                        getClass().getClassLoader().getResourceAsStream("test_gcs_creds.json"))
                .setFile(EncryptionKeyProvider.PUBLIC_KEY_FILE.getKey(), Files.newInputStream(publicKeyPem));
    }

    private DummySecureSettings createPrivateRsaKeyOnlySecureSettings() throws IOException {
        return new DummySecureSettings()
                .setFile(
                        GcsStorageSettings.CREDENTIALS_FILE_SETTING.getKey(),
                        getClass().getClassLoader().getResourceAsStream("test_gcs_creds.json"))
                .setFile(EncryptionKeyProvider.PRIVATE_KEY_FILE.getKey(), Files.newInputStream(privateKeyPem));
    }

    GoogleCredentials loadCredentials() throws IOException {
        try (final var in = getClass().getClassLoader().getResourceAsStream("test_gcs_creds.json")) {
            return UserCredentials.fromStream(in);
        }
    }

}
