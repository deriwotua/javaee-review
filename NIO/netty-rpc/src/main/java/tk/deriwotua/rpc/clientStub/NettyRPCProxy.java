package tk.deriwotua.rpc.clientStub;

import tk.deriwotua.rpc.serverStub.ClassInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 客户端代理类
 */
public class NettyRPCProxy {
    /**
     * 根据接口创建代理对象
     *
     * @param target
     * @return
     */
    public static Object create(Class target) {
        return Proxy.newProxyInstance(target.getClassLoader(), new Class[]{target},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args)
                            throws Throwable {
                        //封装ClassInfo
                        ClassInfo classInfo = new ClassInfo();
                        classInfo.setClassName(target.getName());
                        classInfo.setMethodName(method.getName());
                        classInfo.setObjects(args);
                        // 方法存在重载 需要参数列表确定
                        classInfo.setTypes(method.getParameterTypes());

                        //开始用Netty发送数据
                        EventLoopGroup group = new NioEventLoopGroup();
                        ResultHandler resultHandler = new ResultHandler();
                        try {
                            Bootstrap b = new Bootstrap();
                            b.group(group)
                                    .channel(NioSocketChannel.class)
                                    .handler(new ChannelInitializer<SocketChannel>() {
                                        @Override
                                        public void initChannel(SocketChannel ch) throws Exception {
                                            ChannelPipeline pipeline = ch.pipeline();
                                            //编码器
                                            pipeline.addLast("encoder", new ObjectEncoder());
                                            //解码器  构造方法第一个参数设置二进制数据的最大字节数  第二个参数设置具体使用哪个类解析器
                                            pipeline.addLast("decoder", new ObjectDecoder(Integer.MAX_VALUE, ClassResolvers.cacheDisabled(null)));
                                            //客户端业务处理类
                                            pipeline.addLast("handler", resultHandler);
                                        }
                                    });
                            ChannelFuture future = b.connect("127.0.0.1", 9999).sync();
                            future.channel().writeAndFlush(classInfo).sync();
                            future.channel().closeFuture().sync();
                        } finally {
                            group.shutdownGracefully();
                        }
                        return resultHandler.getResponse();
                    }
                });
    }
}

