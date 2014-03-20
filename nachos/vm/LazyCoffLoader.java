// =============================================================================
//
//            Copyright 2000-2014 Western Digital Corporation
//
//                         All rights reserved.
//
//    This code is CONFIDENTIAL and a TRADE SECRET of Western Digital
//    Corporation and its affiliates ("WDC").  This code is protected
//    under copyright laws as an unpublished work of WDC.  Notice is
//    for informational purposes only and does not imply publication.
//
//    The receipt or possession of this code does not convey any rights to
//    reproduce or disclose its contents, or to manufacture, use, or sell
//    anything that it may describe, in whole or in part, without the
//    specific written consent of WDC.  Any reproduction or distribution
//    of this code without the express written consent of WDC is strictly
//    prohibited, is a violation of the copyright laws, and may subject
//    you to criminal prosecution.
//
//    Use, duplication or disclosure by any commercial industry (public or
//    private), private individual, or by any Government Agency, without
//    an expressed written consent of release from WDC, is subject to
//    restriction set forth in paragraph (b)(3)(B) of the Rights in
//    Technical Data and Computer Software clause in DAR 7-104.9(a).
//
//    Manufacturer is:
//
//        Western Digital Corporation
//        3355 Michelson Drive
//        Suite 100
//        Irvine, CA 92612-0651
//        949-672-7000
//
// =============================================================================

package nachos.vm;

import nachos.machine.Coff;
import nachos.machine.TranslationEntry;

/**
 *
 * @author martin
 */
public class LazyCoffLoader
{
    private Coff coff;

    public LazyCoffLoader(Coff coff)
    {
        this.coff = coff;
    }
    
    
    
    
    
    

    public TranslationEntry load(MemoryPage item, int ppn)
    {
        return new TranslationEntry();
    }

    public Coff getCoff()
    {
        return coff;
    }
    
    
    
    
    
}
