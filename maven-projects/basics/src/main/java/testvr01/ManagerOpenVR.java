package testvr01;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Paths;
import java.util.Arrays;

import org.joml.Matrix4f;
import org.lwjgl.openvr.HmdMatrix34;
import org.lwjgl.openvr.HmdMatrix44;
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
import org.lwjgl.system.MemoryUtil;

import testvr02.Renderer;


public class ManagerOpenVR implements AutoCloseable
{
	private static final String ACTIONS_DEMO = "/actions/demo";
	private static final String ACTIONS_DEMO_IN_HIDE_CUBES = "/actions/demo/in/HideCubes";
	private static final String HELLOVR_ACTIONS_JSON = "hellovr_actions.json";
	private final int openVRtoken;
	private final VREvent event;

	// offsets for translation and rotation from tracker to world space
//	private final Vector3f trackerSpaceOriginToWorldSpaceTranslationOffset = new Vector3f();
//	private final Matrix4f trackerSpaceToWorldspaceRotationOffset = new Matrix4f();
//	private final HmdMatrix34 poseHmdMatrix34 = HmdMatrix34.create();
//	private final FloatBuffer poseHmdMatrix34FloatBuffer = FloatBuffer.allocate( 12 );

	private final LongBuffer pHandleAction1 = MemoryUtil.memAllocLong( 1 );
	private final LongBuffer pHandleActionsDemo = MemoryUtil.memAllocLong( 1 );
	private final InputDigitalActionData pAction1Data = InputDigitalActionData.create();
	private final TrackedDevicePose.Buffer pRenderPoseArray = TrackedDevicePose.create( VR.k_unMaxTrackedDeviceCount );
	private int vrrc;	// return code from VR func
	private long vrHandle;	// handle from VR func
	private Buffer actionSetDemo = null;
//	private ByteBuffer memForActionSetDemo = MemoryUtil.memAlloc( VRActiveActionSet.SIZEOF );
	private Matrix4f projectionMatrixLeft = null;
	private Matrix4f projectionMatrixRight = null;
	private float[] arrayToConvHmdMatrices = new float[16];
	private float[] arrayToConvHmd34Matrices = new float[12];


	public ManagerOpenVR()
	{
		try ( MemoryStack stack = MemoryStack.stackPush() )
		{
			this.event = VREvent.create();
			this.openVRtoken = createOpenVR( stack );
			listDevices( stack );
			initVRInput();
			initVRMatrices( stack );
		}
	}

	private int createOpenVR( MemoryStack stack )
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

