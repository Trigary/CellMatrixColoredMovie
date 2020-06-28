package hu.trigary.cmcm.app.send;

import android.content.res.AssetFileDescriptor;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import hu.trigary.cmcm.app.MainActivity;
import hu.trigary.cmcm.app.R;
import hu.trigary.cmcm.app.display.DisplayTicketFragment;
import hu.trigary.cmcm.app.util.FragmentBase;
import hu.trigary.cmcm.app.util.TicketPayload;
import hu.trigary.cmcm.library.utilities.FrameInfo;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Random;

/**
 * The fragment that lets the user set the "ticket" demo payload.
 */
public class SendContentTicketFragment extends FragmentBase {
	public SendContentTicketFragment() {
		super(R.layout.fragment_send_content_ticket);
	}
	
	@NonNull
	public static SendContentTicketFragment create() {
		return new SendContentTicketFragment();
	}
	
	@Override
	protected void initializeView() {
		addClickListener(R.id.send_content_ticket_back, clicked -> goBack());
		addClickListener(R.id.send_content_ticket_forward, clicked -> tryShowNextFragment());
		
		//noinspection ResultOfObjectAllocationIgnored
		new EditTextGroupValidator(getView(R.id.send_content_ticket_forward), view,
				R.id.send_content_ticket_duration, R.id.send_content_ticket_name, R.id.send_content_ticket_birth_date,
				R.id.send_content_ticket_birth_place, R.id.send_content_ticket_address,
				R.id.send_content_ticket_phone, R.id.send_content_ticket_email);
	}
	
	private void tryShowNextFragment() {
		Random random = new Random();
		long releaseTime = System.currentTimeMillis();
		long releaseId = random.nextLong();
		long validityEnd = releaseTime + Integer.parseInt(getInput(R.id.send_content_ticket_duration)) * 1000L;
		long id = random.nextLong();
		String name = getInput(R.id.send_content_ticket_name);
		String[] rawBirthDate = getInput(R.id.send_content_ticket_birth_date).split("-");
		int birthDate = rawBirthDate.length == 3 ? TicketPayload.createDate(Integer.parseInt(rawBirthDate[0]),
				Integer.parseInt(rawBirthDate[1]), Integer.parseInt(rawBirthDate[2]))
				: TicketPayload.createDate(2000, 8, 30);
		String birthPlace = getInput(R.id.send_content_ticket_birth_place);
		String address = getInput(R.id.send_content_ticket_address);
		String phone = getInput(R.id.send_content_ticket_phone);
		String email = getInput(R.id.send_content_ticket_email);
		byte[] signature = getView(R.id.send_content_ticket_signature, Switch.class).isChecked()
				? readRawBytes(R.raw.signature) : new byte[0];
		
		byte[] payload = TicketPayload.compile(releaseId, releaseTime, releaseTime,
				validityEnd, name, id, birthDate, birthPlace, address, phone, email, signature);
		
		if (new FrameInfo(FrameInfo.ContentResolution._128).getFrameCount(payload.length) == -1) {
			//noinspection ConstantConditions
			Toast.makeText(getContext(), getContext().getString(R.string.send_content_large),
					Toast.LENGTH_SHORT).show();
		} else if (MainActivity.DISPLAY_INSTEAD_OF_SEND) {
			DisplayTicketFragment.create(payload).display();
		} else {
			SendConfigureFragment.create(payload).display();
		}
	}
	
	private String getInput(int id) {
		return getView(id, EditText.class).getText().toString();
	}
	
	private byte[] readRawBytes(int id) {
		try (AssetFileDescriptor descriptor = getResources().openRawResourceFd(id);
				DataInputStream stream = new DataInputStream(descriptor.createInputStream())) {
			byte[] result = new byte[(int) descriptor.getLength()];
			stream.readFully(result, 0, result.length);
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static class EditTextGroupValidator implements TextWatcher {
		private final View toEnableDisable;
		private final EditText[] editTexts;
		
		EditTextGroupValidator(View toEnableDisable, View container, int... ids) {
			this.toEnableDisable = toEnableDisable;
			editTexts = new EditText[ids.length];
			for (int i = 0; i < ids.length; i++) {
				EditText editText = container.findViewById(ids[i]);
				editTexts[i] = editText;
				editText.addTextChangedListener(this);
			}
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) { }
		
		@Override
		public void afterTextChanged(Editable s) {
			toEnableDisable.setEnabled(noneEmpty());
		}
		
		private boolean noneEmpty() {
			for (EditText editText : editTexts) {
				if (editText.getText().length() == 0) {
					return false;
				}
			}
			return true;
		}
	}
}
