package kr.co.kworks.socket_server_test;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import kr.co.kworks.socket_server_test.model.Ack;
import kr.co.kworks.socket_server_test.model.Fire;
import kr.co.kworks.socket_server_test.model.Recognition;
import kr.co.kworks.socket_server_test.model.WeatherSensor;


public class SocketServer extends Thread {

    private static final String CMD_ACK = "ack";
    private static final String CMD_RECOGNITION = "setRecognition";
    private static final String CMD_DATETIME = "getDatetime";
    private static final String CMD_READY_FIRE = "readyFire";
    private static final String CMD_DO_FIRE = "doFire";
    private static final String CMD_WS = "getWeatherSensor";

    private static final String ACK_TYPE_RSP = "response";
    private static final String ACK_TYPE_RES = "result";
    private static final String ACK_MESSAGE_SUCCESS = "success";
    private static final String ACK_MESSAGE_FAIL = "fail";

    private final int port;
    private volatile boolean running = true;

    private String previousCommand;
    private final Gson gson;
    private MainViewModel mainViewModel;
    private CalendarHandler calendarHandler;
    private Handler mHandler;

    private Selector selector;
    private ServerSocketChannel serverChannel;

    private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(64 * 1024);
    private final Map<SocketChannel, ClientState> clients = new ConcurrentHashMap<>();

    public SocketServer(int port, MainViewModel mainViewModel) {
        this.port = port;
        this.mainViewModel = mainViewModel;
        gson = new Gson();
        calendarHandler = new CalendarHandler();
        mHandler = new Handler(Looper.getMainLooper());
    }

    public interface ServerListener {
        default void onClientConnected(SocketChannel ch) {}
        default void onClientDisconnected(SocketChannel ch, Throwable cause) {}
    }

    private volatile ServerListener listener;
    public void setListener(ServerListener l) { this.listener = l; }

