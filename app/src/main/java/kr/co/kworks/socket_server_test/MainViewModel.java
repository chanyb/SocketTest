package kr.co.kworks.socket_server_test;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.LinkedList;
import java.util.Queue;

import kr.co.kworks.socket_server_test.model.DoFire;
import kr.co.kworks.socket_server_test.model.Fire;


public class MainViewModel extends ViewModel {
    public MutableLiveData<Integer> clientCount, autoClickTimer;
    public MutableLiveData<String> commands, imageBase64String;
    public MutableLiveData<Boolean> connectedWithServer;
    public Queue<DoFire> uuidQueue; // add, poll


    public MainViewModel(
    ) {
        clientCount = new MutableLiveData<>(0);
        commands = new MutableLiveData<>();
        connectedWithServer = new MutableLiveData<>(false);
        uuidQueue = new LinkedList<>();
        autoClickTimer = new MutableLiveData<>(5);
        imageBase64String = new MutableLiveData<>();
    }
}
