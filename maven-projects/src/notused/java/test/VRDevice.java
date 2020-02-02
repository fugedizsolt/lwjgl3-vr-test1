package test;

import java.nio.IntBuffer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.openvr.VRControllerState;
import org.lwjgl.openvr.VRSystem;


public class VRDevice
{
	private final VRDevicePose pose;
	private final VRDeviceType type;
	private VRControllerRole role;
	private long buttons = 0;
	private final VRControllerState state = VRControllerState.create();

	// offsets for translation and rotation from tracker to world space
	private final Vector3f trackerSpaceOriginToWorldSpaceTranslationOffset;
	private final Matrix4f trackerSpaceToWorldspaceRotationOffset;

	// tracker space
	private final Vector3f position = new Vector3f();
	private final Vector3f xAxis = new Vector3f();
	private final Vector3f yAxis = new Vector3f();
	private final Vector3f zAxis = new Vector3f();

	// world space
	private final Vector3f positionWorld = new Vector3f();
	private final Vector3f xAxisWorld = new Vector3f();
	private final Vector3f yAxisWorld = new Vector3f();
	private final Vector3f zAxisWorld = new Vector3f();

	private final Vector3f vecTmp = new Vector3f();
	private final Matrix4f matTmp = new Matrix4f();

	private final IntBuffer scratch = BufferUtils.createIntBuffer( 1 );

	VRDevice( VRDevicePose pose,VRDeviceType type,VRControllerRole role,Vector3f trackerSpaceOriginToWorldSpaceTranslationOffset,Matrix4f trackerSpaceToWorldspaceRotationOffset )
	{
		this.pose = pose;
		this.type = type;
		this.role = role;
		this.trackerSpaceOriginToWorldSpaceTranslationOffset = trackerSpaceOriginToWorldSpaceTranslationOffset;
		this.trackerSpaceToWorldspaceRotationOffset = trackerSpaceToWorldspaceRotationOffset;
	}

	/**
	 * @return the most up-to-date {@link VRDevicePose} in tracker space
	 */
	public VRDevicePose getPose()
	{
		return pose;
	}

	public void updateAxesAndPosition()
	{
		Matrix4f matrix = pose.transform;
		matrix.getTranslation( position );
		xAxis.set( matrix.val[Matrix4f.M00],matrix.val[Matrix4f.M10],matrix.val[Matrix4f.M20] ).nor();
		yAxis.set( matrix.val[Matrix4f.M01],matrix.val[Matrix4f.M11],matrix.val[Matrix4f.M21] ).nor();
		zAxis.set( matrix.val[Matrix4f.M02],matrix.val[Matrix4f.M12],matrix.val[Matrix4f.M22] ).nor().scl( -1 );

		matTmp.set( trackerSpaceToWorldspaceRotationOffset );
		positionWorld.set( position ).mul( matTmp );
		positionWorld.add( trackerSpaceOriginToWorldSpaceTranslationOffset );

		matTmp.set( trackerSpaceToWorldspaceRotationOffset );

		xAxisWorld.set( xAxis ).mul( matTmp );
		yAxisWorld.set( yAxis ).mul( matTmp );
		zAxisWorld.set( zAxis ).mul( matTmp );
	}

	/**
	 * @return the position in the given {@link Space}
	 */
	public Vector3f getPosition( Space space )
	{
		return space == Space.Tracker ? position : positionWorld;
	}

	/**
	 * @return the right vector in the given {@link Space}
	 */
	public Vector3f getRight( Space space )
	{
		return space == Space.Tracker ? xAxis : xAxisWorld;
	}

	/**
	 * @return the up vector in the given {@link Space}
	 */
	public Vector3f getUp( Space space )
	{
		return space == Space.Tracker ? yAxis : yAxisWorld;
	}

	/**
	 * @return the direction vector in the given {@link Space}
	 */
	public Vector3f getDirection( Space space )
	{
		return space == Space.Tracker ? zAxis : zAxisWorld;
	}

	/**
	 * @return the {@link VRDeviceType}
	 */
	public VRDeviceType getType()
	{
		return type;
	}

	/**
	 * The {@link VRControllerRole}, indicating if the {@link VRDevice} is assigned to the left or right hand.
	 * 
	 * <p>
	 * <strong>Note</strong>: the role is not reliable! If one controller is connected on startup, it will have a role of {@link VRControllerRole#Unknown} and retain that role even if a second controller is connected (which will also haven an unknown role). The role is only reliable if two controllers are connected already, and none of the controllers disconnects during the application life-time.</br>
	 * At least on the HTC Vive, the first connected controller is always the right hand and the second connected controller is the left hand. The order stays the same even if controllers disconnect/reconnect during the application life-time.
	 * </p>
	 */
	// FIXME role might change as per API, but never saw it
	public VRControllerRole getControllerRole()
	{
		return role;
	}

