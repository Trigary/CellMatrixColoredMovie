package hu.trigary.cmcm.app.receive.processor.java;

import hu.trigary.cmcm.library.utilities.Color;
import org.opencv.core.Scalar;

/**
 * The different colors that parsable movie frames can contain.
 */
public enum CellColor {
	BLACK(Color.BLACK),
	RED(Color.RED),
	GREEN(Color.GREEN),
	BLUE(Color.BLUE),
	YELLOW(Color.YELLOW),
	MAGENTA(Color.MAGENTA),
	AQUA(Color.AQUA),
	WHITE(Color.WHITE);
	
	private static final CellColor[] SORTED_VALUES = new CellColor[8];
	
	static {
		for (CellColor color : values()) {
			SORTED_VALUES[color.color.getBits()] = color;
		}
		
		WHITE.templateOpposite = YELLOW;
		GREEN.templateOpposite = AQUA;
		YELLOW.templateOpposite = WHITE;
		AQUA.templateOpposite = GREEN;
	}
	
	private final Color color;
	private CellColor templateOpposite;
	
	CellColor(Color color) {
		this.color = color;
	}
	
	public Color getColor() {
		return color;
	}
	
	public CellColor getTemplateOpposite() {
		return templateOpposite;
	}
	
	public int getBitmask() {
		return 1 << ordinal();
	}
	
	public static CellColor getClosest(Scalar rgbColor) {
		return SORTED_VALUES[(rgbColor.val[0] > 127 ? 4 : 0)
				| (rgbColor.val[1] > 127 ? 2 : 0)
				| (rgbColor.val[2] > 127 ? 1 : 0)];
	}
}
