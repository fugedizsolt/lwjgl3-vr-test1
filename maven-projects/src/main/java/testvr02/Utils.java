package testvr02;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.joml.Matrix4f;

public class Utils
{
	public static String loadResource( String fileName ) throws Exception
	{
		String result = null;
		try (InputStream in = Utils.class.getResourceAsStream( fileName ); Scanner scanner = new Scanner( in,StandardCharsets.UTF_8.name() ))
		{
			result = scanner.useDelimiter( "\\A" ).next();
		}
		return result;
	}

	public static void printMatrix4f( String msg,Matrix4f mat )
	{
		float[] array = new float[16];
		mat.get( array );
		StringBuilder sb = new StringBuilder();
		for ( float fl : array )
			sb.append( String.format( "%.3f ",fl ) );
		HelloOpenVR.log( "%s: %s",msg,sb.toString() );
	}
}
