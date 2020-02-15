package test;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Iterator;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.openvr.HmdMatrix34;
import org.lwjgl.openvr.InputDigitalActionData;
import org.lwjgl.openvr.OpenVR;
import org.lwjgl.openvr.TrackedDevicePose;
import org.lwjgl.openvr.VR;
import org.lwjgl.openvr.VRActiveActionSet;
import org.lwjgl.openvr.VRActiveActionSet.Buffer;
import org.lwjgl.openvr.VRCompositor;
import org.lwjgl.openvr.VREvent;
import org.lwjgl.openvr.VRInput;
import org.lwjgl.openvr.VRSystem;
import org.lwjgl.system.MemoryStack;


public class ManagerOpenVR implements AutoCloseable
{
	private static final String ACTIONS_DEMO = "/actions/demo";
	private static final String ACTIONS_DEMO_IN_HIDE_CUBES = "/actions/demo/in/HideCubes";
	private static final String HELLOVR_ACTIONS_JSON = "hellovr_actions.json";
	private final MemoryStack stack;
	private final int openVRtoken;
	private final VREvent event;
	private final VRDevicePose[] devicePoses;
	private final VRDevice[] devices;

	// offsets for translation and rotation from tracker to world space
	private final Vector3f trackerSpaceOriginToWorldSpaceTranslationOffset = new Vector3f();
	private final Matrix4f trackerSpaceToWorldspaceRotationOffset = new Matrix4f();

	private final LongBuffer pHandleAction1 = LongBuffer.allocate( 1 );
	private final LongBuffer pHandleActionsDemo = LongBuffer.allocate( 1 );
	private final Buffer mallocActionSet = VRActiveActionSet.create( 1 );
	private final InputDigitalActionData pAction1Data = InputDigitalActionData.create();
	private final TrackedDevicePose.Buffer pRenderPoseArray = TrackedDevicePose.create( VR.k_unMaxTrackedDeviceCount );
	private final HmdMatrix34 poseHmdMatrix34 = HmdMatrix34.create();
	private final FloatBuffer poseHmdMatrix34FloatBuffer = FloatBuffer.allocate( 12 );



	public ManagerOpenVR()
	{
		this.stack = MemoryStack.stackPush();
		try
		{
			this.event = VREvent.create();
			this.devicePoses = new VRDevicePose[VR.k_unMaxTrackedDeviceCount];
			this.devices = new VRDevice[VR.k_unMaxTrackedDeviceCount];

			this.openVRtoken = createOpenVR();

			listDevices();

			initVRInput();
		}
		catch ( Exception exc )
		{
			this.stack.close();
			throw exc;
		}
	}

	private void initVRInput()
	{
		CharSequence pchActionManifestPath = new String( HELLOVR_ACTIONS_JSON );
		VRInput.VRInput_SetActionManifestPath( pchActionManifestPath );

		VRInput.VRInput_GetActionHandle( ACTIONS_DEMO_IN_HIDE_CUBES,pHandleAction1 );
		VRInput.VRInput_GetActionSetHandle( ACTIONS_DEMO,pHandleActionsDemo );
		mallocActionSet.ulActionSet( pHandleActionsDemo.get( 0 ) );
	}

	private int createOpenVR()
	{
		HelloOpenVR.log( "VR_IsRuntimeInstalled(%b)",VR.VR_IsRuntimeInstalled() );
		HelloOpenVR.log( "VR_RuntimePath(%s)",VR.VR_RuntimePath() );
		HelloOpenVR.log( "VR_IsHmdPresent(%b)",VR.VR_IsHmdPresent() );

		IntBuffer peError = stack.mallocInt( 1 );

		int token = 0;
		try
		{
			token = VR.VR_InitInternal( peError,VR.EVRApplicationType_VRApplication_Scene );
			int rc = peError.get( 0 );
			if ( rc!=0 )
			{
				HelloOpenVR.log( "INIT ERROR SYMBOL (%s)",VR.VR_GetVRInitErrorAsSymbol( rc ) );
				HelloOpenVR.log( "INIT ERROR  DESCR (%s)",VR.VR_GetVRInitErrorAsEnglishDescription( rc ) );
				throw new RuntimeException( "VR_InitInternal peError.get( 0 ):" + rc );
			}
			if ( token==0 )
				throw new RuntimeException( "token=0" );
			HelloOpenVR.log( "token=%d",token );

			OpenVR.create( token );
		}
		catch ( Exception exc )
		{
			VR.VR_ShutdownInternal();
			throw exc;
		}
		return token;
	}

