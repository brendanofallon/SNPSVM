SNPSVM 
------
Pronounced "snip ess-vee-ehmmm",  SNPSVM is a support vector machine based method for identifying sequence variants in next-gen sequencing data. It takes as input a BAM-formatted alignment of sequencing reads, and emits a VCF formatted file describing where all the SNPs (single nucleotide polymorphisms) are.  
 SNPSVM is in the early stages of development, but can be used to produce a list of variant calls. Work is ongoing, so expect some bugs and lots of changes. 

-----
Installation:

1. Install libsvm

	SNPSVM uses [libsvm](http://www.csie.ntu.edu.tw/~cjlin/libsvm/) to do the heavy lifting. You must download and install libsvm to use SNPSVM (libsvm appears to be in many package repositories, if you're on linux system try using a package manager such as *yum* or *apt*).

2. Download snpsvm.jar 

3. Tell SNPSVM where libsvm is, like this:

		java -jar snpsvm.jar config -add libsvm=/path/to/libsvm

------
Usage:

SNPSVM can do two things:

1. Build a support vector machine model that knows how to call snps

		java -Xmx1g -jar snpsvm.jar buildmodel -R reference.fasta -T some.true.sites.vcf -F some.false.sites.vcf -B input.bam -M output.model


	All arguments are required. The some.true.sites and some.false.sites files are the true and false training data that will be read from the .bam file provided. The resulting model will be written to a file with filename given by -M (output.model in the example above)


2. Use an existing model to call snps on a new .BAM file:

		java -Xmx1g -jar snpsvm.jar predict -R reference.fasta -B input.bam -M input.model -V output.vcf

	Note : SNPSVM comes with a default model that you can use if you don't have a bunch of training data on hand. It's called 'default.model' and lives in the model directory. 

	Optionally, you can use -L to specify the range of sites you'd like to examine in several ways

		-L chrX                   
	Only call variants on chromosome X

		-L chrX:5-100
	Only call variants on chromosome X on sites between 5 and 100

		-L chr4:10-20,chr7:14-17
	Call variants on chromosome 4, sites 10-20 AND chromosome 17, sites 14-17

		-L regions.bed
	Call variants in areas specified in .BED formatted file
	
	Optional variant calling parameters
	
		-q 2.0
	Minimum quality to report a variant
	
		-d 5
	Minimum depth required for reporting a variant
	
		-v 3
	Minimum number of reads containing variant allele required for reporting variants
		
	


To tell SNPSVM to use multiple threads, use the following command:

		java -Xmx1g -jar snpsvm.jar config -add threads=X
Where X is the number of threads you wish to use. Currently only variant calling (not model building) supports the use of multiple threads.

