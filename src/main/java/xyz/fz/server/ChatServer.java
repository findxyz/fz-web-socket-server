package xyz.fz.server;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;
import xyz.fz.server.util.BaseProperties;
import xyz.fz.server.util.RSAUtil;

import java.util.List;

public class ChatServer {

    private static final String priKey = BaseProperties.get("chat.pri.key");

    private static boolean tokenCheck(String enToken) throws Exception {
        String deToken = new String(RSAUtil.decryptByPrivateKey(Base64.decodeBase64(enToken), priKey), "utf-8");
        return deToken.equals(DateTime.now().toString("yyyyMMdd"));
    }

    public static void startServer() throws InterruptedException {
        Configuration config = new Configuration();
        config.setHostname(BaseProperties.get("chat.host"));
        config.setPort(Integer.parseInt(BaseProperties.get("chat.port")));

        final SocketIOServer server = new SocketIOServer(config);

        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient socketIOClient) {
                List<String> tokenList = socketIOClient.getHandshakeData().getUrlParams().get("token");
                if (tokenList != null && tokenList.size() > 0) {
                    String token = tokenList.get(0);
                    try {
                        if (tokenCheck(token)) {
                            System.out.println("socket connected..." + socketIOClient.getSessionId());
                            return;
                        }
                    } catch (Exception ignore) {
                        ignore.printStackTrace();
                    }
                }
                socketIOClient.disconnect();
            }
        });

        server.addEventListener("message", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) throws Exception {
                server.getBroadcastOperations().sendEvent("message", Base64.encodeBase64URLSafeString(RSAUtil.encryptByPrivateKey(data.getBytes("utf-8"), priKey)));
            }
        });

        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient socketIOClient) {
                System.out.println("socket disconnected..." + socketIOClient.getSessionId());
            }
        });

        server.start();

        Thread.sleep(Long.MAX_VALUE);

        server.stop();
    }

    public static void main(String[] args) throws InterruptedException {
        startServer();
    }
}
