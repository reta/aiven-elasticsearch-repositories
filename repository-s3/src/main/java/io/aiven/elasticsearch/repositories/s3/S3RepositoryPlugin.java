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

package io.aiven.elasticsearch.repositories.s3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.opensearch.common.settings.Setting;
import org.opensearch.common.settings.Settings;

import io.aiven.elasticsearch.repositories.AbstractRepositoryPlugin;
import io.aiven.elasticsearch.repositories.Permissions;

import com.amazonaws.services.s3.AmazonS3Client;

public class S3RepositoryPlugin extends AbstractRepositoryPlugin<AmazonS3Client, S3ClientSettings> {

    public static final String REPOSITORY_TYPE = "aiven-s3";

    public S3RepositoryPlugin(final Settings settings) {
        super(REPOSITORY_TYPE, settings, new S3SettingsProvider());
    }

    @Override
    public List<Setting<?>> getSettings() {
        try {
            //due to the load of constants for AWS SDK use check permissions here
            return Permissions.doPrivileged(() ->
                    List.of(
                            S3ClientSettings.PUBLIC_KEY_FILE,
                            S3ClientSettings.PRIVATE_KEY_FILE,
                            S3ClientSettings.AWS_SECRET_ACCESS_KEY,
                            S3ClientSettings.AWS_ACCESS_KEY_ID,
                            S3ClientSettings.ENDPOINT,
                            S3ClientSettings.MAX_RETRIES,
                            S3ClientSettings.READ_TIMEOUT,
                            S3ClientSettings.USE_THROTTLE_RETRIES
                    ));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
