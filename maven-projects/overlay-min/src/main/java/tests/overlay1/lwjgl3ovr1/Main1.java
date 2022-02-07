package tests.overlay1.lwjgl3ovr1;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tests.overlay1.lwjgl3ovr1.utils.ManagerGLFW;
import tests.overlay1.lwjgl3ovr1.utils.ManagerOpenVR;


public class Main1
{
	private static final Logger logger = LogManager.getLogger( Main1.class );

	private static final String FILENAME_GLFW_ERRORS = "glfw-errors.log";

	public static void main( String[] args ) throws Exception
	{
		log( "Start:" );
		try
		{
			new Main1();
		}
		catch ( Exception exc )
		{
			logger.error( exc );
		}
		log( "End." );
	}

	private Main1() throws Exception
	{
		Path pathStop = Paths.get( "stop.dat" );
		if ( Files.exists( pathStop ) == true ) Files.delete( pathStop );

		try (PrintStream psGLFWError = new PrintStream( FILENAME_GLFW_ERRORS ))
		{
			try (ManagerGLFW mglfw = new ManagerGLFW( psGLFWError ))
			{
				try (ManagerOpenVR managerOpenVR = new ManagerOpenVR())
				{
					mglfw.loop( pathStop,managerOpenVR );
				}
			}
		}
	}

	public static void log( String msg,Object... args )
	{
//		System.out.println( String.format( msg,args ) );
		logger.debug( String.format( msg,args ) );
	}
}