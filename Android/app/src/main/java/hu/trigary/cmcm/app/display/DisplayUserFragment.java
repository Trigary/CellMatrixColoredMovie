package hu.trigary.cmcm.app.display;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.TextView;
import hu.trigary.cmcm.app.R;
import hu.trigary.cmcm.app.util.FragmentBase;

import java.nio.charset.StandardCharsets;

/**
 * The fragment that displays the received payload in the "non-ticket" demo.
 */
public class DisplayUserFragment extends FragmentBase {
	private byte[] rawPayload;
	
	public DisplayUserFragment() {
		super(R.layout.fragment_display_user);
	}
	
	@NonNull
	public static DisplayUserFragment create(@NonNull byte[] payload) {
		return FragmentBase.create(DisplayUserFragment.class,
				bundle -> bundle.putByteArray("payload", payload));
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			savedInstanceState = getArguments();
		}
		
		//noinspection ConstantConditions
		rawPayload = savedInstanceState.getByteArray("payload");
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putByteArray("payload", rawPayload);
	}
	
	@Override
	protected void initializeView() {
		addClickListener(R.id.display_user_back, clicked -> goBack());
		String text = new String(rawPayload, StandardCharsets.UTF_8);
		getView(R.id.display_user_text, TextView.class).setText(text);
	}
}
