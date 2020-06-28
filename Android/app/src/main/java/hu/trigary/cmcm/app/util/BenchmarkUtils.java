package hu.trigary.cmcm.app.util;

import android.support.annotation.NonNull;
import android.util.Log;

import java.util.*;

/**
 * A thread-safe utility that allows the measurement of delta-time of named code sections.
 */
public final class BenchmarkUtils {
	private static final int SKIP_COUNT = 10;
	private static final Map<String, Storage> MAP = new HashMap<>();
	private static final Set<String> LOG_BLACKLIST = new HashSet<>(Arrays.asList(
			//"FrameAvailable",
			//"Process",
			//"Display"
	));
	
	private BenchmarkUtils() { }
	
	/**
	 * Deletes all stored data.
	 */
	public static synchronized void clear() {
		/*for (Map.Entry<String, Storage> entry : MAP.entrySet()) {
			dump(entry);
		}*/
		
		MAP.clear();
	}
	
	//TODO get rid of this, we should get a proper benchmarking thing
	/*private static void dump(Map.Entry<String, Storage> entry) {
		String key = "CMCM-" + entry.getKey();
		Log.d(key, "Dump to follow, values in ms, count of elements: " + entry.getValue().delta.size());
		
		List<Integer> list = entry.getValue().delta;
		StringBuilder output = new StringBuilder();
		for (int i = 0; i < list.size(); ) {
			output.append(',').append(list.get(i));
			if (++i % 250 == 0) {
				Log.d(key, output.toString());
				output.setLength(0);
			}
		}
		
		if (output.length() != 0) {
			Log.d(key, output.toString());
		}
	}*/
	/*package hu.trigary.javatester.code;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;

import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String stringInput = new Scanner(System.in).nextLine();
        int[] rawInput = Arrays.stream(stringInput.split(",")).mapToInt(Integer::parseInt).toArray();
        
        int targetN = 1500;
        int[] input = Arrays.stream(rawInput)
                .skip((rawInput.length - targetN) / 2)
                .limit(targetN)
                .toArray();
        
        IntSummaryStatistics stats = Arrays.stream(input).summaryStatistics();
        System.out.println("Count: " + stats.getCount());
        System.out.println("Min: " + stats.getMin());
        System.out.println("Max: " + stats.getMax());
        System.out.println("Average: " + stats.getAverage());
        System.out.println("Standard deviation: " + new StandardDeviation()
                .evaluate(Arrays.stream(input).mapToDouble(i -> i).toArray()));
        System.out.println("Median: " + new Median()
                .evaluate(Arrays.stream(input).mapToDouble(i -> i).toArray()));
    }
}*/
	
	/**
	 * Adds a new measurement to the specified section.
	 *
	 * @param key the name of the section
	 * @param startNano the time when the section was entered into
	 */
	public static synchronized void update(@NonNull String key, long startNano) {
		if (LOG_BLACKLIST.contains(key)) {
			return;
		}
		
		long endNano = System.nanoTime();
		Storage storage = MAP.get(key);
		if (storage == null) {
			storage = new Storage();
			MAP.put(key, storage);
		}
		
		if (storage.skipped < SKIP_COUNT) {
			if (++storage.skipped == SKIP_COUNT) {
				storage.current = endNano - storage.lastEndNano;
				storage.limit = endNano - startNano;
			}
			storage.lastEndNano = endNano;
			return;
		}
		
		storage.current += (endNano - storage.lastEndNano - storage.current) * 0.1;
		storage.lastEndNano = endNano;
		storage.limit += (endNano - startNano - storage.limit) * 0.1;
		//storage.delta.add((int) ((endNano - startNano) / 1000000));
		
		Log.d("CMCM", String.format("%s | fps-current:%s | fps-limit:%s | delta-ms:%s", key,
				formatFps(storage.current), formatFps(storage.limit), format(storage.limit / 1000000)));
	}
	
	private static String formatFps(double value) {
		return format(1000000000 / value);
	}
	
	private static String format(double value) {
		return String.format(Locale.ENGLISH, "%.2f", value);
	}
	
	private static class Storage {
		int skipped;
		long lastEndNano;
		double current;
		double limit;
		//final List<Integer> delta = new ArrayList<>();
	}
}
