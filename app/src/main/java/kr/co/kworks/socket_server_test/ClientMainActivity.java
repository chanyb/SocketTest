package kr.co.kworks.socket_server_test;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import kr.co.kworks.socket_server_test.databinding.ActivityClientMainBinding;
import kr.co.kworks.socket_server_test.databinding.ActivityMainBinding;


public class ClientMainActivity extends AppCompatActivity {

    private ActivityClientMainBinding binding;
    private CommandAdapter commandAdapter;
    private ArrayList<String> commandList;
    private MainViewModel mainViewModel;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> randomCommand;
    private SocketClient socketClient;
    private Thread socketThread;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_client_main);
        init();
        recyclerViewInit();
        observerInit();
        startSchedule();
        startSocketClient();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        release();
    }

    private void init() {
        executor = Executors.newSingleThreadScheduledExecutor();
        commandList = new ArrayList<>();
        commandAdapter = new CommandAdapter(this, commandList);
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
    }

    private void recyclerViewInit() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(commandAdapter);
    }

    private void observerInit() {
        mainViewModel.commands.observe(this, o -> {
            insertToRecyclerView(o);
        });
        mainViewModel.connectedWithServer.observe(this, o -> {
            binding.txtClient.setText(String.valueOf(o));
        });
    }

    private void startSchedule() {
        stopSchedule();
        randomCommand = executor.scheduleWithFixedDelay(() -> {
            if (!socketClient.isConnected()) {
                socketClient.connect();
                return;
            }

            Random random = new Random();
            int randInt = random.nextInt(100);

            if (randInt < 20) {
                //nothing to do
            } else if (randInt < 50) {
                socketClient.requestDatetime();
            } else {
                Recognition recognition = new Recognition();
                recognition.id = String.valueOf(UUID.randomUUID());
                socketClient.sendRecognition(recognition);
            }
        }, 2000, 100, TimeUnit.MILLISECONDS);
    }

    private void stopSchedule() {
        if (randomCommand != null && !randomCommand.isCancelled()) randomCommand.cancel(true);
    }

    private void insertToRecyclerView(String str) {
        commandList.add(0, str);
        commandAdapter.notifyItemInserted(0);
    }

    private void startSocketClient() {
        socketClient = new SocketClient("192.168.10.40", 7833);
        socketClient.setListener(new SocketClient.ClientListener() {
            @Override
            public void onConnected() {
                SocketClient.ClientListener.super.onConnected();
                mainViewModel.commands.postValue("onConnect");
                mainViewModel.connectedWithServer.postValue(true);
            }

            @Override
            public void onDisconnected() {
                SocketClient.ClientListener.super.onDisconnected();
                mainViewModel.commands.postValue("onDisconnected");
                mainViewModel.connectedWithServer.postValue(false);
            }

            @Override
            public void onPacketReceived(String command, byte[] data) {
                SocketClient.ClientListener.super.onPacketReceived(command, data);
                String datetime = new String(data, StandardCharsets.UTF_8);
                mainViewModel.commands.postValue(command + " " + datetime);
            }

            @Override
            public void onError(Exception e) {
                SocketClient.ClientListener.super.onError(e);
                mainViewModel.commands.postValue("onError: " + e.getMessage());
            }
        });

        socketThread = new Thread(() -> {
            socketClient.connect();
        });

        socketThread.start();
    }

    private void release() {
        if (socketClient.isConnected()) socketClient.disconnect();
        if (socketThread.isAlive()) {
            socketThread.interrupt();
        }
    }
}
