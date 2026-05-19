package chat.richclient.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import chat.richclient.R;
import chat.richclient.bridge.RuntimeState;
import java.util.ArrayList;
import java.util.List;

public final class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.RoomViewHolder> {
    public interface Listener {
        void onRoomSelected(String roomId);
    }

    private final Listener listener;
    private final List<RuntimeState.RoomSummary> rooms = new ArrayList<>();
    private RuntimeState state = new RuntimeState();

    public RoomListAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(RuntimeState nextState) {
        state = nextState;
        rooms.clear();
        rooms.addAll(nextState.rooms);
        notifyDataSetChanged();
    }

    @Override
    public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rich_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RoomViewHolder holder, int position) {
        RuntimeState.RoomSummary room = rooms.get(position);
        boolean selected = room.roomId.equals(state.selectedRoomId);
        List<String> typingUsers = state.typingUsersForRoom(room.roomId);

        holder.itemView.setBackgroundResource(selected ? R.drawable.rich_room_selected_background : 0);
        holder.unreadIndicator.setVisibility(room.unreadCount > 0 ? View.VISIBLE : View.GONE);
        holder.name.setText(room.name.isEmpty() ? room.roomId : room.name);
        holder.avatar.setText(initials(room.name.isEmpty() ? room.roomId : room.name));
        holder.encryptedBadge.setVisibility(room.encrypted ? View.VISIBLE : View.GONE);
        holder.lastEventTime.setText(room.unreadCount > 0 ? "new" : "");

        if (!typingUsers.isEmpty()) {
            holder.subtitle.setVisibility(View.GONE);
            holder.typing.setVisibility(View.VISIBLE);
            holder.typing.setText(typingSummary(typingUsers));
        } else {
            holder.typing.setVisibility(View.GONE);
            holder.subtitle.setVisibility(View.VISIBLE);
            String preview = room.lastMessage.isEmpty() ? room.roomId : room.lastMessage;
            holder.subtitle.setText(preview);
        }

        if (room.unreadCount > 0) {
            holder.unreadBadge.setText(String.valueOf(room.unreadCount));
            holder.unreadBadge.setVisibility(View.VISIBLE);
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(view -> listener.onRoomSelected(room.roomId));
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    private String typingSummary(List<String> users) {
        if (users.size() == 1) {
            return users.get(0) + " is typing";
        }
        return users.size() + " people are typing";
    }

    private String initials(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return "?";
        }
        char first = trimmed.charAt(0);
        if (first == '!' || first == '#' || first == '@') {
            return String.valueOf(Character.toUpperCase(trimmed.length() > 1 ? trimmed.charAt(1) : first));
        }
        return String.valueOf(Character.toUpperCase(first));
    }

    static final class RoomViewHolder extends RecyclerView.ViewHolder {
        final View unreadIndicator;
        final View encryptedBadge;
        final TextView avatar;
        final TextView name;
        final TextView lastEventTime;
        final TextView subtitle;
        final TextView typing;
        final TextView unreadBadge;

        RoomViewHolder(View itemView) {
            super(itemView);
            unreadIndicator = itemView.findViewById(R.id.roomUnreadIndicator);
            encryptedBadge = itemView.findViewById(R.id.roomEncryptedBadge);
            avatar = itemView.findViewById(R.id.roomAvatarInitials);
            name = itemView.findViewById(R.id.roomNameView);
            lastEventTime = itemView.findViewById(R.id.roomLastEventTimeView);
            subtitle = itemView.findViewById(R.id.subtitleView);
            typing = itemView.findViewById(R.id.roomTypingView);
            unreadBadge = itemView.findViewById(R.id.roomUnreadCounterBadgeView);
        }
    }
}
