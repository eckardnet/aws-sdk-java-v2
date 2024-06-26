/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.thirdparty.org.slf4j.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class StaticMarkerBinderTest {
    @Test
    public void getSingleton_doesNoThrow() {
        assertThat(StaticMarkerBinder.getSingleton()).isNotNull();
    }

    @Test
    public void getActualBinder_isCorrectType() {
        assertThat(StaticMarkerBinder.getSingleton().getActualStaticMarkerBinder())
            .isInstanceOf(org.slf4j.impl.StaticMarkerBinder.class);
    }

    @Test
    public void getMarkerFactoryClassStr_returnsCorrectValue() {
        assertThat(StaticMarkerBinder.getSingleton().getMarkerFactoryClassStr())
            .isEqualTo("software.amazon.awssdk.thirdparty.org.slf4j.impl.internal.IMarkerFactoryAdapter");
    }
}
