package test;

public class VRDevice
{
	private final VRDevicePose vrDevicePose;
	private final VRDeviceType type;
	private final VRControllerRole role;


	public VRDevice( VRDevicePose vrDevicePose,VRDeviceType type,VRControllerRole role )
	{
		this.vrDevicePose = vrDevicePose;
		this.type = type;
		this.role = role;
	}

	public VRDeviceType getType()
	{
		return type;
	}

	public void updateAxesAndPosition()
	{
	}

}
