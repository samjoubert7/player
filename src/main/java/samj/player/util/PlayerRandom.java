package samj.player.util;

import java.util.concurrent.locks.ReentrantLock;

/*
Algorithm "xorwow" from p. 5 of Marsaglia, "Xorshift RNGs".

The Marsaglia XORWOW generator has a period of 2^123.  It is a combination of a 
Xorshift generator with a long period and a Weyl generator, which together provide 
a sequence of pseudorandom numbers with excellent statistical properties.

NOT designed for cryptographic use.
*/
public class PlayerRandom {

	ReentrantLock lock = new ReentrantLock();

	// Any random numbers will do for this array. These from random.org hex bytes.
	int x[] = {0xa56b0a5f, 0xc60720ed, 0xfe4d80fe, 0x4bc56002, 0x24f81f34};
	int count = 0;

	// Byte source
	int byteSrc = 0;
	int byteSrcShift = -1;

	// Boolean source
	int boolSrc = 0;
	int boolSrcMask = 0;

	// The primary algorithm.
	public int nextInt() throws InterruptedException {
		lock.lockInterruptibly();
		try {
	        int t = x[4];
	
		    int s = x[0];  /* Perform a contrived 32-bit shift. */
	        x[4] = x[3];
	        x[3] = x[2];
	        x[2] = x[1];
	        x[1] = s;
	
		    t ^= t >> 2;
		    t ^= t << 1;
		    t ^= s ^ (s << 4);
	        x[0] = t;
	        count += 362437; // A "magic number" for the xorwow algorithm.
	        return t + count;
		} finally {
			lock.unlock();
		}
	}

	public int nextShort() throws InterruptedException {
		lock.lockInterruptibly();
		try {
			return ((nextByte() & 0xff) << 8) + (nextByte() & 0xff);
		} finally {
			lock.unlock();
		}
	}
	
	public byte nextByte() throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (byteSrcShift < 0) {
				byteSrc = nextInt();
				byteSrcShift = 24;
			}
			int shift = byteSrcShift;
			byteSrcShift -= 8;
			return (byte) (byteSrc >> shift);
		} finally {
			lock.unlock();
		}
	}
	
	public boolean nextBool() throws InterruptedException {
		lock.lockInterruptibly();
		try {
			if (boolSrcMask == 0) {
				boolSrc = nextByte();
				boolSrcMask = 0x80;
			}
			int mask = boolSrcMask;
			boolSrcMask = boolSrcMask >> 1;
			return ((boolSrc & mask) != 0);
		} finally {
			lock.unlock();
		}
	}
}
