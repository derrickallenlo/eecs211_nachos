package nachos.vm;

import nachos.machine.TranslationEntry;

public class SwapPage {
	int frameNumber;
	MemoryPage memoryPage;

	public SwapPage(MemoryPage memoryPage, int frameNumber) {
		this.memoryPage = memoryPage;
		this.frameNumber = frameNumber;
	}

}
