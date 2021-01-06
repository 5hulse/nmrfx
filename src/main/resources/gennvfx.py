import sys
from refine import *
from molio import readYaml
import os
import re
import osfiles
import runpy
import anneal
from optparse import OptionParser
from org.yaml.snakeyaml import Yaml
from java.util import LinkedHashMap

yamlStr = '''  
anneal :
    dynOptions :
        steps : 15000
        highTemp : 5000.0
    param :
        swap : 20
    force :
        irp : 0.05
'''


def genNEFYaml(fileName):

    input = 'nef : ' + fileName + '\n'
    input += yamlStr

    print input
    
    yaml = Yaml()
    data = yaml.load(input)
    return data

def dumpStages():
    global refiner
    refiner=refine()
    dOpt = dynOptions()
    stages = anneal.getAnnealStages(dOpt,{})
    for stage in stages:
        yaml = Yaml()
        data = yaml.load(str(stage))
        print 'stage:'
        for k in data:
            v = data[k]
            if isinstance(v,LinkedHashMap):
                print "    " + k + " : "
                for k2 in v:
                    v2 = v[k2]
                    print "        " + k2 + " : " + str(v2)
            else:
                print "    " + k + " : " + str(v)

def parseArgs():
    homeDir = os.getcwd()
    parser = OptionParser()
    parser.add_option("-s", "--seed", dest="seed",default='0', help="Random number generator seed")
    parser.add_option("-d", "--directory", dest="directory",default=homeDir, help="Base directory for output files ")
    parser.add_option("-v", "--report", action="store_true",dest="report",default=False, help="Report violations in energy dump file ")
    parser.add_option("-g", "--stages", action="store_true",dest="dumpStages",default=False, help="Dump stages")
    parser.add_option("-r", "--refine", dest="refineFile",default="", help="Name of file to refine ")


    (options, args) = parser.parse_args()

    if options.dumpStages:
        dumpStages()
        exit(0)

    homeDir = options.directory
    outDir = os.path.join(homeDir,'output')
    finDir = os.path.join(homeDir,'final')
    refineDir = os.path.join(homeDir,'refine')
    seed = long(options.seed)
    report = options.report
    refineFile = options.refineFile
    if refineFile != '':
        (refineDir,refineFileName) = os.path.split(refineFile)
        pat = re.compile(r'(.*)\.(pdb|ang)')
        seedMatch = pat.match(refineFileName)
        if seedMatch:
            refineRoot = seedMatch.group(1)
    argFile = args[0]

    dataDir = homeDir + '/'
    outDir = os.path.join(homeDir,'output')
    if not os.path.exists(outDir):
        os.mkdir(outDir)

    data = None
    if argFile.endswith('.yaml'):
        data = readYaml(argFile)
    elif argFile.endswith('.nef'):
        data = genNEFYaml(argFile)

    if data != None:
        global refiner
        refiner=refine()
        osfiles.setOutFiles(refiner,dataDir, seed)
        refiner.setReportDump(report) # if -r seen == True; else False
        refiner.rootName = "temp"
        refiner.loadFromYaml(data,seed)
        if 'anneal' in data:
            if refineFile != '':
                if refineDir == '':
                    refineDir = '.'
                refiner.outDir = refineDir
                if not os.path.exists(refiner.outDir):
                    os.mkdir(refiner.outDir)
                refiner.rootName = 'ref_'+refineRoot
                print 'rootname',refiner.rootName
                refiner.readAngles(refineFile)
                refiner.refine(refiner.dOpt)
            else:
                refiner.anneal(refiner.dOpt)
        if 'script' in data:
            runpy.run_path(data['script'], init_globals=globals())
        if len(args) > 1:
            scriptFile = args[1]
            runpy.run_path(scriptFile, init_globals=globals())
        refiner.output()
    else:
        if argFile.endswith(".py"):
            runpy.run_path(argFile)
        else:
            print "script files (" + argFile + ")must end in .py"
            exit(1)

parseArgs()
sys.exit()
