package hu.trigary.cmcm.app.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.*;
import java.util.Calendar;
import java.util.Locale;

/**
 * Utility class for serializing and deserializing the data in the "ticket" demo.
 */
public class TicketPayload {
	public static byte[] compile(long releaseId, long releaseTime, long validityStart, long validityEnd,
			String name, long id, int birthDate, String birthPlace, String address, String phone,
			String email, byte[] signature) {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try (ObjectOutputStream stream = new ObjectOutputStream(byteStream)) {
			stream.writeLong(releaseId);
			stream.writeLong(releaseTime);
			stream.writeLong(validityStart);
			stream.writeLong(validityEnd);
			stream.writeUTF(name);
			stream.writeLong(id);
			stream.writeInt(birthDate);
			stream.writeUTF(birthPlace);
			stream.writeUTF(address);
			stream.writeUTF(phone);
			stream.writeUTF(email);
			stream.write(signature);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return byteStream.toByteArray();
	}
	
	public static int createDate(int year, int month, int day) {
		return ((year & 0xffff) << 16) | ((month & 0xff) << 8) | (day & 0xff);
	}
	
	private final long releaseId;
	private final long releaseTime;
	private final long validityStart;
	private final long validityEnd;
	private final String name;
	private final long id;
	private final int birthDate;
	private final String birthPlace;
	private final String address;
	private final String phone;
	private final String email;
	private final byte[] signature;
	
	public TicketPayload(byte[] compiled) {
		try (ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(compiled))) {
			releaseId = stream.readLong();
			releaseTime = stream.readLong();
			validityStart = stream.readLong();
			validityEnd = stream.readLong();
			name = stream.readUTF();
			id = stream.readLong();
			birthDate = stream.readInt();
			birthPlace = stream.readUTF();
			address = stream.readUTF();
			phone = stream.readUTF();
			email = stream.readUTF();
			
			int available = stream.available();
			if (available > 0) {
				signature = new byte[available];
				stream.readFully(signature);
			} else {
				signature = null;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getReleaseId() {
		return formatId(releaseId);
	}
	
	public String getReleaseTime() {
		return formatTime(releaseTime);
	}
	
	public boolean isDocumentValid() {
		long current = System.currentTimeMillis();
		return current >= validityStart && current <= validityEnd;
	}
	
	public String getValidityStart() {
		return formatTime(validityStart);
	}
	
	public String getValidityEnd() {
		return formatTime(validityEnd);
	}
	
	public String getName() {
		return name;
	}
	
	public String getId() {
		return formatId(id);
	}
	
	public String getBirthDate() {
		return formatDate(birthDate);
	}
	
	/**
	 * @noinspection UseOfObsoleteDateTimeApi
	 */
	public String getAge() {
		int birthYear = birthDate >> 16;
		int birthMonth = (birthDate >> 8) & 0xff;
		int birthDay = birthDate & 0xff;
		
		Calendar calendar = Calendar.getInstance();
		int currentYear = calendar.get(Calendar.YEAR);
		int currentMonth = calendar.get(Calendar.MONTH);
		int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
		
		int age = currentYear - birthYear;
		if ((birthMonth > currentMonth) || (birthMonth == currentMonth && birthDay > currentDay)) {
			age--;
		}
		return String.valueOf(age);
	}
	
	public String getBirthPlace() {
		return birthPlace;
	}
	
	public String getAddress() {
		return address;
	}
	
	public String getPhone() {
		return phone;
	}
	
	public String getEmail() {
		return email;
	}
	
	public Bitmap getSignature() {
		if (signature == null) {
			return null;
		}
		
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inMutable = true;
		Bitmap bitmap = BitmapFactory.decodeByteArray(signature, 0, signature.length, options);
		bitmap.setHasAlpha(true);
		for (int x = 0; x < bitmap.getWidth(); x++) {
			for (int y = 0; y < bitmap.getHeight(); y++) {
				if (bitmap.getPixel(x, y) != 0xFF000000) {
					bitmap.setPixel(x, y, 0x00000000);
				}
			}
		}
		return bitmap;
	}
	
	private String formatId(long id) {
		return String.format("%016X", id);
	}
	
	private String formatDate(int date) {
		return String.format(Locale.ENGLISH, "%d-%02d-%02d", date >> 16, (date >> 8) & 0xff, date & 0xff);
	}
	
	private String formatTime(long time) {
		return String.format(Locale.ENGLISH, "%tF %tT", time, time);
	}
}
