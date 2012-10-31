
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
	subsamplesize = int( float(totallines)*0.05 )
	os.system(libsvm + "tools/subset.py " + data + " " + str(subsamplesize) + " subsample.csv remainder.csv")
	os.system(libsvm + "svm-train -t 2 -b 1 -c 10.0 remainder.csv testcv.model")
	os.system(libsvm + "svm-predict -b 1 subsample.csv testcv.model subsamplecv.output > cvoutput.txt 2> .garbage.csv ")
	accfh = open("cvoutput.txt", "r")
	line = accfh.readline()
	toks = line.split(" ")
	cvrate = float(toks[2].replace("%", ""))
	print "Got cv of: " + str(cvrate)
	accfh.close()
	return cvrate
	

trainingdata = sys.argv[1]
trainingfh = open(trainingdata, "r")

generateCV(trainingdata)

