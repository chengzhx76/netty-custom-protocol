package org.lyx.netty.custom.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.lyx.netty.MessageType;
import org.lyx.netty.custom.struct.Header;
import org.lyx.netty.custom.struct.NettyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 由于采用长连接通信，在正常业务运行期间，双方通过心跳和业务消息维持链路，任何一方都不需要主动关闭链接。<br/>
 * 但是在以下情况，客户端和服务端都需要关闭连接。<br/>
 * 1.当对方宕机或者重启时，会主动关闭链接，另一方读取到操作系统的通知信号，得知对方REST链路，需要关闭连接，释放自身的句柄等资源，<br/>
 * 由于采用全双工通信，双方都需要关闭连接，节省资源<br/>
 * 2.消息读写过程中发生IO异常，需要主动关闭连接。<br/>
 * 3.心跳消息读写过程发送IO异常，需要主动关闭连接。<br/>
 * 4.心跳超时，需要主动关闭连接。<br/>
 * 5.发生编码异常，需要主动关闭连接。<br/>
 * 
 * @author landyChris
 * @date 2017年8月31日
 * @version 1.0
 */
public class HeartBeatReqHandler extends ChannelInboundHandlerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatReqHandler.class);

	private volatile ScheduledFuture<?> heartBeat;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//		LOGGER.info("-->HeartBeatReqHandler-->channelRead");
		NettyMessage message = (NettyMessage) msg;
		// 握手成功，主动发送心跳消息
		if (message.getHeader() != null && message.getHeader().getType() == MessageType.LOGIN_RESP.value()) {
			heartBeat = ctx.executor().scheduleAtFixedRate(new HeartBeatReqHandler.HeartBeatTask(ctx), 0, 30 * 9, TimeUnit.SECONDS);
		} else if (message.getHeader() != null && message.getHeader().getType() == MessageType.HEARTBEAT_RESP.value()) {
			LOGGER.info("Client receive server heart beat message : ---> {}", message);
		} else {
			ctx.fireChannelRead(msg);
		}
	}

	/**
	 * 发送心跳消息的任务线程
	 * @author landyChris
	 */
	private class HeartBeatTask implements Runnable {
		private final ChannelHandlerContext ctx;

		public HeartBeatTask(final ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void run() {
//            LOGGER.info("-->HeartBeatReqHandler-->HeartBeatTask-->run->发送心跳包");
			NettyMessage heatBeat = buildHeatBeat();
//			LOGGER.info("客户端发送心跳包到服务端 : ---> {}", heatBeat);
			ctx.writeAndFlush(heatBeat);
		}

		private NettyMessage buildHeatBeat() {
//            LOGGER.info("-->HeartBeatReqHandler-->HeartBeatTask-->构建心跳包");
			NettyMessage message = new NettyMessage();
			Header header = new Header();
			header.setType(MessageType.HEARTBEAT_REQ.value());
			message.setHeader(header);
			return message;
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        LOGGER.info("-->HeartBeatReqHandler-->exceptionCaught");
		cause.printStackTrace();
		//断连期间，心跳定时器停止工作，不再发送心跳请求信息
		if (heartBeat != null) {
			heartBeat.cancel(true);
			heartBeat = null;
		}
		ctx.fireExceptionCaught(cause);
	}
}
