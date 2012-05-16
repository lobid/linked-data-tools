/**
 * Copyright 2011 Pascal Christoph. The program is distributed under the terms of
 * the GNU General Public License, see https://www.gnu.org/licenses/gpl-3.0.html
 *
 * @date 2012-05-04
 */

package enrichment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.*;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class Disambiguate {

	/**prerequisites:
	 * cd silk_2.5.3/*_links
	 * cat *.nt|sort  -t' ' -k3   > $filename
	 * 
	 * @param args $filename
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static void main(String[] args)  {
		File file = new File(args[0]);
		if (file.isDirectory()) {
			args = file.list(new OnlyExtFilenameFilter("nt"));
		}

		BufferedReader in;
		for (int q = 0; q < args.length; q++) {
			String filename = null;
			if (file.isDirectory()) {
				filename = file.getPath() + File.separator + args[q];
			} else {
				filename = args[q];
			}
			try {
			FileWriter output = new FileWriter(filename + "_disambiguated.nt");
			String prefix = "@prefix rdrel: <http://rdvocab.info/RDARelationshipsWEMI/> .\n"
					+ "@prefix dbpedia:    <http://de.dbpedia.org/resource/> .\n"
					+ "@prefix frbr:   <http://purl.org/vocab/frbr/core#> .\n"
					+ "@prefix lobid: <http://lobid.org/resource/> .\n"
					+ "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"
					+ "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n"
					+ "@prefix mo: <http://purl.org/ontology/mo/> .\n"
					+ "@prefix wikipedia: <https://de.wikipedia.org/wiki/> .";
			output.append(prefix + "\n\n");
			in = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));

			HashMap<String, HashMap<String, ArrayList<String>>> hm = new HashMap<String, HashMap<String, ArrayList<String>>>();
			String s;
			HashMap<String, ArrayList<String>> hmLobid = new HashMap<String, ArrayList<String>>();
			Stack<String> old_object = new Stack<String>();

			while ((s = in.readLine()) != null) {
				String[] triples = s.split(" ");
				String object = triples[2].substring(1, triples[2].length() - 1);
				if (old_object.size() > 0 && !old_object.firstElement().equals(object)) {
					hmLobid = new HashMap<String, ArrayList<String>>();
					old_object = new Stack<String>();
				}
				old_object.push(object);
				String subject = triples[0].substring(1, triples[0].length() - 1);
				System.out.print("\nSubject=" + object);
				System.out.print("\ntriples[2]=" + triples[2]);
				hmLobid.put(subject, getAllCreators(new URI(subject)));
				hm.put(object, hmLobid);

			}
			// get all dbpedia resources
			for (String key_one : hm.keySet()) {
				System.out.print("\n==============\n==== " + key_one + "\n===============");
				int resources_cnt = hm.get(key_one).keySet().size();
				ArrayList<String>[] creators = new ArrayList[resources_cnt]; 
				HashMap<String, Integer> creators_backed = new HashMap<String, Integer>();
				int x = 0;
				// get all lobid_resources subsumed under the dbpedia resource
				for (String subject_uri : hm.get(key_one).keySet()) {
					creators[x] = new ArrayList<String>();
					System.out.print("\n     subject_uri=" + subject_uri);
					Iterator<String> ite = hm.get(key_one).get(subject_uri).iterator();
					int y = 0;
					// get all creators of the lobid resource
					while (ite.hasNext()) {
						String creator = ite.next();
						System.out.print("\n          " + creator);
						if (creators_backed.containsKey(creator)) {
							y = creators_backed.get(creator);
						} else {
							y =  creators_backed.size();
							creators_backed.put(creator, y);
						}
						while (creators[x].size() <= y) {
							creators[x].add("-");
						}
						creators[x].set(y, creator);
						y++;
					}
					x++;
				}
				if (creators_backed.size() == 1) {
					System.out.println("\n" + 
							"Every resource pointing to "+ key_one+" has the same creator!");
					for (String key_two : hm.get(key_one).keySet()) {
						output.append("<"+key_two + "> rdrel:workManifested <"+ key_one+"> .\n");
				    	output.append("<"+key_two + ">  mo:wikipedia <"+ key_one.replaceAll("dbpedia\\.org/resource", "wikipedia\\.org/wiki") + "> .\n");
					}
				}/*else  {
					for (int a = 0; a < creators.length; a++) {
						System.out.print(creators[a].toString()+",");
					}
				}*/
			}

			output.flush();
			if (output != null) {
				output.close();
			}
		}catch (Exception e){
			System.out.print("Exception while working on "+filename+": \n");
			e.printStackTrace(System.out);
		}
		}
	}

	static ArrayList<String> getAllCreators(URI resource) throws URISyntaxException,
			ClientProtocolException, IOException {
		System.out.print("\nCreators=" + resource.toString());
		ArrayList<String> al = new ArrayList<String>();
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		String query = "CONSTRUCT {  <" + resource + "> <http://purl.org/dc/elements/1.1/creator> ?o } WHERE { <" + resource
				+ "> <http://purl.org/dc/elements/1.1/creator> ?o } ";

		qparams.add(new BasicNameValuePair("query", query));
		URLEncodedUtils.format(qparams, "UTF-8");
		URI uri = URIUtils.createURI("http", "test.lobid.org", -1, "/sparql/",
				URLEncodedUtils.format(qparams, "UTF-8"), null);
		HttpGet httpget = new HttpGet(uri);
		HttpClient httpclient = new DefaultHttpClient();
		httpget.addHeader("Accept", "text/plain");
		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		if (entity != null) {
			BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
			String s;
			while ((s = br.readLine()) != null) {
				s = s.split(" ")[2]; // getting object
					al.add(s);
			}
		}
		return al;
	}

}
