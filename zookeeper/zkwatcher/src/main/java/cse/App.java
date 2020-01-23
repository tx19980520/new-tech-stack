package cse;

/**
 * class entry
 *
 */
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;
public class App
{
    public static void main( String[] args )
    {
        args = new String[3];
        args[0] = "localhost";
        args[1] = "/etc/hosts";
        args[2] = "hosts";
        Executor.main(args);
    }
}
