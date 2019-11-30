package test;

/**
 * Space in which matrices and vectors are returned in by
 * {@link VRDevice} methods taking a {@link Space}.
 * In case {@link Space#World} is specified, all values
 * are transformed by the {@link Matrix4} set via
 * .
 */
public enum Space
{
	Tracker,
	World
}
