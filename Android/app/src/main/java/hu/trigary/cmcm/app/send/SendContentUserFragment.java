package hu.trigary.cmcm.app.send;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;
import hu.trigary.cmcm.app.MainActivity;
import hu.trigary.cmcm.app.R;
import hu.trigary.cmcm.app.display.DisplayUserFragment;
import hu.trigary.cmcm.app.util.FragmentBase;
import hu.trigary.cmcm.library.utilities.FrameInfo;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The fragment that lets the user set the "non-ticket" demo payload.
 */
public class SendContentUserFragment extends FragmentBase implements TextWatcher {
	private byte[] filePayload;
	private String fileName;
	private String text;
	
	public SendContentUserFragment() {
		super(R.layout.fragment_send_content_user);
	}
	
	@NonNull
	public static SendContentUserFragment create() {
		return new SendContentUserFragment();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			filePayload = savedInstanceState.getByteArray("payload");
			fileName = savedInstanceState.getString("file");
			text = savedInstanceState.getString("text");
		} else {
			text = "";
		}
	}
	
	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putByteArray("payload", filePayload);
		outState.putString("file", fileName);
		outState.putString("text", text);
	}
	
	@Override
	protected void initializeView() {
		setEnabled(R.id.send_content_user_forward, false);
		addClickListener(R.id.send_content_user_back, clicked -> goBack());
		addClickListener(R.id.send_content_user_forward, clicked -> tryShowNextFragment());
		
		addClickListener(R.id.send_content_user_file_select, clicked -> {
			if (filePayload == null) {
				openFileSelector();
			} else {
				clearSelectedFile();
			}
		});
		getView(R.id.send_content_user_text, EditText.class).addTextChangedListener(this);
		
		if (filePayload != null) {
			onFileSelected();
		} else if (!text.isEmpty()) {
			getView(R.id.send_content_user_text, EditText.class).setText(text);
		}
	}
	
	private void tryShowNextFragment() {
		byte[] payload = filePayload != null ? filePayload : text.getBytes(StandardCharsets.UTF_8);
		if (new FrameInfo(FrameInfo.ContentResolution._128).getFrameCount(payload.length) == -1) {
			//noinspection ConstantConditions
			Toast.makeText(getContext(), getContext().getString(R.string.send_content_large),
					Toast.LENGTH_SHORT).show();
		} else if (MainActivity.DISPLAY_INSTEAD_OF_SEND) {
			DisplayUserFragment.create(payload).display();
		} else {
			SendConfigureFragment.create(payload).display();
		}
	}
	
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
	
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) { }
	
	@Override
	public void afterTextChanged(Editable s) {
		text = s.toString();
		if (getView(R.id.send_content_user_text).isEnabled()) {
			boolean flag = text.isEmpty();
			setEnabled(R.id.send_content_user_forward, !flag);
			setEnabled(R.id.send_content_user_file_select, flag);
		}
	}
	
	private void openFileSelector() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");
		startActivityForResult(intent, 1);
	}
	
	private void clearSelectedFile() {
		filePayload = null;
		setEnabled(R.id.send_content_user_forward, false);
		setEnabled(R.id.send_content_user_text, true);
		setText(R.id.send_content_user_file_text, R.string.send_content_user_file_none);
		setText(R.id.send_content_user_file_select, R.string.send_content_user_file_select);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK || !getView(R.id.send_content_user_file_select).isEnabled()) {
			return;
		}
		
		if (loadSelectedFile(data.getData())) {
			onFileSelected();
		}
	}
	
	private void onFileSelected() {
		setEnabled(R.id.send_content_user_forward, true);
		setEnabled(R.id.send_content_user_text, false);
		setText(R.id.send_content_user_file_text, R.string.send_content_user_file_selected, fileName);
		setText(R.id.send_content_user_file_select, R.string.send_content_user_file_clear);
	}
	
	private boolean loadSelectedFile(Uri uri) {
		try {
			//noinspection ConstantConditions
			ContentResolver resolver = getContext().getContentResolver();
			try (Cursor cursor = resolver.query(uri, null, null, null, null, null);
					DataInputStream stream = new DataInputStream(resolver.openInputStream(uri))) {
				//noinspection ConstantConditions
				cursor.moveToFirst();
				int length = cursor.getInt(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));
				filePayload = new byte[length];
				stream.readFully(filePayload, 0, length);
				fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			Toast.makeText(getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			filePayload = null;
			fileName = null;
			return false;
		}
	}
}
