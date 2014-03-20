package nachos.vm;

import nachos.machine.Coff;
import nachos.machine.CoffSection;
import nachos.machine.Machine;
import nachos.machine.Processor;
import nachos.machine.TranslationEntry;

/**
 *
 * @author martin
 */
public class LazyCoffLoader
{
    private final Coff coff;
    private final CodePage[] pagesOfCode;

    public LazyCoffLoader(Coff coff)
    {
        this.coff = coff;
        
        int codeNumPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			codeNumPages += section.getLength();
		}


		pagesOfCode = new CodePage[codeNumPages];

		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			for (int i = 0; i < section.getLength(); i++) {
				int virtualPage = section.getFirstVPN() + i;
				pagesOfCode[virtualPage] = new CodePage(s, i);
			}
		}
    }
    
    
    private boolean isCodePageNumber(int virtualPage) {
		return virtualPage >= 0 && virtualPage < pagesOfCode.length;
	}



    public TranslationEntry load(int pid, int virtualPage, int physicalPage)
    {
       
            return isCodePageNumber(virtualPage) ? loadCode(virtualPage, physicalPage) : loadStack(virtualPage, physicalPage);


    }

    	private TranslationEntry loadCode(int virtualPage, int physicalPage) {
		CoffSection section = coff.getSection(pagesOfCode[virtualPage].section);
		TranslationEntry entry = new TranslationEntry(virtualPage, physicalPage, true, section
				.isReadOnly(), false, false);
		section.loadPage(pagesOfCode[virtualPage].offset, physicalPage);
		return entry;
	}

	private TranslationEntry loadStack(int virtualPage, int physicalPage) {
		fillMemory(physicalPage);                                                    //TODO remove me!
		return new TranslationEntry(virtualPage, physicalPage, true, false, false, false);
	}
    
	private void fillMemory(int physicalPage) {
		int pageStart = Processor.makeAddress(physicalPage, 0);
                int pageEnd = pageStart + Processor.pageSize;
		for (int i = pageStart; i < pageEnd; i++)       //TODO system.array copy an empty buffer
                {
			Machine.processor().getMemory()[i] = 0;
                }
	}

    public Coff getCoff()
    {
        return coff;
    }
    
    private class CodePage 
    {
		private CodePage(int section, int offset) {
			this.section = section;
			this.offset = offset;
		}

		private int section;
		private int offset;
	}
    
    
}
