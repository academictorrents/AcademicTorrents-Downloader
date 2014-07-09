package smartnode.utils;

import au.com.bytecode.opencsv.CSVReader;
import smartnode.models.Collection;
import smartnode.models.Dataset;
import smartnode.models.Entry;
import smartnode.models.Paper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by nogueira on 7/2/14.
 */
public class ATFetcher {

    private ATLogger logger;

    /**
     *
     * @param logger
     */
    public ATFetcher(ATLogger logger) {
        this.logger = logger;
    }

    public ArrayList<Collection> getCollections(){

        ArrayList<Collection> collections = new ArrayList<Collection>();

        try {
            // create connection to AT
            logger.log("local string BAD!!", ATLogger.LogLevel.Debug);
            logger.log("Opening connection to AT Getting collections", ATLogger.LogLevel.Info);

            String uri = "http://www.academictorrents.com/collections.php?format=.csv";
            logger.log("uri: " + uri, ATLogger.LogLevel.Debug);
            URI collections_uri = new URI(uri);
            URLConnection collections_con = collections_uri.toURL().openConnection();

            // reader content from connection and create collection
            CSVReader reader = new CSVReader(new InputStreamReader(collections_con.getInputStream()));
            //skip csv header
            String [] line = reader.readNext();
            while((line = reader.readNext()) != null){
                Collection collection = new Collection(line[0], line[1], Integer.parseInt(line[2]), Long.parseLong(line[3]));
                collection.setTorrents(getCollectionEntries(line[1]));
                collections.add(collection);
                logger.log("Added collection to collections", ATLogger.LogLevel.Debug);
            }

        }
        catch (URISyntaxException e){
            logger.log(e.getMessage() + e.getInput() + e.getReason() + e.getIndex(), ATLogger.LogLevel.Error );
        }
        catch (MalformedURLException e){
            logger.log(e.getMessage(), ATLogger.LogLevel.Error );
        }
        catch (IOException e){
            logger.log(e.getMessage(), ATLogger.LogLevel.Error );
        }
        catch (NumberFormatException e){
            logger.log("Parse Error" + e.getMessage(), ATLogger.LogLevel.Error );
        }
        catch(Exception e){
            logger.log("Unknown Exception in fetcher getting collections",  ATLogger.LogLevel.Error);
        }

        return collections;
    }

    public HashMap<String, Entry> getCollectionEntries(String urlname){

        HashMap<String, Entry> entries = new HashMap<String, Entry>();

        try {
            // create connection to AT
            logger.log("local string BAD!!", ATLogger.LogLevel.Debug);
            logger.log("Opening connection to AT Getting collection entries", ATLogger.LogLevel.Info);

            String uri = "http://www.academictorrents.com/collection/" + urlname + ".csv" ;
            logger.log("uri: " + uri, ATLogger.LogLevel.Debug);
            URI collections_uri = new URI(uri);
            URLConnection collections_con = collections_uri.toURL().openConnection();

            // reader content from connection and create collection
            CSVReader reader = new CSVReader(new InputStreamReader(collections_con.getInputStream()));
            //skip csv header
            String [] line = reader.readNext();
            while((line = reader.readNext()) != null){
                Entry entry = new Entry() ;

                entry = new Entry(line[0], line[1], line[2], Long.parseLong(line[3]), Integer.parseInt(line[4]),
                    Integer.parseInt(line[5]), Integer.parseInt(line[6]), Long.parseLong(line[7]), Long.parseLong(line[8]));

                entries.put(line[2], entry);
                logger.log("Added entry to collection", ATLogger.LogLevel.Debug);
            }
        }
        catch (URISyntaxException e){
            logger.log(e.getMessage() + e.getInput() + e.getReason() + e.getIndex(), ATLogger.LogLevel.Error);
        }
        catch (MalformedURLException e){
            logger.log(e.getMessage(), ATLogger.LogLevel.Error);
        }
        catch (IOException e){
            logger.log(e.getMessage(), ATLogger.LogLevel.Error);
        }
        catch (NumberFormatException e){
            logger.log("Parse Error" + e.getMessage(), ATLogger.LogLevel.Error );
        }
        catch(Exception e){
            logger.log("Unknown Exception in fetcher getting collections",  ATLogger.LogLevel.Error);
        }

        return entries;
    }
}
