/*
 * Copyright Strimzi authors. License: Apache License 2.0 (see the file LICENSE or
 * http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.FetchResponseData.FetchableTopicResponse;
import org.apache.kafka.common.message.ProduceRequestData.TopicProduceData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.requests.AbstractResponse;
import org.apache.kafka.common.requests.FetchRequest;
import org.apache.kafka.common.requests.FetchResponse;
import org.apache.kafka.common.requests.ProduceRequest;
import org.apache.kafka.common.requests.RequestHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.strimzi.kafka.proxy.vertx.msg.KafkaReqMsg;
import io.strimzi.kafka.proxy.vertx.msg.KafkaRspMsg;
import io.strimzi.kafka.proxy.vertx.msg.MessageAccumulator;
import io.strimzi.kafka.proxy.vertx.msg.MsgUtil;
import io.strimzi.kafka.proxy.vertx.util.LogUtils;
import io.strimzi.kafka.topicenc.EncryptionModule;
import io.strimzi.kafka.topicenc.kms.KmsException;
import io.strimzi.kafka.topicenc.ser.EncSerDerException;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

/**
 * Handles message flow between a single socket with a Kafka client and a single
 * socket to the broker. As such, class variables are scoped to a socket pair.
 * Global objects such as the Encryption Module are passed through the vertx
 * context argument to the constructor.
 */
