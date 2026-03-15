package kr.co.kworks.socket_server_test;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;

import kr.co.kworks.socket_server_test.databinding.ItemCommandBinding;

public class CommandViewHolder extends RecyclerView.ViewHolder {
    public ItemCommandBinding binding;


    public CommandViewHolder(ItemCommandBinding binding) {
        super(binding.getRoot());
        this.binding = binding;
    }

    public static CommandViewHolder from(@NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ItemCommandBinding binding = DataBindingUtil.inflate(layoutInflater, R.layout.item_command, parent, false);
        return new CommandViewHolder(binding);
    }

    public void bind() {
        binding.executePendingBindings();
    }
}
