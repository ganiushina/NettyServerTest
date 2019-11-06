import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import io.netty.util.CharsetUtil;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class ClientController {
    private Logger logger = LoggerFactory.getLogger(Client.class );



    @FXML
    TextField tfHost;

    @FXML
    TextField tfPort;

    @FXML
    Button btnConnect;

    @FXML
    Button btnDisconnect;

    @FXML
    Button toServer;

    @FXML
    Button toClient;

    @FXML
    HBox hboxStatus;

    @FXML
    ProgressIndicator piStatus;

    @FXML
    Label lblStatus;

    @FXML
    ListView<String> filesClientsList;

    @FXML
    ListView<String> filesServerList;

    private BooleanProperty connected = new SimpleBooleanProperty(false);
    private StringProperty receivingMessageModel = new SimpleStringProperty("");
    private Channel channel;
    private EventLoopGroup group;

    @FXML
    public void initialize() {

        hboxStatus.setVisible(false);

        btnConnect.disableProperty().bind( connected );
        tfHost.disableProperty().bind( connected );
        tfPort.disableProperty().bind( connected );
        btnDisconnect.disableProperty().bind( connected.not() );
        toServer.setMaxWidth(Double.MAX_VALUE);
        toClient.setMaxWidth(Double.MAX_VALUE);

        initializeFilesTable();
    }

    private void initializeFilesTable() {

        Task<Void> task = new Task<Void>(){
            @Override
            protected Void call() throws Exception {

                try {
                    filesClientsList.getItems().clear();
                    filesServerList.getItems().clear();
                    Files.list(Paths.get("client_storage")).map(p -> p.getFileName().toString()).forEach(o -> filesClientsList.getItems().add(o));
                    Files.list(Paths.get("server_storage")).map(p -> p.getFileName().toString()).forEach(o -> filesServerList.getItems().add(o));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();

    }


    @FXML
    public void connect() {

        if( connected.get() ) {
            if( logger.isWarnEnabled() ) {
                logger.warn("client already connected; skipping connect");
            }
            return;  // already connected; should be prevented with disabled
        }

        String host = tfHost.getText();
        int port = Integer.parseInt(tfPort.getText());

        group = new NioEventLoopGroup();

        Task<Channel> task = new Task<Channel>() {

            @Override
            protected Channel call() throws Exception {

                updateMessage("Bootstrapping");
                updateProgress(0.1d, 1.0d);

                Bootstrap b = new Bootstrap();
                b
                        .group(group)
                        .channel(NioSocketChannel.class)
                        .remoteAddress( new InetSocketAddress(host, port) )
                        .handler( new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast(new ClientHandler(receivingMessageModel));
                            }
                        });

                ChannelFuture f = b.connect();
                Channel chn = f.channel();

                updateMessage("Connecting");
                updateProgress(0.2d, 1.0d);

                f.sync();

                return chn;
            }

            @Override
            protected void succeeded() {

                channel = getValue();
                connected.set(true);
            }

            @Override
            protected void failed() {

                Throwable exc = getException();
                logger.error( "client connect error", exc );
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Client");
                alert.setHeaderText( exc.getClass().getName() );
                alert.setContentText( exc.getMessage() );
                alert.showAndWait();

                connected.set(false);
            }
        };

        hboxStatus.visibleProperty().bind( task.runningProperty() );
        lblStatus.textProperty().bind( task.messageProperty() );
        piStatus.progressProperty().bind(task.progressProperty());

        new Thread(task).start();
    }

    @FXML
    public void disconnect() {

        if( !connected.get() ) {
            if( logger.isWarnEnabled() ) {
                logger.warn("client not connected; skipping disconnect");
            }
            return;
        }

        if( logger.isDebugEnabled() ) {
            logger.debug("[DISCONNECT]");
        }

        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() throws Exception {

                updateMessage("Disconnecting");
                updateProgress(0.1d, 1.0d);

                channel.close().sync();

                updateMessage("Closing group");
                updateProgress(0.5d, 1.0d);
                group.shutdownGracefully().sync();

                return null;
            }

            @Override
            protected void succeeded() {

                connected.set(false);
            }

            @Override
            protected void failed() {

                connected.set(false);

                Throwable t = getException();
                logger.error( "client disconnect error", t );
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Client");
                alert.setHeaderText( t.getClass().getName() );
                alert.setContentText( t.getMessage() );
                alert.showAndWait();

            }

        };

        hboxStatus.visibleProperty().bind( task.runningProperty() );
        lblStatus.textProperty().bind( task.messageProperty() );
        piStatus.progressProperty().bind(task.progressProperty());

        new Thread(task).start();
    }

    public void sendToServer(ActionEvent actionEvent) throws IOException {
//        Socket socket = new Socket("localhost", 8189);
//        ObjectEncoderOutputStream out = new ObjectEncoderOutputStream(socket.getOutputStream());
//        ObjectDecoderInputStream in = new ObjectDecoderInputStream(socket.getInputStream(), 100 * 1024 * 1024);
//        out.writeObject(new FileRequest(filesServerList.getSelectionModel().getSelectedItem()));
//        new Thread(() -> {
//            try {
//                while (true) {
//                    Object input = in.readObject();
//                    FileMessage fm = (FileMessage) input;
//                    boolean append = true;
//
//                    FileOutputStream fos = new FileOutputStream(fm.getFilename(), append);
//                    fos.write(fm.getData());
//                    fos.close();
//
//                }
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();
//        in.close();
//        out.close();
       System.out.println("Client file : " + filesServerList.getSelectionModel().getSelectedItem());
    }

    public void sendToClient(ActionEvent actionEvent) {
        System.out.println("Server file : " + filesClientsList.getSelectionModel().getSelectedItem());
    }
}
