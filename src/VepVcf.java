//Dependencies htsjdk library 2.7.0
/*
Comment here
 */

import java.io.*;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import htsjdk.samtools.*;
import htsjdk.tribble.*;
import htsjdk.tribble.readers.*;
import htsjdk.variant.variantcontext.*;
import htsjdk.variant.variantcontext.writer.*;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.vcf.*;

import htsjdk.variant.utils.*;
import org.broadinstitute.hellbender.utils.reference.ReferenceUtils.*;
import com.google.common.collect.Multimap;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.TreeMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.ImmutableList;


/**
 * Created by Sara on 08-Nov-16.
 */

public class VepVcf {

    private static final Logger Log = Logger.getLogger(VepVcf.class.getName());
    //private File vcfFilePath;

    //Maybe move this inside the method
    //private LinkedHashMap<GenomeVariant, CsqObject> variantHashMap =  new LinkedHashMap<GenomeVariant, CsqObject>(); //Linked hash map preserves order

    //HashMap containing per-variant and allele information
    private LinkedHashMap<String, VariantDataObject> variantHashMap = new LinkedHashMap<String, VariantDataObject>();

    //HashMap containing per sample, per position (and contig), (and including alternate allele information)
    private LinkedHashMap<String, SampleVariantDataObject> sampleVariantHashMap = new LinkedHashMap<String, SampleVariantDataObject>();

    //Complete this
    //private LinkedHashMap<String, String> finalHashMap = new LinkedHashMap<String, String>();


    public VCFFileReader openFiles(File vcfFilePath) { //throws IOException  {
        Log.log(Level.INFO, "Opening VEP VCF file");
        //Read in the file
        //try(final VCFFileReader vcfFile = new VCFFileReader(vcfFilePath, false)){
        //VCFHeader currentHeader = vcfFile.getFileHeader();
        //System.out.println(currentHeader); //The file header

        try {
            final VCFFileReader vcfFile = new VCFFileReader(vcfFilePath, false);
            return vcfFile;
        } catch (Exception e) {
            Log.log(Level.SEVERE, "Could not read VCF file: " + e.getMessage());
        }
        return null; //This is what it returns if the try catch block fails- syntax??
    }