	private void listDevices( MemoryStack stack )
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
			}
		}
		HelloOpenVR.log( "countBaseStations=%d",countBaseStations );

		IntBuffer w = stack.mallocInt( 1 );
		IntBuffer h = stack.mallocInt( 1 );
		VRSystem.VRSystem_GetRecommendedRenderTargetSize( w,h );
		HelloOpenVR.log( "Recommended width : " + w.get( 0 ) );
		HelloOpenVR.log( "Recommended height: " + h.get( 0 ) );
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
			HelloOpenVR.log( "event trackedDeviceIndex(\t%03d) eventType(%d)",trackedDeviceIndex,eventType );
		}
	}

	@Override
	public void close() throws Exception
	{
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

	private void initVRInput()
	{
		vrrc = VRInput.VRInput_SetActionManifestPath( Paths.get( HELLOVR_ACTIONS_JSON ).toAbsolutePath().toString() );
			HelloOpenVR.log( "VRInput_SetActionManifestPath rc=%d",vrrc );

//		long[] tmpArray1 = new long[]{ 0 };
//		LongBuffer tmpLongBuffer1 = LongBuffer.wrap( tmpArray1 );
//		long memAddress1 = MemoryUtil.memAddress( tmpLongBuffer1 );
//		LongBuffer tmpLongBuffer2 = MemoryUtil.memAllocLong( 1 );
//		long memAddress2 = MemoryUtil.memAddress( tmpLongBuffer2 );
//		HelloOpenVR.log( "memAddress1=%d memAddress2=%d",memAddress1,memAddress2 ); // az első mindig 0, a 2. OK :)

		vrrc = VRInput.VRInput_GetActionHandle( ACTIONS_DEMO_IN_HIDE_CUBES,pHandleAction1 );
		vrHandle = pHandleAction1.get( 0 );
			HelloOpenVR.log( "VRInput_GetActionHandle rc=%d vrHandle=%d",vrrc,vrHandle );

		vrrc = VRInput.VRInput_GetActionSetHandle( ACTIONS_DEMO,pHandleActionsDemo );
		vrHandle = pHandleActionsDemo.get( 0 );
			HelloOpenVR.log( "VRInput_GetActionSetHandle rc=%d vrHandle=%d",vrrc,vrHandle );

		actionSetDemo = VRActiveActionSet.create( 1 );
		actionSetDemo.ulActionSet( vrHandle );
//		long memAddress2 = MemoryUtil.memAddress( actionSetDemo );
//		HelloOpenVR.log( "VRActiveActionSet memAddress2=%d",memAddress2 );
	}

	private void initVRMatrices( MemoryStack stack )
	{
		HmdMatrix44 result = HmdMatrix44.mallocStack( stack );
		this.projectionMatrixLeft = specM44ToM4f( result,VR.EVREye_Eye_Left );
		this.projectionMatrixRight = specM44ToM4f( result,VR.EVREye_Eye_Right );

		HelloOpenVR.log( "projectionMatrixLeftX(%s)",this.projectionMatrixLeft.toString() );
		HelloOpenVR.log( "projectionMatrixRightX(%s)",this.projectionMatrixRight.toString() );
//		Matrix4f tmpMat4f = new Matrix4f();
//		convertSteamVRMatrix4ToMatrix4f( projectionHMDMatrixL,tmpMat4f );
//		HelloOpenVR.log( "projectionMatrixLeft2(%s)",tmpMat4f.toString() );

//		HmdMatrix44 resultRight = HmdMatrix44.mallocStack( stack );
//		HmdMatrix44 projectionHMDMatrixR = VRSystem.VRSystem_GetProjectionMatrix( VR.EVREye_Eye_Right,Renderer.Z_NEAR,Renderer.Z_FAR,result );
//		this.projectionMatrixRight = new Matrix4f( projectionHMDMatrixR.m() );
//		HelloOpenVR.log( "projectionMatrixRight(%s)",this.projectionMatrixRight.toString() );
	}

	private Matrix4f specM44ToM4f( HmdMatrix44 result,int indexEye )
	{
		HmdMatrix44 projectionHMDMatrixL = VRSystem.VRSystem_GetProjectionMatrix( indexEye,Renderer.Z_NEAR,Renderer.Z_FAR,result );
		projectionHMDMatrixL.m().get( arrayToConvHmdMatrices );
		return new Matrix4f( 
				arrayToConvHmdMatrices[0],arrayToConvHmdMatrices[1],arrayToConvHmdMatrices[2],arrayToConvHmdMatrices[3],
				arrayToConvHmdMatrices[4],arrayToConvHmdMatrices[5],arrayToConvHmdMatrices[6],arrayToConvHmdMatrices[7],
				arrayToConvHmdMatrices[8],arrayToConvHmdMatrices[9],arrayToConvHmdMatrices[10],arrayToConvHmdMatrices[11],
				arrayToConvHmdMatrices[12],arrayToConvHmdMatrices[13],arrayToConvHmdMatrices[14],arrayToConvHmdMatrices[15] );
	}

	public static void convertSteamVRMatrix4ToMatrix4f( float[] array,Matrix4f mat )
	{
		mat.set( array[0],array[1],array[2],array[3],
				array[4],array[5],array[6],array[7],
				array[8],array[9],array[10],array[11],
				array[12],array[13],array[14],array[15] );
	}

	// ez az fgv ugyanazt adja, mint a FloatBuffer-es 
	public static Matrix4f convertSteamVRMatrix4ToMatrix4f( HmdMatrix44 hmdMatrix,Matrix4f mat )
	{
		mat.set( hmdMatrix.m( 0 ),hmdMatrix.m( 1 ),hmdMatrix.m( 2 ),hmdMatrix.m( 3 ),hmdMatrix.m( 4 ),hmdMatrix.m( 5 ),hmdMatrix.m( 6 ),hmdMatrix.m( 7 ),hmdMatrix.m( 8 ),hmdMatrix.m( 9 ),hmdMatrix.m( 10 ),hmdMatrix.m( 11 ),hmdMatrix.m( 12 ),hmdMatrix.m( 13 ),hmdMatrix.m( 14 ),hmdMatrix.m( 15 ) );
		return mat;
	}

	public void handleInputs() throws InterruptedException
	{
		vrrc = VRInput.VRInput_UpdateActionState( actionSetDemo,VRActiveActionSet.SIZEOF );
			HelloOpenVR.log( "VRInput_UpdateActionState rc=%d",vrrc );

		vrrc = VRInput.VRInput_GetDigitalActionData( pHandleAction1.get( 0 ),pAction1Data,VR.k_ulInvalidInputValueHandle );
			HelloOpenVR.log( "pAction1Data rc=%d active(%b) state(%b)",vrrc,pAction1Data.bActive(),pAction1Data.bState() );

		VRCompositor.VRCompositor_WaitGetPoses( pRenderPoseArray,null );
		for ( int ic=0; ic<VR.k_unMaxTrackedDeviceCount; ic++ )
		{
			TrackedDevicePose pose = pRenderPoseArray.get( ic );
			if ( pose.bPoseIsValid()==true )
			{
//				int eTrackingResult = pose.eTrackingResult();	// mindig 200
				HmdMatrix34 hmdMatrix34 = pose.mDeviceToAbsoluteTracking();
				hmdMatrix34.m().get( arrayToConvHmd34Matrices );
				Matrix4f tmpMat = new Matrix4f( 
						arrayToConvHmd34Matrices[0],arrayToConvHmd34Matrices[1],arrayToConvHmd34Matrices[2],0.0f,
						arrayToConvHmd34Matrices[3],arrayToConvHmd34Matrices[4],arrayToConvHmd34Matrices[5],0.0f,
						arrayToConvHmd34Matrices[6],arrayToConvHmd34Matrices[7],arrayToConvHmd34Matrices[8],0.0f,
						arrayToConvHmd34Matrices[9],arrayToConvHmd34Matrices[10],arrayToConvHmd34Matrices[11],1.0f );
				tmpMat.invert();

//				pose.mDeviceToAbsoluteTracking( poseHmdMatrix34 );
//				FloatBuffer floatBuffer = poseHmdMatrix34.m();
//				poseHmdMatrix34FloatBuffer.reset();
//				poseHmdMatrix34.m( poseHmdMatrix34FloatBuffer );
//				float[] array = poseHmdMatrix34FloatBuffer.array();

//				float[] array = new float[12];
//				floatBuffer.get( array );
				HelloOpenVR.log( "pose (%d) (%s)",ic,Arrays.toString( arrayToConvHmd34Matrices ) );
			}
		}
		Thread.sleep( 1000 );
	}
}