    @Override public void run() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            while (running) {
                selector.select(1000); // 1s 타임아웃 (깨우기용)
//                mainViewModel.commands.postValue("...");
                for (Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext();) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        Logger.getInstance().info("acceptable");
                        accept();
                        continue;
                    }
                    if (key.isReadable())   {
                        Logger.getInstance().info("readable");
                        read((SocketChannel) key.channel());
                        continue;
                    }
                    if (key.isWritable())   {
                        Logger.getInstance().info("writeable");
                        write((SocketChannel) key.channel());
                        continue;
                    }
                }
                // 좀비 세션 처리
                sweepIdleClients(180_000); // 3분 이상 idle 정리
            }
        } catch (IOException e) {
            // 로그 처리
            Logger.getInstance().error("SocketSever: ", e);
        } catch (Exception e) {
            Logger.getInstance().error("SocketSever: ", e);
        } finally {
            closeAll();
        }

    }

    private void accept() throws IOException {
        SocketChannel channel = serverChannel.accept();
        if (channel == null) return;
        channel.configureBlocking(false);
        channel.setOption(StandardSocketOptions.TCP_NODELAY, true);
        channel.register(selector, SelectionKey.OP_READ);
        clients.put(channel, new ClientState());

        if (listener != null) {
            listener.onClientConnected(channel);
        }
    }

    private void read(SocketChannel channel) {
        ClientState clientState = clients.get(channel);
        if (clientState == null) {
            close(channel);
            return;
        }

        readBuffer.clear();
        try {
            int n = channel.read(readBuffer);
            if (n == -1) {
                close(channel);
                return;
            }
            if (n == 0) {
                return;
            }

            readBuffer.flip();
            clientState.appendAndParse(readBuffer, packet -> {
                clientState.touch();
                handlePacket(channel, packet);
            });
            clientState.touch();
        } catch (Exception e) {
            Logger.getInstance().error("read error: ", e);
            safeClose(channel);
        }
    }

    private void handlePacket(SocketChannel channel, Packet packet) {
        String command = packet.command == null ? "" : packet.command.trim();
        byte[] data = packet.data;

        switch (command) {
            case CMD_ACK:
                handleAck(channel, data);
                break;

            case CMD_RECOGNITION:
                handleRecognition(channel, data);
                break;

            case CMD_DATETIME:
                handleDatetime(channel, data);
                break;

            case CMD_READY_FIRE:
                handleReadyFire(channel, data);
                break;

            case CMD_WS:
                handleWeatherSensor(channel, data);
                break;

            default:
                Logger.getInstance().info("unknown command: " + command);
                Ack ack = new Ack();
                ack.command = command;
                ack.message = "unknown command";
                ack.type = ACK_TYPE_RES;
                enqueuePacket(channel, CMD_ACK, getByteArrayFromAck(ack));
                break;
        }
    }

    private void handleRecognition(SocketChannel channel, byte[] data) {
        Ack ack = new Ack();
        ack.type = ACK_TYPE_RES;
        ack.command = CMD_RECOGNITION;
        ack.message = ACK_MESSAGE_FAIL;

        try {
            String json = new String(data, StandardCharsets.UTF_8);
            Recognition recognition = gson.fromJson(json, Recognition.class);
            ack.commandId = recognition.id;

            // 필요하면 ViewModel 전달
            mHandler.post(() -> {
                mainViewModel.commands.setValue("[in] recognition " + recognition.toString());
            });

            ack.message = ACK_MESSAGE_SUCCESS;
            enqueuePacket(channel, CMD_ACK, getByteArrayFromAck(ack));

        } catch (Exception e) {
            Logger.getInstance().error("handleRecognition error: ", e);
            enqueuePacket(channel, CMD_ACK, getByteArrayFromAck(ack));
        }
    }

    private void handleDatetime(SocketChannel channel, byte[] data) {
        mHandler.post(() -> {
            mainViewModel.commands.setValue("[in] " + CMD_DATETIME);
        });

        try {
            String current = calendarHandler.getCurrentDatetimeString();
            String commandId = getStringFromByteArray(data);
            Ack ack = new Ack();
            ack.command = CMD_DATETIME;
            ack.message = current;
            ack.commandId = commandId;
            ack.type = ACK_TYPE_RSP;

            enqueuePacket(channel, CMD_ACK, getByteArrayFromAck(ack));
        } catch (Exception e) {
            Logger.getInstance().error("handleDatetime error: ", e);
            enqueueTextPacket(channel, "error", "datetime error");
        }
    }

    private void handleReadyFire(SocketChannel channel, byte[] data) {
        Ack ack = new Ack();
        ack.type = ACK_TYPE_RES;
        ack.command = CMD_READY_FIRE;
        ack.message = ACK_MESSAGE_FAIL;

        try {
            String json = new String(data, StandardCharsets.UTF_8);
            Recognition recognition = gson.fromJson(json, Recognition.class);
            ack.commandId = recognition.id;

            // 필요하면 ViewModel 전달
            mHandler.post(() -> {
                mainViewModel.commands.setValue("[in] recognition " + recognition.toString());
            });

            ack.message = ACK_MESSAGE_SUCCESS;
            enqueuePacket(channel, CMD_ACK, getByteArrayFromAck(ack));

        } catch (Exception e) {
            Logger.getInstance().error("handleRecognition error: ", e);
            enqueuePacket(channel, CMD_ACK, getByteArrayFromAck(ack));
        }
    }

    private void handleWeatherSensor(SocketChannel channel, byte[] data) {
        Ack ack = new Ack();
        ack.type = ACK_TYPE_RSP;
        ack.command = CMD_WS;
        ack.message = ACK_MESSAGE_FAIL;

        try {

            // 필요하면 ViewModel 전달
            mHandler.post(() -> {
                mainViewModel.commands.setValue("[in] " + CMD_WS);
            });

            WeatherSensor ws = new WeatherSensor();
            ack.message = gson.toJson(ws);
            enqueuePacket(channel, CMD_ACK, getByteArrayFromAck(ack));

        } catch (Exception e) {
            Logger.getInstance().error("handleRecognition error: ", e);
            enqueuePacket(channel, CMD_ACK, getByteArrayFromAck(ack));
        }
    }

    private void sendDoFire(SocketChannel channel, Fire fire) {
        String jsonString = gson.toJson(fire);
        enqueuePacket(channel, CMD_DO_FIRE, jsonString.getBytes(StandardCharsets.UTF_8));
    }

    public void enqueuePacket(SocketChannel ch, String command, byte[] data) {
        String str = getStringFromByteArray(data);
        mHandler.post(() -> {
            mainViewModel.commands.postValue("[out] " + "ack " + str);
        });
        ClientState st = clients.get(ch);
        if (st == null) return;

        byte[] cmdBytes = command.getBytes(StandardCharsets.UTF_8);
        if (data == null) data = new byte[0];

        ByteBuffer frame = ByteBuffer.allocate(4 + cmdBytes.length + 4 + data.length);
        frame.putInt(cmdBytes.length);
        frame.put(cmdBytes);
        frame.putInt(data.length);
        frame.put(data);
        frame.flip();

        synchronized (st.outQueue) {
            st.outQueue.add(frame);
        }

        SelectionKey key = ch.keyFor(selector);
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
        if (selector != null) selector.wakeup();
    }

    public void enqueueTextPacket(SocketChannel ch, String command, String message) {
        byte[] data = message == null ? new byte[0] : message.getBytes(StandardCharsets.UTF_8);
        enqueuePacket(ch, command, data);
    }

    public static boolean isAsciiText(byte[] data) {
        if (data == null || data.length == 0) return false;

        int printable = 0;
        for (int i = 0; i < data.length && i < 20; i++) {
            int b = data[i] & 0xFF;
            if (b == 0) return false; // NUL 있으면 바이너리 가능성 높음
            if (b >= 0x20 && b <= 0x7E) { // ASCII printable
                printable++;
            }
        }
        double ratio = (double) printable / data.length;
        return ratio > 0.99; // 99% 이상이 프린터블이면 텍스트
    }


    /** payload가 텍스트처럼 보이는지 판정 */
    private static boolean isLikelyText(byte[] data) {
        if (data.length == 0) return false;
        int printable = 0, checked = 0;
        for (int i = 0; i < data.length && i < 20; i++) { // 처음 512바이트만 검사
            int b = data[i] & 0xff;
            if (b == 0) return false; // NUL 있으면 바이너리 가능성 높음
            if (b >= 0x09 && b <= 0x0d) { checked++; printable++; continue; } // \t..\r
            if (b >= 0x20 && b <= 0x7e) { checked++; printable++; continue; } // ASCII printable
            checked++;
        }
        return checked > 0 && (printable * 1.0 / checked) > 0.85;
    }

    public Bitmap getBitmapFromByteArray(byte[] bytes) {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /** 문자열/바이너리 모두 length 프레임으로 큐잉 */
    private void enqueueString(SocketChannel ch, String msg) {
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        enqueueInt(ch, bytes.length);
        enqueueBinary(ch, bytes);
    }

    private void enqueueInt(SocketChannel ch, int integer) {
        Logger.getInstance().info("enqueueInt");
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (integer >> 24); // 최상위 바이트
        bytes[1] = (byte) (integer >> 16);
        bytes[2] = (byte) (integer >> 8);
        bytes[3] = (byte) (integer);       // 최하위 바이트
        enqueueBinary(ch, bytes);
    }

    public void enqueueBinary(SocketChannel ch, byte[] payload) {
        ClientState st = clients.get(ch);
        if (st == null) return;
        // 프레임: 4B length + payload
        ByteBuffer frame = ByteBuffer.allocate(4 + payload.length);
        frame.putInt(payload.length);
        frame.put(payload);
        frame.flip();
        synchronized (st.outQueue) {
            st.outQueue.add(frame);
        }
        SelectionKey key = ch.keyFor(selector);
        if (key != null && key.isValid()) key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        if (selector != null) selector.wakeup();
    }

    private void write(SocketChannel channel) {
        ClientState st = clients.get(channel);
        if (st == null) { close(channel); return; }

        try {
            // pendingWrite가 있으면 먼저 처리
            if (st.pendingWrite != null) {
                channel.write(st.pendingWrite);
                if (st.pendingWrite.hasRemaining()) {
                    // 아직 못 보냄 → 다음 라운드
                    return;
                } else {
                    st.pendingWrite = null;
                }
            }

            // 큐에서 계속 꺼내서 보냄
            while (true) {
                ByteBuffer buf;
                synchronized (st.outQueue) {
                    buf = st.outQueue.poll();
                }
                if (buf == null) break;

                channel.write(buf);
                if (buf.hasRemaining()) {
                    // 다음 라운드에 마저 보냄
                    st.pendingWrite = buf;
                    break;
                }
            }
            st.touch();

            // 더 보낼 게 없으면 WRITE 관심 해제
            SelectionKey key = channel.keyFor(selector);
            if (key != null && key.isValid()
                && st.pendingWrite == null
                && st.outQueue.isEmpty()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
            Logger.getInstance().error("write error: ", e);
            close(channel);
        }
    }

    private void sweepIdleClients(long idleMillis) {
        long now = System.currentTimeMillis();
        for (Map.Entry<SocketChannel, ClientState> e : clients.entrySet()) {
            if (now - e.getValue().lastActive > idleMillis) {
                Logger.getInstance().info("idle close: " + e.getKey());
                close(e.getKey());
            }
        }
        mainViewModel.clientCount.postValue(clients.size());
    }

    private void close(SocketChannel channel) {
        clients.remove(channel);
        try { channel.close(); } catch (IOException ignored) {}
    }

    private void safeClose(SocketChannel channel) {
        try {
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                try { key.cancel(); } catch (Exception ignored) {}
            }
            clients.remove(channel);
            try { channel.close(); } catch (Exception ignored) {}
        } finally {
            // selector가 즉시 취소된 키를 반영하게
            if (selector != null) selector.wakeup();
        }
    }

    private void closeAll() {
        Logger.getInstance().info("closeAll");
        try { if (serverChannel != null) serverChannel.close(); } catch (IOException ignored) {}
        try { if (selector != null) selector.close(); } catch (IOException ignored) {}
        for (SocketChannel channel : clients.keySet()) close(channel);
        clients.clear();
    }

    public void shutdown() {
        running = false;
        if (selector != null) selector.wakeup();
    }

    static class ClientState {
        private ByteBuffer byteBuffer = ByteBuffer.allocate(64 * 1024);

        private int commandLength = -1;
        private byte[] commandBytes = null;
        private int dataLength = -1;

        final ArrayDeque<ByteBuffer> outQueue = new ArrayDeque<>();
        ByteBuffer pendingWrite = null;

        volatile long lastActive = System.currentTimeMillis();
        void touch() { lastActive = System.currentTimeMillis(); }

        interface PacketHandler {
            void onPacket(Packet packet);
        }

        void appendAndParse(ByteBuffer src, PacketHandler handler) {
            ensureCapacity(src.remaining());
            byteBuffer.put(src);
            byteBuffer.flip();

            while (true) {
                // 1) command length
                if (commandLength < 0) {
                    if (byteBuffer.remaining() < 4) break;
                    commandLength = byteBuffer.getInt();

                    if (commandLength <= 0 || commandLength > 1024) {
                        throw new IllegalStateException("Bad command length: " + commandLength);
                    }
                }

                // 2) command bytes
                if (commandBytes == null) {
                    if (byteBuffer.remaining() < commandLength) break;
                    commandBytes = new byte[commandLength];
                    byteBuffer.get(commandBytes);
                }

                // 3) data length
                if (dataLength < 0) {
                    if (byteBuffer.remaining() < 4) break;
                    dataLength = byteBuffer.getInt();

                    if (dataLength < 0 || dataLength > (100 * 1024 * 1024)) {
                        throw new IllegalStateException("Bad data length: " + dataLength);
                    }
                }

                // 4) data bytes
                if (byteBuffer.remaining() < dataLength) break;

                byte[] dataBytes = new byte[dataLength];
                byteBuffer.get(dataBytes);

                String command = new String(commandBytes, StandardCharsets.UTF_8);
                handler.onPacket(new Packet(command, dataBytes));

                // 다음 프레임을 위해 상태 초기화
                commandLength = -1;
                commandBytes = null;
                dataLength = -1;
            }

            byteBuffer.compact();
        }

        private void ensureCapacity(int need) {
            if (byteBuffer.remaining() >= need) return;

            byteBuffer.flip();
            int newCap = Math.max(byteBuffer.capacity() * 2, byteBuffer.limit() + need);
            ByteBuffer bigger = ByteBuffer.allocate(newCap);
            bigger.put(byteBuffer);
            byteBuffer = bigger;
        }
    }

    static class Packet {
        final String command;
        final byte[] data;

        Packet(String command, byte[] data) {
            this.command = command;
            this.data = data;
        }
    }

    private void handleAck(SocketChannel socketChannel, byte[] data) {
        Ack ack = getAckFromByteArray(data);

        switch (ack.type) {
            case ACK_TYPE_RES:
                // SUCCESS, FAIL
                mHandler.post(() -> {
                    mainViewModel.commands.setValue("[in] ack " + ack.toString());
                });
                break;
            case ACK_TYPE_RSP:
                if (ack.command.equals(CMD_RECOGNITION)) {
                    Recognition recognition = gson.fromJson(ack.message, Recognition.class);
                    mHandler.post(() -> {
                        mainViewModel.commands.setValue("[in] ack " + recognition.toString());
                    });
                    ack.type = ACK_TYPE_RES;
                    ack.message = ACK_MESSAGE_SUCCESS;
                    enqueuePacket(socketChannel, CMD_ACK, getByteArrayFromAck(ack));
                }
                break;
        }
    }

    private String getStringFromByteArray(byte[] bytes) {
        return bytes==null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] getByteArrayFromAck(Ack ack) {
        String jsonString = gson.toJson(ack);
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }

    private Ack getAckFromByteArray(byte[] bytes) {
        String jsonString = getStringFromByteArray(bytes);
        return gson.fromJson(jsonString, Ack.class);
    }

}