	private void listDevices()
	{
		IntBuffer peError = stack.mallocInt( 1 );

		HelloOpenVR.log( "Model Number (%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( VR.k_unTrackedDeviceIndex_Hmd,VR.ETrackedDeviceProperty_Prop_ModelNumber_String,peError ) );
		HelloOpenVR.log( "Serial Number(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( VR.k_unTrackedDeviceIndex_Hmd,VR.ETrackedDeviceProperty_Prop_SerialNumber_String,peError ) );

		int countBaseStations = 0;
		for ( int ic=VR.k_unTrackedDeviceIndex_Hmd; ic<VR.k_unMaxTrackedDeviceCount; ic++ )
		{
			if ( VRSystem.VRSystem_IsTrackedDeviceConnected( ic )==true )
			{
				int trackedDeviceClass = VRSystem.VRSystem_GetTrackedDeviceClass( ic );
				HelloOpenVR.log( "id=%d,trackedDeviceClass=%d",ic,trackedDeviceClass );
				HelloOpenVR.log( "TrackingSystemName(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_TrackingSystemName_String,peError ) );
				HelloOpenVR.log( "ModeLabel(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_ModeLabel_String,peError ) );
				HelloOpenVR.log( "ModelNumber(%s)",VRSystem.VRSystem_GetStringTrackedDeviceProperty( ic,VR.ETrackedDeviceProperty_Prop_ModelNumber_String,peError ) );

				if ( trackedDeviceClass==VR.ETrackedDeviceClass_TrackedDeviceClass_TrackingReference )
					countBaseStations++;

				devicePoses[ic] = new VRDevicePose( ic );
				createDevice( ic );
			}
		}
		HelloOpenVR.log( "countBaseStations=%d",countBaseStations );

		IntBuffer w = stack.mallocInt( 1 );
		IntBuffer h = stack.mallocInt( 1 );
		VRSystem.VRSystem_GetRecommendedRenderTargetSize( w,h );
		HelloOpenVR.log( "Recommended width : " + w.get( 0 ) );
		HelloOpenVR.log( "Recommended height: " + h.get( 0 ) );
	}

	private void createDevice( int index )
	{
		VRDeviceType type = null;
		int deviceClass = VRSystem.VRSystem_GetTrackedDeviceClass( index );
		switch ( deviceClass )
		{
		case VR.ETrackedDeviceClass_TrackedDeviceClass_HMD:
			type = VRDeviceType.HeadMountedDisplay;
			break;
		case VR.ETrackedDeviceClass_TrackedDeviceClass_Controller:
			type = VRDeviceType.Controller;
			break;
		case VR.ETrackedDeviceClass_TrackedDeviceClass_TrackingReference:
			type = VRDeviceType.BaseStation;
			break;
		case VR.ETrackedDeviceClass_TrackedDeviceClass_GenericTracker:
			type = VRDeviceType.Generic;
			break;
		default:
			HelloOpenVR.log( "createDevice ??? deviceClass(%d)",deviceClass );
			return;
		}

		VRControllerRole role = VRControllerRole.Unknown;
		if ( type == VRDeviceType.Controller )
		{
			int r = VRSystem.VRSystem_GetControllerRoleForTrackedDeviceIndex( index );
			switch ( r )
			{
			case VR.ETrackedControllerRole_TrackedControllerRole_LeftHand:
				role = VRControllerRole.LeftHand;
				break;
			case VR.ETrackedControllerRole_TrackedControllerRole_RightHand:
				role = VRControllerRole.RightHand;
				break;
			}
		}

		devices[index] = new VRDevice( devicePoses[index],type,role );
//		devices[index].updateAxesAndPosition();
	}

	public void pollEvents()
	{
		while ( true )
		{
			boolean bHasNext = VRSystem.VRSystem_PollNextEvent( this.event );
			if ( bHasNext==false )
				break;

			int eventType = this.event.eventType();
			int trackedDeviceIndex = this.event.trackedDeviceIndex();
			HelloOpenVR.log( "event eventType(%d) trackedDeviceIndex(%d)",eventType,trackedDeviceIndex );
		}
	}

	@Override
	public void close() throws Exception
	{
		try
		{
			this.stack.close();
		}
		catch ( Exception exc )
		{
			exc.printStackTrace();
		}
		try
		{
			HelloOpenVR.log( "calling VR_ShutdownInternal..." );
			VR.VR_ShutdownInternal();
			HelloOpenVR.log( "VR_ShutdownInternal...end" );
		}
		catch ( Exception exc )
		{
			exc.printStackTrace();
		}
	}

	public int getOpenVRtoken()
	{
		return openVRtoken;
	}

	public VRDevice getDeviceByType( VRDeviceType type )
	{
		for ( VRDevice dev : devices )
		{
			if ( dev!=null && dev.getType()==type ) return dev;
		}
		return null;
	}

	public void handleInputs()
	{
		VRInput.VRInput_UpdateActionState( mallocActionSet,1 );

		VRInput.VRInput_GetDigitalActionData( pHandleAction1.get( 0 ),pAction1Data,VR.k_ulInvalidInputValueHandle );
		HelloOpenVR.log( "pAction1Data active(%b) state(%b)",pAction1Data.bActive(),pAction1Data.bState() );

		VRCompositor.VRCompositor_WaitGetPoses( pRenderPoseArray,null );
		for ( int ic=0; ic<VR.k_unMaxTrackedDeviceCount; ic++ )
		{
			TrackedDevicePose pose = pRenderPoseArray.get( ic );
			if ( pose.bPoseIsValid()==true )
			{
				pose.mDeviceToAbsoluteTracking( poseHmdMatrix34 );

				FloatBuffer floatBuffer = poseHmdMatrix34.m();
//				poseHmdMatrix34FloatBuffer.reset();
//				poseHmdMatrix34.m( poseHmdMatrix34FloatBuffer );
//				float[] array = poseHmdMatrix34FloatBuffer.array();

				float[] array = new float[12];
				floatBuffer.get( array );
				HelloOpenVR.log( "pose (%d) (%s)",ic,Arrays.toString( array ) );
			}
		}
	}
}
