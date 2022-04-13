/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.topicenc;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.apache.kafka.common.message.FetchResponseData.FetchableTopicResponse;
import org.apache.kafka.common.message.ProduceRequestData.TopicProduceData;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.MemoryRecordsBuilder;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.record.SimpleRecord;
import org.apache.kafka.common.record.TimestampType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.strimzi.kafka.topicenc.enc.AesGcmEncrypter;
import io.strimzi.kafka.topicenc.enc.EncData;
import io.strimzi.kafka.topicenc.enc.EncrypterDecrypter;
import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;
import io.strimzi.kafka.topicenc.policy.PolicyRepository;
import io.strimzi.kafka.topicenc.policy.TopicPolicy;
import io.strimzi.kafka.topicenc.ser.AesGcmV1SerDer;
import io.strimzi.kafka.topicenc.ser.EncSerDer;


/**
 * This class is the encompassing, deployable component containing
 * the Kafka topic encryption implementation.
 */
public class EncryptionModule implements EncModControl {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionModule.class);
	
	PolicyRepository policyRepo;
	KeyMgtSystem kms;	
	Map<String,EncrypterDecrypter> keyCache;
	EncSerDer encSerDer;
	
	public EncryptionModule (PolicyRepository policyRepo, KeyMgtSystem kms) {
		this.policyRepo = policyRepo;
		this.kms = kms;
		keyCache = new HashMap<>(); 
		encSerDer = new AesGcmV1SerDer();
		// init kms connection
		// init policy Repo
		// init kms cache
		// create enc/dec
		// init encdec cache
	}
	
	public boolean encrypt(TopicProduceData topicData) {
		
		final EncrypterDecrypter encrypter;
		try {
            encrypter = getTopicEncrypter(topicData.name());
        } catch (Exception e1) {
            LOGGER.error("Error obtaining encrypter for topic " + topicData.name());
            return false;
        }
		
		if (encrypter == null) {
		    LOGGER.debug(
		            "No encryption - topic {} is not configured for encryption",topicData.name());
		    return false;
		}

		// If this far, the data should be encrypted. 
		// Navigate into each record and encrypt.
		topicData.partitionData().forEach(partitionData -> {
			MemoryRecords recs = (MemoryRecords) partitionData.records();
			// todo: note assumption of *memory* records
			MemoryRecordsBuilder builder = createMemoryRecsBuilder(recs.buffer().capacity());
			recs.records().forEach(record -> {
				if (record.hasValue()) {
					try {
					    // encrypt record value:
					    byte[] plaintext = new byte[record.valueSize()];
					    record.value().get(plaintext);
						EncData ciphertext = encrypter.encrypt(plaintext);
						
						// serialize the ciphertext and metadata, add to the builder:
						encSerDer.serialize(builder, record, ciphertext);
						
					} catch (Exception e) {
					    LOGGER.error("Error encrypting topic record for topic, " + topicData.name(), e);
					} 
				}
			});
			// overwrite the partition's memoryrecords with the encrypted records:
			partitionData.setRecords(builder.build());
		});
		return true;
	}
	
    public boolean decrypt(FetchableTopicResponse fetchRsp) {
        
        String topicName = fetchRsp.topic();
        final EncrypterDecrypter encrypter;
        try {
            encrypter = getTopicEncrypter(topicName);
        } catch (Exception e) {
            LOGGER.error("Error obtaining encrypter for topic " + topicName, e);
            return false;
        }
        
        if (encrypter == null) {
            LOGGER.debug(
                    "No decryption - topic {} is not configured for encryption", topicName);
            return false;
        }

        // If this far, the data was encrypted. 
        // Navigate into each record and decrypt.
        fetchRsp.partitionResponses().forEach(partitionData -> {
            /*String msg = String.format("partition: %d, logStartOffset: %08X, lastStableOffset: %08X,  partition leader epoch: %04X",
                                        partitionData.partition(),
                                        partitionData.currentLeader().leaderEpoch(),
                                        partitionData.logStartOffset(),
                                        partitionData.lastStableOffset());
            LOGGER.debug(msg); */

            MemoryRecords recs = (MemoryRecords) partitionData.recordSet();
            
            long firstOffset = getFirstOffset(recs);
            MemoryRecordsBuilder builder =
                    createMemoryRecsBuilder(recs.sizeInBytes(),
                                            partitionData.currentLeader().leaderEpoch(),
                                            firstOffset);
            recs.records().forEach(record -> {
                if (record.hasValue()) {
                    try {
                        byte[] ciphertext = new byte[record.valueSize()];
                        record.value().get(ciphertext);
                        
                        // serialize value into version, iv, ciphertext:
                        EncData md = encSerDer.deserialize(ciphertext);

                        // decrypt, add to records builder:
                        byte[] plaintext = encrypter.decrypt(md);
                        
                        SimpleRecord newRec = new SimpleRecord(record.timestamp(),
                                                           record.key(),
                                                           ByteBuffer.wrap(plaintext),
                                                           record.headers());
                        builder.append(newRec);

                    } catch (Exception e) {
                        // this must make its way back to caller - not so currently
                        LOGGER.error("Error decrypting topic record for topic, " + topicName, e);
                    } 
                }
            });
            // overwrite the partition's memoryrecords with the decrypted records:
            MemoryRecords newRecs = builder.build();
            partitionData.setRecordSet(newRecs);
        });
        return true;
    }

    /**
     * EncMod control interface. Empty, placeholder implementation
     * for the time being.
     */
    @Override
	public void purgeKey(String keyref) {
	}

	/**
	 * Consults the policy db whether a topic is to be encrypted. 
	 * If topic is not to be encrypted, returns null.
	 * @throws Exception 
	 */
	protected EncrypterDecrypter getTopicEncrypter (String topicName) throws Exception {
		
		String topicKey = topicName.toLowerCase();
		
		// first check cache		
		EncrypterDecrypter enc = keyCache.get(topicKey);
		if (enc != null) {
			return enc;
		}
		
		// query policy db for a policy for this topic: 
		TopicPolicy policy = policyRepo.getTopicPolicy(topicKey);
		if (policy == null) {
			return null; // no encryption policy for this topic. return null
		}

		// encryption policy exists for this topic. Retrieve key 
		SecretKey key = getKey(policy);
		
		// instantiate the encrypter/decrypter for this topic 
		// todo: factory for creating type of encrypter - comes from policy
		enc = new AesGcmEncrypter(key);
		
		// add to cache and return
		keyCache.put(topicKey, enc);
		return enc;
	}
	
	private long getFirstOffset(MemoryRecords recs) {
        for (org.apache.kafka.common.record.Record r : recs.records()) {
            if (r.hasValue()) {
                return r.offset();
            }
        }
        return 0;
	}

	/**
	 * Given a encryption policy retrieve and return the encryption key.
	 * @param policy
	 * @return
	 * @throws Exception 
	 */
	private SecretKey getKey(TopicPolicy policy) throws Exception {
	    return kms.getKey(policy.getKeyReference());
	}
	
    private MemoryRecordsBuilder createMemoryRecsBuilder(int bufSize) {
        return createMemoryRecsBuilder(bufSize, RecordBatch.NO_PARTITION_LEADER_EPOCH);
    }

    private MemoryRecordsBuilder createMemoryRecsBuilder(int bufSize, int partitionEpoch) {
        return createMemoryRecsBuilder(bufSize, partitionEpoch, 0L);
	}

    private MemoryRecordsBuilder createMemoryRecsBuilder(int bufSize, int partitionEpoch, long baseOffset) {
        ByteBuffer buffer = ByteBuffer.allocate(10); // will be expanded as needed
        return new MemoryRecordsBuilder(
                  buffer,
                  RecordBatch.CURRENT_MAGIC_VALUE, 
                  CompressionType.NONE,
                  TimestampType.CREATE_TIME,
                  baseOffset,
                  RecordBatch.NO_TIMESTAMP, // log appendTime
                  RecordBatch.NO_PRODUCER_ID,
                  RecordBatch.NO_PRODUCER_EPOCH,
                  0,              // baseSequence. partitionEpoch > 0 ? (partitionEpoch-1) : 0, RecordBatch.NO_SEQUENCE,
                  false,          // isTransactional
                  false,          // isBatch
                  partitionEpoch, // RecordBatch.NO_PARTITION_LEADER_EPOCH,
                  bufSize);     
    }
}
