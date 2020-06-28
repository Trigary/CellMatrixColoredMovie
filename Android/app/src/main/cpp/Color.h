#ifndef CMCM_NATIVE_COLOR_H
#define CMCM_NATIVE_COLOR_H

#include <opencv2/core/types.hpp>

namespace cmcm {

class Color {
public:
	static const Color BLACK; //0
	static const Color BLUE; //1
	static const Color GREEN; //2
	static const Color AQUA; //3
	static const Color RED; //4
	static const Color MAGENTA; //5
	static const Color YELLOW; //6
	static const Color WHITE; //7

private:
	static Color SORTED[8];
	int bits;
	int templateOpposite;

public:
	inline static Color getClosest(const Scalar& rgbColor) {
		return SORTED[(rgbColor.val[0] > 127 ? 4 : 0)
							| (rgbColor.val[1] > 127 ? 2 : 0)
							| (rgbColor.val[2] > 127 ? 1 : 0)];
	}

	Color() {
		bits = 15;
		templateOpposite = -1;
	}

	inline bool operator==(const Color& other){
		return bits == other.bits;
	}

	inline int getBitMask() const {
		return 1 << bits;
	}

	inline int getBits(int x, int y) {
		return (bits + 8 - getMask(x, y)) % 8;
	}

	inline bool isTemplateOpposite(Color color) {
		return templateOpposite == color.bits;
	}

private:
	inline Color(bool red, bool green, bool blue, int oppositeBits) {
		templateOpposite = oppositeBits;
		bits = (red ? 4 : 0) | (green ? 2 : 0) | (blue ? 1 : 0);
		SORTED[bits] = *this;
	}

	inline static int getMask(int x, int y) {
		return ((x * y) % (2 * x + y + 1) + y) % 8;
	}
};

Color Color::SORTED[8];
Color const Color::BLACK = Color(false, false, false, -1);
Color const Color::BLUE = Color(false, false, true, -1);
Color const Color::GREEN = Color(false, true, false, 3);
Color const Color::AQUA = Color(false, true, true, 2);
Color const Color::RED = Color(true, false, false, -1);
Color const Color::MAGENTA = Color(true, false, true, -1);
Color const Color::YELLOW = Color(true, true, false, 7);
Color const Color::WHITE = Color(true, true, true, 6);

}

#endif //CMCM_NATIVE_COLOR_H
