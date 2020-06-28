package hu.trigary.cmcm.app.util;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Consumer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import hu.trigary.cmcm.app.MainActivity;
import hu.trigary.cmcm.app.R;

/**
 * A superclass for all fragments in this project.
 * Attempts to make the developer's life easier.
 */
public abstract class FragmentBase extends Fragment {
	private final int layoutId;
	protected View view;
	
	/**
	 * Creates a new instance.
	 *
	 * @param layoutId the ID of the layout of this fragment
	 */
	protected FragmentBase(int layoutId) {
		this.layoutId = layoutId;
	}
	
	/**
	 * Utility method. Creates a new instance of the specified fragment,
	 * setting its arguments to the same instance passed to the callback.
	 *
	 * @param clazz the fragment's class
	 * @param initializer the callback that puts data into the arguments
	 * @param <T> the fragment's type
	 * @return the newly created fragment instance
	 */
	@NonNull
	public static <T extends Fragment> T create(@NonNull Class<T> clazz, @NonNull Consumer<Bundle> initializer) {
		try {
			T instance = clazz.newInstance();
			Bundle bundle = new Bundle();
			initializer.accept(bundle);
			instance.setArguments(bundle);
			return instance;
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		view = inflater.inflate(layoutId, container, false);
		initializeView();
		return view;
	}
	
	/**
	 * Called in the {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} callback,
	 * should be used to initialize the view.
	 */
	protected abstract void initializeView();
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		view = null;
	}
	
	/**
	 * Gets the view of the specified ID.
	 *
	 * @param id the view's ID
	 * @return the instance
	 */
	protected View getView(int id) {
		return view == null ? null : view.findViewById(id);
	}
	
	/**
	 * Gets the view of the specified ID and casts it to the specified class.
	 *
	 * @param id the view's ID
	 * @param clazz the clazz of the type to cast to
	 * @param <T> the type to cast to
	 * @return the instance cast to the specified class
	 */
	protected <T> T getView(int id, @NonNull Class<T> clazz) {
		return clazz.cast(getView(id));
	}
	
	/**
	 * Adds a click listener callback to the specified view.
	 *
	 * @param id the view that will be clicked
	 * @param listener the callback that will be called on clicks
	 */
	protected void addClickListener(int id, @NonNull View.OnClickListener listener) {
		getView(id).setOnClickListener(listener);
	}
	
	/**
	 * Sets the text of the specified view to the specified text resource,
	 * with the placeholders replaced by the specified values.
	 *
	 * @param viewId the view's ID to set the text for
	 * @param textId the text resource's ID
	 * @param values the placeholders' replacements
	 */
	protected void setText(int viewId, int textId, @Nullable Object... values) {
		getView(viewId, TextView.class).setText(getString(textId, values));
	}
	
	/**
	 * Enables or disables the specifies view.
	 *
	 * @param viewId the view's ID to enable or disable
	 * @param enabled the new status
	 */
	protected void setEnabled(int viewId, boolean enabled) {
		getView(viewId).setEnabled(enabled);
	}
	
	/**
	 * Makes this fragment the displayed one.
	 * Usually called directly after construction.
	 */
	public void display() {
		//TODO why am I doing it like this? does getActivity return null?
		MainActivity.getInstance().getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.main_content, this)
				.addToBackStack(null)
				.commit();
	}
	
	/**
	 * Goes back to the previous fragment.
	 * Should be called when the back/previous GUI button is clicked.
	 */
	protected void goBack() {
		//noinspection ConstantConditions
		getActivity().getSupportFragmentManager().popBackStackImmediate();
	}
}
