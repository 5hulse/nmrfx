import os
from pyproc import *
procOpts(nprocess=6)
FID('../testfids/bruker/hnconus/7')
CREATE('../../tmp/tst_hnconus_skip3.nv')
acqOrder('321')
skip(0,0,1)
label('1H','15N','13C')
acqsize(0,0,0)
tdsize(0,0,0)
sf('SFO1,1','SFO1,2','SFO1,3')
sw('SW_h,1','SW_h,2','SW_h,3')
ref('h2o','N','C')
DIM(1)
SB()
ZF()
FT()
PHASE(ph0=-202.8,ph1=0.0,dimag=True)
EXTRACT(start=245,end=756,mode='region')
#DIM(2,3)
#GRINS(scale=0.5, noise=0.0, zf=1, logToFile=False, synthetic=False, preserve=False)
DIM(2)
SB(c=0.5, apodSize=132)
ZF(size=256)
FT(negatePairs=True,negateImag=True)
PHASE(ph0=0.0,ph1=0.0,dimag=True)
DIM(3)
SB(c=0.5, apodSize=192)
ZF(size=256)
FT(negatePairs=True,negateImag=True)
PHASE(ph0=0.0,ph1=0.0,dimag=True)
#EXTRACT(start=8,end=120,mode='region')
#EXTRACT(start=16,end=150,mode='region')
run()
