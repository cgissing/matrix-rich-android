package chat.richclient.ui;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import chat.richclient.R;
import chat.richclient.bridge.RuntimeState;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.EventViewHolder> {
    public interface Listener {
        void onReactionSelected(String eventId, String key);
    }

    private static final String DEFAULT_REACTION_KEY = "\uD83D\uDC4D";

    private final RichMarkdownRenderer markdownRenderer;
    private final Listener listener;
    private final List<RuntimeState.TimelineEvent> events = new ArrayList<>();
    private RuntimeState state = new RuntimeState();

    public TimelineAdapter(RichMarkdownRenderer markdownRenderer, Listener listener) {
        this.markdownRenderer = markdownRenderer;
        this.listener = listener;
    }

    public void submit(RuntimeState nextState) {
        state = nextState;
        events.clear();
        events.addAll(nextState.timelineForSelectedRoom());
        notifyDataSetChanged();
    }

    @Override
    public EventViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rich_timeline_message, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(EventViewHolder holder, int position) {
        RuntimeState.TimelineEvent event = events.get(position);
        Context context = holder.itemView.getContext();
        String time = formatTime(context, event.timestamp);

        holder.sender.setText(event.outgoing ? "You" : event.sender);
        holder.time.setText(time);
        holder.footerTime.setText(time);
        holder.avatar.setText(initials(event.outgoing ? "You" : event.sender));
        holder.avatar.setVisibility(event.outgoing ? View.GONE : View.VISIBLE);
        holder.headerRow.setGravity(event.outgoing ? Gravity.END | Gravity.CENTER_VERTICAL : Gravity.CENTER_VERTICAL);
        holder.headerRow.setPadding(event.outgoing ? 8 : dp(context, 52), 0, dp(context, 8), 0);
        holder.bubble.setBackgroundResource(event.outgoing
                ? R.drawable.rich_message_bubble_outgoing
                : R.drawable.rich_message_bubble_incoming);
        holder.body.setTextColor(event.outgoing
                ? Color.WHITE
                : ContextCompat.getColor(context, R.color.rich_text_primary));
        holder.footerTime.setTextColor(event.outgoing
                ? 0xCCFFFFFF
                : ContextCompat.getColor(context, R.color.rich_text_tertiary));
        markdownRenderer.setMarkdown(holder.body, event.body);

        FrameLayout.LayoutParams bubbleParams = (FrameLayout.LayoutParams) holder.bubble.getLayoutParams();
        bubbleParams.gravity = event.outgoing ? Gravity.END : Gravity.START;
        bubbleParams.leftMargin = event.outgoing ? dp(context, 48) : dp(context, 52);
        bubbleParams.rightMargin = event.outgoing ? dp(context, 4) : dp(context, 16);
        holder.bubble.setLayoutParams(bubbleParams);

        renderReactions(holder, event);
        holder.bubble.setOnLongClickListener(view -> {
            listener.onReactionSelected(event.eventId, DEFAULT_REACTION_KEY);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    private void renderReactions(EventViewHolder holder, RuntimeState.TimelineEvent event) {
        holder.reactions.removeAllViews();
        List<RuntimeState.ReactionSummary> reactions = state.reactionsForEvent(event.eventId);
        holder.reactions.setGravity(event.outgoing ? Gravity.END : Gravity.START);
        holder.reactions.setVisibility(reactions.isEmpty() ? View.GONE : View.VISIBLE);
        for (RuntimeState.ReactionSummary reaction : reactions) {
            TextView chip = new TextView(holder.itemView.getContext());
            chip.setText(reaction.key + " " + reaction.count);
            chip.setTextSize(12);
            chip.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                    reaction.selected ? R.color.rich_primary : R.color.rich_text_secondary));
            chip.setGravity(Gravity.CENTER);
            chip.setMinHeight(dp(holder.itemView.getContext(), 28));
            chip.setPadding(
                    dp(holder.itemView.getContext(), 8),
                    dp(holder.itemView.getContext(), 3),
                    dp(holder.itemView.getContext(), 8),
                    dp(holder.itemView.getContext(), 3));
            chip.setBackgroundResource(R.drawable.rich_reaction_chip);
            chip.setOnClickListener(view -> listener.onReactionSelected(event.eventId, reaction.key));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dp(holder.itemView.getContext(), 6), dp(holder.itemView.getContext(), 4));
            holder.reactions.addView(chip, params);
        }
    }

    private String formatTime(Context context, long timestamp) {
        if (timestamp <= 0L) {
            return "";
        }
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(timestamp));
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

    private int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5F);
    }

    static final class EventViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout headerRow;
        final TextView sender;
        final TextView time;
        final TextView avatar;
        final LinearLayout bubble;
        final TextView body;
        final TextView footerTime;
        final LinearLayout reactions;

        EventViewHolder(View itemView) {
            super(itemView);
            headerRow = itemView.findViewById(R.id.messageHeaderRow);
            sender = itemView.findViewById(R.id.messageMemberNameView);
            time = itemView.findViewById(R.id.messageTimeView);
            avatar = itemView.findViewById(R.id.messageAvatarImageView);
            bubble = itemView.findViewById(R.id.bubbleView);
            body = itemView.findViewById(R.id.messageBodyView);
            footerTime = itemView.findViewById(R.id.bubbleFooterMessageTimeView);
            reactions = itemView.findViewById(R.id.reactionsContainer);
        }
    }
}
