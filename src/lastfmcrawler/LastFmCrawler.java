package lastfmcrawler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class LastFmCrawler 
{
    private final static int attemptCount = 5;
    
    public static void main(String[] args) 
    {
        for (int i=0; i<attemptCount; i++)
        {
            try 
            {
                LastFmCrawler crawler = new LastFmCrawler();                
                //crawler.ProcessGroup("http://www.last.fm/group/I+Still+Buy+CDs/members");
                //crawler.CreateListOfArtists("output.txt", "artists.lst", 200000);
                crawler.AnalyzeUser("Arkles");
                return;
            } 
            catch (Exception ex) 
            {
                Logger.getLogger(LastFmCrawler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private HashSet<String> foundArtists = new HashSet<>();       
    
    public void LoadArtistsFromFile(String path)
    {        
        foundArtists.clear();
        try 
        {
            BufferedReader br = new BufferedReader(new FileReader("output.txt"));
            String line;
            while ((line = br.readLine()) != null) 
            {
                String[] artists = line.split(",");
                for (String s : artists)
                {
                    foundArtists.add(s.trim());
                }
            }
            br.close();
        } 
        catch (IOException ex)
        {
            System.out.println("Loading of artists failed.");
        }
        System.out.println(foundArtists.size() + " artists loaded.");
    }
    
    public void CreateListOfArtists(String inputFile, String outputFile, int maxArtistCount)
    {
        foundArtists.clear();
        try 
        {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(outputFile)));
            String line;
            int i = 0;
            while ((line = br.readLine()) != null && i<maxArtistCount) 
            {
                String[] artists = line.split(",");
                for (String s : artists)
                {
                    String artist = s.trim();
                    if (artist.length() > 2 && Character.isLetter(artist.charAt(0)) && 
                            Character.isUpperCase(artist.charAt(0)) && !foundArtists.contains(artist))
                    {
                        writer.write(s.trim() + "\n");
                        foundArtists.add(s.trim());
                        i++;
                    }
                }
            }
            br.close();
        } 
        catch (IOException ex)
        {
            System.out.println("Creating of artist list failed.");
        }
    }
    
    public void ProcessGroup(String url) throws Exception
    {
        LoadArtistsFromFile("output.txt");
        Document page = CrawlPage(url);
        int pageCount = GetNumberOfPages(page.toString());
        for (int i=1; i<=pageCount; i++)
        {
            try
            {
                ProcessGroupPage(url + "?memberspage=" + i);
                System.out.println("Processing of group page " + i + " completed.");
            }
            catch(Exception ex)
            {
                System.out.println("Processing of group page " + i + " failed.");                        
            }                    
        }
    }
    
    public void ProcessGroupPage(String url) throws Exception
    {       
        Document page = CrawlPage(url);
        String temp[] = page.toString().split("hcard-");
        for (String s : temp)
        {
           String user = s.split("\" class=\"vcard\">")[0];
           if (user.length() > 0 && user.length() < 30)           
               ProcessUser(user);               
        }
    }
    
    public void AnalyzeUser(String userName) throws Exception
    {
        String libraryUrl = "http://www.last.fm/user/" + userName + "/charts?rangetype=overall&subtype=artists";
        Document document = CrawlPage(libraryUrl);
        String text = document.text().split("12 months Overall")[1].split("Artists and Labels")[0];
        GateClient gate = new GateClient();
        gate.run(text);
    }
    
    public void ProcessUser(String userName) throws Exception
    {
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter("output.txt", true)))) 
        {
            String libraryUrl = "http://www.last.fm/user/" + userName + "/charts?rangetype=overall&subtype=artists";
            String text = ProcessPage(libraryUrl);
            String processedText = "";
            String textWords[] = text.split(" ");
            boolean number = true;
            String currentBand = "";
            for (String word : textWords)
            {
                if (word.length() == 0)
                    continue;
                if (Character.isDigit(word.charAt(0)))
                {
                    if (number)
                        continue;                    
                    number = true;
                    if (!foundArtists.contains(currentBand.trim()))
                    {
                        processedText += currentBand + ",\n";
                        foundArtists.add(currentBand.trim());
                    }
                    currentBand = "";
                }
                else                    
                {
                    number = false;
                    currentBand += word + " ";
                }
            }
            writer.append(processedText);
        }
    }
    
    public String ProcessPage(String url) throws Exception
    {        
        Document page = CrawlPage(url);
        String text = "";
        if (page != null)
        try
        {
            text = page.text().split("12 months Overall")[1].split("Artists and Labels")[0];
        }           
        catch(Exception ex) 
        {}        
        return text;        
    }
    
    public void PrintPage(String url) throws Exception
    {
        Document page = CrawlPage(url);
        System.out.println(page);
    }
    
    public Document CrawlPage(String url) throws Exception 
    {      
        for (int i=0; i<attemptCount; i++)
        {
            try 
            {
                return Jsoup.connect(url).timeout(20000).get();
            }
            catch (IOException ex) 
            {
                System.out.println("Processing of page " + url + " failed, repeating.");
            }
        }
        return null;
    }
    
    public int GetNumberOfPages(String text)
    {
        String [] words = text.split("lastpage\">")[1].split("</a>");
        return Integer.parseInt(words[0]);
    }
}
