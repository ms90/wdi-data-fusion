/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uma.wdi.fusion;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uma.wdi.fusion.input.Dataset;
import uma.wdi.fusion.input.Duplicates;
import uma.wdi.fusion.resolution.AbstractResolutionFunction;
import uma.wdi.fusion.resolution.Average;
import uma.wdi.fusion.resolution.Maximum;
import uma.wdi.fusion.resolution.MostRecent;
import uma.wdi.fusion.resolution.MostTrusted;
import uma.wdi.fusion.resolution.Vote;
import uma.wdi.fusion.utils.XMLUtils;


/* Main class of the Data Fusion WDI project
 * Define your inputs, conflict resolution strategies and run evaluation here
 * 
 *  @author Volha
 * */
public class DataFusion 
{
	private static Dataset unionDs = new Dataset();
	private static DataUnion du = new DataUnion();

	// Read duplicate pairs, create clusters
	// Create merged representation
	// Calculate density for input datasets
	// Calculate density and consistency for merged dataset
	// Create the first part of fusion report
	public static  boolean runDataUnion(String rootElementName, String idDataPath, String idProvPath, 
			Set<String> filesDuplicates, Set<String> filesData, 
			String unionFn, String fnFusionReport)
	{
		// read duplicate clusters
		Duplicates dupl = new Duplicates();
		dupl.read(filesDuplicates);
		
		PrintStream out = System.out;
		if (fnFusionReport != null)
		try 
		{
			out = new PrintStream(new FileOutputStream(fnFusionReport));
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}

		// read input datasets, calculate dataset-centric measures (slide 56)
		Set<Dataset> datasets = new HashSet<Dataset>();
		for(String fn : filesData) 
		{
			Dataset ds = new Dataset();
			if (!ds.loadFromFile(fn, idDataPath, idProvPath, true, false)) return false;
			datasets.add(ds);
		}
		
		// create a merged representation, calculate consistency (slide 57)
		// DataUnion du = new DataUnion();
		if (du.createUnion(datasets, dupl, rootElementName) == null)
		{
			System.out.println("Error: merged dataset could not be created correctly");
			return false;
		}
		du.writeUnionToFile(unionFn);
		
		unionDs.loadFromFile(unionFn, idDataPath, idProvPath, false, true);
		unionDs.getDatasetDensityCounts();
		
		// calculate consistency
		unionDs.calculateDatasetConsistency(du.getNonListAttributes(), datasets.size());
	
		// output the results:
		out.println("FUSION REPORT");
		out.println();
		out.println("Number of node clusters : " + dupl.size());
		out.println("Average cluster size : " + dupl.avgClusterSize());
		out.println();
		
		// density, attribute: % of non-null values of an attribute in a dataset
		// density, dataset: % of non-null values for all attributes in a dataset
		double attrCnt = du.getAllAttributes().size()-1; // don't coun id
		for(Dataset ds : datasets) 
		{
			double datasetD = ds.getDatasetDensity(attrCnt);
			
			out.println("dataset " + ds.getProvenanceID() + ":");
			out.println("dataset density : " + datasetD);
			out.println("average number of non-null attributes per object : " + datasetD*attrCnt);	
			for (String a : du.getAllAttributes())
			{
				out.println("density, attribute " + a + " : " + ds.getAttributetDensity(a));
			}
			out.println();
		}
		
		// Dataset unionDs = new Dataset();
		out.println("Merged dataset:");
		double unionD = unionDs.getDatasetDensity(attrCnt);
		double unionC = unionDs.getConsistency();
		out.println("dataset density : " + unionD);
		out.println("average number of non-null attributes per object : " + unionD*attrCnt);	
		out.println("dataset consistency : " + unionC);
		out.println();
		Map<String, Double> unionAttrC = unionDs.calculateDatasetConsistency(du.getNonListAttributes(), datasets.size());
		for (String a : du.getAllAttributes())
		{
			out.println("density, attribute " + a + " : " + unionDs.getAttributetDensity(a));
			
			Double cons = unionAttrC.get(a);
			if (cons == null) out.println("consistency, attribute " + a + " : not defined for list attributes");
			else out.println("consistency, attribute " + a + " : " + cons);
		}
		// out.close();
		return true;
	}

