SNPSVM 
------
Pronounced "snip ess-vee-ehmmm",  SNPSVM is a support vector machine based method for identifying sequence variants in next-gen sequencing data. It takes as input a BAM-formatted alignment of sequencing reads, and emits a VCF formatted describing where all the SNPs (single nucleotide polymorphisms) are. At least, that's the idea. 
 SNPSVM is in the early stages of development, but can be used to produce a list of variant calls. Work is ongoing, so expect some bugs and lots of changes. 

-----
Installation:

1. Install libsvm

SNPSVM uses libsvm (http://www.csie.ntu.edu.tw/~cjlin/libsvm/) to do the heavy lifting. You must download and install libsvm to use SNPSVM

2. Download and snpsvm.jar 

3. Tell SNPSVM where libsvm is, like this:
    java -jar snpsvm.jar config -add libsvm=/path/to/libsvm

------
Usage:

SNPSVM can do two things:
1. Build a support vector machine model that knows how to call snps

    java -Xmx1g -jar snpsvm.jar buildmodel -R reference.fasta -T some.true.sites.vcf -F some.false.sites.vcf -B input.bam -M output.model

All arguments are required


2. Use an already-constructed model to call snps on a new .BAM file:

    java -Xmx1g -jar snpsvm.jar predict -R reference.fasta -B input.bam -M input.model -V output.vcf

Optionally, you can use -L to specify the range of sites you'd like to examine in several ways:

    -L chrX                   
Only call variants on chromosome X
    -L chrX:5-100
Only call variants on chromosome X on sites between 5 and 100
    -L chr4:10-20,chr7:14-17
Call variants on chromosome 4, sites 10-20 AND chromosome 17, sites 14-17
    -L regions.bed
Call variants in areas specified in .BED formatted file



