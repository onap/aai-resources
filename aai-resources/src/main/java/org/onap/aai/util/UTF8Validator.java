/**
 * ============LICENSE_START=======================================================
 * org.onap.aai
 * ================================================================================
 * Copyright © 2025 Deutsche Telekom. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.aai.util;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

import org.onap.aai.exceptions.AAIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UTF8Validator {
	private static final Logger LOGGER = LoggerFactory.getLogger(UTF8Validator.class);
	
	private static CharsetDecoder decoder = null;
	/**
	 * Validates if the given string content is valid UTF-8
	 * 
	 * @param content The string to validate
	 * @return true if content is valid UTF-8, false otherwise
	 * @throws AAIException 
	 */
	public static boolean isValidUTF8(String content){
		if (content == null) {
			return false;
		}

		try {
			// Convert string to bytes using ISO-8859-1 to preserve byte values
            byte[] bytes = content.getBytes(StandardCharsets.ISO_8859_1);
            
            // Create a decoder with strict validation
            decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

            // Attempt to decode the bytes
            decoder.decode(ByteBuffer.wrap(bytes));
			return true;
		} catch (CharacterCodingException e) {
            // Print the problematic bytes
			LOGGER.error("Invalid UTF-8 sequence detected");
		//	throw new AAIException("AAI_3000", "Invalid UTF-8 character detected....!!" );
			return false;
        }
	}
}
