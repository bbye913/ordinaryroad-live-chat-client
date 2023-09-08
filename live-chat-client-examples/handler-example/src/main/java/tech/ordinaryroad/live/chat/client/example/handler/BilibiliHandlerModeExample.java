/*
 * MIT License
 *
 * Copyright (c) 2023 OrdinaryRoad
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tech.ordinaryroad.live.chat.client.example.handler;


import com.fasterxml.jackson.databind.JsonNode;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.bilibili.constant.BilibiliCmdEnum;
import tech.ordinaryroad.live.chat.client.bilibili.constant.ProtoverEnum;
import tech.ordinaryroad.live.chat.client.bilibili.listener.IBilibiliSendSmsReplyMsgListener;
import tech.ordinaryroad.live.chat.client.bilibili.msg.DanmuMsgMsg;
import tech.ordinaryroad.live.chat.client.bilibili.msg.SendSmsReplyMsg;
import tech.ordinaryroad.live.chat.client.bilibili.netty.frame.factory.BilibiliWebSocketFrameFactory;
import tech.ordinaryroad.live.chat.client.bilibili.netty.handler.BilibiliBinaryFrameHandler;
import tech.ordinaryroad.live.chat.client.bilibili.netty.handler.BilibiliConnectionHandler;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseConnectionListener;
import tech.ordinaryroad.live.chat.client.commons.base.msg.BaseCmdMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.BaseMsg;

import java.net.URI;
import java.util.concurrent.TimeUnit;


/**
 * @author mjz
 * @date 2023/1/7
 */
@Slf4j
public class BilibiliHandlerModeExample {

    static Channel channel;
    // TODO 修改房间ID
    static long roomId = 7777;
    // TODO 设置浏览器Cookie
    static String cookie = System.getenv("cookie");
    // TODO 修改版本
    static ProtoverEnum protover = ProtoverEnum.NORMAL_BROTLI;

