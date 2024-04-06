/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.internal.ChannelUtils;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

import static io.netty.channel.internal.ChannelUtils.WRITE_STATUS_SNDBUF_FULL;

/**
 * {@link AbstractNioChannel} base class for {@link Channel}s that operate on bytes.
 */
public abstract class AbstractNioByteChannel extends AbstractNioChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(false, 16);
    private static final String EXPECTED_TYPES =
            " (expected: " + StringUtil.simpleClassName(ByteBuf.class) + ", " +
                    StringUtil.simpleClassName(FileRegion.class) + ')';

    private final Runnable flushTask = new Runnable() {
        @Override
        public void run() {
            // Calling flush0 directly to ensure we not try to flush messages that were added via write(...) in the
            // meantime.
            ((AbstractNioUnsafe) unsafe()).flush0();
        }
    };
    private boolean inputClosedSeenErrorOnRead;

    /**
     * Create a new instance
     *
     * @param parent the parent {@link Channel} by which this instance was created. May be {@code null}
     * @param ch     the underlying {@link SelectableChannel} on which it operates
     */
    protected AbstractNioByteChannel(Channel parent, SelectableChannel ch) {
        super(parent, ch, SelectionKey.OP_READ);
    }

    /**
     * Shutdown the input side of the channel.
     */
    protected abstract ChannelFuture shutdownInput();

    protected boolean isInputShutdown0() {
        return false;
    }

    @Override
    protected AbstractNioUnsafe newUnsafe() {
        return new NioByteUnsafe();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    final boolean shouldBreakReadReady(ChannelConfig config) {
        return isInputShutdown0() && (inputClosedSeenErrorOnRead || !isAllowHalfClosure(config));
    }

    private static boolean isAllowHalfClosure(ChannelConfig config) {
        return config instanceof SocketChannelConfig &&
                ((SocketChannelConfig) config).isAllowHalfClosure();
    }

    protected class NioByteUnsafe extends AbstractNioUnsafe {

        private void closeOnRead(ChannelPipeline pipeline) {
            /**
             * 这里的 isInputShutdown0 方法是用来判断 TCP 连接上的读通道是否关闭，在当前情况下，服务端的读通道肯定还没有关闭，
             * 因为目前 Netty 还没有调用任何关闭连接的系统调用。
             *
             * 那这里为什么要对读通道是否关闭进行判断？
             *
             */
            if (!isInputShutdown0()) {
                if (isAllowHalfClosure(config())) {
                    shutdownInput();
                    pipeline.fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
                } else {
                    // 如果不支持半关闭，则服务端直接调用close方法向客户端发送fin,结束close_wait状态进入last_ack状态
                    close(voidPromise());
                }
            } else if (!inputClosedSeenErrorOnRead) {
                inputClosedSeenErrorOnRead = true;
                pipeline.fireUserEventTriggered(ChannelInputShutdownReadComplete.INSTANCE);
            }
        }

        private void handleReadException(ChannelPipeline pipeline, ByteBuf byteBuf, Throwable cause, boolean close, RecvByteBufAllocator.Handle allocHandle) {
            if (byteBuf != null) {
                if (byteBuf.isReadable()) {
                    readPending = false;
                    // 如果发生异常时，已经读取到了部分数据，则触发ChannelRead事件
                    pipeline.fireChannelRead(byteBuf);
                } else {
                    byteBuf.release();
                }
            }
            allocHandle.readComplete();
            pipeline.fireChannelReadComplete();
            pipeline.fireExceptionCaught(cause);

            // If oom will close the read event, release connection.
            // See https://github.com/netty/netty/issues/10434
            if (close || cause instanceof OutOfMemoryError || cause instanceof IOException) {
                closeOnRead(pipeline);
            }
        }

        @Override
        public final void read() {
            // 获取客户端NioSocketChannel的Channel配置类NioSocketChannelConfig
            final ChannelConfig config = config();
            if (shouldBreakReadReady(config)) {
                clearReadPending();
                return;
            }

            // 获取NioSocketChannel的pipeline。
            final ChannelPipeline pipeline = pipeline();

            /**
             * 具体执行内存分配动作的是这里的ByteBufAllocator类型为PooledByteBufAllocator。
             * 它会根据AdaptiveRecvByteBufAllocator动态调整出来的大小去真正的申请内存分配ByteBuffer。
             *
             * PooledByteBufAllocator为Netty中的内存池，用来管理堆外内存DirectByteBuffer。
             *
             * @see DefaultChannelConfig#getAllocator()
             */
            final ByteBufAllocator allocator = config.getAllocator();

            /**
             * AdaptiveRecvByteBufAllocator并不会真正的去分配ByteBuffer，它只是负责动态调整分配ByteBuffer的大小。
             * @see AdaptiveRecvByteBufAllocator#AdaptiveRecvByteBufAllocator()
             */
            final RecvByteBufAllocator.Handle allocHandle = recvBufAllocHandle();

            /**
             * 在每轮循环开始前，执行 reset 操作清空上一轮read loop的统计指标。
             * @see DefaultMaxMessagesRecvByteBufAllocator.MaxMessageHandle#reset(ChannelConfig)
             */
            allocHandle.reset(config);

            ByteBuf byteBuf = null;
            boolean close = false;
            try {
                do {
                    /**
                     * 利用PooledByteBufAllocator分配合适大小的byteBuf，初始大小为2048
                     * @see DefaultMaxMessagesRecvByteBufAllocator.MaxMessageHandle#allocate(ByteBufAllocator)
                     */
                    byteBuf = allocHandle.allocate(allocator);

                    /**
                     * 记录本次读取了多少字节数据，并统计本轮read loop目前总共读取了多少字节。
                     * @see NioSocketChannel#doReadBytes(ByteBuf)
                     *
                     * 同 TCP 正常关闭收到 FIN 包一样，当服务端收到 RST 包后，OP_READ 事件活跃，这里和 TCP 正常关闭不同的是，
                     * 在调用 doReadBytes 方法从 Channel 中读取数据的时候会抛出 IOException 异常。
                     * 这里会有两种情况抛出异常：
                     */
                    allocHandle.lastBytesRead(doReadBytes(byteBuf));

                    // 如果本次没有读取到任何字节，则退出循环，进行下一轮事件轮询
                    if (allocHandle.lastBytesRead() <= 0) {
                        // nothing was read. release the buffer.
                        byteBuf.release();
                        byteBuf = null;
                        close = allocHandle.lastBytesRead() < 0;
                        if (close) {
                            // There is nothing left to read as we received an EOF.
                            // 表示客户端发起连接关闭
                            readPending = false;
                        }
                        break;
                    }

                    // read loop读取数据次数+1
                    allocHandle.incMessagesRead(1);

                    readPending = false;

                    // 客户端NioSocketChannel的pipeline中触发ChannelRead事件
                    pipeline.fireChannelRead(byteBuf);

                    // 解除本次读取数据分配的ByteBuffer引用，方便下一轮read loop分配
                    byteBuf = null;
                } while (allocHandle.continueReading());// @see io.netty.channel.DefaultMaxMessagesRecvByteBufAllocator.MaxMessageHandle.continueReading()

                /**
                 * 根据本次read loop总共读取的字节数，决定下次是否扩容或者缩容
                 * @see AdaptiveRecvByteBufAllocator.HandleImpl#readComplete()
                 */
                allocHandle.readComplete();

                /**
                 * 在NioSocketChannel的pipeline中触发ChannelReadComplete事件，表示一次read事件处理完毕。
                 * 但这并不表示客户端发送来的数据已经全部读完，因为如果数据太多的话，这里只会读取16次，剩下的会等到下次read事件到来后在处理。
                 */
                pipeline.fireChannelReadComplete();

                if (close) {
                    /**
                     * 此时客户端发送fin1（fi_wait_1状态）主动关闭连接，服务端接收到fin，并回复ack进入close_wait状态
                     * 在服务端进入close_wait状态 需要调用close 方法向客户端发送fin_ack，服务端才能结束close_wait状态
                     */
                    closeOnRead(pipeline);
                }
            } catch (Throwable t) {
                handleReadException(pipeline, byteBuf, t, close, allocHandle);
            } finally {
                // Check if there is a readPending which was not processed yet.
                // This could be for two reasons:
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelRead(...) method
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelReadComplete(...) method
                //
                // See https://github.com/netty/netty/issues/2254
                if (!readPending && !config.isAutoRead()) {
                    removeReadOp();
                }
            }
        }
    }

    /**
     * Write objects to the OS.
     *
     * @param in the collection which contains objects to write.
     * @return The value that should be decremented from the write quantum which starts at
     * {@link ChannelConfig#getWriteSpinCount()}. The typical use cases are as follows:
     * <ul>
     *     <li>0 - if no write was attempted. This is appropriate if an empty {@link ByteBuf} (or other empty content)
     *     is encountered</li>
     *     <li>1 - if a single call to write data was made to the OS</li>
     *     <li>{@link ChannelUtils#WRITE_STATUS_SNDBUF_FULL} - if an attempt to write data was made to the OS, but no
     *     data was accepted</li>
     * </ul>
     * @throws Exception if an I/O exception occurs during write.
     */
    protected final int doWrite0(ChannelOutboundBuffer in) throws Exception {
        Object msg = in.current();
        if (msg == null) {
            // Directly return here so incompleteWrite(...) is not called.
            return 0;
        }
        return doWriteInternal(in, in.current());
    }

    private int doWriteInternal(ChannelOutboundBuffer in, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            if (!buf.isReadable()) {
                in.remove();
                return 0;
            }

            final int localFlushedAmount = doWriteBytes(buf);
            if (localFlushedAmount > 0) {
                in.progress(localFlushedAmount);
                if (!buf.isReadable()) {
                    in.remove();
                }
                return 1;
            }
        } else if (msg instanceof FileRegion) {
            FileRegion region = (FileRegion) msg;
            // 表示当前 FileRegion 中的文件数据已经传输完毕。那么在这种情况下本次 write loop 没有写入任何数据到 Socket ，所以返回 0 ，writeSpinCount - 0 意思就是本次 write loop 不算，继续循环。
            if (region.transferred() >= region.count()) {
                in.remove();
                return 0;
            }

            // doWriteFileRegion 方法中通过 FileChannel#transferTo 方法底层用到的系统调用为 sendFile 实现零拷贝网络文件的传输。
            long localFlushedAmount = doWriteFileRegion(region);
            // 表示本 write loop 中写入了一些数据到 Socket 中，会有返回 1，writeSpinCount - 1 减少一次 write loop 次数。
            if (localFlushedAmount > 0) {
                in.progress(localFlushedAmount);
                if (region.transferred() >= region.count()) {
                    in.remove();
                }
                return 1;
            }
        } else {
            // Should not reach here.
            throw new Error();
        }

        /**
         * localFlushedAmount <= 0 ：表示当前 Socket 发送缓冲区已满，无法写入数据，那么就返回 WRITE_STATUS_SNDBUF_FULL = Integer.MAX_VALUE。
         * writeSpinCount - Integer.MAX_VALUE 必然是负数，直接退出循环，向 Reactor 注册 OP_WRITE 事件并退出 flush 流程。等 Socket 发送缓冲区可写了，Reactor 会通知 channel 继续发送文件数据。
         */
        return WRITE_STATUS_SNDBUF_FULL;
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        int writeSpinCount = config().getWriteSpinCount();
        do {
            Object msg = in.current();
            if (msg == null) {
                // Wrote all messages.
                clearOpWrite();
                // Directly return here so incompleteWrite(...) is not called.
                return;
            }
            writeSpinCount -= doWriteInternal(in, msg);
        } while (writeSpinCount > 0);

        incompleteWrite(writeSpinCount < 0);
    }

    @Override
    protected final Object filterOutboundMessage(Object msg) {
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            // Netty为了减少数据从 堆内内存 到 堆外内存 的拷贝以及缓解GC的压力，所以这里必须采用 DirectByteBuffer 使用堆外内存来存放网络发送数据。
            if (buf.isDirect()) {
                return msg;
            }
            return newDirectBuffer(buf);
        }

        if (msg instanceof FileRegion) {
            return msg;
        }

        throw new UnsupportedOperationException(
                "unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
    }

    protected final void incompleteWrite(boolean setOpWrite) {
        // Did not write completely.
        if (setOpWrite) {
            /**
             * 这里处理还没写满16次 但是socket缓冲区已满写不进去的情况 注册write事件
             * 什么时候socket可写了， epoll会通知reactor线程继续写
             */
            setOpWrite();
        } else {
            // It is possible that we have set the write OP, woken up by NIO because the socket is writable, and then
            // use our write quantum. In this case we no longer want to set the write OP because the socket is still
            // writable (as far as we know). We will find out next time we attempt to write if the socket is writable
            // and set the write OP if necessary.
            /**
             * 思考：为什么在向 reactor 提交 flushTask 之前需要清理 OP_WRITE 事件呢？ 我们并没有注册 OP_WRITE 事件呀？
             *  可能之前socket缓存区写满的时候但是数据尚未完全写完这种场景下注册的。
             */
            clearOpWrite();

            // Schedule flush again later so other tasks can be picked up in the meantime
            /**
             * 思考：为什么这里不再继续注册 OP_WRITE 事件而是通过向 reactor 提交一个 flushTask 来完成 channel 中剩下数据的写入呢？
             *  因为 JDK NIO Selector 默认的是 epoll 的水平触发。执行到此表示socket缓存区依然可以继续写入，只不过当前Channel的16次
             *  写入数据的次数已经用完，之后的写入工作由异步任务的形式进行处理，如果不把之前注册的写事件删除，因为水平触发，下次select时
             *  这个Channel就会被重复处理。
             */
            eventLoop().execute(flushTask);
        }
    }

    /**
     * Write a {@link FileRegion}
     *
     * @param region the {@link FileRegion} from which the bytes should be written
     * @return amount       the amount of written bytes
     */
    protected abstract long doWriteFileRegion(FileRegion region) throws Exception;

    /**
     * Read bytes into the given {@link ByteBuf} and return the amount.
     */
    protected abstract int doReadBytes(ByteBuf buf) throws Exception;

    /**
     * Write bytes form the given {@link ByteBuf} to the underlying {@link java.nio.channels.Channel}.
     *
     * @param buf the {@link ByteBuf} from which the bytes should be written
     * @return amount       the amount of written bytes
     */
    protected abstract int doWriteBytes(ByteBuf buf) throws Exception;

    protected final void setOpWrite() {
        final SelectionKey key = selectionKey();
        // Check first if the key is still valid as it may be canceled as part of the deregistration
        // from the EventLoop
        // See https://github.com/netty/netty/issues/2104
        if (!key.isValid()) {
            return;
        }
        final int interestOps = key.interestOps();
        if ((interestOps & SelectionKey.OP_WRITE) == 0) {
            key.interestOps(interestOps | SelectionKey.OP_WRITE);
        }
    }

    protected final void clearOpWrite() {
        final SelectionKey key = selectionKey();
        // Check first if the key is still valid as it may be canceled as part of the deregistration
        // from the EventLoop
        // See https://github.com/netty/netty/issues/2104
        if (!key.isValid()) {
            return;
        }
        final int interestOps = key.interestOps();
        if ((interestOps & SelectionKey.OP_WRITE) != 0) {
            key.interestOps(interestOps & ~SelectionKey.OP_WRITE);
        }
    }
}
