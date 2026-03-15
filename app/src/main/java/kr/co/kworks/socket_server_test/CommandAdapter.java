package kr.co.kworks.socket_server_test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.function.Consumer;

public class CommandAdapter extends RecyclerView.Adapter<CommandViewHolder> {
    private Context mContext;
    private SecurityManager securityManager;
    private int selectedPosition;
    private List<String> noticeList;

    public CommandAdapter(Context context, List<String> noticeList) {
        selectedPosition = -1;
        this.mContext = context;
        this.noticeList = noticeList;
    }

    @NonNull
    @Override
    public CommandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 뷰홀더를 건네줘야 함.
        mContext = parent.getContext();
        return CommandViewHolder.from(parent);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull CommandViewHolder holder, int position) {
        // 바인딩 될 때마다 뭐할래?
        if (noticeList.isEmpty()) {
        } else {
            String str = noticeList.get(holder.getAdapterPosition());
            holder.binding.txt.setText(str);
        }

    }

    @Override
    public int getItemCount() {
        if(noticeList.isEmpty()) return 0;
        return noticeList.size();
    }

}
