package test;

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
		try ( ManagerOpenVR movr = new ManagerOpenVR() )
		{
			movr.listDevices();

			Path pathStop = Paths.get( "stop.dat" );
			while ( true )
			{
				if ( Files.exists( pathStop )==true )
					break;

				movr.pollEvents();
			}
		}
	}

	public static void log( String msg )
	{
		System.out.println( msg );
	}
}