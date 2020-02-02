package test;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Represents the pose of a {@link VRDevice}, including its transform, velocity and angular velocity. Also indicates whether the pose is valid and whether the device is connected.
 */
public class VRDevicePose
{
	/** transform encoding the position and rotation of the device in tracker space **/
	public final Matrix4f transform = new Matrix4f();
	/** the velocity in m/s in tracker space space **/
	public final Vector3f velocity = new Vector3f();
	/** the angular velocity in radians/s in tracker space **/
	public final Vector3f angularVelocity = new Vector3f();
	/** whether the pose is valid our invalid, e.g. outdated because of tracking failure **/
	public boolean isValid;
	/** whether the device is connected **/
	public boolean isConnected;
	/** the device index **/
	protected final int index;

	public VRDevicePose( int index )
	{
		this.index = index;
	}
}
