#ifndef CMCM_NATIVE_CHUNK_EXTRACTOR_H
#define CMCM_NATIVE_CHUNK_EXTRACTOR_H

#include <jni.h>
#include "Color.h"

namespace cmcm {

void readChunksInRow(Color pixels[], jint contentArray[], int startX, int y, int resolution, int chunkCount, int chunkIndex) {
	for (int i = 0; i < chunkCount; i++) {
		int value = pixels[startX + y * resolution].getBits(startX, y) << 9;
		value |= pixels[startX + 1 + y * resolution].getBits(startX + 1, y) << 6;
		value |= pixels[startX + (y + 1) * resolution].getBits(startX, y + 1) << 3;
		value |= pixels[startX + 1 + (y + 1) * resolution].getBits(startX + 1, y + 1);
		contentArray[chunkIndex++] = value;
		startX += 2;
	}
}

void extractChunks(Color pixels[], jint contentArray[], int resolution) {
	int firstChunk = pixels[resolution].getBits(0, 0) << 9;
	firstChunk |= pixels[1 + resolution].getBits(1, 0) << 6;
	firstChunk |= pixels[2 * resolution - 1].getBits(0, 1) << 3;
	firstChunk |= pixels[2 * resolution - 2].getBits(1, 1);
	contentArray[0] = firstChunk;

	int chunkIndex = 1;
	int chunkCount = resolution / 2;
	readChunksInRow(pixels, contentArray, 2, 0, resolution, chunkCount - 2, chunkIndex);
	int y = 2;
	chunkIndex += chunkCount - 2;
	for (int i = 2; i < chunkCount; i++) {
		readChunksInRow(pixels, contentArray, 0, y, resolution, chunkCount, chunkIndex);
		chunkIndex += chunkCount;
		y += 2;
	}
	readChunksInRow(pixels, contentArray, 1, y, resolution, chunkCount - 1, chunkIndex);
}

}

#endif //CMCM_NATIVE_CHUNK_EXTRACTOR_H
