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
package io.matthewnelson.encoding.core.internal

import io.matthewnelson.encoding.core.internal.ByteChar.Companion.toByteChar
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

@JvmInline
@InternalEncodingApi
public value class EncodingTable private constructor(private val value: ByteArray) {

    @Throws(IndexOutOfBoundsException::class)
    public fun get(index: Int): ByteChar = value[index].toByteChar()

    public companion object {
        @JvmStatic
        public fun from(chars: String): EncodingTable = EncodingTable(chars.encodeToByteArray())
    }
}
