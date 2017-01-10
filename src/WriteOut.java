import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;

/**
 * Created by Sara on 08-Dec-16.
 */

public class WriteOut{

    private LinkedHashMap<String, SampleVariantDataObject> sampleVariantHashMap;
    private LinkedHashMap<String, VariantDataObject> variantHashMap;

    public WriteOut(LinkedHashMap<String, SampleVariantDataObject> sampleVariantHashMap,
                    LinkedHashMap<String, VariantDataObject> variantHashMap){
            this.sampleVariantHashMap = sampleVariantHashMap;
            this.variantHashMap = variantHashMap;
        }

    //Create logic to write out variables to a file (tsv)

    //Temp write out all vep annotations
    public void writeOutVepAnnotations() throws Exception{
        final File outputFile = new File("C:\\Users\\Sara\\Documents\\Work\\VCFtoTab\\OutputFiles\\VEP.txt");
        //outputFile.createNewFile();

        //Use bufferedwriter (syntax for Java 7 and above)- try automatically closes the stream
        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(outputFile), "utf-8"))){

            //Set variable to write headers only once at the beginning of the output file
            boolean headers = true;

            //Create key array for robust lookup ordering to ensure that there are no issues if order changes
            List<String> keyArray = new ArrayList<String>();

            //Decides which fields from the CSQ object to print out- put what field is named in keyArray in search
            //Put this in in the order you want them to come out
            List<String> csqFieldsToPrint = new ArrayList<String>();
            csqFieldsToPrint.add("Allele");
            csqFieldsToPrint.add("AFR_MAF");
            csqFieldsToPrint.add("AMR_MAF");
            csqFieldsToPrint.add("EAS_MAF");
            csqFieldsToPrint.add("EUR_MAF");
            csqFieldsToPrint.add("SAS_MAF");
            csqFieldsToPrint.add("ExAC_AFR_MAF");
            csqFieldsToPrint.add("ExAC_AMR_MAF");
            csqFieldsToPrint.add("ExAC_EAS_MAF");
            csqFieldsToPrint.add("ExAC_FIN_MAF");
            csqFieldsToPrint.add("ExAC_NFE_MAF");
            csqFieldsToPrint.add("ExAC_OTH_MAF");
            csqFieldsToPrint.add("ExAC_SAS_MAF");
            csqFieldsToPrint.add("Feature");
            csqFieldsToPrint.add("SYMBOL");
            csqFieldsToPrint.add("HGVSc");
            csqFieldsToPrint.add("HGVSp");
            csqFieldsToPrint.add("Consequence");
            csqFieldsToPrint.add("EXON");
            csqFieldsToPrint.add("INTRON");
            csqFieldsToPrint.add("SIFT");
            csqFieldsToPrint.add("PolyPhen");

            for (String sampleVariantHashMapKey : sampleVariantHashMap.keySet()) {
                String[] splitKey = sampleVariantHashMapKey.split(",");
                String forVariantRetrieval = splitKey[1] + splitKey[2];

                //Retrieve id field which contains dbSNP identifier (if applicable)
                VariantDataObject variantDataObject = variantHashMap.get(forVariantRetrieval);

                for (VepAnnotationObject vepAnnObj : variantDataObject.getCsqObject()) {

                    //Write headers which are not from the CSQ object- set the text to what you want to output
                    if (headers) {

                        writer.write("SampleID");
                        writer.write("\t");
                        writer.write("Chromosome");
                        writer.write("\t");
                        writer.write("Variant");
                        writer.write("\t");
                        writer.write("AlleleFrequency");
                        writer.write("\t");
                        writer.write("Quality");

                        for (String key : vepAnnObj.getVepAnnotationHashMap().keySet()) {
                            //Populate the keyArray with the headers for later retrieval of data
                            keyArray.add(key); //Could consider linking this with what want to call each field in output
                            //writer.write("\t"); //No- print only required data
                            //writer.write(key); //No- print only required data
                        }

                        //Write out the headers- temporary workaround
                        for (String headersKey : csqFieldsToPrint) {
                            //Populate the keyArray with the headers for later retrieval of data
                            writer.write("\t");
                            writer.write(headersKey);
                        }


                        headers = false;
                        writer.newLine();
                    }

                    //Sample name
                    writer.write(sampleVariantHashMapKey.split(",")[0]);
                    writer.write("\t");

                    //Sample chromosome
                    writer.write(sampleVariantHashMapKey.split(",")[1].split(":")[0]);
                    writer.write("\t");

                    //Sample variant as g.posref>alt
                    //System.out.println("g." + (sampleVariantHashMapKey.split(",")[1].split(":")[1]) +
                    //(sampleVariantHashMapKey.split(",")[2]));

                    //Minimise alleles? GenomeVariant class
                    GenomeVariant genomeVariant = new GenomeVariant((sampleVariantHashMapKey.split(",")[1]) +
                        (sampleVariantHashMapKey.split(",")[2]));
                    //genomeVariant.convertToMinimalRepresentation();
                    //System.out.println(genomeVariant);

                    writer.write("g." + (genomeVariant));
                    writer.write("\t");

                    //Sample allele frequency
                    //Truncate output
                    writer.write(String.format("%.2f", sampleVariantHashMap.get(sampleVariantHashMapKey).getAlleleFrequency()));
                    writer.write("\t");

                    //Sample quality
                    writer.write(Integer.toString(sampleVariantHashMap.get(sampleVariantHashMapKey).getGenotypeQuality()));
                    writer.write("\t");

                    for (String keyToPrint : csqFieldsToPrint) {
                        //writer.write(keyToPrint);
                        writer.write(vepAnnObj.getVepEntry(keyToPrint));
                        writer.write("\t");
                        //writer.write(vepAnnObj.getVepEntry(keyArray.get(keyArray.indexOf(keyToPrint))));
                    }

                    /*
                    for (String key : keyArray){
                        writer.write(vepAnnObj.getVepEntry(key));
                        writer.write("\t");
                    }
                    */

                    writer.newLine();
                }
            }
        }
    }
}

