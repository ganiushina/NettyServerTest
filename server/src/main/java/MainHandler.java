import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class MainHandler extends SimpleChannelInboundHandler<String> {
    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        File file = new File(msg);
        if (file.exists()) {
            if (!file.isFile()) {
                ctx.writeAndFlush("Not a file: " + file + '\n');
                return;
            }
            ctx.write(file + " " + file.length() + '\n');
            FileRegion region = new DefaultFileRegion(new FileInputStream(file).getChannel(), 0, file.length());
            ctx.write(region);
            ctx.writeAndFlush("\n");
        } else {
            ctx.writeAndFlush("File not found: " + file + '\n');
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}

//public class MainHandler extends ChannelInboundHandlerAdapter {
////@ChannelHandler.Sharable
////public class MainHandler extends SimpleChannelInboundHandler<ByteBuf> {
////
////    private Logger logger = LoggerFactory.getLogger( MainHandler.class );
////
////    @Override
////    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
////
////        String in_s = in.toString(CharsetUtil.UTF_8);
////        String uc = in_s.toUpperCase();
////        if( logger.isInfoEnabled() ) {
////            logger.info("[READ] read " + in_s + ", writing " + uc);
////        }
////        in.setBytes(0,  uc.getBytes(CharsetUtil.UTF_8));
////        ctx.write(in);
////    }
////
////    @Override
////    public void channelActive(ChannelHandlerContext ctx) throws Exception {
////        super.channelActive(ctx);
////        if(logger.isDebugEnabled() ) {
////            logger.debug("[CHANNEL ACTIVE]");
////        }
////        ctx.channel().closeFuture().addListener(f -> logger.debug("[CLOSE]"));
////    }
////
////    @Override
////    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
////        logger.error( "error in echo server", cause);
////        ctx.close();
////    }
//
//    @Override
//    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//        try {
//            if (msg instanceof FileRequest) {
//                FileRequest fr = (FileRequest) msg;
//                if (Files.exists(Paths.get("server_storage/" + fr.getFilename()))) {
//                    FileMessage fm = new FileMessage(Paths.get("server_storage/" + fr.getFilename()));
//                    ctx.writeAndFlush(fm);
//                }
//            }
//        } finally {
//            ReferenceCountUtil.release(msg);
//        }
//    }
//
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        cause.printStackTrace();
//        ctx.close();
//    }
//}
