/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.kafka.proxy.vertx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

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

import io.strimzi.kafka.topicenc.EncryptionModule;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

/**
 * Handles message flow between a single socket with a Kafka client
 * and a single socket to the broker.  As such, class variables are 
 * scoped to a socket pair.
 * Global objects such as the Encryption Module are passed through the
 * vertx context argument to the constructor.
 */
public class MessageHandler implements Handler<Buffer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandler.class);

    Context context;
    Config config;
    EncryptionModule encMod;
    NetSocket clientSocket;
    NetClient brokerClient;
    Future<NetSocket> brokerSocketFuture;
    Map<Integer, RequestHeader> fetchHeaderCache = new HashMap<>();
    MessageAccumulator currBrokerRsp = new MessageAccumulator();
    MessageAccumulator currClientReq = new MessageAccumulator();

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

        // open, initialize the connection to the broker.
        this.brokerClient = context.owner().createNetClient();
        String broker = config.kafkaHostname();
        String[] tokens = broker.split(":");
        if (tokens == null || tokens.length != 2) {
            throw new IllegalArgumentException("Broker must be specified as 'hostname:port'");
        }
        int port = Integer.valueOf(tokens[1]);
        String hostname = tokens[0];
        this.brokerSocketFuture = brokerClient.connect(port, hostname);
        brokerSocketFuture
             .onSuccess(h -> {
                 LOGGER.debug("brokerSocketFuture success");
                 h.handler(buffer -> processBrokerResponse(buffer));
              })
             .onFailure(e -> LOGGER.debug("Error connecting to broker", e))
             .onComplete(h -> LOGGER.debug("brokerSocketFuture complete"));
        LOGGER.info("MessageHandler created");
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
     * Close and clear resources so the message handler does not hang
     * around the heap after the client socket has been closed.
     */
    public void close() {
        LOGGER.debug("Closing Message Handler");
        clientSocket = null;
        context = null;
        fetchHeaderCache.clear();
        fetchHeaderCache = null;
        NetSocket brokerSocket = brokerSocketFuture.result();
        if (brokerSocket != null) {
            brokerSocket.close();
        }
        brokerSocketFuture = null;
    }

    /**
     * This is the main entry to message handling, triggered by the
     * arrival of data from the Kafka client. The received buffer
     * may not be a complete Kafka message, so we assemble
     * fragments until we have a complete message.
     */
    @Override
    public void handle(Buffer buffer) {
        
        LOGGER.debug("Request buffer from client arrived");
        currClientReq.append(buffer);
        
        boolean isComplete = currClientReq.isComplete();
        LOGGER.debug("Kafka msg complete {}", isComplete);
        if (!isComplete) {
            return;
        }
        
        // We have a complete kafka msg - process it, forward to broker
        Buffer sendBuffer = processRequest(currClientReq.getBuffer());
        currClientReq.reset();
        forwardToBroker(sendBuffer);
    } 
    
    /**
     * Inspects the incoming Kafka request message and dispatches it
     * depending on apikey (i.e., Kafka message type). If not a request
     * type we are interested in, returns the unaltered buffer
     * so it is forwarded to the broker as-is.
     *
     * @param buffer
     * @return
     */
    public Buffer processRequest(Buffer buffer) {
        if (buffer.length() < 10) {
            LOGGER.debug("processRequest():  buffer too small, ignoring.");
            return buffer;
        } 
        short apikey = MsgUtil.getApiKey(buffer);

        if (LOGGER.isDebugEnabled()) {
            int corrId = MsgUtil.getReqCorrId(buffer);
            String apikeyName = ApiKeys.forId(apikey).name();
            LOGGER.debug("Request rcvd, apikey = {}, corrid = {}, socket = {}", 
                         apikeyName, corrId, clientSocket.remoteAddress().toString());
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
     * Process a produce request. We introspect the request and determine
     * whether it contains topic data which should be encrypted. If so,
     * the encrypted records are re-computed and overwrite the original
     * plaintext.
     *
     * @param kafkaMsg
     * @return
     */
    public Buffer processProduceRequest(Buffer buffer) {

        if (LOGGER.isDebugEnabled()) {
            LogUtils.hexDump("client->proxy: PRODUCE request", buffer);
        }
        // create an KafkaReqMsg instance - this enables access to the header, apiversion
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
        // encryption module where they are encrypted, if required.
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
        // records were altered by encrypting. Serialize and return the modified message
        return MsgUtil.toSendBuffer(kafkaMsg.getHeaderBytes(), req);
    }
    
    /**
     * Process fetch requests. We cache the fetch request headers
     * in order to identify fetch responses on the back flow.
     *
     * @param kafkaMsg
     * @return
     */
    private Buffer processFetchRequest(Buffer buffer) {
        try {
            // cache the request header which we need later for response processing 
            KafkaReqMsg req = new KafkaReqMsg(buffer);
            fetchHeaderCache.put(req.getHeader().correlationId(),
                                 req.getHeader());
            
            if (LOGGER.isDebugEnabled()) {
                FetchRequest fetch = FetchRequest.parse(req.getPayload(),
                        req.getHeader().apiVersion());

                String msg = String.format("FETCH epoch = %04X, session = %04X",
                                fetch.metadata().epoch(), fetch.metadata().sessionId());
                LOGGER.debug(msg);
            }
            return req.rawMsg;
            
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

        if (!brokerSocketFuture.isComplete()) {
            LOGGER.info("broker socket not ready");
            // broker socket not ready. Return an empty buffer to get off the thread.
            // This is a hack ... must find a better way to handle this.
            clientSocket.write(Buffer.buffer(0));
            return;
        }
        if (brokerSocketFuture.failed()) {
            LOGGER.error("broker connection failed", brokerSocketFuture.cause());
            return;
        }
        NetSocket brokerSocket = brokerSocketFuture.result();
        brokerSocket.write(sendBuffer);
        LOGGER.debug("Forwarded message to broker");
    }

    /**
     * This method is the handler for broker responses.
     * We check responses as to whether they match a cached Fetch request,
     * based on correlation ID. If so, pass to the encryption module
     * to check whether decryption is needed.
     * 
     * @param brokerRsp
     */
    public void processBrokerResponse(Buffer brokerRsp) {
        // accumulate message fragments
        currBrokerRsp.append(brokerRsp);
        if (!currBrokerRsp.isComplete()) {
            return;
        }
        
        // if this far, we have a complete message. process it.
        Buffer brokerRspMsg = currBrokerRsp.getBuffer();

        int corrId = MsgUtil.getRspCorrId(brokerRspMsg);
        if (corrId != -1) {
            RequestHeader reqHeader = fetchHeaderCache.get(corrId);
            if (reqHeader != null) {
                // The response matches a recently cached fetch request.
                LOGGER.debug("Broker response matches cached FETCH req header corrId={}", corrId);
                fetchHeaderCache.remove(corrId);
                
                // processFetchResponse calls enc module for decryption:
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
                String msg = String.format("proxy->client: broker response corrId=%d (%02X), thread = %s, socket=%s",
                        corrId, corrId, Thread.currentThread().getName(), clientSocket.remoteAddress().toString());
                LogUtils.hexDump(msg, rspBuf);
            }
        });
        // reset the broker rsp buffer:
        currBrokerRsp.reset();
    }
    
    /**
     * Fetch responses are processed here. We navigate the topic responses,
     * passing to the encryption module which determines if they are to be decrypted. 
     *
     * @param buffer
     * @param reqHeader
     * @return
     */
    public Buffer processFetchResponse(Buffer buffer, RequestHeader reqHeader) {

        // instantiate FetchResponse instance
        KafkaRspMsg rsp = new KafkaRspMsg(buffer, reqHeader.apiVersion());
        FetchResponse<?> fetch = 
                (FetchResponse<?>) AbstractResponse.parseResponse(rsp.getPayload(), reqHeader);

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
}
