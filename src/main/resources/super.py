import sys
import glob
import os.path
from org.nmrfx.structure.chemistry import Molecule
from org.nmrfx.structure.chemistry import SuperMol
from org.nmrfx.structure.chemistry.io import PDBFile
import molio
from java.util import TreeSet
import argparse

def median(values):
    values.sort()
    if (len(values) == 0):
        return None
    elif (len(values) == 1):
        return values[0]
    else:
        n = len(values)
        v1 = values[n/2]
        v2 = values[(n-1)/2]
        return (v1+v2)/2.0

def loadPDBModels(files):
    try:
        fileName = files[0]
    except IndexError:
	errMsg = "\nCan't load final PDB models. Please inspect the 'final' and 'output' directories."
        errMsg += "\nNote: problem related to the retrieval of summary lines in temp*.txt."
        raise LookupError(errMsg)
    pdb = PDBFile()
    molecule = pdb.read(fileName)
    iFile = 1
    for file in files:
        pdb.readCoordinates(file,iFile,False, False)
        iFile += 1
    return molecule

def parseArgs():
    parser = argparse.ArgumentParser(description="super options")
    parser.add_argument("-r", dest="resListE", default='', help="Residues to exclude from comparison. Can specify semi-colon-separated chains and comma-separated residue ranges and individual values (e.g. A: 2, 3; B: 2-5, 10).")
    parser.add_argument("-a", dest="atomListE", default='', help="Atoms to exclude from comparison.")
    parser.add_argument("-R", dest="resListI", default="*", help="Residues to include in comparison")
    parser.add_argument("-A", dest="atomListI", default="*", help="Atoms to include in comparison")
    parser.add_argument("-f", dest="calcPDBList", help="Comma-separated list of calculated PDBs to compare to reference PDB (e.g. final1.pdb, final2.pdb). Use '*' to use all final PDBs.")
    parser.add_argument("-F", dest="refPDBFile", help="Reference PDB file.")
    # parser.add_argument("fileNames",nargs="*")
    args = parser.parse_args()

    resArgE = args.resListE
    resListE = getResList(resArgE)
    resArgI = args.resListI
    resListI = getResList(resArgI)
    resListEI = [value for value in resListE if value in resListI]
    if resListEI != []:
        print "Error: residues", resListEI, "cannot be both excluded and included in comparison."
        sys.exit()

    atomListES = args.atomListE.split(",")
    atomListE = [atom.strip() for atom in atomListES if atom != ""]
    atomListIS = args.atomListI.split(",")
    atomListI = [atom.strip() for atom in atomListIS if atom != ""]
    atomListEI = [value for value in atomListE if value in atomListI]
    if atomListEI != []:
        print "Error: atoms", atomListEI, "cannot be both excluded and included in comparison."
        sys.exit()

    fileArg = args.calcPDBList
    if fileArg != None:
        fileNames = makeFileList(fileArg)
    else:
        print "Error: Calculated structure PDB file(s) must be specified with the -f flag."
        sys.exit()
    refArg = args.refPDBFile
    if refArg != None:
        fileNames.append(args.refPDBFile)
    else:
        print "Error: Reference structure PDB file must be specified with the -F flag."
        sys.exit()

    # print resListE, atomListE, resListI, atomListI, fileNames

    return resListE, atomListE, resListI, atomListI, fileNames

def getResList(resArg):
    if ";" in resArg: #multiple chains separated by ; (e.g. A: 2-5, 10; B: 1-4, 12)
        resList1 = []
        chainSplit = resArg.split(";")
        for chainArgs in chainSplit:
            chain = chainArgs.split(":")[0].strip();
            resArgs = chainArgs.split(":")[1].strip();
            resList1.append(makeResList(resArgs, chain))
        resList = [item for sublist in resList1 for item in sublist]
    else: #single chain
        if ":" in resArg: #chain specified (e.g. B: 5-10)
            chain = resArg.split(":")[0].strip();
            resArgs = resArg.split(":")[1].strip();
            resList = makeResList(resArgs, chain)
        else: #chain not specified (e.g. 2-5). Defaults to chain A
            resList = makeResList(resArg, "A")
    return resList

def makeResList(resArg, chain):
    # print resArg
    if resArg == "":
        resList = []
    elif resArg == "*":
        resList = [resArg]
    else:
        if ("-" in resArg) and ("," in resArg): #comma-separated ranges (e.g. 2-5, 10-12) or ranges and individual residues (e.g. 2-5, 7, 10-12, 20)
            resList1 = []
            resArgSplit = resArg.split(",")
            for val in resArgSplit:
                if "-" in val: #range of residues
                    valSplit = val.split("-")
                    firstRes = int(valSplit[0].strip())
                    lastRes = int(valSplit[1].strip()) + 1
                    resList1.append([res for res in range(firstRes, lastRes)])
                else: #single residue
                    resList1.append([val.strip()])
            resList = [chain + "." + str(item) for sublist in resList1 for item in sublist]
        elif "-" in resArg: #single range of residues (e.g. 2-5)
            resArgSplit = resArg.split("-")
            firstRes = int(resArgSplit[0].strip())
            lastRes = int(resArgSplit[1].strip()) + 1
            resList = [chain + "." + str(res) for res in range(firstRes, lastRes)]
        elif "," in resArg: #comma-separated individual residues (e.g. 2, 3, 4, 5)
            resListS = resArg.split(",")
            resList = [chain + "." + str(res).strip() for res in resListS if res != ""]

    return resList

