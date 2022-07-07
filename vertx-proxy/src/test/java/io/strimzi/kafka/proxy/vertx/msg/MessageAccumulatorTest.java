package io.strimzi.kafka.proxy.vertx.msg;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import io.vertx.core.buffer.Buffer;

public class MessageAccumulatorTest {

    @Test
    public void newAccumulatorReturnsNoMessages() {
        MessageAccumulator acc = new MessageAccumulator();

        List<Buffer> actualMessages = acc.take();

        assertEquals(Collections.EMPTY_LIST, actualMessages);
    }

    @Test
    public void fewerBytesThatACompleteSizeValueReturnsNoMessages() {
        MessageAccumulator acc = new MessageAccumulator();
        acc.append(Buffer.buffer(new byte[] { 0x00 }));

        List<Buffer> actualMessages = acc.take();

        assertEquals(Collections.EMPTY_LIST, actualMessages);
    }

    // Requests or responses with zero length should not occur in the Kafka
    // protocol. Test that the MessageAccumulator class does something sensible
    // if it encounters one.
    @Test
    public void zeroSizeMessage() {
        MessageAccumulator acc = new MessageAccumulator();
        acc.append(Buffer.buffer(new byte[] { 0x00, 0x00, 0x00, 0x00 }));

        List<Buffer> actualMessages = acc.take();

        assertEquals(1, actualMessages.size());
        assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x00 }, actualMessages.get(0).getBytes());
    }

    @Test
    public void exactlyOneMessage() {
        MessageAccumulator acc = new MessageAccumulator();
        acc.append(Buffer.buffer(new byte[] { 0x00, 0x00, 0x00, 0x01, 0x02 }));

        List<Buffer> actualMessages = acc.take();

        assertEquals(1, actualMessages.size());
        assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x01, 0x02 },
                actualMessages.get(0).getBytes());

        List<Buffer> furtherMessages = acc.take();
        assertEquals(Collections.EMPTY_LIST, furtherMessages);
    }

    @Test
    public void notQuiteTwoMessages() {
        MessageAccumulator acc = new MessageAccumulator();
        acc.append(Buffer.buffer(new byte[] {
                0x00, 0x00, 0x00, 0x01, 0x02,
                0x00, 0x00, 0x00, 0x02,
        }));

        List<Buffer> actualMessages = acc.take();

        assertEquals(1, actualMessages.size());
        assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x01, 0x02 },
                actualMessages.get(0).getBytes());

        List<Buffer> furtherMessages = acc.take();
        assertEquals(Collections.EMPTY_LIST, furtherMessages);
    }

    @Test
    public void exactlyTwoMessages() {
        MessageAccumulator acc = new MessageAccumulator();
        acc.append(Buffer.buffer(new byte[] {
                0x00, 0x00, 0x00, 0x01, 0x02,
                0x00, 0x00, 0x00, 0x02, 0x03, 0x04,
        }));

        List<Buffer> actualMessages = acc.take();

        assertEquals(2, actualMessages.size());
        assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x01, 0x02 },
                actualMessages.get(0).getBytes());
        assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x02, 0x03, 0x04 },
                actualMessages.get(1).getBytes());
    }

    @Test
    public void twoMessagesAccumulatedInSmallPieces() {
        MessageAccumulator acc = new MessageAccumulator();
        acc.append(Buffer.buffer(new byte[] {
                0x00, 0x00, 0x00, 0x01 })); // incomplete first message

        assertEquals(Collections.EMPTY_LIST, acc.take()); // no messages returned

        acc.append(Buffer.buffer(new byte[] {
                0x02, // completes first message
                0x00, 0x00, 0x00, 0x02 })); // incomplete second message

        List<Buffer> actualMessages = acc.take();

        assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x01, 0x02 },
                actualMessages.get(0).getBytes());
        assertEquals(Collections.EMPTY_LIST, acc.take()); // no more messages

        acc.append(Buffer.buffer(new byte[] {
                0x03, 0x04 })); // incomplete second message

        actualMessages = acc.take();
        assertArrayEquals(new byte[] { 0x00, 0x00, 0x00, 0x02, 0x03, 0x04 },
                actualMessages.get(0).getBytes());
        assertEquals(Collections.EMPTY_LIST, acc.take()); // no more messages
    }
}