    public void parseVepVcf(VCFFileReader vcfFile) {
        Log.log(Level.INFO, "Parsing VEP VCF file");
        //For the alternate alleles
        //Required for code execution as otherwise variable is initialised only in else clause
        boolean variantFiltered = false; //Default setting
        boolean variantSite = false; //Default setting
        String idField = null; //Default setting

        for (final VariantContext vc : vcfFile) {

            ///Work on allele minimisation here?///- no- this is all the variants- maybe do per sample?
            //Each allele is associated with its allelenum (in case ordering is changed later on and so that the
            //genotyping part of the code has access to this information)

            //Create an object to store the alleles- preferably immutable as allelenum will be dependent on order- not needed
            List<Allele> allAlleles = Collections.unmodifiableList(vc.getAlleles());

            //Store the alleles that are not the reference allele
            List<Allele> altAlleles = vc.getAlternateAlleles();

            //Make the object to hold the annotations- note this currently iterates every time and gets the same headers (same vcf)
            //Obtain keys for each transcript entry (header in vcf file)
            CsqUtilities currentCsqRecord = new CsqUtilities();

            //Bug fixing 19/12/2016- singletons weren't working
            List attribute = vc.getAttributeAsList("CSQ");

            ArrayList<String> attributeArr = new ArrayList<String>();

            for (int i = 0; i < attribute.size(); i++){
                attributeArr.add(attribute.get(i).toString());
            }

            ArrayListMultimap<Integer, VepAnnotationObject> csq = currentCsqRecord.createCsqRecordOfVepAnnObjects(
                    currentCsqRecord.vepHeaders(vcfFile), attributeArr);

            //Create a CsqObject (optional step)
            CsqObject currentCsqObject = new CsqObject();
            currentCsqObject.setCsqObject(csq);

            variantFiltered = vc.isFiltered();
            variantSite = vc.isVariant();
            idField = vc.getID();

            for (int allele = 0; allele < altAlleles.size(); allele++) {

                //Avoid storing an empty object (as there are no Vep annotations) nested within the VariantDataObject
                //* just denotes an overlapping indel and is not a SNV at that position
                if (altAlleles.get(allele).toString().equals("*")) {
                    //Break out of for loop and start next iteration
                    continue;

                }

                //Key
                GenomeVariant variantObject = createAlleleKey(vc, altAlleles.get(allele).toString());

                //Allele num starts at 1 for the altAlleles, as 0 is the reference allele
                ArrayList<VepAnnotationObject> alleleCsq = currentCsqObject.getSpecificVepAnnObjects(allele + 1);

                //System.out.println(alleleCsq);

                //Data
                VariantDataObject currentVariantDataObject = new VariantDataObject(alleleCsq,
                        variantFiltered, variantSite, idField);

                variantHashMap.put(variantObject.toString(), currentVariantDataObject);

            }


            //This part of the code associates the specific alleles and some additional specific sample-allele metadata
            //with the different samples present in the vcf

            //Extract data associated with each specific genotype within the variant context
            GenotypesContext gt = vc.getGenotypes();
            //Iterate through each sample entry within the variant context
            Iterator<Genotype> gtIter = gt.iterator();
            while (gtIter.hasNext()) {
                Genotype currentGenotype = gtIter.next();

                //Don't add the variant to the sample if there is no call or only a hom ref call
                if (!(currentGenotype.isHomRef()) && !(currentGenotype.isNoCall())) {

                    //Create immutable list here as order is important
                    List<Allele> currentGenotypeAlleles = Collections.unmodifiableList(currentGenotype.getAlleles());

                    //Zygosity
                    String zygosity = obtainZygosity(currentGenotype);

                    //Creation of SampleData object- the data associated once with each sample
                    //(rather than with each specific allele in the sample)
                    //This object will be encapsulated within the object which incorporates the variant-specific
                    //sample information- called SampleVariantDataObject
                    //SampleDataObject currentSampleDataObject =
                    //new SampleDataObject(currentGenotype.getSampleName(),
                    //currentGenotype.isFiltered(), currentGenotype.isMixed(),
                    //currentGenotype.getPloidy(), zygosity, currentGenotype.getGQ());


                    //Obtain depth of both alleles at the locus
                    int locusDepth = calcLocusDepth(currentGenotypeAlleles, allAlleles, currentGenotype);

                    /*
                    int locusDepth = 0; //Better solution for an empty integer? -1?- reset at each allele
                    for (Allele currentAllele : currentGenotypeAlleles) {
                        locusDepth += (currentGenotype.getAD()[allAlleles.indexOf(currentAllele)]);
                    }
                    */

                    //System.out.println(locusDepth);

                    //Creation of the SampleVariantData object and sampleVariantHashMap
                    //Locate what is the key in the variantHashMap for the specific allele
                    //Generate keyForVariant
                    for (Allele currentAllele : currentGenotypeAlleles) {

                        //System.out.println(currentAllele);

                        //Skip alleles that we don't want to store. This won't create a problem for allele depth
                        //as locus depth is calculated above

                        int alleleNum = allAlleles.indexOf(currentAllele);
                        int alleleDepth = currentGenotype.getAD()[alleleNum];
                        double alleleFrequency = calcAlleleFrequency(locusDepth, alleleDepth);

                        GenomeVariant keyForVariant = createSampleVariantKey(vc, currentAllele);

                        //Key
                        SampleVariant currentSampleVariant = new SampleVariant(currentGenotype.getSampleName(),
                                vc.getContig(), vc.getStart(), currentGenotypeAlleles, currentAllele,
                                vc.getReference());

                        //Skip reference allele (don't store it)
                        if (currentAllele.isNonReference() && !(currentAllele.toString().equals("*"))) {

                         /*
                        //Skip the overlapping indel case (don't store reference allele)
                        //* just denotes an overlapping indel and is not a SNV at that position
                        if (currentAllele.toString().equals("*")){
                                //Break out of for loop and start next iteration
                                System.out.println(allAlleles);
                                System.out.println(allAlleles.indexOf(currentAllele));
                                System.out.println(currentGenotype.getAD()[allAlleles.indexOf(currentAllele)]);
                                continue;
                        }
                        */

                            //System.out.println("Variant to store"); //for test

                            //Data
                            SampleVariantDataObject currentSampleVariantDataObject =
                                    new SampleVariantDataObject(keyForVariant, alleleDepth, alleleNum,
                                            currentGenotype.getSampleName(), currentGenotype.isFiltered(),
                                            currentGenotype.isMixed(), currentGenotype.getPloidy(), zygosity,
                                            currentGenotype.getGQ(), alleleFrequency);

                            sampleVariantHashMap.put(currentSampleVariant.toString(), currentSampleVariantDataObject);
                        }

                    }

                }

            }

            //break; //first allele only for ease of testing

        }
    }


