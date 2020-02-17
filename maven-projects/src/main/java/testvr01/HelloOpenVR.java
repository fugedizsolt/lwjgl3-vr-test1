package testvr01;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HelloOpenVR
{
	public static void main( String[] args ) throws Exception
	{
		new HelloOpenVR();
	}

	private HelloOpenVR() throws Exception
	{
		Path pathStop = Paths.get( "stop.dat" );
		if ( Files.exists( pathStop )==true )
			Files.delete( pathStop );

		try ( ManagerOpenVR movr = new ManagerOpenVR() )
		{
			for ( int ic=0; ic<2; ic++ )
			{
				if ( Files.exists( pathStop )==true )
					break;

				movr.pollEvents();
				movr.handleInputs();
			}
		}
	}

	public static void log( String msg,Object ... args )
	{
		System.out.println( String.format( msg,args ) );
	}
}