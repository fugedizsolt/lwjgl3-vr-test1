package test;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

/**
 * Represents the pose of a {@link VRDevice}, including its transform, velocity and angular velocity. Also indicates whether the pose is valid and whether the device is connected.
 */
public class VRDevicePose
{
	/** transform encoding the position and rotation of the device in tracker space **/
	public final Matrix4 transform = new Matrix4();
	/** the velocity in m/s in tracker space space **/
	public final Vector3 velocity = new Vector3();
	/** the angular velocity in radians/s in tracker space **/
	public final Vector3 angularVelocity = new Vector3();
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
