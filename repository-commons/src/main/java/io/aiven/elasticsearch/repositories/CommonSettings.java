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

import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.repositories.RepositoryException;

public interface CommonSettings {

    interface KeystoreSettings {

        String AIVEN_PREFIX = "aiven";

        static String withPrefix(final String key) {
            return String.format("%s.%s", AIVEN_PREFIX, key);
        }

    }

    interface RepositorySettings {

        Setting<String> BASE_PATH =
                Setting.simpleString(
                        "base_path",
                        Setting.Property.NodeScope,
                        Setting.Property.Dynamic
                );

        Setting<ByteSizeValue> CHUNK_SIZE =
                Setting.byteSizeSetting(
                        "chunk_size",
                        new ByteSizeValue(100, ByteSizeUnit.MB),
                        new ByteSizeValue(1, ByteSizeUnit.BYTES),
                        new ByteSizeValue(100, ByteSizeUnit.MB),
                        Setting.Property.NodeScope
                );

        Setting<String> BUCKET_NAME =
                Setting.simpleString(
                        "bucket_name",
                        Setting.Property.NodeScope,
                        Setting.Property.Dynamic
                );

        default void checkSettings(final String repoType, final Setting<String> setting, final Settings settings) {
            if (!setting.exists(settings)) {
                throw new RepositoryException(repoType, setting.getKey() + " hasn't been defined");
            }
        }

    }

}
