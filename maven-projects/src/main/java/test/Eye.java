package test;

/**
 * Used to select for which eye a specific property should be accessed.
 */
public enum Eye
{
	Left(0),Right(1);

	final int index;

	Eye( int index )
	{
		this.index = index;
	}
}
