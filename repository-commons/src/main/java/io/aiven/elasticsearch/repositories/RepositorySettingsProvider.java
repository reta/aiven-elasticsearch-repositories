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

package io.aiven.elasticsearch.repositories;

import java.io.IOException;
import java.util.Objects;

import io.aiven.elasticsearch.repositories.security.EncryptionKeyProvider;

import org.elasticsearch.common.settings.Settings;

public abstract class RepositorySettingsProvider<T> {

    private volatile RepositoryStorageIOProvider<T> repositoryStorageIOProvider;

    public synchronized RepositoryStorageIOProvider<T> repositoryStorageIOProvider() throws IOException {
        if (Objects.isNull(repositoryStorageIOProvider)) {
            throw new IOException("Cloud storage client haven't been configured");
        }
        return repositoryStorageIOProvider;
    }

    public synchronized void reload(final Settings settings) throws IOException {
        if (settings.isEmpty()) {
            return;
        }
        try {
            final var encryptionKeyProvider = EncryptionKeyProvider.of(settings);
            this.repositoryStorageIOProvider = createRepositoryStorageIOProvider(settings, encryptionKeyProvider);
        } catch (final Exception e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    protected abstract RepositoryStorageIOProvider<T> createRepositoryStorageIOProvider(
            final Settings settings, final EncryptionKeyProvider encryptionKeyProvider) throws IOException;

}