package test;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestLog4j2
{
	private static final Logger logger = LogManager.getLogger( TestLog4j2.class );

	public static void main( String[] args )
	{
//		Configurator.initialize( "",null,"log4j2-config.xml" );
		System.out.println( "sysout Hello, World!" );
		logger.info( "Hello, World!" );
		System.out.println( "sysout end" );
	}
}
