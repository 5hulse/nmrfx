import os
from pyproc import *
FID('../testfids/jcamp/TESTFID.DX')
CREATE('../../tmp/tst_jcamp1d.nv')
acqOrder()
skip(0)
label('1H')
sf('SFO1,1')
sw('SW_h,1')
printInfo()
ref('h2o')
DIM(1)
SB()
ZF()
FT()
PHASE(ph0=50,ph1=0.0)
run()