        //Test hash map is working correctly

        //System.out.println(sampleVariantHashMap);
        //System.out.println(variantHashMap);

        /*
        System.out.println(sampleVariantHashMap.get("23M,1:241663902 TGAGA"));
        System.out.println(sampleVariantHashMap.get("23M,1:241663902 T").getVariantObjectKey());
        System.out.println(sampleVariantHashMap.get("23M,1:241663902 TGAGA").getVariantObjectKey());
        System.out.println(variantHashMap.get(sampleVariantHashMap.get("23M,1:241663902 TGAGA").getVariantObjectKey()));
        System.out.println(variantHashMap.get("1:241663902TGA>TGAGA"));


        Iterator<VepAnnotationObject> vpIter = variantHashMap.get(sampleVariantHashMap.get("23M,1:241663902 TGAGA").
                getVariantObjectKey()).getCsqObject().getEntireCsqObject().iterator();
        while (vpIter.hasNext()){
            System.out.println(vpIter.next().getVepRecord());

        */



        //This is hom ref so although it is in the sample variant hash map it won't be found in the variant hash map
        //This is het ref not hom ref, hom ref has not been stored
        //System.out.println(variantHashMap.get("1:241663902TGA>TGA"));


        //return variantHashMap;
        //return sampleHashMap

    public GenomeVariant createAlleleKey(VariantContext vc, String altAllele) { //LinkedHashMap
        //Log.log(Level.INFO, "Parsing Alleles");
        //Requires String for GenomeVariant class
        //This is intended as the key to the hashmap
        return new GenomeVariant(vc.getContig(), vc.getStart(),
                vc.getReference().toString().replaceAll("\\*", ""), altAllele);
    }


    public GenomeVariant createSampleVariantKey(VariantContext vc, Allele currentAllele){

        return new GenomeVariant(vc.getContig(), vc.getStart(), vc.getReference().toString().replaceAll("\\*", ""),
                currentAllele.toString().replaceAll("\\*", ""));

    }

    public String obtainZygosity(Genotype currentGenotype){

        String zygosity = "UNDETERMINED";

        if (currentGenotype.isHom()) {
            zygosity = "HOM";
        } else if (currentGenotype.isHet()) {
            zygosity = "HET";
        }

        return zygosity;
    }

    public double calcAlleleFrequency(int locusDepth, int alleleDepth) {
        double alleleFrequency = ((double)alleleDepth/(double)locusDepth);
        //System.out.println(alleleDepth);
        //System.out.println(locusDepth);
        //System.out.println(alleleFrequency);

        return alleleFrequency;
    }

    public int calcLocusDepth(List<Allele> Alleles, List<Allele> locusAlleles, Genotype currentGenotype){
        int locusDepth = -1;
        for (Allele currentAllele : Alleles) {

            //System.out.println(currentAllele);
            locusDepth += (currentGenotype.getAD()[locusAlleles.indexOf(currentAllele)]);
        }

        //System.out.println(locusDepth);
        return locusDepth;
    }


    public LinkedHashMap<String, VariantDataObject> getVariantHashMap() {return this.variantHashMap;}

    public LinkedHashMap<String, SampleVariantDataObject> getSampleVariantHashMap() {return this.sampleVariantHashMap;}

}

