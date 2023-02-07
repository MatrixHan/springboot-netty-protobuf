package com.netty.client.handler;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelFuture;
import org.apache.log4j.Logger;


import com.netty.client.NettyClient;
import com.netty.common.protobuf.Command.CommandType;
import com.netty.common.protobuf.Message.MessageBase;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class IdleClientHandler extends SimpleChannelInboundHandler<MessageBase> {
	public Logger log = Logger.getLogger(this.getClass());	

	private NettyClient nettyClient;
	private int heartbeatCount = 0;
	private final static String CLIENTID = "123456789";

	/**
	 * @param nettyClient
	 */
	public IdleClientHandler(NettyClient nettyClient) {
		this.nettyClient = nettyClient;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) evt;
			String type = "";
			if (event.state() == IdleState.READER_IDLE) {
				type = "read idle";
			} else if (event.state() == IdleState.WRITER_IDLE) {
				type = "write idle";
			} else if (event.state() == IdleState.ALL_IDLE) {
				type = "all idle";
			}
			log.debug(ctx.channel().remoteAddress() + "超时类型：" + type);
			sendPingMsg(ctx);
		} else {
			super.userEventTriggered(ctx, evt);
		}
	}

	/**
	 * 发送ping消息
	 * @param context
	 */
	protected void sendPingMsg(ChannelHandlerContext context) {

		MessageBase.Builder pingMsg = MessageBase.newBuilder();
		pingMsg.setClientId(CLIENTID);
		pingMsg.setCmd(CommandType.PING);
		pingMsg.setData("This is a ping msg");
		ChannelFuture future =  context.writeAndFlush(
				pingMsg.build()
				);
		if(!future.isSuccess())
		{
			log.error("send ping package error."+future.cause());

			return;
		}
		heartbeatCount++;
		log.info("Client sent ping msg to " + context.channel().remoteAddress() + ", count: " + heartbeatCount);
	}

	/**
	 * 处理断开重连
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		final EventLoop eventLoop = ctx.channel().eventLoop();  
		eventLoop.schedule(() -> nettyClient.doConnect(new Bootstrap(), eventLoop), 10L, TimeUnit.SECONDS);  
		super.channelInactive(ctx);  
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, MessageBase msg) throws Exception {

	}
}
