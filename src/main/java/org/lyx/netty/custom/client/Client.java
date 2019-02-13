package org.lyx.netty.custom.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.lyx.netty.NettyConstant;
import org.lyx.netty.custom.codec.NettyMessageDecoder;
import org.lyx.netty.custom.codec.NettyMessageEncoder;
import org.lyx.netty.custom.struct.Header;
import org.lyx.netty.custom.struct.NettyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {

	private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

	private volatile Channel channel;

	public static void main(String[] args) throws Exception {
		Client client = new Client();
		Thread hook = client.new ShutDown();
		hook.setName("Shutdown");
		Runtime.getRuntime().addShutdownHook(hook);

		client.sendMsg();

		client.connect(NettyConstant.PORT, NettyConstant.REMOTEIP);
	}

	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	//创建工作线程组
	EventLoopGroup group = new NioEventLoopGroup();

	public void connect(int port, String host) throws Exception {
		// 配置客户端NIO线程组
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							// 进站
							ch.pipeline().addLast(new NettyMessageDecoder(1024 * 1024, 4, 4));
							// 出站
							ch.pipeline().addLast(new NettyMessageEncoder());
							ch.pipeline().addLast("readTimeoutHandler", new ReadTimeoutHandler(60 * 9));
							ch.pipeline().addLast("LoginAuthHandler", new LoginAuthReqHandler());
							ch.pipeline().addLast("HeartBeatHandler", new HeartBeatReqHandler());
							ch.pipeline().addLast(new ClientHandler());
						}
					});
			// 发起异步连接操作
			ChannelFuture future = b.connect(new InetSocketAddress(host, port),
					new InetSocketAddress(NettyConstant.LOCALIP, NettyConstant.LOCAL_PORT)).sync();

			channel = future.channel();

			//手动发测试数据，验证是否会产生TCP粘包/拆包情况
//			Channel c = future.channel();
//
//			for (int i = 0; i < 500; i++) {
//				NettyMessage message = new NettyMessage();
//				Header header = new Header();
//				header.setSessionID(1001L);
//				header.setPriority((byte) 1);
//				header.setType((byte) 0);
//				message.setHeader(header);
//				message.setBody("我是请求数据" + i);
//				c.writeAndFlush(message);
//			}

			future.channel().closeFuture().sync();
		} finally {
			// 所有资源释放完成之后，清空资源，再次发起重连操作
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						TimeUnit.SECONDS.sleep(1);
						try {
							connect(NettyConstant.PORT, NettyConstant.REMOTEIP);// 发起重连操作
						} catch (Exception e) {
							e.printStackTrace();
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
	public class ShutDown extends Thread{
		// 退出执行
		@Override
		public void run(){
			LOGGER.info("shutdown [OK]");
		}
	}

	void sendMsg() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				for (;;) {
					if (channel != null) {
						NettyMessage message = new NettyMessage();
						Header header = new Header();
						header.setSessionID(1001L);
						header.setPriority((byte) 1);
						header.setType((byte) 0);
						message.setHeader(header);
						message.setBody("我是请求数据" + new Random().nextInt(1000));
						channel.writeAndFlush(message);
						LOGGER.info("请求完成....");
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}).start();
	}

}