    public static void main(String[] args) {
        log.error("cookie: {}", cookie);

        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        BilibiliConnectionHandler connectionHandler = null;
        IBaseConnectionListener<BilibiliConnectionHandler> connectionListener = new IBaseConnectionListener<>() {

            @Override
            public void onConnected(BilibiliConnectionHandler connectionHandler) {
                log.error("{} 连接成功", roomId);
            }

            @Override
            public void onDisconnected(BilibiliConnectionHandler connectionHandler) {
                log.error("连接断开，5s后将重新连接");
                workerGroup.schedule(() -> {
                    bootstrap.connect().addListener((ChannelFutureListener) connectFuture -> {
                        if (connectFuture.isSuccess()) {
                            log.debug("连接建立成功！");
                            channel = connectFuture.channel();
                            // 监听是否握手成功
                            connectionHandler.getHandshakeFuture().addListener((ChannelFutureListener) handshakeFuture -> {
                                // 5s内认证
                                sendAuth();
                            });
                        } else {
                            log.error("连接建立失败", connectFuture.cause());
                            this.onConnectFailed(connectionHandler);
                        }
                    });
                }, 5, TimeUnit.SECONDS);
            }

            @Override
            public void onConnectFailed(BilibiliConnectionHandler connectionHandler) {
                onDisconnected(connectionHandler);
            }
        };

        try {
            URI websocketURI = new URI("wss://broadcastlv.chat.bilibili.com:443/sub");

            connectionHandler = new BilibiliConnectionHandler(
                    WebSocketClientHandshakerFactory.newHandshaker(
                            websocketURI,
                            WebSocketVersion.V13,
                            null,
                            true,
                            new DefaultHttpHeaders()),
                    roomId, protover, connectionListener, cookie
            );
            BilibiliBinaryFrameHandler bilibiliHandler = new BilibiliBinaryFrameHandler(new IBilibiliSendSmsReplyMsgListener() {
                @Override
                public void onDanmuMsg(BilibiliBinaryFrameHandler binaryFrameHandler, DanmuMsgMsg msg) {
                    IBilibiliSendSmsReplyMsgListener.super.onDanmuMsg(binaryFrameHandler, msg);
                    JsonNode info = msg.getInfo();
                    JsonNode jsonNode1 = info.get(1);
                    String danmuText = jsonNode1.asText();
                    JsonNode jsonNode2 = info.get(2);
                    Long uid = jsonNode2.get(0).asLong();
                    String uname = jsonNode2.get(1).asText();
                    log.info("{} 收到弹幕 {}({})：{}", binaryFrameHandler.getRoomId(), uname, uid, danmuText);
                }

                @Override
                public void onSendGift(BilibiliBinaryFrameHandler binaryFrameHandler, SendSmsReplyMsg msg) {
                    JsonNode data = msg.getData();
                    String action = data.get("action").asText();
                    String giftName = data.get("giftName").asText();
                    Integer num = data.get("num").asInt();
                    String uname = data.get("uname").asText();
                    Integer price = data.get("price").asInt();
                    log.info("收到礼物 {} {} {}x{}({})", uname, action, giftName, num, price);
                }

                @Override
                public void onEnterRoom(BilibiliBinaryFrameHandler binaryFrameHandler, SendSmsReplyMsg msg) {
                    log.debug("普通用户进入直播间 {}", msg.getData().get("uname").asText());
                }

                @Override
                public void onEntryEffect(BilibiliBinaryFrameHandler binaryFrameHandler, SendSmsReplyMsg msg) {
                    JsonNode data = msg.getData();
                    String copyWriting = data.get("copy_writing").asText();
                    log.info("入场效果 {}", copyWriting);
                }

                @Override
                public void onWatchedChange(BilibiliBinaryFrameHandler binaryFrameHandler, SendSmsReplyMsg msg) {
                    JsonNode data = msg.getData();
                    int num = data.get("num").asInt();
                    String textSmall = data.get("text_small").asText();
                    String textLarge = data.get("text_large").asText();
                    log.debug("观看人数变化 {} {} {}", num, textSmall, textLarge);
                }

                @Override
                public void onClickLike(BilibiliBinaryFrameHandler binaryFrameHandler, SendSmsReplyMsg msg) {
                    JsonNode data = msg.getData();
                    String uname = data.get("uname").asText();
                    String likeText = data.get("like_text").asText();
                    log.debug("为主播点赞 {} {}", uname, likeText);
                }

                @Override
                public void onClickUpdate(BilibiliBinaryFrameHandler binaryFrameHandler, SendSmsReplyMsg msg) {
                    JsonNode data = msg.getData();
                    int clickCount = data.get("click_count").asInt();
                    log.debug("点赞数更新 {}", clickCount);
                }

                @Override
                public void onOtherCmdMsg(BilibiliCmdEnum cmd, BaseCmdMsg<BilibiliCmdEnum> cmdMsg) {
                    log.info("其他消息 {}", cmd);
                }

                @Override
                public void onUnknownCmd(String cmdString, BaseMsg cmdMsg) {
                    log.info("未知cmd {}", cmdString);
                }
            }, roomId);

            //进行握手
            log.info("握手开始");
//            SslContext sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
            SslContext sslCtx = SslContextBuilder.forClient().build();

            BilibiliConnectionHandler finalConnectionHandler = connectionHandler;
            bootstrap.group(workerGroup)
                    // 创建Channel
                    .channel(NioSocketChannel.class)
                    .remoteAddress(websocketURI.getHost(), websocketURI.getPort())
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    // Channel配置
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 责任链
                            ChannelPipeline pipeline = ch.pipeline();

                            //放到第一位 addFirst 支持wss链接服务端
                            pipeline.addFirst(sslCtx.newHandler(ch.alloc(), websocketURI.getHost(), websocketURI.getPort()));

                            // 添加一个http的编解码器
                            pipeline.addLast(new HttpClientCodec());
                            // 添加一个用于支持大数据流的支持
                            pipeline.addLast(new ChunkedWriteHandler());
                            // 添加一个聚合器，这个聚合器主要是将HttpMessage聚合成FullHttpRequest/Response
                            pipeline.addLast(new HttpObjectAggregator(1024 * 64));

//                            pipeline.addLast(WebSocketClientCompressionHandler.INSTANCE);
//                            pipeline.addLast(new WebSocketServerProtocolHandler("/sub", null, true, 65536 * 10));

                            // 连接处理器
                            pipeline.addLast(finalConnectionHandler);
                            pipeline.addLast(bilibiliHandler);
                        }
                    });

            channel = bootstrap.connect().sync().channel();
            // 阻塞等待是否握手成功
            connectionHandler.getHandshakeFuture().sync();
            log.error("连接成功，10s后将断开连接，模拟自动重连");
            workerGroup.schedule(() -> {
                channel.close();
            }, 10, TimeUnit.SECONDS);
            // 5s内认证
            sendAuth();

            channel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            connectionListener.onConnectFailed(connectionHandler);
//            throw new RuntimeException(e);
        }
    }

    private static void sendAuth() {
        log.debug("发送认证包");
        channel.writeAndFlush(BilibiliWebSocketFrameFactory.getInstance(roomId).createAuth(protover, cookie));
    }
}