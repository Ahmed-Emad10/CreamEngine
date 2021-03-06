package webcrawler;


// ---------------- includes ---------------------
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.io.FileWriter;
import java.util.Scanner;
import java.net.MalformedURLException;
import java.net.URL;

// --------------------- Crawler Class ---------------------
public class Crawler implements  Runnable {
    public static ArrayList<String> visited = new ArrayList<String>();
    public static ArrayList<String> nonvisited = new ArrayList<String>();
    public static int numLinks = 0;
    public static int maxSize = 5000;
    public int id;

    public static Object lock = new Object();
    public static FileWriter visited_Links_File = null, non_visited_Links_File = null;
    // --------------------- Crawler Constructor ---------------------
    public Crawler() {}
    public Crawler(int x) {
        id = x;
    }

    // --------------------- Crawling function ---------------------
    @Override
    public void run() {
        // -----------------------------------------------------------------
        while (!nonvisited.isEmpty() && numLinks < maxSize ) {
            String url;
            int docName=0;
            
            synchronized (lock) {
                url = nonvisited.get(0);
                nonvisited.remove(0);
            }
            if (!visited.contains(url)) {
                try {
                    // --------------------- if not visited Add it ---------------------
                	if(!isAllowed_Link(url))
                        continue;
                    synchronized (lock) {
                        if (visited.add(url)) {
                            System.out.println("Thread num: " + id + "  -->  link num " + numLinks + "...... "+ url);
                            docName=numLinks++;

                            visited_Links_File= new FileWriter("visitedLinks.txt", true);
                            visited_Links_File.write(url + "\n");
                            visited_Links_File.close();
                        }
                    }
                    // ---------- connect to the doc and Write doc content to the file --------
                    Document document = Jsoup.connect(url).get();
                    try {
                        // ------- Write the doc content to a file --------------------
                        String body = document.text();
                        String title =document.title();
                        String description;
                        if(document.select("meta[name=description]").isEmpty())
                            description ="  ";
                        else
                           description=document.select("meta[name=description]").get(0)
                                        .attr("content");

                        FileWriter doc = new FileWriter("docs\\" + (docName) + ".txt");
                        doc.write(title+ "\n");
                        doc.write(description+"\n");
                        doc.write(url + "\n");
                        doc.write(body);
                        doc.close();
                    } catch (IOException e) {
                        System.out.println("An error occurred while writing to file  " + e.getMessage());
                    }

                    // --------------------- Add links to non visited list ---------------------
                    Elements linksOnPage = document.select("a[href]");
                    synchronized (lock) {
                        for (Element page : linksOnPage) {
                            nonvisited.add(page.attr("abs:href"));
                            non_visited_Links_File =new FileWriter("nonVisitedLinks.txt" ,true);
                            non_visited_Links_File.write(page.attr("abs:href") + "\n");
                            non_visited_Links_File.close();
                        }
                    }
                } catch (IOException e) {
                    System.err.println("For '" + url + "': " + e.getMessage());
                }
            }
        }
    }

    public static void fillSeed(boolean isInitialState) {
        if(isInitialState ) {
            nonvisited = readFile("Seed.txt");
            try {
                new File("nonVisitedLinks.txt").delete();
                new File("visitedLinks.txt").delete();
                new File("nonVisitedLinks.txt").createNewFile();
                new File("visitedLinks.txt").createNewFile();
//                System.exit(0);
            } catch (IOException e) {
                System.out.println("Error while creating files "+ e.getMessage());
            }
        }
        else   {
            if (readFile("nonVisitedLinks.txt") != null) {
                nonvisited.addAll(readFile("nonVisitedLinks.txt"));
            }
            else {
                 nonvisited =readFile("Seed.txt");
            }
            visited = readFile("visitedLinks.txt");
            numLinks = visited.size();
        }
    }

    public static  ArrayList<String> readFile(String fileName) {
        try {
            File myObj = new File(fileName);
            Scanner myReader = new Scanner(myObj);
            ArrayList<String> s = new ArrayList<>();
            while (myReader.hasNextLine()) {
                String tmp =myReader.nextLine();
                if(tmp.isEmpty())
                    continue;
                s.add(tmp);
            }
            myReader.close();
            return s;
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred. while reading " + fileName);
            return null;
        }
    }

    public void Crawl(int thradNum ,boolean isInitialState  ) {
        fillSeed(isInitialState);
        if(thradNum > nonvisited.size())
            thradNum =nonvisited.size()-1;
        // --------------------- do the Crawling ---------------------
        Thread[] T = new Thread[thradNum];
        for (int i = 0; i < thradNum; i++) {
            T[i] = new Thread(new Crawler((i + 1)));
            T[i].start();
        }
        try {
            for (int i = 0; i < thradNum; i++) {
                T[i].join();
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }

        // --------------------- print visited links ---------------------
        System.out.println("end crawl print now ");
        for (int i = 0; i < visited.size(); i++) {
            System.out.println(i + "--> \t" + visited.get(i));
        }
        System.out.println("------------ \t DONE CRAWLING\t   ------------------- ");
        System.out.println("-----------------------------------------------------------------------------------");
    }
    
    public  boolean isAllowed_Link(String link) {
        // ----------------  Get the robot.txt link ------------
        String robot;
        ArrayList<String> disallow =new ArrayList<>();
        try {
            URL url =new URL(link);
            robot= url.getProtocol() + "://" + url.getHost() + "/robots.txt" ;
        } catch (MalformedURLException malformedURLException) {
            System.out.println(malformedURLException.getMessage());
            return true;    // no robot . txt then allowed
        }
        // ----------------  connect to the file ------------
        Document document = null;
        try {
            document = Jsoup.connect(robot).get();
        } catch (IOException e) {
            return false;
        }
        //----------------  get the USER-Agent: *  ------------
        String []body =document.body().text().split(" ");
        int idx =-1;
        for (int i = 0; i < body.length; i++) {
            if(body[i].equals("User-agent:") && body[i+1].equals("*"))
                idx=i+2;
        }
        if(idx == -1 )
            return true ;   // no User-agent:*  then allowed
        //----------------  get the disallowed routes ------------
        for (int i = idx; i < body.length -1 ; i+=2) {
            if(body[i].equals("Disallow:")) {
                disallow.add(body[i+1]);
            }
            else if(body[i].equals("Allow:"))
                continue;
            else {
                break;
            }
        }
        for (int i = 0; i < disallow.size(); i++) {
            if(link.contains(disallow.get(i)))
                return false;
        }
        return true;
    }
}
