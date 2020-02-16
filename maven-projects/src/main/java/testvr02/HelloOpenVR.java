package testvr02;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;


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
			GL.createCapabilities();
			Renderer renderer = new Renderer();
			try
			{
				renderer.init( movr.getRenderWidth(),movr.getRenderHeight() );

				while ( true )
				{
					if ( Files.exists( pathStop )==true )
						break;

					movr.pollEvents();
					movr.handleInputs();

					renderer.render();
					movr.copyFrameBuffersToHMD();
				}
			}
			finally
			{
				renderer.cleanup();
			}
		}
	}

	public static void log( String msg,Object ... args )
	{
		System.out.println( String.format( msg,args ) );
	}
}