public class MessageHandler implements Handler<Buffer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandler.class);

    private Context context;
    private final Config config;
    private final EncryptionModule encMod;
    private NetSocket clientSocket;
    private NetClient brokerClient;
    private Future<NetSocket> brokerSocketFuture;
    private Map<Integer, RequestHeader> fetchHeaderCache = new HashMap<>();
    private MessageAccumulator currBrokerRsp = new MessageAccumulator();
    private MessageAccumulator currClientReq = new MessageAccumulator();

    /**
     * The real constructor, as opposed to the test constructor.
     * 
     * @param context
     * @param clientSocket
     */
    public MessageHandler(Context context, NetSocket clientSocket) {
        this.context = context;
        this.clientSocket = clientSocket;

        this.config = context.get(KafkaProxyVerticle.CTX_KEY_CONFIG);
        if (Objects.isNull(config)) {
            throw new NullPointerException("No config object");
        }

        this.encMod = context.get(KafkaProxyVerticle.CTX_KEY_ENCMOD);
        if (Objects.isNull(encMod)) {
            throw new NullPointerException("No encryption module");
        }

        connectToBroker(clientSocket);
        LOGGER.debug("MessageHandler created. isComplete: {}", brokerSocketFuture.isComplete());
    }

    /**
     * This constructor used for testing.
     *
     * @param encMod
     * @param config
     */
    public MessageHandler(EncryptionModule encMod, Config config) {
        super();
        if (Objects.isNull(encMod)) {
            throw new NullPointerException("Null encryption module"); // different exception?
        }
        this.encMod = encMod;
        this.config = config;
    }

    /**
     * Close and clear resources so the message handler does not hang around the
     * heap after the client socket has been closed. TODO: set state machine to
     * closing
     */
    public void close() {
        LOGGER.debug("Closing MessageHandler");
        if (brokerSocketFuture != null) {
            NetSocket brokerSocket = brokerSocketFuture.result();
            if (brokerSocket != null) {
                brokerSocket.close();
            }
            brokerSocketFuture = null;
        }
        clientSocket = null;
        context = null;
        fetchHeaderCache.clear();
        fetchHeaderCache = null;
    }

    /**
     * This is the main entry to message handling, triggered by the arrival of data
     * from the Kafka client. The received buffer may not be a complete Kafka
     * message, so we assemble fragments until we have a complete message.
     */
    @Override
    public void handle(Buffer buffer) {

        LOGGER.debug("Request buffer from client arrived");
        currClientReq.append(buffer);

        List<Buffer> sendBuffers = currClientReq.take();
        LOGGER.debug("Number of complete Kafka msgs: {}", sendBuffers.size());
        if (sendBuffers.isEmpty()) {
            return;
        }

        for (Buffer sendBuffer : sendBuffers) {
            // We have a complete kafka msg - process it, forward to broker
            try {
                sendBuffer = processRequest(sendBuffer);
            } catch (EncSerDerException | GeneralSecurityException | KmsException e) {
                LOGGER.error("Encryption error processing request", e);
                // TODO: send back Kafka error msg
                return;
            }
            forwardToBroker(sendBuffer);
        }
    }

    /**
     * Inspects the incoming Kafka request message and dispatches it depending on
     * apikey (i.e., Kafka message type). If not a request type we are interested
     * in, returns the unaltered buffer so it is forwarded to the broker as-is.
     *
     * @param buffer
     * @return
     * @throws GeneralSecurityException
     * @throws EncSerDerException
     * @throws KmsException
     */
    public Buffer processRequest(Buffer buffer)
            throws EncSerDerException, GeneralSecurityException, KmsException {
        if (buffer.length() < 10) {
            LOGGER.debug("processRequest():  buffer too small, ignoring.");
            return buffer;
        }
        short apikey = MsgUtil.getApiKey(buffer);

        if (LOGGER.isDebugEnabled()) {
            int corrId = MsgUtil.getReqCorrId(buffer);
            String apikeyName = ApiKeys.forId(apikey).name();
            LOGGER.debug("Request: apikey = {}, corrid = {}, socket = {}", apikeyName, corrId,
                    clientSocket == null ? "null" : clientSocket.remoteAddress().toString());
        }
        // dispatch based on apikey.Currently PRODUCE and FETCH only
        if (apikey == ApiKeys.PRODUCE.id) {
            return processProduceRequest(buffer);
        } else if (apikey == ApiKeys.FETCH.id) {
            return processFetchRequest(buffer);
        } else {
            // not interested in the msg type - pass back as-is.
            return buffer;
        }
    }

    /**
     * Process a produce request. We introspect the request and determine whether it
     * contains topic data which should be encrypted. If so, the encrypted records
     * are re-computed and overwrite the original plaintext.
     *
     * @param kafkaMsg
     * @return
     * @throws GeneralSecurityException
     * @throws EncSerDerException
     * @throws KmsException
     */
    public Buffer processProduceRequest(Buffer buffer)
            throws EncSerDerException, GeneralSecurityException, KmsException {

        if (LOGGER.isDebugEnabled()) {
            LogUtils.hexDump("client->proxy: PRODUCE request", buffer);
        }
        // create an KafkaReqMsg instance - this enables access to the header,
        // apiversion
        KafkaReqMsg kafkaMsg;
        try {
            kafkaMsg = new KafkaReqMsg(buffer);

        } catch (IllegalArgumentException e) {
            LOGGER.error("Error parsing PRODUCE request", e);
            if (LOGGER.isDebugEnabled()) {
                LogUtils.hexDump("Request causing error", buffer);
            }
            return buffer;
        }

        // deserialize the msg to a Produce instance:
        ProduceRequest req = ProduceRequest.parse(kafkaMsg.getPayload(),
                kafkaMsg.getHeader().apiVersion());

        // iterate over the request's partitions, passing them to the
        // encryption module where they are assessed for encryptopn.
        int numEncryptions = 0;
        if (req.data() != null && req.data().topicData() != null) {
            for (TopicProduceData topicData : req.data().topicData()) {
                boolean wasEncrypted = encMod.encrypt(topicData);
                if (wasEncrypted) {
                    numEncryptions++;
                }
            }
        }

        if (numEncryptions == 0) {
            // no encryptions performed, return original buffer as-is
            return buffer;
        }
        // records were altered by encryption. Serialize and return the modified message
        return MsgUtil.toSendBuffer(kafkaMsg.getHeaderBytes(), req);
    }

    /**
     * Process fetch requests. We cache the fetch request headers in order to
     * identify fetch responses on the back flow.
     *
     * @param kafkaMsg
     * @return
     */
    private Buffer processFetchRequest(Buffer buffer) {
        try {
            // cache the request header which we need later for response processing
            KafkaReqMsg req = new KafkaReqMsg(buffer);
            fetchHeaderCache.put(req.getHeader().correlationId(), req.getHeader());

            if (LOGGER.isDebugEnabled()) {
                FetchRequest fetch = FetchRequest.parse(req.getPayload(),
                        req.getHeader().apiVersion());

                LOGGER.debug("FETCH epoch = {}, session = {}",
                        Integer.toHexString(fetch.metadata().epoch()),
                        Integer.toHexString(fetch.metadata().sessionId()));
            }
            return req.getRawMsg();

        } catch (Exception e) {
            LOGGER.error("Error in processFetchRequest()", e);
            if (LOGGER.isDebugEnabled()) {
                LogUtils.hexDump("processFetchRequest: Error parsing buffer", buffer);
            }
            return buffer;
        }
    }

    /**
     * Once a client request is processed it is forwarded to the broker.
     * 
     * @param sendBuffer
     */
    private void forwardToBroker(Buffer sendBuffer) {

        if (sendBuffer == null || sendBuffer.length() == 0) {
            LOGGER.debug("forwardToBroker(): empty send Buffer");
            return;
        }

        if (!brokerSocketFuture.isComplete()) {
            LOGGER.info("broker socket not ready. Thread = {}", Thread.currentThread().getName());
            // broker socket not ready.
            return;
        }
        if (brokerSocketFuture.failed()) {
            LOGGER.error("Broker connection failed {}", brokerSocketFuture.cause());
            return;
        }
        NetSocket brokerSocket = brokerSocketFuture.result();
        brokerSocket.write(sendBuffer);
        LOGGER.debug("Forwarded message to broker");
    }

    /**
     * This method is the handler for broker responses. We check responses as to
     * whether they match a cached Fetch request, based on correlation ID. If so,
     * pass to the encryption module to check whether decryption is needed.
     * 
     * @param brokerRsp
     * @throws GeneralSecurityException
     * @throws EncSerDerException
     * @throws KmsException
     */
    public void processBrokerResponse(Buffer brokerRsp)
            throws EncSerDerException, GeneralSecurityException, KmsException {

        // accumulate message fragments
        currBrokerRsp.append(brokerRsp);

        List<Buffer> brokerRspMsgs = currBrokerRsp.take();
        if (brokerRspMsgs.isEmpty()) {
            return;
        }

        // process all the messages returned by the accumulator
        for (Buffer brokerRspMsg : brokerRspMsgs) {
            int corrId = MsgUtil.getRspCorrId(brokerRspMsg);
            if (corrId != -1) {
                RequestHeader reqHeader = fetchHeaderCache.remove(corrId);
                if (reqHeader == null) {
                    LOGGER.debug("Fetch req header not in cache corrId={}", corrId);
                    // TODO: when to drop the connection to the client?
                } else {
                    // The response matches a recently cached fetch request.
                    LOGGER.debug("Broker response matches cached FETCH req header corrId={}",
                            corrId);
                    // call enc module for decryption:
                    brokerRspMsg = processFetchResponse(brokerRspMsg, reqHeader);
                }
            }

            // Finished with broker response processing.
            // Forward to the Kafka client.
            Future<Void> writeFuture = clientSocket.write(brokerRspMsg);

            // logging:
            final Buffer rspBuf = brokerRspMsg;
            writeFuture.onSuccess(h -> {
                if (LOGGER.isDebugEnabled()) {
                    String msg = String.format(
                            "proxy->client: broker response corrId=%d (%02X), thread = %s, socket=%s",
                            corrId, corrId, Thread.currentThread().getName(),
                            clientSocket.remoteAddress().toString());
                    LogUtils.hexDump(msg, rspBuf);
                }
            });
        }
    }

    /**
     * Fetch responses are processed here. We navigate the topic responses, passing
     * to the encryption module which determines if they are to be decrypted.
     *
     * @param buffer
     * @param reqHeader
     * @return
     * @throws GeneralSecurityException
     * @throws EncSerDerException
     * @throws KmsException
     */
    public Buffer processFetchResponse(Buffer buffer, RequestHeader reqHeader)
            throws EncSerDerException, GeneralSecurityException, KmsException {
        // instantiate FetchResponse instance
        KafkaRspMsg rsp = new KafkaRspMsg(buffer, reqHeader.apiVersion());
        FetchResponse<?> fetch = (FetchResponse<?>) AbstractResponse.parseResponse(rsp.getPayload(),
                reqHeader);

        // iterate through response records, decrypting where needed
        FetchResponseData data = fetch.data();
        if (data == null) {
            return buffer;
        }
        List<FetchableTopicResponse> responses = data.responses();
        int numDecryptions = 0;
        for (FetchableTopicResponse topicRsp : responses) {
            boolean wasDecrypted = encMod.decrypt(topicRsp);
            if (wasDecrypted) {
                numDecryptions++;
            }
        }

        if (numDecryptions == 0) {
            // no decryptions were performed, return original buffer
            return buffer;
        } else {
            // records were decrypted. Serialize and return the modified message
            return MsgUtil.toSendBuffer(fetch, reqHeader);
        }
    }

    /**
     * Given a socket from a Kafka client, open and initialize a corresponding
     * socket to the broker.
     *
     * @param clientSocket
     */
    private void connectToBroker(NetSocket clientSocket) {

        this.brokerClient = context.owner().createNetClient();
        String broker = config.kafkaHostname();
        String[] tokens = broker.split(":");
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Broker must be specified as 'hostname:port'");
        }
        int port = Integer.valueOf(tokens[1]);
        String hostname = tokens[0];

        // open connection in a thread and then wait later
        LOGGER.debug("Connecting to broker {}", broker);
        this.brokerSocketFuture = brokerClient.connect(port, hostname);
        brokerSocketFuture.onSuccess(socket -> {
            LOGGER.debug("broker connected. Thread = {}", Thread.currentThread().getName());
            clientSocket.resume();
            socket.handler(buffer -> {
                try {
                    processBrokerResponse(buffer);
                } catch (EncSerDerException | GeneralSecurityException | KmsException e) {
                    LOGGER.error("Error decrypting broker response", e);
                    // TODO: forward error to client
                }
            }).closeHandler(brokerClose -> {
                LOGGER.debug("Broker connection closed");
                clientSocket.close();
            });
        }).onFailure(e -> {
            LOGGER.debug("Error connecting to broker", e);
            // TODO: return error to client
        });
    }

}
