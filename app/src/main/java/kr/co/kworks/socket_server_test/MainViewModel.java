package kr.co.kworks.socket_server_test;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


public class MainViewModel extends ViewModel {
    public MutableLiveData<Integer> clientCount;
    public MutableLiveData<String> commands;
    public MutableLiveData<Boolean> connectedWithServer;


    public MainViewModel(
    ) {
        clientCount = new MutableLiveData<>(0);
        commands = new MutableLiveData<>();
        connectedWithServer = new MutableLiveData<>(false);
    }
}
