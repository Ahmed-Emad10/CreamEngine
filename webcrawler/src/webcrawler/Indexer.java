package webcrawler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.security.KeyPair;
import java.util.*;
import java.lang.ClassNotFoundException;
import opennlp.tools.stemmer.PorterStemmer ;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.*;

class Pair<T,U> {
    public  T key;     public  U value;
    public Pair(T key, U value) {      this.key = key;    this.value = value;   }
    public Pair() {  }
}

class Data
{
    public  int doc_num;     public  int freq ; public double tf ;
    public String url ;      public String descrition ;   public String title ;
    public Data() {  }
    public Data(int a , int b , double c , String e, String f , String g)
    { doc_num = a ; freq = b ; tf = c ;  url = e ; descrition = f; title = g ; }
}

public class Indexer {
    static String [] stop_words  = new String [] { "a", "about" , "above" , "after" , "again" , "against", "all", "am" ,"an" ,
            "and" , "any" , "are" , "aren't" , "as" , "at" , "be","because" , "been" , "before" , "being" , "below" , "below" ,
            "between" , "both" , "but" , "by" , "can't" , "cannot" , "could" , "couldn't" , "did" , "didn't" ,  "do" , "does" ,
            "doesn't" , "doing" , "don't" ,"down" , "during" , "each" , "few" , "for" , "from" , "further" , "had" , "hadn't" ,
            "has" , "has" , "hasn't" , "have"  , "haven't" , "having" , "he" , "he'd" , "he'll" , "his"  , "how"  , "how's"   ,
            "i", "i'd", "i'll", "i'm", "i've", "if", "in" ,  "into", "is", "isn't", "it", "it's", "its", "itself", "me","more",
            "most", "mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only",  "or", "other" , "ought",
            "our", "ours", "ourselves", "your", "yours", "yourself" , "yourselves" , "why's", "with", "won't", "you've" ,
            "out", "over", "own", "same", "shan't", "she", "she'd", "she'll", "she's", "should", "shouldn't","so","some","such",
            "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there", "there's","these","they",
            "too", "under", "until" , "up", "very", "was", "wasn't", "we", "we'd", "we'll", "which", "while", "would","through",
            "to", "when's", "where", "wouldn't"  , "you", "you'd", "where's", "they'd", "they'll", "this", "when", "those","who",
            "they're", "they've", "we're", "you'll", "you're", "we've", "were", "weren't", "what", "what's", "he's","her","here",
            "here's", "hers", "herself", "him", "himself", "let's", "who's", "why", "whom" } ;

    static Map<String, Integer> stop_words_mp = new HashMap<String , Integer>();
    static Map<String, Pair< Double , Vector<Data> >> map = new HashMap<String, Pair< Double , Vector<Data>>>();
    static PorterStemmer stemmer = new PorterStemmer() ;
    // fill the two vectors from files

    static Vector<String> doc = new Vector<String>();
    static Vector<String> urls = new Vector<String>();
    static Vector<String> Description = new Vector<String>();
    static Vector<String> title = new Vector<String>();
    
    
    