	// Apply conflict strategies, compare against gold standard, calculate accuracy, finish the fusion report
	// (appends to the fusion report)
	public static void runDataFusion(Map<String, AbstractResolutionFunction> rf, String idDataPath, String fnGold, String fnOutput, String fnFusionReport)
	{
		PrintStream out = System.out;
		if (fnFusionReport != null)
		try 
		{
			out = new PrintStream(new FileOutputStream(fnFusionReport,true)); // append!
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		Evaluator evaluator = new Evaluator();
		evaluator.evaluate(rf, unionDs, idDataPath, du.getNonListAttributes(), fnGold, fnOutput);
		
		out.println();
		out.println("Fused dataset:");
		out.println("overall accuracy : " + evaluator.GetAccuracy());
		for (String a : du.getAllAttributes())
		{
			Double accuracy = evaluator.GetAccuracyPerAttribute().get(a);
			if (accuracy == null) out.println("accuracy, attribute " + a + " : not defined for list attributes");
			else out.println("accuracy, attribute " + a + " : " + accuracy);
		}
		out.close();
	}
	
	public static void main(String[] args) 
	{
		// Start logging time
		Long startTime = new Date().getTime();
		
		// *** Step -1 ***
		// STUDENT PROJECT TODO: DO IDENTITY RESOLUTION EXERCISE!

		// *** Step 0 ***
		// STUDENT PROJECT TODO: ADD PROVENANCE TO YOUR DATA!
		
		// *** Step 1 ***
		// STUDENT PROJECT TODO: DEFINE YOUR INPUT HERE!

		// input 1: data and provenance id strings
		String rootElementName = "data";
		String idDataPath = "/data/videogame/id";
		String idProvPath = "/data/provenance/id";
		
		// input 2: a list of csv-files, with lists of pairs of (comma-separated) IDs
		// (this is the output of Exercise 2, Identity Resolution)
		Set<String> filesDuplicates = new HashSet<String>();
		filesDuplicates.add("resources/videogames/duplicates/matched-1-2.txt");
		filesDuplicates.add("resources/videogames/duplicates/matched-2-3.txt");
		
		// input 3: datasets (the same as for Exercise 2, BUT with provenance)
		Set<String> filesData = new HashSet<String>();
		filesData.add("resources/videogames/datasets/dbpedia.xml");
		filesData.add("resources/videogames/datasets/giantbomb.xml");
		filesData.add("resources/videogames/datasets/thegamesdb.xml");

		// input 4: output files
		String unionFn = "resources/videogames/merged.xml";
		String fnFusionReport = "resources/videogames/fusion-report.txt";
		String fnOutput = "resources/videogames/fused.xml";

		// *** Step 2 ***
		// produce a "merged" dataset
		boolean unionRes = runDataUnion(rootElementName, idDataPath, idProvPath, filesDuplicates, filesData, unionFn, fnFusionReport);
		// if (!unionRes) return;

		// *** Step 3 ***
		// STUDENT PROJECT TODO: NOW STOP AND PRODUCE A GOLD STANDARD BASED ON unionFn FILE!
		// ** Important ** KEEP IDS UNCHANGED!

		// input 5: gold standard file
		String fnGold = "resources/videogames/gold.xml";
		
		// *** Step 4 ***
		// STUDENT PROJECT TODO: define your conflict resolution functions (per attribute) here!
		Map<String, AbstractResolutionFunction> rf = new HashMap<String, AbstractResolutionFunction>();
		rf.put("title", new Vote());
		Map<String,Double> trustDesc = new HashMap<String,Double>();
		trustDesc.put("dbpedia.xml", 1.0);
		trustDesc.put("giantbomb.xml", 0.0);
		trustDesc.put("thegamesdb.xml", 0.0);
		rf.put("description", new MostTrusted(trustDesc,true));
		Map<String,Double> trustRel = new HashMap<String,Double>();
		trustRel.put("dbpedia.xml", 0.0);
		trustRel.put("giantbomb.xml", 1.0);
		trustRel.put("thegamesdb.xml", 0.0);
		rf.put("release", new MostTrusted(trustRel,true));
		rf.put("platform", new Vote());
		rf.put("developer", new Vote());
		rf.put("publisher", new Vote());
		rf.put("mode", new Vote());
		Map<String,Double> trustComp = new HashMap<String,Double>();
		trustComp.put("dbpedia.xml", 1.0);
		trustComp.put("giantbomb.xml", 0.0);
		trustComp.put("thegamesdb.xml", 0.0);
		rf.put("computingmedia", new MostTrusted(trustComp,true));
			

		// *** Step 5 ***
		// fuse and compare to gold standard
		runDataFusion(rf, idDataPath, fnGold, fnOutput, fnFusionReport);
		
		System.out.println("Fusion successfully finished");
		
		// End logging time
        Long endTime = new Date().getTime();
        double runTime = endTime - startTime;
		System.out.println("fast = " + XMLUtils.fastGetValue);
		System.out.println("slow = " + XMLUtils.slowGetValue);
        System.out.println("runtime : " + runTime);
	}
}
