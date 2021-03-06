import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by Sara on 16-Nov-16.
 */

public class VepAnnotationObject {

    //This class is designed to hold the HashMap of each VEP annotation record held in each separate CSQ field

    private LinkedHashMap<String, String> vepAnnotationHashMap;

    public VepAnnotationObject() {}

    public void setVepRecord(LinkedHashMap<String, String> vepAnnotationHashMap) {
        this.vepAnnotationHashMap = vepAnnotationHashMap;
    }

    public LinkedHashMap getVepRecord(){
        //Returns entire vep record entry as a hash map
        return this.vepAnnotationHashMap;
    }

    public Collection<String> getVepHeaders() { return this.vepAnnotationHashMap.keySet(); }

    public String getAlleleNum(){
        return this.vepAnnotationHashMap.get("ALLELE_NUM");
    } //Note, this is a String

    public String getVepEntry(String key) { return vepAnnotationHashMap.get(key); }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VepAnnotationObject that = (VepAnnotationObject) o;

        return vepAnnotationHashMap != null ? vepAnnotationHashMap.equals(that.vepAnnotationHashMap) : that.vepAnnotationHashMap == null;
    }

    @Override
    public int hashCode() {
        return vepAnnotationHashMap != null ? vepAnnotationHashMap.hashCode() : 0;
    }
}








    /*
    public void tester(){

        ArrayList<String> vepEntries = new ArrayList<>();
        HashMap<String, String> vepHashMap = new HashMap<String, String>();

        for (Object splitEntries: vepAnn) {
            //Iterate over the annotation array containing the different entries for each transcript/effect etc.
            //System.out.println(vepHead);
            System.out.println(splitEntries);
            //Identify a useful unique identifier for each transcript

            String[] annotations = splitEntries.toString().split("\\|");
            String[] headers = vepHead.split("\\|");
            for (String element: annotations) {
                //Obtain each element of the CSQ entry and append to an array
                vepEntries.add(element);
            }
            for (int i=0 ; i < annotations.length; i++) {
                //System.out.println(headers[i]);
                //System.out.println(vepEntries.get(i));
                //Generate HashMap on the fly
                vepHashMap.put(headers[i],vepEntries.get(i));
                //System.out.println(vepHashMap);
            }
        //Test output
        // System.out.println(vepHashMap);
        //System.out.println(vepHashMap.get("Allele"));
        //System.out.println(vepHashMap.get("Feature"));
        }
    }

}
*/
