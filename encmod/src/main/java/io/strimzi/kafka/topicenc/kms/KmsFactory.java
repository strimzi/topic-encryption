package io.strimzi.kafka.topicenc.kms;

public class KmsFactory {

    private static final String TEST_KMS = "test";
    private static final String VAULT_KMS = "vault";

    public static KeyMgtSystem createKms(KmsDefinition kmsDef) {

        switch (kmsDef.getType()) {
        case TEST_KMS:
            return new TestKms();
        case VAULT_KMS:
            return new VaultKms(kmsDef);
        default:
            throw new IllegalArgumentException("Unknown KMS type: " + kmsDef.getType());
        }
    }

}
