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
@file:Suppress("SpellCheckingInspection", "RemoveRedundantQualifierName")

package io.matthewnelson.encoding.base16

import io.matthewnelson.encoding.core.*
import io.matthewnelson.encoding.core.util.DecoderInput
import io.matthewnelson.encoding.core.util.FeedBuffer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmSynthetic

/**
 * Base16 (aka "hex") encoding/decoding in accordance with
 * RFC 4648 section 8.
 *
 * https://www.ietf.org/rfc/rfc4648.html#section-8
 *
 * e.g.
 *
 *     val base16 = Base16 {
 *         isLenient = true
 *         lineBreakInterval = 64
 *         encodeToLowercase = true
 *     }
 *
 *     val text = "Hello World!"
 *     val bytes = text.encodeToByteArray()
 *     val encoded = bytes.encodeToString(base16)
 *     println(encoded) // 48656c6c6f20576f726c6421
 *
 *     // Alternatively, use the static implementaton instead of
 *     // configuring your own settings.
 *     val decoded = encoded.decodeToByteArray(Base16).decodeToString()
 *     assertEquals(text, decoded)
 *
 * @see [io.matthewnelson.encoding.base16.Base16]
 * @see [Base16.Config]
 * @see [Base16.CHARS_UPPER]
 * @see [Base16.CHARS_LOWER]
 * @see [Base16.Companion]
 * @see [EncoderDecoder]
 * @see [Decoder.decodeToByteArray]
 * @see [Decoder.decodeToByteArrayOrNull]
 * @see [Encoder.encodeToString]
 * @see [Encoder.encodeToCharArray]
 * @see [Encoder.encodeToByteArray]
 * */
public class Base16(config: Base16.Config): EncoderDecoder<Base16.Config>(config) {

    /**
     * Configuration for [Base16] encoding/decoding.
     *
     * Use [Base16ConfigBuilder] to create.
     *
     * @see [Base16ConfigBuilder]
     * @see [EncoderDecoder.Config]
     * */
    public class Config private constructor(
        isLenient: Boolean,
        lineBreakInterval: Byte,
        @JvmField
        public val encodeToLowercase: Boolean,
    ): EncoderDecoder.Config(
        isLenient = isLenient,
        lineBreakInterval = lineBreakInterval,
        paddingChar = null
    ) {

        protected override fun decodeOutMaxSizeProtected(encodedSize: Long): Long {
            return encodedSize / 2L
        }

        protected override fun decodeOutMaxSizeOrFailProtected(encodedSize: Int, input: DecoderInput): Int {
            return encodedSize / 2
        }

        @Throws(EncodingSizeException::class)
        protected override fun encodeOutSizeProtected(unEncodedSize: Long): Long {
            if (unEncodedSize > (Long.MAX_VALUE / 2)) {
                throw outSizeExceedsMaxEncodingSizeException(unEncodedSize, Long.MAX_VALUE)
            }

            return unEncodedSize * 2L
        }

        protected override fun toStringAddSettings(): Set<Setting> {
            return buildSet {
                add(Setting(name = "encodeToLowercase", value = encodeToLowercase))
            }
        }

        internal companion object {

            @JvmSynthetic
            internal fun from(builder: Base16ConfigBuilder): Config {
                return Config(
                    isLenient = builder.isLenient,
                    lineBreakInterval = builder.lineBreakInterval,
                    encodeToLowercase = builder.encodeToLowercase,
                )
            }
        }
    }

    /**
     * Doubles as a static implementation with default settings
     * and a lineBreakInterval of 64.
     *
     * e.g.
     *
     *     val encoded = "Hello World!"
     *         .encodeToByteArray()
     *         .encodeToString(Base16)
     *
     *     println(encoded) // 48656c6c6f20576f726c6421
     *
     * */
    public companion object: EncoderDecoder<Base16.Config>(
        config = Base16ConfigBuilder().apply { lineBreakInterval = 64 }.build()
    ) {

        /**
         * Uppercase Base16 encoding characters.
         * */
        public const val CHARS_UPPER: String = "0123456789ABCDEF"

        /**
         * Lowercase Base16 encoding characters.
         * */
        public const val CHARS_LOWER: String = "0123456789abcdef"

        private val DELEGATE = Base16(config)
        protected override fun name(): String = DELEGATE.name()
        protected override fun newDecoderFeedProtected(out: Decoder.OutFeed): Decoder<Base16.Config>.Feed {
            return DELEGATE.newDecoderFeedProtected(out)
        }
        protected override fun newEncoderFeedProtected(out: OutFeed): Encoder<Base16.Config>.Feed {
            return DELEGATE.newEncoderFeedProtected(out)
        }
    }

    protected override fun newDecoderFeedProtected(out: Decoder.OutFeed): Decoder<Config>.Feed {
        return object : Decoder<Config>.Feed() {

            private val buffer = DecodingBuffer(out)

            @Throws(EncodingException::class)
            override fun consumeProtected(input: Char) {
                val bits: Int = when (input) {
                    in '0'..'9' -> {
                        // char ASCII value
                        // 0     48    0
                        // 9     57    9 (ASCII - 48)
                        input.code - 48
                    }
                    in 'a'..'f' -> {
                        // char ASCII value
                        //   A   65    10
                        //   F   70    15 (ASCII - 55)
                        input.uppercaseChar().code - 55
                    }
                    in 'A'..'F' -> {
                        // char ASCII value
                        //   A   65    10
                        //   F   70    15 (ASCII - 55)
                        input.code - 55
                    }
                    else -> {
                        throw EncodingException("Char[${input}] is not a valid Base16 character")
                    }
                }

                buffer.update(bits)
            }

            @Throws(EncodingException::class)
            override fun doFinalProtected() {
                buffer.finalize()
            }
        }
    }

    protected override fun newEncoderFeedProtected(out: OutFeed): Encoder<Config>.Feed {
        return object : Encoder<Config>.Feed() {

            private val table = if (config.encodeToLowercase) {
                CHARS_LOWER
            } else {
                CHARS_UPPER
            }

            override fun consumeProtected(input: Byte) {
                // A FeedBuffer is not necessary here as every 1
                // byte of input, 2 characters are output.
                val bits = input.toInt() and 0xff
                out.output(table[bits shr    4])
                out.output(table[bits and 0x0f])
            }

            override fun doFinalProtected() { /* no-op */ }
        }
    }

    protected override fun name(): String = "Base16"

    private inner class DecodingBuffer(out: Decoder.OutFeed): FeedBuffer(
        blockSize = 2,
        flush = { buffer ->
            var bitBuffer = 0
            for (bits in buffer) {
                bitBuffer = (bitBuffer shl 4) or bits
            }

            out.output(bitBuffer.toByte())
        },
        finalize = { modulus, _->
            when (modulus) {
                0 -> { /* no-op */ }
                else -> {
                    // 4*1 = 4 bits. Truncated, fail.
                    throw truncatedInputEncodingException(modulus)
                }
            }
        }
    )
}
