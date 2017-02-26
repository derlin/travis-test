package ch.derlin.easycmd.doc;


import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Read the commands documentation from a json file and constructs a queriable object.
 * The json file should be an array of objects (dictionary) like:
 * <pre>
 * {
 *   "name" : "pinit",
 *   "args" : "",
 *   "descr": "same as cybe init && cybe pull"
 * },
 * </pre>
 * Each command should at least have a name and a description. The args part can be empty.
 *
 * @author: Lucy Linder
 * @date: 27.06.2014
 */
public class CmdDoc {
    private List<CmdDescription> commandsUsage;
    private static final String NEW_LINE = System.getProperty("line.separator");


    /**
     * Create a doc object which will fetch the documentation from the given stream
     *
     * @param stream the stream from the json file
     */
    public CmdDoc(InputStream stream) {
        CmdDescription[] descr = (CmdDescription[]) getJsonFromFile(stream, new CmdDescription[0]);
        commandsUsage = new ArrayList<>(Arrays.asList(descr));
    }


    /**
     * Get a string with the detail of all available commands
     *
     * @return the string
     */
    public String man() {
        return commandsUsage.stream()   //
                .map(CmdDescription::fullDescription)   //
                .collect(Collectors.joining(NEW_LINE + "* "));

    }


    /**
     * Get a string with the list of all available commands (no details)
     *
     * @return the string
     */
    public String help() {
        return "Available commands: " + commandsUsage.stream()   //
                .map(CmdDescription::toString)   //
                .collect(Collectors.joining(", "));
    }


    /**
     * get the description of a command
     *
     * @param cmd the command name
     * @return the command description or null if the command does not exist/is not documented
     */
    public CmdDescription get(String cmd) {
        Optional<CmdDescription> descr = commandsUsage.stream()  //
                .filter(s -> s.name.equals(cmd))  //
                .findFirst();

        return descr.isPresent() ? descr.get() : null;
    }//end usage


    /**
     * Call {@link #betterMatch(String, int)} with an infinite threshold.
     */
    public CmdDescription betterMatch(String cmd) {
        return betterMatch(cmd, Integer.MAX_VALUE);
    }


    /**
     * Get the closest command matching the given string based on the
     * Levenshtein distance metric
     *
     * @param cmd       the input
     * @param threshold the maximum tolerated distance for a match
     * @return the closest available command
     */
    public CmdDescription betterMatch(String cmd, int threshold) {
        int minDist = Integer.MAX_VALUE;
        CmdDescription betterMatch = null;

        for (CmdDescription s : commandsUsage) {
            int distance = LevenshteinDistance.getDistance(s.name, cmd);
            if (distance < minDist) {
                minDist = distance;
                betterMatch = s;
            }
        }//end for

        return minDist < threshold ? betterMatch : null;
    }//end betterMatch

    // ----------------------------------------------------


    /**
     * read a json stream and store its content into the fields of
     * the container object. <br />
     * The json entries must match the container fields. If a field of the container does not
     * appear in the json file (or its value is left empty), the field is set to null.
     * non-existent json entries ,
     *
     * @param stream the json stream to read from
     */
    public static Object getJsonFromFile( InputStream stream, Object container ){

        try{
            return new GsonBuilder().create() //
                    .fromJson( new InputStreamReader( stream ), container.getClass() );

        }catch( Exception e ){
            System.out.println( e.getMessage() );
            e.printStackTrace();

        }finally{
            try{
                stream.close();
            }catch( IOException e ){
                //
            }
        }
        return null;

    }// end getJsonFromFile
    // ----------------------------------------------------


    public static class CmdDescription implements Comparable<CmdDescription> {
        private String name, args, descr;


        public String getName() {
            return name;
        }


        public String getArgs() {
            return args;
        }


        public String getDescr() {
            return descr;
        }


        @Override
        public String toString() {
            return name;
        }


        public String fullDescription() {
            return String.format("%s%n    %s ", name + " " + args, descr);
        }


        public String syntax() {
            return name + " " + args;
        }


        @Override
        public int compareTo(CmdDescription o) {
            return name.compareTo(o.name);
        }
    }
}//end class
