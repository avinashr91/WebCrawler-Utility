package crawler;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiCrawler {
	private String SeedURL;
	private String[] KeyWords;
	private int MAX;
	private String FileName;
	private static final String BASE_URL = "https://en.wikipedia.org";
	private static final String A_TAG_PATTERN = "(?i)<a([^>]+)>.+?</a>"; // Extracts HREF tag content
	private static final String LINK_PATTERN = 
		"\\s*(?i)href\\s*=\\s*(\"([^\"#:]*\")|'[^'#:]*'|([^'\"#:>\\s]+))"; // Extracts link content 
																			//and excludes links containing # :
	Queue<String> Q;
	HashSet<String> Visited;
	List<String[]> Edges;
	HashSet<String> DisallowedLinks;
	private Pattern patternTag, patternLink;
	private int Requestcounter;

	public WikiCrawler(String seedURL, String[] keyWords, int max, String fileName) {
		
		SeedURL = seedURL; 		// Make sure lower case, Not really
		KeyWords = keyWords; // Make sure they are lower case
		this.MAX = max;
		FileName = fileName;
		Q = new LinkedList<String>();
		Visited = new HashSet<String>();
		Edges = new ArrayList<String[]>();
		patternTag = Pattern.compile(A_TAG_PATTERN);
		patternLink = Pattern.compile(LINK_PATTERN);

		Q.add(SeedURL);
		Visited.add(SeedURL);
		DisallowedLinks = new HashSet<String>();
		Requestcounter = 0;
		PopulateDisallowed();
	}

	public void crawl() {
		double start = System.currentTimeMillis();
		try {
			while(Q.size() != 0) {
				String currentlink = Q.remove();
				URL url = new URL(BASE_URL + currentlink);
				CheckifPolite();
				BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
				String s;
				boolean foundp = false;
				LinkedHashSet<String> links = new LinkedHashSet<String>();// To avoid duplicate links
				
		        while ((s = in.readLine()) != null) {
		        	if(!foundp
		            		&& s.contains("<p>")) {
		    
		            	foundp = true;
		            }
		            if(foundp) {
		            	ArrayList<String> linksfromline = ExtractLinks(s);
		            	for(String l:linksfromline) {
		            		links.add(l);
		            	}
		            }
		        }
		        in.close();

		        links.remove(currentlink); // To avoid self loop
		    
		        for(String link:links) {
					if(!DisallowedLinks.contains(link)) {
						if(Visited.size() < MAX) {
							if(LinkContainsKeyWords(link)) {
								
								String[] edge = {currentlink, link};
								System.out.println("Edge " + currentlink + " -> " + link);
								Edges.add(edge);
								if (!Visited.contains(link)) {
									Q.add(link);
									Visited.add(link);
								}
								System.out.println("Current Vertices size " + Visited.size());
								
							}
						}
						else {
							if(Visited.contains(link)) {
								String[] edge = {currentlink, link};
								System.out.println("Edge " + currentlink + " -> " + link);
								Edges.add(edge);
							}
						}
					}
				}
		    }
			
			WriteEdgesToFile();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		double end = System.currentTimeMillis();
		System.out.println("Time taken :" + (end - start)/1000 + "s");
		
	}

	private void CheckifPolite() {
		// TODO Auto-generated method stub
		if (Requestcounter == 100) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Requestcounter = 0;
		}
		Requestcounter++;
	}

	private boolean IsLinkPolite(String link) {
		// TODO Auto-generated method stub
		return false;
	}

	private void WriteEdgesToFile() {
		// TODO Auto-generated method stub
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(FileName));
			bw.write(Integer.toString(MAX));
			bw.newLine();
			for(String[] edge : Edges) {
				bw.write(edge[0] + " " + edge[1]);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public boolean LinkContainsKeyWords(String link) {
		// TODO Auto-generated method stub
		try {
			//URL url = new URL(BASE_URL + "/w/index.php?title=" + link.substring(6) + "&action=raw");
			URL url = new URL(BASE_URL + link);
			CheckifPolite();
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			String s, full = null;
			boolean foundp = false;
			while ((s = in.readLine()) != null) {
	            
	            if(!foundp
	            		&& s.contains("<p>")) {
	    
	            	foundp = true;
	            }
	            if(foundp) {
	    
	            	full += s.toLowerCase();
	            }
	            
	        }
	        in.close();
	        for(String key: KeyWords) {
	        	if(full == null
	        			|| !full.contains(key)) {
	        		return false;
	        	}
	        }
	        return true;
		} catch (FileNotFoundException e) {
			System.out.println(link + " File not found exp");
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	public ArrayList<String> ExtractLinks(String s) {
		// TODO Auto-generated method stub
		Matcher matcherTag = patternTag.matcher(s);
		Matcher matcherLink;
		ArrayList<String> links = new ArrayList<String>();

		while (matcherTag.find()) {
			String href = matcherTag.group(1); // href
			matcherLink = patternLink.matcher(href);

			while (matcherLink.find()) {
				String l = matcherLink.group(1); 
				if(l.contains("/wiki/")) {
					links.add(l.substring(1, l.length() - 1));
				}
			}
		}
		return links;
	}

	private void PopulateDisallowed() {
		// TODO Auto-generated method stub
		try {
			//URL url = new URL(BASE_URL + "/w/index.php?title=" + link.substring(6) + "&action=raw");
			URL url = new URL(BASE_URL + "/robots.txt");
			CheckifPolite();
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			String s;
			boolean found = false;
			while ((s = in.readLine()) != null) {
	            
				if (!found
	            	&& s.contains("User-agent: *")) {
	            	
					found = true;
	            }
				if(found
					&& s.contains("Disallow: ")) {
					
					DisallowedLinks.add(s.substring(10));
				}
	        }
	        in.close();
	        
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String[] topics = {"tennis", "grand slam"};
		WikiCrawler w = new WikiCrawler("/wiki/tennis", topics, 10, "WikiTennisGraph.txt");
		w.crawl();
	}
}
