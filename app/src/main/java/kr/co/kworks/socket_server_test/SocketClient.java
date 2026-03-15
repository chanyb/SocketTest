package kr.co.kworks.socket_server_test;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketClient {

    private static final String CMD_RECOGNITION = "recognition";
    private static final String CMD_DATETIME = "datetime";
    private static final String CMD_ACK = "ack";
    private static final String ACK_TYPE_RSP = "response";
    private static final String ACK_TYPE_RES = "result";

    private final String host;
    private final int port;
    private final Gson gson = new Gson();

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;

    private final ExecutorService ioExecutor = Executors.newScheduledThreadPool(5);
    private volatile boolean running = false;

    public interface ClientListener {
        default void onConnected() {}
        default void onDisconnected() {}
        default void onPacketReceived(String command, byte[] data) {}
        default void onResponseReceived(Ack ack) {}
        default void onRecognitionReceived(Recognition recognition) {}
        default void onError(Exception e) {}
    }

    private ClientListener listener;

    public SocketClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return socket != null
            && socket.isConnected()
            && !socket.isClosed();
    }

    public void connect() {
        if (running) return;

        running = true;
        ioExecutor.execute(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 5000);
                socket.setTcpNoDelay(true);

                inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                if (listener != null) listener.onConnected();

                readLoop();

            } catch (Exception e) {
                if (listener != null) listener.onError(e);
            } finally {
                running = false;
                closeInternal();
                if (listener != null) listener.onDisconnected();
            }
        });
    }

    private void readLoop() throws IOException {
        while (running && isConnected()) {
            int commandLength;
            try {
                commandLength = inputStream.readInt();
            } catch (IOException e) {
                throw e;
            }

            if (commandLength <= 0 || commandLength > 1024) {
                throw new IOException("Invalid command length: " + commandLength);
            }

            byte[] commandBytes = new byte[commandLength];
            inputStream.readFully(commandBytes);
            String command = new String(commandBytes, StandardCharsets.UTF_8);

            int dataLength = inputStream.readInt();
            if (dataLength < 0 || dataLength > (100 * 1024 * 1024)) {
                throw new IOException("Invalid data length: " + dataLength);
            }

            byte[] data = new byte[dataLength];
            if (dataLength > 0) {
                inputStream.readFully(data);
            }

            if (listener != null) {
                listener.onPacketReceived(command, data);
            }

            handleIncomingPacket(command, data);
        }
    }

    private void handleIncomingPacket(String command, byte[] data) {
        try {
            switch (command) {
                case CMD_ACK:
                    String jsonString = new String(data, StandardCharsets.UTF_8);
                    Ack ack = gson.fromJson(jsonString, Ack.class);
                    if (ack.type.equals(ACK_TYPE_RES)) {
                        if (listener != null) listener.onResponseReceived(ack);
                    } else if (ack.type.equals(ACK_TYPE_RSP)) {
                        if (ack.command.equals(CMD_RECOGNITION)) {
                            Recognition recognition = gson.fromJson(ack.message, Recognition.class);
                            if (listener != null) listener.onRecognitionReceived(recognition);
                            ack.type = ACK_TYPE_RSP;
                            ack.message = "success";
                            sendPacket(CMD_ACK, getByteArrayFromAck(ack));
                        }
                    }

                    break; // CMD_ACK BREAK;

            }
        } catch (Exception e) {
            if (listener != null) listener.onError(e);
        }
    }

    public void requestDatetime() {
        Logger.getInstance().info("requestDatetime()");
        sendPacket(CMD_DATETIME, new byte[0]);
    }

    public void sendRecognition(Recognition recognition) {
        try {
            String json = gson.toJson(recognition);
            sendPacket(CMD_RECOGNITION, json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            if (listener != null) listener.onError(e);
        }
    }

    public void sendAck(Ack ack) {
        String jsonString = gson.toJson(ack);
        byte[] bytes = ack == null
            ? new byte[0]
            : jsonString.getBytes(StandardCharsets.UTF_8);
        sendPacket(CMD_ACK, bytes);
    }

    public void sendText(String command, String message) {
        byte[] data = message == null
            ? new byte[0]
            : message.getBytes(StandardCharsets.UTF_8);
        sendPacket(command, data);
    }

    public void sendPacket(String command, byte[] data) {
        ioExecutor.execute(() -> {
            try {
                if (!isConnected()) {
                    throw new IOException("Socket is not connected");
                }

                byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);
                byte[] payload = data == null ? new byte[0] : data;

                synchronized (this) {
                    outputStream.writeInt(commandBytes.length);
                    outputStream.write(commandBytes);
                    outputStream.writeInt(payload.length);
                    if (payload.length > 0) {
                        outputStream.write(payload);
                    }
                    outputStream.flush();
                }

            } catch (Exception e) {
                Logger.getInstance().error("sendPacket: ", e);
                if (listener != null) listener.onError(e);
            }
        });
    }

    public void disconnect() {
        running = false;
        ioExecutor.execute(this::closeInternal);
    }

    private void closeInternal() {
        try {
            if (inputStream != null) inputStream.close();
        } catch (Exception ignored) {}

        try {
            if (outputStream != null) outputStream.close();
        } catch (Exception ignored) {}

        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}

        inputStream = null;
        outputStream = null;
        socket = null;
    }

    private byte[] getByteArrayFromAck(Ack ack) {
        String jsonString = gson.toJson(ack);
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }

    private Ack getAckFromByteArray(byte[] bytes) {
        String jsonString = new String(bytes, StandardCharsets.UTF_8);
        return gson.fromJson(jsonString, Ack.class);
    }
}