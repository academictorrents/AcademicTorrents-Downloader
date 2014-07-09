package smartnode.utils;

import smartnode.models.Entry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.Date;

/**
 * Created by nogueira on 7/2/14.
 */
public class ATLogger {

    BufferedWriter log_file_writer;
    Date date;
    LogLevel logger_level;

    public enum LogLevel{
        Error, Info, Warning, Debug
    }

    /**
     *
     * @param log_file_path
     * @param level
     */
    public ATLogger(String log_file_path, LogLevel level) {
        File log_file = new File(log_file_path);
        date = new Date();

        //lowest level of the logger is 3.
        //TODO add better filtering after
        if(level == LogLevel.Debug){
           logger_level = LogLevel.Debug;
        } else {
            logger_level = LogLevel.Error;
        }

        try {
            log_file_writer  = new BufferedWriter(new FileWriter(log_file));
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     *
     * @param log_message
     * @param level
     */
    public void log(String log_message, LogLevel level){

        try{
            switch (level){
                case Error:
                    logError(log_message);
                    break;
                case Info:
                    logInfo(log_message);
                    break;
                case Warning:
                    logWarning(log_message);
                    break;
                case Debug:
                    if(logger_level == LogLevel.Debug) {
                        logDebug(log_message);
                    }
                    break;
            }
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     *
     * @param message
     * @throws IOException
     */
    private void logError(String message) throws IOException{
        log_file_writer.write(date.toString() + " [Error] " +  message + '\n');
        log_file_writer.flush();
    }

    /**
     *
     * @param message
     * @throws IOException
     */
    private void logInfo(String message) throws IOException{
        log_file_writer.write(date.toString() + " [INFO] " +  message + '\n');
        log_file_writer.flush();
    }

    /**
     *
     * @param message
     * @throws IOException
     */
    private void logWarning(String message) throws IOException{
        log_file_writer.write(date.toString() + " [WARNING] " +  message + '\n');
        log_file_writer.flush();
    }

    /**
     *
     * @param message
     * @throws IOException
     */
    private void logDebug(String message) throws IOException{
        log_file_writer.write(date.toString() + " [DEBUG] " +  message + '\n');
        log_file_writer.flush();
    }
}
