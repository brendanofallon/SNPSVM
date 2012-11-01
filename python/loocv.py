
import sys
import os

libsvm = "/home/brendan/libsvm-3.12/"

#Execute a leave-one-out-cross-validation ("loocv") to determine which 
#factors are most important in generating a high CV rate


def countlines(filename):
	fh = open(filename, "r")
	line = fh.readline()
	count = 0
	while(line):
		count = count + 1
		line = fh.readline()
	return count

def generateCV(data):
	totallines = countlines(data)
	subsamplesize = int( float(totallines)*0.20 )
	cmd = libsvm + "tools/subset.py " + data + " " + str(subsamplesize) + " subsample.csv remainder.csv"
	#print cmd
	os.system(cmd)

	cmd = libsvm + "svm-train -t 2 -b 1 -c 10.0 remainder.csv testcv.model > .ignore.txt "
	#print cmd
	os.system(cmd)
	#print cmd
	cmd = libsvm + "svm-predict -b 1 subsample.csv testcv.model subsamplecv.output > cvoutput.txt"
	os.system(cmd)
	accfh = open("cvoutput.txt", "r")
	line = accfh.readline()
	toks = line.split(" ")
	cvrate = float(toks[2].replace("%", ""))
	#print "Got cv of: " + str(cvrate)
	accfh.close()
	return cvrate

def removeFeature(inputdata, col, outputdata):
	infh = open(inputdata, "r")
	outfh = open(outputdata, "w")
	line = infh.readline()
	while(line):
		toks = line.split("\t")
		outfh.write(toks[0] + "\t")
		for tok in toks[1:]:
			subtok = tok.split(":")
			if (len(subtok)==1):
				outfh.write(subtok)
			else:
				tokcol = int(subtok[0])
				index = tokcol
				if (tokcol > col):
					index = index - 1
				if (not (tokcol == col)):
					outfh.write(str(index) +":" + subtok[1].rstrip())
					if (not tok == toks[len(toks)-1]):
						outfh.write("\t"),
		outfh.write("\n")
		line = infh.readline()
	outfh.close()
	infh.close()

	
	

trainingdata = sys.argv[1]
trainingfh = open(trainingdata, "r")

cvs = []
for i in range(1,10):
	cvs.append( generateCV(trainingdata))

mean = sum(cvs) / float(len(cvs))
print "Base cv for all data : " + str(mean) + "( " + str(min(cvs)) + "-" + str(max(cvs)) + ")"


for col in range(1, 20):
	removedfilename = "removed" + str(col) + ".txt"
	removeFeature(trainingdata, col, removedfilename)
	cvs = []
	for i in range(1,10):
		cvs.append( generateCV(removedfilename) )
	mean = sum(cvs) / float(len(cvs))
	print "Mean cv after removal of column " + str(col) + " : " + str(mean) + "( " + str(min(cvs)) + "-" + str(max(cvs)) + ")"
	

