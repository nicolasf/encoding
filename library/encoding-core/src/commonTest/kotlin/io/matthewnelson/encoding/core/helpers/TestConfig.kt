/*
 * Copyright (c) 2023 Matthew Nelson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package io.matthewnelson.encoding.core.helpers

import io.matthewnelson.encoding.core.EncoderDecoder
import io.matthewnelson.encoding.core.ExperimentalEncodingApi
import io.matthewnelson.encoding.core.util.DecoderInput
import io.matthewnelson.encoding.core.util.byte

@OptIn(ExperimentalEncodingApi::class)
class TestConfig(
    isLenient: Boolean? = false,
    paddingByte: Byte? = '='.byte,
    private val encodeReturn: (unEncodedSize: Long) -> Long = { -1L },
    private val decodeInputReturn: (encodedSize: Int) -> Int = { -1 },
    private val decodeReturn: (encodedSize: Long) -> Long = { -1L },
): EncoderDecoder.Config(isLenient, paddingByte) {
    override fun decodeOutMaxSizeProtected(encodedSize: Long): Long {
        return decodeReturn.invoke(encodedSize)
    }
    override fun decodeOutMaxSizeOrFailProtected(encodedSize: Int, input: DecoderInput): Int {
        return decodeInputReturn.invoke(encodedSize)
    }
    override fun encodeOutSizeProtected(unEncodedSize: Long): Long {
        return encodeReturn.invoke(unEncodedSize)
    }
    override fun toStringAddSettings(sb: StringBuilder) {}
}