    public  void index() 
    {
      
       

        File directory=new File("docs\\");
        int docNum= directory.list().length;
        
        System.out.println(docNum);
        
        File [] f=  directory.listFiles();
        Vector<Integer> fileName_List =new Vector<>();
        for (int i = 0; i < f.length; i++) {
            fileName_List.add(Integer.parseInt(f[i].getName().replace(".txt" ,"")));
        }
        Collections.sort(fileName_List);
        for (int i = 0; i < fileName_List.size(); i++) {
            ArrayList<String> temp =readBody("docs\\"+fileName_List.get(i)+".txt");
            title.add(temp.get(0));
            Description.add( temp.get(1));
            urls.add(temp.get(2));
            doc.add(temp.get(3));
            System.out.println((i)+ " --> " + fileName_List.get(i)+".txt");
        }


        //for (int i = 0; i < Description.size(); i++) {
              //  System.out.println(i+ " --> "+ Description.get(i));}

        //hash the stop words in a map
        for(int i =0 ; i<stop_words.length; i++) {
            stop_words_mp.put(stop_words[i], 1);
        }
        
        int num = docNum ;
        for (int i = 0; i < num; i++) {
            try {
                fill_map(i);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
        
       
      //-------------------database connection--------------------//
        String dburl="jdbc:mysql://localhost:3306/searchengine";
        String user="root";
        String password="asd123asd";
        try
        {
	        Connection myConn= DriverManager.getConnection(dburl,user,password);
	        //Statement myStat=myConn.createStatement();
	        
	        
	        // print outputs
	        String t1="INSERT INTO Pages (doc_id ,title,link, des) VALUES(?,?,?,?)";
	        for(int i=0;i<docNum;i++)
	        {
			      PreparedStatement myStat = myConn.prepareStatement(t1);
		          myStat.setInt(1,i);
		          myStat.setString(2,title.get(i));
		          myStat.setString(3, urls.get(i));
		          myStat.setString(4, Description.get(i));
		          myStat.executeUpdate();
	        }
	        
	        
	        String t2;
	        
            for (Map.Entry<String, Pair< Double , Vector<Data>>> me : map.entrySet()) 
            {
	            System.out.println (me.getKey() + ":");
                me.getValue().key =docNum/ Double.valueOf(me.getValue().value.size()) ;   //IDF
                Vector<Data> v = me.getValue().value;
                
                String t3="INSERT INTO word_idf ( wordss ,idf ) VALUES(?,?)";
            	
            	PreparedStatement myStat = myConn.prepareStatement(t3);
                myStat.setString(1, me.getKey());
                myStat.setDouble(2,  me.getValue().key);
                myStat.executeUpdate();
                
                
                for (int i = 0; i < v.size(); i++)
	            {

	            	t2="INSERT INTO Words (word ,id,freq, tf) VALUES(?,?,?,?)";
	            	
	            	myStat = myConn.prepareStatement(t2);
	            	myStat.setString(1, me.getKey());
	            	myStat.setInt(2,v.get(i).doc_num );
                    myStat.setInt(3, v.get(i).freq);
                    myStat.setDouble(4, v.get(i).tf);
                    myStat.executeUpdate();

	                System.out.println("doc num : " + v.get(i).doc_num + "   freq: " + v.get(i).freq + "   url : " + v.get(i).url +
	                        "  desc: " + v.get(i).descrition + "  title: " +  v.get(i).title   + "  tf: " +  v.get(i).tf);
	            }
	            System.out.println();
	        }
        }
        catch(SQLException e) { System.out.println(e.getMessage());}
    }
    
    
    //--------------------------MAIN------------------//
    public static void main(String[] args) throws IOException,SQLException {
    	
        new Indexer().index();
        
        

    }

    static void fill_map(Integer doc_num) throws IOException {

        String words[] = doc.get(doc_num).split(" ");

        Vector<String> stemmed_words = new Vector<String>();

        // ------------------------------REMOVE STOP WORDS and do stemming----------------------------------//

        for (int i = 0; i < words.length; i++) {

            String s = stemmer.stem(words[i].replaceAll("[^A-Za-z ]",""));
            s=s.toLowerCase();
            if(s.length()>20)
            {
            	s=s.substring(0,20);
            }
            if (stop_words_mp.get(s) == null && s != "" )
            {           
                stemmed_words.add(s);
            }

        }

        for (int i = 0; i < stemmed_words.size(); i++)
        {

           Pair<Double , Vector<Data> >  p = map.get(stemmed_words.get(i));

           int freq = 1, set_index = 0;

            boolean already_exist = false;

            if (p != null) {

                already_exist = true;

                for (int j = 0; j < p.value.size(); j++) {

                    if (p.value.get(j).doc_num == doc_num) {

                        set_index = j;
                        freq = p.value.get(j).freq + 1;
                        break;
                    }
                }
            }

            if (freq > 1)
            {
                p.value.set(set_index, new Data(doc_num, freq ,(double)freq / (double)stemmed_words.size() , urls.get(doc_num) ,Description.get(doc_num) , title.get(doc_num) ));
            }
            else
            {
                if (!already_exist)
                {
                    p = new Pair<Double , Vector<Data> >();
                    p.value = new Vector<Data>() ;
                }

                p.value.add(new Data(doc_num,  freq, (double)freq / (double)stemmed_words.size() , urls.get(doc_num) , Description.get(doc_num) , title.get(doc_num) ) );
            }
            map.put(stemmed_words.get(i), p);
        }
    }
    
    public static  ArrayList<String> readBody(String fileName) {
        try {
            File myObj = new File(fileName);
            Scanner myReader = new Scanner(myObj);
            String body ="";
            ArrayList<String> s = new ArrayList<>();
            while (myReader.hasNextLine()) {
                s.add(myReader.nextLine()); // title
                s.add(myReader.nextLine()); // description
                s.add(myReader.nextLine()); // url
                while (myReader.hasNextLine()) {
                    String tmp =myReader.nextLine();
                    if(tmp.isEmpty())
                        continue;
                    body+=tmp;
                }
            }
            s.add(body);
            myReader.close();
            return s;
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred. while reading " + fileName);
            return null;
        }
    }
}