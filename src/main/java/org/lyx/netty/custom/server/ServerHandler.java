package org.lyx.netty.custom.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.lyx.netty.custom.struct.Header;
import org.lyx.netty.custom.struct.NettyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerHandler extends ChannelInboundHandlerAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);
	/**
	 * 当我们通道进行激活的时候 触发的监听方法
	 */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		LOGGER.info("-->ServerHandler-->channelActive-->通道激活");
    }
	
    /**
     * 当我们的通道里有数据进行读取的时候 触发的监听方法
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx /*NETTY服务上下文*/, Object msg /*实际的传输数据*/) throws Exception {
		LOGGER.info("-->ServerHandler-->channelRead-->");
    	NettyMessage requestMessage = (NettyMessage)msg;

		LOGGER.info("服务器从客户端接收消息：{}", requestMessage.getBody());

    	NettyMessage responseMessage = new NettyMessage();
		Header header = new Header();
		header.setSessionID(2002L);
		header.setPriority((byte)2);
		header.setType((byte)1);
		responseMessage.setHeader(header);
		responseMessage.setBody("我是响应数据: " + requestMessage.getBody());
		ctx.writeAndFlush(responseMessage);
    	
    }
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		LOGGER.info("-->ServerHandler-->channelReadComplete-->数据读取完毕");
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		LOGGER.info("-->ServerHandler-->exceptionCaught-->服务器数据读异常");
    	cause.printStackTrace();
        ctx.close();
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
}
