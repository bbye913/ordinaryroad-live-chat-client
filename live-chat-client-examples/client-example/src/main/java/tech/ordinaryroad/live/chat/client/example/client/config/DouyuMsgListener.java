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

package tech.ordinaryroad.live.chat.client.example.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import tech.ordinaryroad.live.chat.client.douyu.listener.IDouyuDouyuCmdMsgListener;
import tech.ordinaryroad.live.chat.client.douyu.msg.ChatmsgMsg;
import tech.ordinaryroad.live.chat.client.douyu.msg.DgbMsg;
import tech.ordinaryroad.live.chat.client.douyu.netty.handler.DouyuBinaryFrameHandler;

/**
 * @author mjz
 * @date 2023/9/3
 */
@Slf4j
@Primary
@Service
public class DouyuMsgListener implements IDouyuDouyuCmdMsgListener {

    @Override
    public void onDanmuMsg(DouyuBinaryFrameHandler binaryFrameHandler, ChatmsgMsg msg) {
        IDouyuDouyuCmdMsgListener.super.onDanmuMsg(binaryFrameHandler, msg);

        log.info("{} 收到弹幕 {}({})：{}", binaryFrameHandler.getRoomId(), msg.getNn(), msg.getUid(), msg.getTxt());
    }

    @Override
    public void onGiftMsg(DouyuBinaryFrameHandler binaryFrameHandler, DgbMsg msg) {
        IDouyuDouyuCmdMsgListener.super.onGiftMsg(binaryFrameHandler, msg);

        String username = msg.getNn();
        String uid = msg.getUid();
        // 礼物ID
        String gfid = msg.getGfid();
        // 礼物个数
        String gfcnt = msg.getGfcnt();
        // 礼物价格
        int price = -1;
        log.info("{} 收到礼物 {}({}) {} {}({})x{}({})", binaryFrameHandler.getRoomId(), username, uid, "赠送", "`礼物名未知`", gfid, gfcnt, price);
    }

}