	/**
	 * @return whether the device is connected
	 */
	public boolean isConnected()
	{
		return VRSystem.VRSystem_IsTrackedDeviceConnected( pose.index );
	}

	/**
	 * @return whether the button from {@link VRControllerButtons} is pressed
	 */
	public boolean isButtonPressed( int button )
	{
		if ( button < 0 || button >= 64 ) return false;
		return (buttons & (1l << button)) != 0;
	}

	void setButton( int button,boolean pressed )
	{
		if ( pressed )
		{
			buttons |= (1l << button);
		}
		else
		{
			buttons ^= (1l << button);
		}
	}

	/**
	 * @return the x-coordinate in the range [-1, 1] of the given axis from {@link VRControllerAxes}
	 */
	public float getAxisX( int axis )
	{
		if ( axis < 0 || axis >= 5 ) return 0;
		VRSystem.VRSystem_GetControllerState( pose.index,state );
		return state.rAxis( axis ).x();
	}

	/**
	 * @return the y-coordinate in the range [-1, 1] of the given axis from {@link VRControllerAxes}
	 */
	public float getAxisY( int axis )
	{
		if ( axis < 0 || axis >= 5 ) return 0;
		VRSystem.VRSystem_GetControllerState( pose.index,state );
		return state.rAxis( axis ).y();
	}

	/**
	 * Trigger a haptic pulse (vibrate) for the duration in microseconds. Subsequent calls to this method within 5ms will be ignored.
	 * 
	 * @param duration
	 *            pulse duration in microseconds
	 */
	public void triggerHapticPulse( short duration )
	{
		VRSystem.VRSystem_TriggerHapticPulse( pose.index,0,duration );
	}

	/**
	 * @return a boolean property or false if the query failed
	 */
	public boolean getBooleanProperty( VRDeviceProperty property )
	{
		scratch.put( 0,0 );
		boolean result = VRSystem.VRSystem_GetBoolTrackedDeviceProperty( this.pose.index,property.value,scratch );
		if ( scratch.get( 0 ) != 0 )
			return false;
		else
			return result;
	}

	/**
	 * @return a float property or 0 if the query failed
	 */
	public float getFloatProperty( VRDeviceProperty property )
	{
		scratch.put( 0,0 );
		float result = VRSystem.VRSystem_GetFloatTrackedDeviceProperty( this.pose.index,property.value,scratch );
		if ( scratch.get( 0 ) != 0 )
			return 0;
		else
			return result;
	}

	/**
	 * @return an int property or 0 if the query failed
	 */
	public int getInt32Property( VRDeviceProperty property )
	{
		scratch.put( 0,0 );
		int result = VRSystem.VRSystem_GetInt32TrackedDeviceProperty( this.pose.index,property.value,scratch );
		if ( scratch.get( 0 ) != 0 )
			return 0;
		else
			return result;
	}

	/**
	 * @return a long property or 0 if the query failed
	 */
	public long getUInt64Property( VRDeviceProperty property )
	{
		scratch.put( 0,0 );
		long result = VRSystem.VRSystem_GetUint64TrackedDeviceProperty( this.pose.index,property.value,scratch );
		if ( scratch.get( 0 ) != 0 )
			return 0;
		else
			return result;
	}

	/**
	 * @return a string property or null if the query failed
	 */
	public String getStringProperty( VRDeviceProperty property )
	{
		scratch.put( 0,0 );

		String result = VRSystem.VRSystem_GetStringTrackedDeviceProperty( this.pose.index,property.value,scratch );
		if ( scratch.get( 0 ) != 0 ) return null;
		return result;
	}

	/**
	 * @return a {@link ModelInstance} with the transform updated to the latest tracked position and orientation in world space for rendering or null
	 */
//	public ModelInstance getModelInstance()
//	{
//		return modelInstance;
//	}

	@Override
	public String toString()
	{
		return "VRDevice[manufacturer=" + getStringProperty( VRDeviceProperty.ManufacturerName_String ) + ", renderModel=" + getStringProperty( VRDeviceProperty.RenderModelName_String ) + ", index=" + pose.index + ", type=" + type + ", role=" + role + "]";
	}
}
