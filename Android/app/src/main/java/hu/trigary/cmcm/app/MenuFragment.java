package hu.trigary.cmcm.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import hu.trigary.cmcm.app.receive.ReceiveFragment;
import hu.trigary.cmcm.app.send.SendContentTicketFragment;
import hu.trigary.cmcm.app.send.SendContentUserFragment;
import hu.trigary.cmcm.app.util.FragmentBase;

/**
 * The fragment that displays the main menu.
 */
public class MenuFragment extends FragmentBase {
	public MenuFragment() {
		super(R.layout.fragment_menu);
	}
	
	@NonNull
	public static MenuFragment create() {
		return new MenuFragment();
	}
	
	@Override
	protected void initializeView() {
		addClickListener(R.id.menu_send, clicked -> {
			//noinspection ConstantConditions
			if (ContextCompat.checkSelfPermission(getActivity(),
					Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
				showContentFragment();
			} else {
				requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
			}
		});
		
		addClickListener(R.id.menu_receive, clicked -> {
			//noinspection ConstantConditions
			if (ContextCompat.checkSelfPermission(getActivity(),
					Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
				ReceiveFragment.create().display();
			} else {
				requestPermissions(new String[]{Manifest.permission.CAMERA}, 2);
			}
		});
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			if (requestCode == 1) {
				showContentFragment();
			} else if (requestCode == 2) {
				ReceiveFragment.create().display();
			}
		}
	}
	
	private void showContentFragment() {
		if (MainActivity.USE_TICKET_DEMO) {
			SendContentTicketFragment.create().display();
		} else {
			SendContentUserFragment.create().display();
		}
	}
}
