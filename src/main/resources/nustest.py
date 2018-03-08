import os
import os.path
import glob
import sys.argv
import argparse
from itertools import izip

from org.nmrfx.processor.datasets import Dataset
from pyproc import *
from dscript import nd

def parseArgs():
    parser = argparse.ArgumentParser(description="Test NUS Processing")
    parser.add_argument("-b",dest='beta', type=float, default=12.0,help="Kaiser beta value")
    parser.add_argument("-t",dest='tolFinal', type=float, default=2.5,help="Final Tolerance")
    parser.add_argument("-T",dest='threshold', type=float, default=0.0,help="Skip Threshold")
    parser.add_argument("-f",dest='istFraction', type=float, default=0.9,help="IST Fractional Threshold")
    parser.add_argument("-m",dest='muFinal', type=float, default=6.0,help="Final mu")
    parser.add_argument("-i",dest='nInner',type=int, default=20,help="Number of inner iterations")
    parser.add_argument("-o",dest='nOuter',type=int, default=15,help="Number of outer iterations")
    parser.add_argument("-l",dest='schedLines',default="125",help="Number of lines in schedule")
    parser.add_argument("-s",dest='schedType',default='pg',help="Schedule type")
    parser.add_argument("-n",dest='schedNum',default='0',help="Schedule number")
    parser.add_argument("-a",dest='nusAlg',default='NESTA',help="NUS Mode")
    parser.add_argument("-w",dest='apod',default='kaiser',help="Apodization Window")
    parser.add_argument("-u",dest='uniform',action='store_true', help="NUS Mode")
    parser.add_argument("fileNames",nargs="*")
    args = parser.parse_args()
    return args

def report(args):
        result = "alg %s beta %f tolFinal %.1f muFinal %.1f nOuter %d nInner %d" % (args.nusAlg, args.beta, args.tolFinal, args.muFinal, args.nOuter, args.nInner)
        return result

def analyzeLog(fidDirName):
    fileName = os.path.join(fidDirName,'nesta','*.log')
    files = glob.glob(fileName)
    sum = 0.0
    for file in files:
        with open(file,'r') as f1:
            for line in f1:
                if line.startswith('iNorm'):
                   fields = line.split()
                   fNorm = float(fields[3].strip())
                   sum += fNorm
    return sum

def compareFiles(dName1, dName2, threshold):
    dataset1 = Dataset(dName1,'data1.nv',False)
    dataset2 = Dataset(dName2,'data2.nv',False)
    iDim=0

    sum = 0.0
    for (vec1,vec2) in izip(dataset1.vectors(iDim), dataset2.vectors(iDim)):
        vec1.softThreshold(threshold)
        vec2.softThreshold(threshold)
        vec3 = vec1 - vec2
        vec3.abs()
        sum += vec3.sum().getReal()
    return sum

def testData(fidRootDir, datasetDir, challenge, sample, expName, pars):
    args = parseArgs()
    schedType=args.schedType
    schedLines=args.schedLines
    schedNum=args.schedNum
    mode=args.nusAlg

    fidFileDir = os.path.join(fidRootDir,challenge,sample+"-"+expName,"US_data")
    schedFileDir = os.path.join(fidRootDir,challenge,sample+"-"+expName,"sample_schedules")
    uniformFileName=sample+"-"+expName+"-uniform.nv"
    #statFileName=sample+"-"+expName+"-uniform.txt"
    pipeFileName = "pipe-"+sample+"-"+expName+"-uniform"
    if args.uniform:
        scheduleFileName = None
        datasetFileName=uniformFileName
        mode = "ft"
    else:
        scheduleName=sample+"-"+expName+"-"+schedType+"-"+schedLines+"-"+schedNum
        scheduleFileName = os.path.join(schedFileDir,scheduleName+".txt")
        fileRootName = scheduleName+"-"+mode
        datasetFileName = fileRootName+".nv"
        #statFileName = fileRootName+".txt"
        pipeFileName = "pipe-"+fileRootName

    if not os.path.exists(datasetDir):
        os.mkdir(datasetDir)

    pipeFileDir = os.path.join(datasetDir, pipeFileName)
    if not os.path.exists(pipeFileDir):
        os.mkdir(pipeFileDir)

    statFileName = sample + "-" + expName + "-" + "report.txt"

    datasetFile = os.path.join(datasetDir, datasetFileName)
    uniformFile = os.path.join(datasetDir, uniformFileName)
    statFile = os.path.join(datasetDir, statFileName)
    pipeFile = os.path.join(pipeFileDir, "test%03d.ft")


    execNUS(fidFileDir, datasetFile, scheduleFileName, pars, args)

    dataset=nd.open(datasetFile)
    nd.toPipe(dataset, pipeFile)

    fNormSum = analyzeLog(fidFileDir)
    compareSum = compareFiles(datasetFile,uniformFile, pars['threshold'])
    with open(statFile,'a') as fStat:
        outStr = datasetFileName + " l1 " + str(fNormSum) + " diff " +  str(compareSum) + " " + report(args) + "\n"
        fStat.write(outStr)

def getNegation(pars, varName, nDim=3):
    if varName in pars:
        result = pars[varName]
    else:
        result = [False]*nDim
    return result 
        
def execNUS(fidDirName, datasetName, scheduleName,  pars, args):
    FID(fidDirName)
    CREATE(datasetName)
    phases=pars['phases']
    range=pars['range']
    tdcomb = pars['tdcomb']
    negPairs = getNegation(pars, 'negPairs', 3)
    negImag = getNegation(pars, 'negImag', 3)
    mode = args.nusAlg
    if scheduleName != None:
        readNUS(scheduleName)
    else:
        mode = "ft"
    acqOrder('321')
    acqarray(0,0,0)
    skip(0,0,0)
    label('1H','15N','13C')
    acqsize(0,0,0)
    tdsize(0,0,0)
    sf('SFO1,1','SFO1,2','SFO1,3')
    sw('SW_h,1','SW_h,2','SW_h,3')
    ref(4.773,'N','C')
    apodize=True
    DIM(1)
    if tdcomb != "":
        TDCOMB(coef=tdcomb)
    if args.apod == "blackman":
        BLACKMAN()
    else:
        KAISER()
    ZF()
    FT()
    (ph0,ph1) = phases[0]
    PHASE(ph0=ph0,ph1=ph1,dimag=False)
    if range != None:
        start,end = range
        EXTRACT(start=start,end=end,mode='region')
    DIM(2,3)
    if args.apod == "blackman":
        BLACKMAN(c=0.5)
    else:
        KAISER(c=0.5, beta=args.beta)
    for dim,phase in enumerate(phases[1:]):
        if len(phase) == 2:
            (ph0,ph1) = phase
            PHASEND(ph0=ph0,ph1=ph1,dim=dim+1)
    if mode == "NESTA":
        NESTA(tolFinal=args.tolFinal, muFinal=args.muFinal, threshold=args.threshold, nOuter=args.nOuter, nInner=args.nInner, logToFile=True)
    elif mode == "IST":
        ISTMATRIX(threshold=args.istFraction, iterations=args.nInner, alg="std")
    DIM(2)
    ZF()
    FT(negatePairs=negPairs[1], negateImag=negImag[1])
    REAL()
    DIM(3)
    ZF()
    #FT(negatePairs=True,negateImag=True)
    FT(negatePairs=negPairs[2], negateImag=negImag[2])
    REAL()
    run()
