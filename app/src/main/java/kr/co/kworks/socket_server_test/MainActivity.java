package kr.co.kworks.socket_server_test;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.gson.Gson;

import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import kr.co.kworks.socket_server_test.databinding.ActivityMainBinding;
import kr.co.kworks.socket_server_test.model.DoFire;


public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private CommandAdapter commandAdapter;
    private CopyOnWriteArrayList<String> commandList;
    private CopyOnWriteArrayList<String> waiting;
    private MainViewModel mainViewModel;
    private ScheduledExecutorService executor;
    private ScheduledFuture<?> waitingToCommand, fireDialogScheduled;
    private SocketServer socketServer;
    private Thread socketThread;
    private Gson gson;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        init();
        recyclerViewInit();
        observerInit();
        startWaitingToCommand();
        startSocketServer();
        startSchedule();
        initClickListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        release();
    }

    private void init() {
        executor = Executors.newScheduledThreadPool(5);
        commandList = new CopyOnWriteArrayList<>();
        waiting = new CopyOnWriteArrayList<>();
        commandAdapter = new CommandAdapter(this, commandList);
        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        gson = new Gson();
    }

    private void recyclerViewInit() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(commandAdapter);
    }

    private void observerInit() {
        mainViewModel.commands.observe(this, o -> {
            waiting.add(o);
            binding.txtQueue.setText(String.valueOf(waiting.size()));
        });
        mainViewModel.clientCount.observe(this, o -> {
            binding.txtClient.setText(String.valueOf(o));
        });
    }

    private void startSchedule() {
        stopSchedule();
        fireDialogScheduled = executor.scheduleWithFixedDelay(() -> {
            if (binding.loFire.getVisibility() == View.VISIBLE) return;
            DoFire doFire = mainViewModel.uuidQueue.peek();
            if (doFire == null) return;
            runOnUiThread(() -> {
                binding.txtUuid.setText(doFire.fire.id);
                binding.loFire.setVisibility(View.VISIBLE);
            });
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    private void stopSchedule() {
        if (fireDialogScheduled != null && !fireDialogScheduled.isCancelled()) fireDialogScheduled.cancel(true);
    }

    private void insertToRecyclerView(String str) {
        commandList.add(0, str);
        commandAdapter.notifyItemInserted(0);
        binding.recyclerView.scrollToPosition(0);
    }

    private void startSocketServer() {
        socketServer = new SocketServer(7833, mainViewModel);
        socketServer.setListener(new SocketServer.ServerListener() {
            @Override
            public void onClientConnected(SocketChannel ch) {
                SocketServer.ServerListener.super.onClientConnected(ch);
                mainViewModel.commands.postValue("onClientConnected: " + ch.toString());
            }

            @Override
            public void onClientDisconnected(SocketChannel ch, Throwable cause) {
                SocketServer.ServerListener.super.onClientDisconnected(ch, cause);
                mainViewModel.commands.postValue("onClientDisconnected: " + ch.toString());
            }
        });

        socketThread = new Thread(() -> {
            socketServer.run();
        });
        socketThread.start();
    }

    private void release() {
        stopSchedule();
        stopWaitingToCommand();
        if (socketServer != null) socketServer.shutdown();
        if (socketThread.isAlive()) socketThread.interrupt();
    }

    private void stopWaitingToCommand() {
        if (waitingToCommand != null && !waitingToCommand.isCancelled()) waitingToCommand.cancel(true);
    }

    private void startWaitingToCommand() {
        stopWaitingToCommand();
        waitingToCommand = executor.scheduleWithFixedDelay(() -> {
            if (waiting.isEmpty()) return;
            String str = waiting.get(0);
            waiting.remove(0);
            runOnUiThread(() -> {
                insertToRecyclerView(str);
                binding.txtQueue.setText(String.valueOf(waiting.size()));
            });
        },0, 200, TimeUnit.MILLISECONDS);
    }

    private void initClickListener() {
        binding.txtFire.setOnClickListener(v -> { // fire
            DoFire doFire = mainViewModel.uuidQueue.peek();
            String jsonString = gson.toJson(doFire.fire);
            socketServer.enqueuePacket(doFire.channel, SocketServer.CMD_DO_FIRE, jsonString.getBytes(StandardCharsets.UTF_8));
            binding.txtCancel.callOnClick();
        });

        binding.txtCancel.setOnClickListener(v -> { // cancel
            binding.loFire.setVisibility(View.GONE);
            mainViewModel.uuidQueue.poll();
        });
    }
}
