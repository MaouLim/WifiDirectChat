package bupt.wifidirectchat.activities.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import bupt.wifidirectchat.R;

/**
 * Created by Liu Cong on 2017/7/6.
 */

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.viewHolder> {

	private List<Pair> pairs = new ArrayList<>();
	private LayoutInflater inflater;

	public ListAdapter(Context context) {
		inflater = LayoutInflater.from(context);
	}


	@Override
	public ListAdapter.viewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return new viewHolder(inflater.inflate(R.layout.item_pair, parent, false));
	}

	public interface ItemClick {
		void onItemClick(int position, String itemContent);
	}

	private ItemClick itemClick = null;

	public void setItemClick(ItemClick itemClick) {
		this.itemClick = itemClick;
	}

	@Override
	public void onBindViewHolder(final ListAdapter.viewHolder holder, int position) {
		holder.title.setText(pairs.get(position).getTitle());
		holder.content.setText(pairs.get(position).getContent());

		holder.linearLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (itemClick != null) {
					itemClick.onItemClick(
						holder.getAdapterPosition(),
						pairs.get(holder.getAdapterPosition()).getContent()
					);
				}
			}
		});
	}

	@Override
	public int getItemCount() {
		return pairs.size();
	}

	public class viewHolder extends RecyclerView.ViewHolder {
		private TextView title;
		private TextView content;
		private LinearLayout linearLayout;

		public viewHolder(View itemView) {
			super(itemView);

			title = (TextView) itemView.findViewById(R.id.title);
			content = (TextView) itemView.findViewById(R.id.content);
			linearLayout = (LinearLayout) itemView.findViewById(R.id.item);
		}
	}

	public void initData(List<Pair> pairs) {
		this.pairs.addAll(pairs);
		notifyDataSetChanged();
	}

	public void newItem(Pair pair) {
		pairs.add(pair);
		notifyDataSetChanged();
	}

	public void removeItem(Pair pair) {
		int i = 0;
		for (; i < pairs.size(); i++) {
			if (pairs.get(i).getTitle().equals(pair.getTitle())) {
				break;
			}
		}

		pairs.remove(0);
		notifyDataSetChanged();
	}

	public void updateItems(List<Pair> pairs) {
		this.pairs.clear();
		this.pairs.addAll(pairs);
		Log.e("Adapter", pairs.size() + " ");
		notifyDataSetChanged();
	}

}
