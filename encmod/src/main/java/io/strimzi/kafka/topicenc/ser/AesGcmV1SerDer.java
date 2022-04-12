/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc.ser;

import java.nio.ByteBuffer;

import org.apache.kafka.common.record.MemoryRecordsBuilder;
import org.apache.kafka.common.record.Record;

import io.strimzi.kafka.topicenc.enc.EncData;

/**
 * Serializes and deserializes messages encrypted with AES GCM.
 * Actually we need access to requests so we can access headers if needed.
 */
public class AesGcmV1SerDer implements EncSerDer {

	public static final short VERSION = 1;
	private static final String VERSION_ERRMSG = "Unsupported serialization version: %d, expected %d";
	
	@Override
	public byte[] serialize(EncData md) throws EncSerDerException {
		int len = Short.BYTES +              // version 
				  Short.BYTES +              // iv length
				  md.getIv().length +        // iv
				  Integer.BYTES +            // data len
				  md.getCiphertext().length; // data
		
	  	ByteBuffer buf = ByteBuffer.allocate(len);
	  	buf.putShort(VERSION);
	  	buf.putShort((short) md.getIv().length);
	  	buf.put(md.getIv());
	  	buf.putInt(md.getCiphertext().length);
	  	buf.put(md.getCiphertext());
	  	return buf.array();
	}
	
    @Override
    public void serialize(MemoryRecordsBuilder builder, Record r, EncData encResult) throws EncSerDerException {
        byte[] serializedBuf = serialize(encResult);
        builder.append(r.timestamp(), 
                r.key() != null ? r.key().array() : null,
                serializedBuf, 
                r.headers());
    }

	@Override
	public EncData deserialize(byte[] msg) throws EncSerDerException {
		ByteBuffer buf = ByteBuffer.wrap(msg);
		int bufLen = buf.remaining();
		
		if (bufLen < 2*Short.BYTES) {
			throw new EncSerDerException("Message too small, cannot deserialize.");
		}
		short version = buf.getShort();
		if (version != VERSION) {
			String errMsg = createVersionErrMsg(version, VERSION);
			throw new EncSerDerException(errMsg);
		}
		
		short ivLen = buf.getShort();
		if (ivLen == 0) {
			throw new EncSerDerException("Invalid message: IV length is 0.");
		}
		if (ivLen > bufLen) {
			throw new EncSerDerException("Invalid message: IV length exceeds message length.");
		}
		
		byte[] iv = new byte[ivLen];
		buf.get(iv);
	
		if ((buf.position() + Integer.BYTES) > bufLen) {
			throw new EncSerDerException("Invalid message: message too short.");
		}
		int ciphertextLen = buf.getInt();
		if (ciphertextLen < 0) {
			throw new EncSerDerException("Invalid message: negative ciphertext length.");
		} else if ((buf.position() + ciphertextLen) > bufLen) {
			throw new EncSerDerException("Invalid message: ciphertext length exceeds message length");			
		}
		
		byte[] ciphertext = new byte[ciphertextLen];
		buf.get(ciphertext);
		EncData result = new EncData(iv, ciphertext);
		return result;
	}
	
	private static String createVersionErrMsg(short rcvd, short expected) {
		return String.format(VERSION_ERRMSG, rcvd, expected);		
	}
}
