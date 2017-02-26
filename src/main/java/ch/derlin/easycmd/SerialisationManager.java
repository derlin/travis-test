package ch.derlin.easycmd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.ssl.OpenSSL;

import java.io.*;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;


/**
 * this class provides utilities in order to encrypt/data data (openssl style,
 * pass and auto-generated salt, no iv) and to serialize/deserialize them (in a
 * json format).
 * <p/>
 * it is also possible to write the content of a list in a cleartext "pretty"
 * valid json format.
 *
 * @author Lucy Linder
 * @date Dec 21, 2012
 */
public class SerialisationManager {

    /**
     * encrypts the arraylist of objects with the cipher given in parameter and
     * serializes it in json format.
     *
     * @param data     the data
     * @param algo     the algorithm (aes-128-cbc for example, see the openssl conventions)
     * @param filepath the output filepath
     * @param password the password
     * @throws IOException
     */
    public static void serialize( Object data, String algo, String filepath,
                           String password ) throws IOException {
       serialize( data, algo, new FileOutputStream( filepath ), password );
    }// end serialize


    /**
     * encrypts the arraylist of objects with the cipher given in parameter and
     * serializes it in json format.
     *
     * @param data     the data
     * @param algo     the algorithm (aes-128-cbc for example, see the openssl conventions)
     * @param outStream the output stream to write to
     * @param password the password
     * @throws IOException
     */
    public static void serialize( Object data, String algo, OutputStream outStream,
                           String password ) throws IOException {

        if( outStream == null ) {
            throw new IllegalStateException( "The outputstream cannot be null!" );
        }
        try {

            Gson gson = new GsonBuilder().create();

            outStream.write( OpenSSL.encrypt( algo, password.toCharArray(),
                    gson.toJson( data ).getBytes( "UTF-8" ) ) );
            outStream.write( "\r\n".getBytes() );
            outStream.write( System.getProperty( "line.separator" ).getBytes() );
            outStream.flush();

        } catch( GeneralSecurityException e ) {
            e.printStackTrace();
        } finally {
            outStream.close();
        }

    }// end serialize


    /**
     * deserializes and returns the object of type "Type" contained in the
     * specified file. the decryption of the data is performed with the cipher
     * given in parameter.<br />
     * The object in the file must have been encrypted after a json serialisation.
     *
     * @param algo     the algorithm (aes-128-cbc for example, see the openssl conventions)
     * @param filepath the filepath
     * @param password the password
     * @param type     the type of the data serialized
     * @return the decrypted data ( a list of ? )
     * @throws WrongCredentialsException if the password or the magic number is incorrect
     * @throws IOException
     */
    public static Object deserialize( String algo, String filepath, String password,
                               Type type ) throws WrongCredentialsException, IOException {
        return deserialize( algo, new FileInputStream( filepath ), password, type );
    }// end deserialize


    /**
     * deserializes and returns the object of type "Type" contained in the
     * specified file. the decryption of the data is performed with the cipher
     * given in parameter.<br />
     * The object in the file must have been encrypted after a json serialisation.
     *
     * @param algo     the algorithm (aes-128-cbc for example, see the openssl conventions)
     * @param stream   the stream to read from
     * @param password the password
     * @param type     the type of the data serialized
     * @return the decrypted data ( a list of ? )
     * @throws WrongCredentialsException if the password or the magic number is incorrect
     * @throws IOException
     */
    public static Object deserialize( String algo, InputStream stream, String password,
                               Type type ) throws WrongCredentialsException, IOException {

        if( stream == null || stream.available() == 0 ) {
            throw new IllegalStateException( "the " +
                    "stream" +
                    " " +
                    "is null or unavailable" );
        }
        try {

            Object data = ( new GsonBuilder().create().fromJson( new InputStreamReader( OpenSSL
                    .decrypt( algo, password.toCharArray(), stream ), "UTF-8" ), type ) );
            if( data == null ) {
                throw new WrongCredentialsException();
            } else {
                return data;
            }

        } catch( JsonIOException e ) {
            throw new WrongCredentialsException( e.getMessage() );
        } catch( JsonSyntaxException e ) {
            throw new WrongCredentialsException( e.getMessage() );
        } catch( GeneralSecurityException e ) {
            throw new WrongCredentialsException( e.getMessage() );
        } finally {
            if( stream != null ) stream.close();
        }// end try

    }// end deserialize


    public static class WrongCredentialsException extends Exception {
        public WrongCredentialsException() {
            super();
        }
        public WrongCredentialsException( String message ) {
            super( message );
        }
    }

}// end class