def makeFileList(fileArg):
    # print fileArg
    if fileArg == "":
        fileList = []
    elif fileArg == "*": #use all final.pdb files
        fileList = glob.glob(os.path.join('final','final*.pdb'))
    elif "," in fileArg: #comma-separated individual files (e.g. final1.pdb, final2.pdb)
        fileListS = fileArg.split(",")
        fileList = [os.path.join('final',file.strip()) for file in fileListS if file != ""]
    else: #single final.pdb file
        fileList = [os.path.join('final',fileArg.strip())]

    return fileList

def findRepresentative(mol, resNums='*',atomNames="ca,c,n,o,p,o5',c5',c4',c3',o3'"):
    treeSet = TreeSet()
    structures = mol.getStructures()
    for structure in structures:
        if structure != 0:
            treeSet.add(structure)
    nFiles = treeSet.size()

    doSelections(mol, resNums,atomNames)
    sup = SuperMol(mol)
    mol.setActiveStructures(treeSet)

    superResults = sup.doSuper(-1, -1, False)
    totalRMS = 0.0
    averageToI = {}
    n = len(superResults)
    for superResult in superResults:
        iFix = superResult.getiFix()
        iMove = superResult.getiMove()
        rms = superResult.getRms()
        totalRMS += rms
        if iFix not in averageToI:
            averageToI[iFix] = 0.0
        averageToI[iFix] += rms
    minRMS = 1.0e6
    #for i in range(1,nFiles+1):
    for i in treeSet:
        averageToI[i] /= nFiles-1
        if averageToI[i] < minRMS:
            minRMS = averageToI[i]
            minIndex = i
    avgRMS = totalRMS/len(superResults)
    return (minIndex, minRMS, avgRMS)

def findCore(mol, minIndex, excludeRes, excludeAtoms, includeRes, includeAtoms, atomNames="ca,c,n,o,p,o5',c5',c4',c3',o3'"):
    atomNameList = atomNames.split(',')
    sup = SuperMol(mol)
    superResults = sup.doSuper(minIndex, -1, True)
    mol.calcRMSD()
    polymers = mol.getPolymers()
    resRMSs = []
    resValues = []
    excludeAtomsL = [atom.lower() for atom in excludeAtoms]
    includeAtomsL = [atom.lower() for atom in includeAtoms]
    for polymer in polymers:
        residues = polymer.getResidues()
        chainCode = polymer.getName()
        for residue in residues:
            res = chainCode + "." + residue.getNumber()
            if res in excludeRes or ("*" not in includeRes and res not in includeRes):
                continue
            atoms = residue.getAtoms('*')
            resSum = 0.0
            nAtoms = 0
            for atom in atoms:
                aName = atom.getName().lower()
                if aName in excludeAtomsL or ("*" not in includeAtomsL and aName not in includeAtomsL):
                    continue
                if aName in atomNameList:
                    resSum += atom.getBFactor()
                    nAtoms += 1
                # print aName, resSum, nAtoms
            if nAtoms > 0:
                resRMS = resSum/nAtoms
                resRMSs.append(resRMS)
                resValues.append((polymer.getName(),residue.getNumber(),resRMS))
    med = median(resRMSs)

    coreRes = []
    lastPolymer = ""
    state = "out"
    (polymer,lastNum,rms) = resValues[-1]
    for (polymer,num,rms) in resValues:
        # print polymer, num, rms
        last = ""
        if rms < 2.0*med:
            newState = "in"
        else:
            newState = "out"
        if state == "out":
            if newState == "out":
                pass
            else:
                start = num
                #coreRes.append(num)
        else:
            if newState == "out":
                coreRes.append((start,lastRes))
            elif num == lastNum:
                coreRes.append((start,num))
                #coreRes.append(num)
        state = newState
        lastRes = num

    resSelect = []
    for (start,end) in coreRes:
        if (start == end):
           resSelect.append(start)
        else:
           resSelect.append(start+'-'+end)
    return resSelect

def doSelections(mol, resSelects, atomSelect):
    polymer = 'A'
    mol.selectAtoms("*.*")
    mol.setAtomProperty(2,False)
    for resSelect in resSelects:
        selection = resSelect+'.'+atomSelect
        mol.selectAtoms(selection)
        mol.setAtomProperty(2,True)


def superImpose(mol, target,resSelect,atomSelect="ca,c,n,o,p,o5',c5',c4',c3',o3'"):
    doSelections(mol, resSelect,atomSelect)
    sup = SuperMol(mol)
    superResults = sup.doSuper(target, -1, True)

def saveModels(mol, files):
    active = mol.getActiveStructures()
    for (i,file) in zip(active,files):
        (dir,fileName) = os.path.split(file)
        newFileName = 'sup_' + fileName
        newFile = os.path.join(dir,newFileName)
        molio.savePDB(mol, newFile, i)

def runSuper(excludeRes, excludeAtoms, includeRes, includeAtoms, files, newBase='super'):
    mol = loadPDBModels(files)
    polymers = mol.getPolymers()
    if len(polymers) > 0:
        (minI,rms,avgRMS) = findRepresentative(mol)
        print 'repModel',minI,'rms',rms,'avgrms',avgRMS
        coreRes = findCore(mol, minI, excludeRes, excludeAtoms, includeRes, includeAtoms)
        print 'coreResidues',coreRes
        (minI,rms,avgRMS) = findRepresentative(mol, coreRes)
        print 'repModel',minI,'rms',rms,'avgrms',avgRMS
        superImpose(mol, minI, coreRes)
    else:
        (minI,rms,avgRMS) = findRepresentative(mol,'*','c*,n*,o*,p*')
        print 'repModel',minI,'rms',rms,'avgrms',avgRMS
        coreRes = ['*']
        superImpose(mol, minI, coreRes,'c*,n*,o*,p*')
    (dir,fileName) = os.path.split(files[0])
    (base,ext) = os.path.splitext(fileName)

    saveModels(mol, files)
