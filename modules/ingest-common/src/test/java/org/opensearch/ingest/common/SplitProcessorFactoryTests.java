/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.ingest.common;

import org.opensearch.OpenSearchParseException;
import org.opensearch.test.OpenSearchTestCase;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;

public class SplitProcessorFactoryTests extends OpenSearchTestCase {

    public void testCreate() throws Exception {
        SplitProcessor.Factory factory = new SplitProcessor.Factory();
        Map<String, Object> config = new HashMap<>();
        config.put("field", "field1");
        config.put("separator", "\\.");
        String processorTag = randomAlphaOfLength(10);
        SplitProcessor splitProcessor = factory.create(null, processorTag, null, config);
        assertThat(splitProcessor.getTag(), equalTo(processorTag));
        assertThat(splitProcessor.getField(), equalTo("field1"));
        assertThat(splitProcessor.getSeparator(), equalTo("\\."));
        assertFalse(splitProcessor.isIgnoreMissing());
        assertThat(splitProcessor.getTargetField(), equalTo("field1"));
    }

    public void testCreateNoFieldPresent() throws Exception {
        SplitProcessor.Factory factory = new SplitProcessor.Factory();
        Map<String, Object> config = new HashMap<>();
        config.put("separator", "\\.");
        try {
            factory.create(null, null, null, config);
            fail("factory create should have failed");
        } catch(OpenSearchParseException e) {
            assertThat(e.getMessage(), equalTo("[field] required property is missing"));
        }
    }

    public void testCreateNoSeparatorPresent() throws Exception {
        SplitProcessor.Factory factory = new SplitProcessor.Factory();
        Map<String, Object> config = new HashMap<>();
        config.put("field", "field1");
        try {
            factory.create(null, null, null, config);
            fail("factory create should have failed");
        } catch(OpenSearchParseException e) {
            assertThat(e.getMessage(), equalTo("[separator] required property is missing"));
        }
    }

    public void testCreateWithTargetField() throws Exception {
        SplitProcessor.Factory factory = new SplitProcessor.Factory();
        Map<String, Object> config = new HashMap<>();
        config.put("field", "field1");
        config.put("separator", "\\.");
        config.put("target_field", "target");
        String processorTag = randomAlphaOfLength(10);
        SplitProcessor splitProcessor = factory.create(null, processorTag, null, config);
        assertThat(splitProcessor.getTag(), equalTo(processorTag));
        assertThat(splitProcessor.getField(), equalTo("field1"));
        assertThat(splitProcessor.getSeparator(), equalTo("\\."));
        assertFalse(splitProcessor.isIgnoreMissing());
        assertFalse(splitProcessor.isPreserveTrailing());
        assertThat(splitProcessor.getTargetField(), equalTo("target"));
    }

    public void testCreateWithPreserveTrailing() throws Exception {
        SplitProcessor.Factory factory = new SplitProcessor.Factory();
        Map<String, Object> config = new HashMap<>();
        config.put("field", "field1");
        config.put("separator", "\\.");
        config.put("target_field", "target");
        config.put("preserve_trailing", true);
        String processorTag = randomAlphaOfLength(10);
        SplitProcessor splitProcessor = factory.create(null, processorTag, null, config);
        assertThat(splitProcessor.getTag(), equalTo(processorTag));
        assertThat(splitProcessor.getField(), equalTo("field1"));
        assertThat(splitProcessor.getSeparator(), equalTo("\\."));
        assertFalse(splitProcessor.isIgnoreMissing());
        assertThat(splitProcessor.getTargetField(), equalTo("target"));
    }

}
