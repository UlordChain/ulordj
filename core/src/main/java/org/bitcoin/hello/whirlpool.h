#ifndef WHIRLPOOL_H
#define WHIRLPOOL_H

#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

	/*
     * FUNCTION: one-way function Whirlpool
     *      1. input : input message, msglength
     *		2. output: Whirlpool message
	*/
	void whirlpool(uint8_t *input, uint32_t inputLen, uint8_t *output);

#ifdef __cplusplus
}
#endif
	
#endif