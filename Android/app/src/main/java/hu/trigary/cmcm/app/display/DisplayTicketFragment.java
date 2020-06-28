package hu.trigary.cmcm.app.display;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import hu.trigary.cmcm.app.util.TicketPayload;
import hu.trigary.cmcm.app.R;
import hu.trigary.cmcm.app.util.FragmentBase;

/**
 * The fragment that displays the received payload in the "ticket" demo.
 */
public class DisplayTicketFragment extends FragmentBase {
	private byte[] rawPayload;
	
	public DisplayTicketFragment() {
		super(R.layout.fragment_display_ticket);
	}
	
	@NonNull
	public static DisplayTicketFragment create(@NonNull byte[] payload) {
		return FragmentBase.create(DisplayTicketFragment.class,
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
		addClickListener(R.id.display_ticket_back, clicked -> goBack());
		
		TicketPayload payload = new TicketPayload(rawPayload);
		setText(R.id.display_ticket_release_id, R.string.display_ticket_release_id, payload.getReleaseId());
		setText(R.id.display_ticket_release_time, R.string.display_ticket_release_time, payload.getReleaseTime());
		setText(R.id.display_ticket_validity, payload.isDocumentValid()
				? R.string.display_ticket_validity_valid : R.string.display_ticket_validity_invalid);
		setText(R.id.display_ticket_valid_start, R.string.display_ticket_validity_start, payload.getValidityStart());
		setText(R.id.display_ticket_valid_end, R.string.display_ticket_validity_end, payload.getValidityEnd());
		setText(R.id.display_ticket_name, R.string.display_ticket_name, payload.getName());
		setText(R.id.display_ticket_id, R.string.display_ticket_id, payload.getId());
		setText(R.id.display_ticket_birth_date, R.string.display_ticket_birth_date, payload.getBirthDate(), payload.getAge());
		setText(R.id.display_ticket_birth_place, R.string.display_ticket_birth_place, payload.getBirthPlace());
		setText(R.id.display_ticket_address, R.string.display_ticket_address, payload.getAddress());
		setText(R.id.display_ticket_phone, R.string.display_ticket_phone, payload.getPhone());
		setText(R.id.display_ticket_email, R.string.display_ticket_email, payload.getEmail());
		
		Bitmap signature = payload.getSignature();
		if (signature == null) {
			getView(R.id.display_ticket_signature).setVisibility(View.GONE);
		} else {
			getView(R.id.display_ticket_signature, ImageView.class).setImageBitmap(signature);
		}
	}
}
