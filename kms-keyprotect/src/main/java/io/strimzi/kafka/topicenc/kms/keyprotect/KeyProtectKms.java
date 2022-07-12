package io.strimzi.kafka.topicenc.kms.keyprotect;

import static java.util.Objects.isNull;

import java.util.List;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.cloud.ibm_key_protect_api.v2.IbmKeyProtectApi;
import com.ibm.cloud.ibm_key_protect_api.v2.model.GetKey;
import com.ibm.cloud.ibm_key_protect_api.v2.model.GetKeyOptions;
import com.ibm.cloud.ibm_key_protect_api.v2.model.KeyWithPayload;
import com.ibm.cloud.sdk.core.http.Response;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;

import io.strimzi.kafka.topicenc.enc.CryptoUtils;
import io.strimzi.kafka.topicenc.kms.KeyMgtSystem;
import io.strimzi.kafka.topicenc.kms.KmsDefinition;
import io.strimzi.kafka.topicenc.kms.KmsException;

public class KeyProtectKms implements KeyMgtSystem {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyProtectKms.class);

    private KmsDefinition kmsDef;
    private IbmKeyProtectApi keyProtect;

    public KeyProtectKms(KmsDefinition kmsDef) {
        if (isNull(kmsDef)) {
            throw new IllegalArgumentException("Null KmsDefinition argument.");
        }
        if (isNull(kmsDef.getUri())) {
            throw new IllegalArgumentException("Required argument 'baseUri' is missing.");
        }
        if (isNull(kmsDef.getCredential())) {
            throw new IllegalArgumentException("Required argument 'token' is missing.");
        }
        if (isNull(kmsDef.getInstanceId())) {
            throw new IllegalArgumentException("Required argument 'instanceid' is missing.");
        }
        this.kmsDef = kmsDef;
        this.keyProtect = initKeyProtect(kmsDef);
        LOGGER.debug("Initialization of KeyProtectKms complete");
    }

    @Override
    public SecretKey getKey(String keyReference) throws KmsException {

        GetKeyOptions options = new GetKeyOptions.Builder()
                .id(keyReference)
                .bluemixInstance(kmsDef.getInstanceId())
                .build();

        Response<GetKey> response = keyProtect.getKey(options).execute();
        if (response.getStatusCode() != 200) {
            String errMsg = String.format("Error obtaining key: HTTP %d (%s)",
                    response.getStatusCode(), response.getStatusMessage());
            throw new KmsException(errMsg);
        }
        List<KeyWithPayload> keys = response.getResult().getResources();
        if (keys == null || keys.size() == 0) {
            throw new KmsException("No key returned for key reference.");
        }
        KeyWithPayload key = keys.get(0);
        return CryptoUtils.createAesSecretKey(key.getPayload());
    }

    private static IbmKeyProtectApi initKeyProtect(KmsDefinition kmsDef) {

        IamAuthenticator authenticator = new IamAuthenticator(kmsDef.getCredential());
        // authenticator.setURL("");
        authenticator.validate();

        IbmKeyProtectApi keyProtect = IbmKeyProtectApi.newInstance(authenticator);
        keyProtect.setServiceUrl(kmsDef.getUri().toString());
        return keyProtect;
    }
}
